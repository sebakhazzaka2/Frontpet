package com.frontpet.common;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Convierte un nombre en un slug apto para URL.
 *
 * <p>{@code "Ração Golden 15kg"} → {@code "racao-golden-15kg"}
 *
 * <p>El paso no obvio es {@link Normalizer}: en forma NFD, Java parte cada
 * letra acentuada en dos caracteres (la letra pelada + el acento como marca
 * aparte). Una vez separados, borrar las marcas deja {@code ç → c} y
 * {@code ã → a} sin necesidad de una tabla de reemplazos a mano.
 */
public final class Slugify {

    private Slugify() {
        // clase de utilidad, no se instancia
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")          // borra los acentos que NFD separó
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")     // lo que no sea letra/número → guión
                .replaceAll("^-+|-+$", "");        // sin guiones colgando en las puntas
    }
}
