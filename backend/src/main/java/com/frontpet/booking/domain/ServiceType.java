package com.frontpet.booking.domain;

/**
 * Discrimina un {@link ServiceOffering} entre banho base y adicional
 * (ADR 011). Un turno referencia exactamente 1 {@code BASE} y N {@code ADDON}.
 */
public enum ServiceType {
    BASE,
    ADDON
}
