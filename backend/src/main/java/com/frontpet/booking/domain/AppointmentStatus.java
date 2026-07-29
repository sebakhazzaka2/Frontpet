package com.frontpet.booking.domain;

/**
 * Estados do turno. Sem estados além destes três (CLAUDE.md §6).
 * PENDING e CONFIRMED ocupam cupo; CANCELLED libera (ADR 013 §9).
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
