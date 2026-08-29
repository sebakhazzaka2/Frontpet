package com.frontpet.orders;

import com.frontpet.orders.domain.OrderStatus;
import com.frontpet.orders.dto.CreateOrderRequest;
import com.frontpet.orders.dto.OrderDetail;
import com.frontpet.orders.dto.OrderStatusCounts;
import com.frontpet.orders.dto.OrderSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Puerta de entrada al módulo orders (CLAUDE.md §5: el repositorio vive en
 * {@code domain}, esto en la raíz). Todo método recibe {@code tenantId}
 * explícito, igual que {@code catalog.ProductService}.
 */
public interface OrderService {

    /**
     * Checkout público y anónimo (tarea 4.8). Los precios se recalculan
     * siempre contra {@code ProductService.resolveOrderLine} — el request no
     * manda ni nombre ni precio, y si los mandara se ignorarían.
     *
     * @throws com.frontpet.catalog.ProductNotFoundException se algum item
     *                                  referencia um produto inexistente/inativo
     * @throws IllegalArgumentException honeypot preenchido, endereço faltando
     *                                  em modalidade ENTREGA, ou variante inválida
     */
    OrderDetail create(UUID tenantId, CreateOrderRequest request);

    /** @throws OrderNotFoundException se não existe ou pertence a outro tenant */
    OrderDetail getByPublicId(UUID tenantId, UUID publicId);

    /** Listado paginado del admin. {@code status} null = todos los estados. */
    Page<OrderSummary> list(UUID tenantId, OrderStatus status, Pageable pageable);

    /** Contadores para las tabs Todos/Pendentes/Confirmados/Cancelados. */
    OrderStatusCounts countByStatus(UUID tenantId);

    /**
     * Cambio de estado (tarea 4.14). Idempotente: la misma transición
     * repetida no vuelve a sellar el timestamp. Transiciones válidas:
     * {@code PENDING→CONFIRMED}, {@code PENDING→CANCELLED},
     * {@code CONFIRMED→CANCELLED} (CLAUDE.md §6: no hay más estados que
     * PENDING/CONFIRMED/CANCELLED, y no se retrocede a PENDING).
     *
     * @throws OrderNotFoundException  se não existe ou pertence a outro tenant
     * @throws IllegalArgumentException se a transição não é permitida
     */
    OrderDetail updateStatus(UUID tenantId, UUID publicId, OrderStatus newStatus);

    /**
     * Direito de eliminação LGPD (ADR 023) — {@code com.frontpet.privacy}
     * consome isto, nunca {@code OrderRepository} direto (CLAUDE.md §5).
     *
     * @param clienteTelefoneNorm já normalizado (ver {@code PhoneNormalizer})
     */
    List<UUID> findPublicIdsByPhone(UUID tenantId, String clienteTelefoneNorm);

    /**
     * Anonimiza (não apaga) todo pedido do tenant com este telefone: dados
     * pessoais viram marcadores, snapshot de itens/preço fica intacto (ADR
     * 023). Idempotente — rodar de novo com o mesmo telefone não encontra
     * mais nada, porque {@code cliente_telefone_norm} já é {@code NULL}.
     *
     * @return quantos pedidos foram anonimizados
     */
    int anonymizeByPhone(UUID tenantId, String clienteTelefoneNorm);
}
