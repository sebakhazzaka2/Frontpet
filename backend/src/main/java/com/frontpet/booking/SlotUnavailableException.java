package com.frontpet.booking;

/**
 * O horário pedido já não tem cupo disponível — recontado dentro do
 * {@code pg_advisory_xact_lock} de {@code AppointmentServiceImpl.create}
 * (ADR 020). Distinto de {@link IllegalArgumentException} (400): o slot
 * existe e é válido, só que a capacidade já foi consumida por outro turno
 * concorrente.
 */
public class SlotUnavailableException extends RuntimeException {

    public SlotUnavailableException(String message) {
        super(message);
    }
}
