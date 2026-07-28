package com.frontpet.orders.domain;

/**
 * Modalidad elegida por el cliente en el checkout (pantalla Stitch "Sua
 * Sacola"). No es lo mismo que {@link FreteMode} — esta es la entrada del
 * usuario; {@code FreteMode} es el dato que persiste {@code orders} y sale
 * de acá más una regla de negocio, no de un cálculo de distancia (ADR 003,
 * act. 2026-07-09: "el sistema no calcula distancia en MVP1").
 *
 * <p>Mapeo aplicado en {@code OrderServiceImpl} al crear el pedido:
 * <ul>
 *   <li>{@code RETIRADA} → {@code enderecoEntrega = "Retirada na loja"},
 *       {@code freteMode = GRATIS}.
 *   <li>{@code ENTREGA} → {@code enderecoEntrega} = el que mandó el cliente
 *       (obligatorio), {@code freteMode = GRATIS} por defecto — FrontPet
 *       ajusta a {@code A_COMBINAR} manualmente por WhatsApp si la dirección
 *       real supera los 5km (no hay UI de admin para esto en Sprint 4;
 *       documentado como deuda conocida, ver docs/pending-decisions.md).
 * </ul>
 */
public enum ModalidadeEntrega {
    ENTREGA,
    RETIRADA
}
