package com.frontpet.orders.dto;

import com.frontpet.orders.domain.FormaPagamento;
import com.frontpet.orders.domain.ModalidadeEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body de {@code POST /api/v1/orders} — checkout público y anónimo.
 *
 * <p>Campos alineados con lo confirmado por el cliente
 * (docs/preguntas-cliente.md §4.2) y con {@code V4__orders.sql}, no con los
 * 3 campos que dibuja la pantalla de Stitch "Sua Sacola" (drift #3 del plan
 * de Sprint 4: Stitch solo pide nome/telefone/modalidade — acá se agregan
 * endereço, forma de pagamento y horário porque la migración los exige
 * {@code NOT NULL} y el cliente los pidió explícitamente).
 *
 * <p>{@code enderecoEntrega} no lleva {@code @NotBlank}: es obligatorio solo
 * cuando {@code modalidade == ENTREGA} (regla cruzada, validada en
 * {@code OrderServiceImpl} igual que la invariante precio/variantes de
 * {@code catalog}). Con {@code RETIRADA} el service lo completa solo.
 *
 * <p>{@code consentimentoLgpd}: gate de envío de la tarea 4.16, no una
 * columna de {@code orders} — la migración no la tiene y no amerita una
 * nueva. Igual se valida en el backend, no solo deshabilitando el botón en
 * el cliente.
 *
 * <p>{@code honeypot}: campo señuelo de la tarea 4.15. Un formulario real
 * nunca lo completa; si llega no vacío, {@code OrderServiceImpl} rechaza el
 * pedido sin decírselo al bot que lo mandó.
 */
public record CreateOrderRequest(
        @NotBlank @Size(max = 160) String clienteNome,
        @NotBlank @Size(max = 30) String clienteTelefone,
        @NotNull ModalidadeEntrega modalidade,
        String enderecoEntrega,
        @NotNull FormaPagamento formaPagamento,
        @Size(max = 120) String horarioEntrega,
        @AssertTrue(message = "É necessário aceitar a política de privacidade.") boolean consentimentoLgpd,
        String honeypot,
        @NotEmpty @Valid List<CreateOrderItemRequest> items
) {
}
