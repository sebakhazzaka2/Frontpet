package com.frontpet.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Producto tal como se ve en la tabla del admin (issue #33, Bloque E) — no
 * existía ningún endpoint de listado para el admin hasta acá, solo
 * {@link ProductSummary} para la grilla pública (que fuerza {@code active =
 * true} y no expone {@code active}/{@code stock}). Sin esos dos campos el
 * admin no puede distinguir productos ocultos ni mostrar el badge de estoque.
 *
 * @param stock       cantidad en el producto simple; {@code 0}/irrelevante si
 *                    {@code hasVariants} — el frontend muestra "Com variantes"
 *                    en vez de un número en ese caso
 * @param brandNome   se usa como eyebrow de la card en vez de categoria/espécie
 *                    (que el mock de Stitch sí muestra): agregarlas exigiría
 *                    una segunda query de agregación por fila; brandNome ya
 *                    sale gratis del mismo JOIN que arma el resto del summary
 */
public record AdminProductSummary(
        UUID publicId,
        String slug,
        String nome,
        String mainImageUrl,
        BigDecimal price,
        BigDecimal priceOriginal,
        String brandNome,
        boolean hasVariants,
        boolean active,
        Integer stock
) {
}
