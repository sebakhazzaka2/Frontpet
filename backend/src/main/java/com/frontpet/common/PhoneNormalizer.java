package com.frontpet.common;

/**
 * Normaliza teléfonos brasileños a dígitos puros con DDI, para
 * {@code orders.cliente_telefone_norm} (columna {@code VARCHAR(20)}).
 *
 * <p>{@code "(51) 99999-8888"} → {@code "5551999998888"}. El formato de
 * entrada no importa (paréntesis, guiones, espacios): se descarta todo lo
 * que no sea dígito y se prefija {@code 55} si todavía no lo tiene.
 *
 * <p>Vive en {@code common/} porque el booking del Sprint 6 va a necesitar
 * exactamente lo mismo para el teléfono del cliente que reserva un turno.
 */
public final class PhoneNormalizer {

    private static final String COUNTRY_CODE = "55";
    private static final int MAX_LENGTH = 20;

    private PhoneNormalizer() {
        // clase de utilidad, no se instancia
    }

    /**
     * @throws IllegalArgumentException si, tras limpiar el formato, no quedan
     *                                  dígitos suficientes para ser un teléfono
     *                                  brasileiro válido (DDD + número)
     */
    public static String normalizeBr(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Telefone inválido.");
        }
        String digits = input.replaceAll("[^0-9]", "");

        // Já vem com DDI: 12-13 dígitos (55 + DDD + 8/9 dígitos do número).
        String normalized = (digits.startsWith(COUNTRY_CODE) && digits.length() >= 12)
                ? digits
                : COUNTRY_CODE + digits;

        // Sem DDI: 10-11 dígitos (DDD + 8/9 dígitos). Com o prefixo 55 já
        // aplicado acima, o total esperado é 12-13.
        if (normalized.length() < 12 || normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Telefone inválido.");
        }
        return normalized;
    }
}
