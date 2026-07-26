package com.frontpet.catalog;

import com.frontpet.catalog.domain.Brand;

import java.util.UUID;

/**
 * Alta de marcas — sin ABM propio (docs/pending-decisions.md §1).
 *
 * <p>MVP1 no tiene pantalla de marcas: el admin las escribe en el formulario
 * de producto (autocompletar sobre las existentes, texto libre si es nueva) y
 * el backend las crea al vuelo. Este service es esa puerta de entrada.
 */
public interface BrandService {

    /**
     * Devuelve la marca existente que matchea {@code nome} sin distinguir
     * mayúsculas, o la crea si no existe.
     *
     * <p>"Golden" y "golden" son la misma marca — lo garantiza el índice
     * único funcional {@code uq_brands_tenant_nome_lower} (V9), no la
     * disciplina de la UI. Es la única manera real de evitarlo: dos altas
     * concurrentes con distinta capitalización pasan cualquier chequeo hecho
     * solo en Java.
     */
    Brand findOrCreate(UUID tenantId, String nome);
}
