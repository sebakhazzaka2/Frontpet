package com.frontpet.orders;

import com.frontpet.catalog.ProductService;
import com.frontpet.catalog.dto.OrderLineSnapshot;
import com.frontpet.common.PhoneNormalizer;
import com.frontpet.common.UuidV7;
import com.frontpet.orders.domain.FreteMode;
import com.frontpet.orders.domain.ModalidadeEntrega;
import com.frontpet.orders.domain.Order;
import com.frontpet.orders.domain.OrderItem;
import com.frontpet.orders.domain.OrderRepository;
import com.frontpet.orders.domain.OrderStatus;
import com.frontpet.orders.dto.CreateOrderItemRequest;
import com.frontpet.orders.dto.CreateOrderRequest;
import com.frontpet.orders.dto.OrderDetail;
import com.frontpet.orders.dto.OrderItemDetail;
import com.frontpet.orders.dto.OrderStatusCounts;
import com.frontpet.orders.dto.OrderSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** {@code RETIRADA} sempre cai aqui — não há coluna própria para essa modalidade (drift #1). */
    private static final String RETIRADA_ENDERECO = "Retirada na loja";

    /** Marcadores da anonimização LGPD (ADR 023) — nunca NULL em colunas NOT NULL. */
    private static final String ANONYMIZED_NOME = "Titular removido";
    private static final String ANONYMIZED_MARKER = "—";

    /**
     * Transições de status permitidas (CLAUDE.md §6: só PENDING/CONFIRMED/
     * CANCELLED, nunca se volta a PENDING). Mesmo status→mesmo status é
     * tratado à parte, como no-op idempotente.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.CANCELLED),
            OrderStatus.CANCELLED, Set.of()));

    private final OrderRepository orderRepository;
    private final ProductService productService;

    @Override
    @Transactional
    public OrderDetail create(UUID tenantId, CreateOrderRequest request) {
        // Honeypot: mensagem genérica a propósito — não delata ao bot qual
        // campo o denunciou.
        if (StringUtils.hasText(request.honeypot())) {
            throw new IllegalArgumentException("Requisição inválida.");
        }

        Order order = new Order();
        order.setPublicId(UuidV7.generate());
        order.setTenantId(tenantId);
        order.setStatus(OrderStatus.PENDING);
        order.setClienteNome(request.clienteNome());
        order.setClienteTelefone(request.clienteTelefone());
        order.setClienteTelefoneNorm(PhoneNormalizer.normalizeBr(request.clienteTelefone()));
        order.setFormaPagamento(request.formaPagamento());
        order.setHorarioEntrega(request.horarioEntrega());
        applyModalidade(order, request);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : request.items()) {
            OrderLineSnapshot snapshot = productService.resolveOrderLine(
                    tenantId, itemRequest.productPublicId(), itemRequest.variantId());

            OrderItem item = new OrderItem();
            item.setProductId(snapshot.productId());
            item.setProductVariantId(snapshot.variantId());
            item.setNomeSnapshot(snapshot.nomeSnapshot());
            item.setUnitPriceSnapshot(snapshot.unitPrice());
            item.setQuantidade(itemRequest.quantidade());
            order.addItem(item);

            subtotal = subtotal.add(snapshot.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantidade())));
        }
        order.setSubtotalSnapshot(subtotal.setScale(2, RoundingMode.HALF_UP));

        Order saved = orderRepository.save(order);
        return toDetail(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetail getByPublicId(UUID tenantId, UUID publicId) {
        Order order = orderRepository.findByTenantIdAndPublicId(tenantId, publicId)
                .orElseThrow(() -> new OrderNotFoundException("Pedido não encontrado: " + publicId));
        return toDetail(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummary> list(UUID tenantId, OrderStatus status, Pageable pageable) {
        return orderRepository.findSummaries(tenantId, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatusCounts countByStatus(UUID tenantId) {
        long pendentes = 0;
        long confirmados = 0;
        long cancelados = 0;
        for (Object[] row : orderRepository.countGroupedByStatus(tenantId)) {
            OrderStatus status = (OrderStatus) row[0];
            long count = (Long) row[1];
            switch (status) {
                case PENDING -> pendentes = count;
                case CONFIRMED -> confirmados = count;
                case CANCELLED -> cancelados = count;
            }
        }
        return new OrderStatusCounts(pendentes + confirmados + cancelados, pendentes, confirmados, cancelados);
    }

    @Override
    @Transactional
    public OrderDetail updateStatus(UUID tenantId, UUID publicId, OrderStatus newStatus) {
        Order order = orderRepository.findByTenantIdAndPublicId(tenantId, publicId)
                .orElseThrow(() -> new OrderNotFoundException("Pedido não encontrado: " + publicId));

        OrderStatus current = order.getStatus();
        if (current == newStatus) {
            // No-op idempotente: repetir la misma transición no vuelve a sellar el timestamp.
            return toDetail(order);
        }
        if (!ALLOWED_TRANSITIONS.get(current).contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Não é possível mudar o pedido de " + current + " para " + newStatus + ".");
        }

        order.setStatus(newStatus);
        Instant now = Instant.now();
        if (newStatus == OrderStatus.CONFIRMED) {
            order.setConfirmedAt(now);
        } else if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(now);
        }
        // Sin save() explícito: managed dentro de la transacción.
        return toDetail(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findPublicIdsByPhone(UUID tenantId, String clienteTelefoneNorm) {
        return orderRepository.findByTenantIdAndClienteTelefoneNorm(tenantId, clienteTelefoneNorm).stream()
                .map(Order::getPublicId)
                .toList();
    }

    @Override
    @Transactional
    public int anonymizeByPhone(UUID tenantId, String clienteTelefoneNorm) {
        List<Order> orders = orderRepository.findByTenantIdAndClienteTelefoneNorm(tenantId, clienteTelefoneNorm);
        Instant now = Instant.now();
        for (Order order : orders) {
            order.setClienteNome(ANONYMIZED_NOME);
            order.setClienteTelefone(ANONYMIZED_MARKER);
            order.setClienteTelefoneNorm(null);
            order.setEnderecoEntrega(ANONYMIZED_MARKER);
            order.setHorarioEntrega(null);
            order.setAnonymizedAt(now);
            // Sin save() explícito: managed dentro de la transacción, igual
            // que updateStatus. subtotalSnapshot e items ficam intactos —
            // são dado contábil, não pessoal (ADR 023).
        }
        return orders.size();
    }

    // ---- helpers de negocio --------------------------------------------

    /**
     * Mapeo de {@link ModalidadeEntrega} a ({@code enderecoEntrega},
     * {@code freteMode}) — regla cruzada que {@code CreateOrderRequest}
     * delega acá a propósito (su javadoc explica por qué el campo no lleva
     * {@code @NotBlank}).
     */
    private void applyModalidade(Order order, CreateOrderRequest request) {
        if (request.modalidade() == ModalidadeEntrega.RETIRADA) {
            order.setEnderecoEntrega(RETIRADA_ENDERECO);
            order.setFreteMode(FreteMode.GRATIS);
            return;
        }
        if (!StringUtils.hasText(request.enderecoEntrega())) {
            throw new IllegalArgumentException("Endereço de entrega é obrigatório para entrega.");
        }
        order.setEnderecoEntrega(request.enderecoEntrega());
        // GRATIS por defecto; FrontPet lo pasa a A_COMBINAR a mano por
        // WhatsApp si la dirección real supera los 5km (deuda documentada en
        // FreteMode, sin UI de admin en Sprint 4).
        order.setFreteMode(FreteMode.GRATIS);
    }

    // ---- mapeo entidad → DTO -------------------------------------------

    private OrderDetail toDetail(Order order) {
        // N+1 acotado al tamaño del carrito (2-5 productos), no a una lista
        // paginada: mismo criterio que @EntityGraph en catalog, aceptable
        // porque es una sola fila. order_items no guarda publicId (referencia
        // el producto por id interno, igual que la FK real de la DB).
        List<OrderItemDetail> items = order.getItems().stream()
                .map(i -> new OrderItemDetail(
                        productService.getPublicIdById(order.getTenantId(), i.getProductId()),
                        i.getProductVariantId(),
                        i.getNomeSnapshot(),
                        i.getUnitPriceSnapshot(),
                        i.getQuantidade()))
                .toList();

        return new OrderDetail(
                order.getPublicId(),
                order.getStatus(),
                order.getClienteNome(),
                order.getClienteTelefone(),
                order.getFormaPagamento(),
                order.getFreteMode(),
                RETIRADA_ENDERECO.equals(order.getEnderecoEntrega())
                        ? ModalidadeEntrega.RETIRADA
                        : ModalidadeEntrega.ENTREGA,
                order.getEnderecoEntrega(),
                order.getHorarioEntrega(),
                order.getSubtotalSnapshot(),
                items,
                order.getCreatedAt(),
                order.getConfirmedAt(),
                order.getCancelledAt());
    }
}
