package com.frontpet.booking.domain;

/**
 * Porte do animal — dimensão que resolve preço e duração em
 * {@link ServicePricing} (ADR 011). {@code P} inclui filhotes.
 */
public enum Porte {
    P,
    M,
    G,
    GG
}
