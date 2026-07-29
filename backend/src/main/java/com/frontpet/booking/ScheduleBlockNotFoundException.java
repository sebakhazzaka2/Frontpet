package com.frontpet.booking;

/** O bloqueio pedido não existe ou não pertence ao tenant (mesmo 404 para os dois casos). */
public class ScheduleBlockNotFoundException extends RuntimeException {

    public ScheduleBlockNotFoundException(String message) {
        super(message);
    }
}
