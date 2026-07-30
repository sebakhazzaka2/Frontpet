package com.frontpet.booking;

/**
 * El turno pedido não existe, não pertence ao tenant, ou pertence a outro
 * (mesmo critério que {@code orders.OrderNotFoundException}: de fora os três
 * casos são o mesmo 404).
 */
public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(String message) {
        super(message);
    }
}
