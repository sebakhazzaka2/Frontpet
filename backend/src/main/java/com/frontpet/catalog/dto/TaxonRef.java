package com.frontpet.catalog.dto;

/**
 * Referencia mínima a una categoría o una especie: lo justo para mostrar el
 * nombre y armar el link del filtro ({@code /produtos?categoria=racoes}).
 *
 * <p>Un solo record para las dos porque tienen exactamente la misma forma y
 * el mismo uso. Si alguna vez divergen, se parte en dos — todavía no.
 */
public record TaxonRef(String nome, String slug) {
}
