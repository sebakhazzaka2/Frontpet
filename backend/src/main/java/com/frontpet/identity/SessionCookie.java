package com.frontpet.identity;

/**
 * Nombre de la cookie de sesión (ADR 004: JWT en cookie HttpOnly).
 *
 * <p>Compartido entre {@code AuthController} (la setea) y {@code JwtAuthFilter}
 * (la lee) para que no se puedan desincronizar.
 */
public final class SessionCookie {

    public static final String NAME = "frontpet_session";

    private SessionCookie() {
    }
}
