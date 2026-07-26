package com.frontpet.common;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generador de UUID v7 (RFC 9562).
 *
 * <p>¿Por qué a mano? Java trae {@link UUID#randomUUID()}, pero eso es v4:
 * 122 bits de puro azar. Hibernate 6.6 tampoco sabe generar v7 (verificado
 * contra el jar: solo trae v4 y un formato viejo propio). Sumar una librería
 * entera para las ~20 líneas de acá no pasa el filtro del CLAUDE.md §6.
 *
 * <p>La diferencia con v4: el v7 arranca con el timestamp en milisegundos, así
 * que los IDs generados salen <b>ordenados en el tiempo</b>. Eso le da localidad
 * al índice — las inserciones caen juntas al final del B-tree en vez de
 * salpicarse por todos lados como pasa con los v4. Ver ADR 013 §1.
 *
 * <p>Layout de los 128 bits, según la RFC:
 * <pre>
 *   48 bits · timestamp unix en ms
 *    4 bits · versión (0111 = 7)
 *   12 bits · azar
 *    2 bits · variante (10)
 *   62 bits · azar
 * </pre>
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
        // clase de utilidad, no se instancia
    }

    public static UUID generate() {
        long timestampMs = System.currentTimeMillis();

        // ---- 64 bits altos: timestamp + versión + azar --------------------
        // El timestamp ocupa los 48 bits de más arriba; el << 16 le deja
        // libres los 16 de abajo para la versión y el relleno aleatorio.
        long msb = (timestampMs & 0xFFFF_FFFF_FFFFL) << 16;
        msb |= RANDOM.nextInt(0x1000);   // 12 bits de azar (bits 0-11)
        msb &= ~(0xFL << 12);            // vacía los bits 12-15...
        msb |= (0x7L << 12);             // ...y escribe ahí la versión 7

        // ---- 64 bits bajos: variante + azar -------------------------------
        long lsb = RANDOM.nextLong();
        lsb &= ~(0x3L << 62);            // vacía los 2 bits de más arriba...
        lsb |= (0x2L << 62);             // ...y escribe la variante RFC (binario 10)

        return new UUID(msb, lsb);
    }
}
