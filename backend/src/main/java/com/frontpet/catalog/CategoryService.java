package com.frontpet.catalog;

import com.frontpet.catalog.dto.TaxonRef;

import java.util.List;
import java.util.UUID;

/**
 * Categorías y especies del catálogo.
 *
 * <p>Solo lectura a propósito: son listas cerradas, sembradas en
 * {@code V8__catalog_reference_data.sql}, sin CRUD de admin (CLAUDE.md §6).
 */
public interface CategoryService {

    /** Las 7 categorías, para los chips del filtro. */
    List<TaxonRef> listCategories(UUID tenantId);

    /** Las 2 especies (Cães, Gatos). */
    List<TaxonRef> listSpecies(UUID tenantId);
}
