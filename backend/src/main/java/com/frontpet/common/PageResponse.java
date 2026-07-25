package com.frontpet.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envoltorio de paginación para las respuestas JSON.
 *
 * <p>Existe en vez de serializar el {@code Page} de Spring directo: la forma
 * JSON de {@code PageImpl} no es un contrato estable (Spring avisa de eso desde
 * la 3.3) y el frontend tiene que tipar contra algo que no se mueva.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }

    /** Variante para cuando hay que mapear el contenido de paso. */
    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        return of(page.map(mapper));
    }
}
