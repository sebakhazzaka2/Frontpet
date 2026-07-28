package com.frontpet.orders;

import com.frontpet.AbstractIntegrationTest;
import com.frontpet.catalog.ProductNotFoundException;
import com.frontpet.catalog.domain.Product;
import com.frontpet.catalog.domain.ProductRepository;
import com.frontpet.catalog.domain.ProductVariant;
import com.frontpet.common.UuidV7;
import com.frontpet.orders.domain.FormaPagamento;
import com.frontpet.orders.domain.FreteMode;
import com.frontpet.orders.domain.ModalidadeEntrega;
import com.frontpet.orders.domain.OrderStatus;
import com.frontpet.orders.dto.CreateOrderItemRequest;
import com.frontpet.orders.dto.CreateOrderRequest;
import com.frontpet.orders.dto.OrderDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Happy path del checkout contra Postgres real, mismo patrón que
 * {@code ProductServiceIntegrationTest}: el objetivo es ejecutar las queries,
 * no solo levantar el contexto.
 */
@SpringBootTest
@Transactional
class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    private static final UUID TENANT = UUID.fromString("01924ccf-0000-7000-8000-000000000001");

    @Autowired OrderService orderService;
    @Autowired ProductRepository productRepository;

    @Test
    @DisplayName("crea un pedido con 2 items, uno con variante, y congela los snapshots del catálogo")
    void createsOrderWithSnapshots() {
        Product simple = persistProduct("Brinquedo Mordedor", "brinquedo-mordedor-order", new BigDecimal("29.90"));
        Product withVariant = persistProductWithVariant("Ração com Variantes Order", "racao-variantes-order",
                "15kg", new BigDecimal("189.90"));
        Long variantId = withVariant.getVariants().getFirst().getId();

        CreateOrderRequest request = new CreateOrderRequest(
                "Maria Souza", "(51) 99999-8888", ModalidadeEntrega.ENTREGA,
                "Rua das Flores, 123", FormaPagamento.PIX, "à tarde",
                true, null,
                List.of(
                        new CreateOrderItemRequest(simple.getPublicId(), null, 2),
                        new CreateOrderItemRequest(withVariant.getPublicId(), variantId, 1)));

        OrderDetail detail = orderService.create(TENANT, request);

        assertThat(detail.publicId()).isNotNull();
        assertThat(detail.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(detail.enderecoEntrega()).isEqualTo("Rua das Flores, 123");
        assertThat(detail.freteMode()).isEqualTo(FreteMode.GRATIS);
        assertThat(detail.items()).hasSize(2);
        assertThat(detail.subtotal()).isEqualByComparingTo(
                new BigDecimal("29.90").multiply(BigDecimal.valueOf(2)).add(new BigDecimal("189.90")));
        assertThat(detail.whatsappMessage()).isNotBlank();
    }

    @Test
    @DisplayName("RETIRADA persiste 'Retirada na loja' y frete GRATIS, sin exigir endereço")
    void retiradaDoesNotRequireAddress() {
        Product product = persistProduct("Produto Retirada", "produto-retirada-order", new BigDecimal("15.00"));

        OrderDetail detail = orderService.create(TENANT, new CreateOrderRequest(
                "João", "51999998888", ModalidadeEntrega.RETIRADA,
                null, FormaPagamento.DINHEIRO, null, true, null,
                List.of(new CreateOrderItemRequest(product.getPublicId(), null, 1))));

        assertThat(detail.enderecoEntrega()).isEqualTo("Retirada na loja");
        assertThat(detail.freteMode()).isEqualTo(FreteMode.GRATIS);
    }

    @Test
    @DisplayName("ENTREGA sin endereço rechaza con 400")
    void entregaRequiresAddress() {
        Product product = persistProduct("Produto Entrega Sem Endereco", "produto-entrega-sem-endereco", new BigDecimal("15.00"));

        CreateOrderRequest request = new CreateOrderRequest(
                "João", "51999998888", ModalidadeEntrega.ENTREGA,
                null, FormaPagamento.DINHEIRO, null, true, null,
                List.of(new CreateOrderItemRequest(product.getPublicId(), null, 1)));

        assertThatThrownBy(() -> orderService.create(TENANT, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("honeypot preenchido rejeita o pedido")
    void honeypotRejectsOrder() {
        Product product = persistProduct("Produto Honeypot", "produto-honeypot", new BigDecimal("15.00"));

        CreateOrderRequest request = new CreateOrderRequest(
                "Bot", "51999998888", ModalidadeEntrega.RETIRADA,
                null, FormaPagamento.DINHEIRO, null, true, "preenchido-por-um-bot",
                List.of(new CreateOrderItemRequest(product.getPublicId(), null, 1)));

        assertThatThrownBy(() -> orderService.create(TENANT, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("produto inexistente/inativo da ProductNotFoundException")
    void missingProductFails() {
        CreateOrderRequest request = new CreateOrderRequest(
                "João", "51999998888", ModalidadeEntrega.RETIRADA,
                null, FormaPagamento.DINHEIRO, null, true, null,
                List.of(new CreateOrderItemRequest(UuidV7.generate(), null, 1)));

        assertThatThrownBy(() -> orderService.create(TENANT, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("variante que não pertence ao produto da 400")
    void foreignVariantFails() {
        Product a = persistProductWithVariant("Produto Variante A Order", "produto-variante-a-order",
                "Único", new BigDecimal("10.00"));
        Product b = persistProductWithVariant("Produto Variante B Order", "produto-variante-b-order",
                "Único", new BigDecimal("20.00"));
        Long foreignVariantId = b.getVariants().getFirst().getId();

        CreateOrderRequest request = new CreateOrderRequest(
                "João", "51999998888", ModalidadeEntrega.RETIRADA,
                null, FormaPagamento.DINHEIRO, null, true, null,
                List.of(new CreateOrderItemRequest(a.getPublicId(), foreignVariantId, 1)));

        assertThatThrownBy(() -> orderService.create(TENANT, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PENDING→CONFIRMED sella confirmedAt; CANCELLED→CONFIRMED da 400; mismo→mismo no toca timestamps")
    void statusTransitions() {
        Product product = persistProduct("Produto Transições", "produto-transicoes", new BigDecimal("10.00"));
        OrderDetail created = orderService.create(TENANT, new CreateOrderRequest(
                "João", "51999998888", ModalidadeEntrega.RETIRADA,
                null, FormaPagamento.DINHEIRO, null, true, null,
                List.of(new CreateOrderItemRequest(product.getPublicId(), null, 1))));

        OrderDetail confirmed = orderService.updateStatus(TENANT, created.publicId(), OrderStatus.CONFIRMED);
        assertThat(confirmed.confirmedAt()).isNotNull();
        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);

        OrderDetail sameStatus = orderService.updateStatus(TENANT, created.publicId(), OrderStatus.CONFIRMED);
        assertThat(sameStatus.confirmedAt()).isEqualTo(confirmed.confirmedAt());

        OrderDetail cancelled = orderService.updateStatus(TENANT, created.publicId(), OrderStatus.CANCELLED);
        assertThat(cancelled.cancelledAt()).isNotNull();

        assertThatThrownBy(() -> orderService.updateStatus(TENANT, created.publicId(), OrderStatus.CONFIRMED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- helpers -------------------------------------------------------

    private Product persistProduct(String nome, String slug, BigDecimal price) {
        Product p = new Product();
        p.setPublicId(UuidV7.generate());
        p.setTenantId(TENANT);
        p.setNome(nome);
        p.setSlug(slug);
        p.setPrice(price);
        p.setStock(10);
        p.setActive(true);
        return productRepository.saveAndFlush(p);
    }

    private Product persistProductWithVariant(String nome, String slug, String variantNome, BigDecimal price) {
        Product p = new Product();
        p.setPublicId(UuidV7.generate());
        p.setTenantId(TENANT);
        p.setNome(nome);
        p.setSlug(slug);
        p.setActive(true);

        ProductVariant v = new ProductVariant();
        v.setProduct(p);
        v.setNomeVariante(variantNome);
        v.setPrice(price);
        v.setStock(5);
        v.setActive(true);
        p.getVariants().add(v);

        return productRepository.saveAndFlush(p);
    }
}
