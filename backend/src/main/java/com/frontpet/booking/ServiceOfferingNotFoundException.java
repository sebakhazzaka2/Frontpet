package com.frontpet.booking;

/** El serviço pedido não existe ou não pertence ao tenant (mesmo 404 para os dois casos). */
public class ServiceOfferingNotFoundException extends RuntimeException {

    public ServiceOfferingNotFoundException(String message) {
        super(message);
    }
}
