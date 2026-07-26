-- =====================================================================
-- V7 — slug de producto (URL pública legible)
-- =====================================================================
-- El detalle de producto se resuelve por slug, no por public_id:
--   /produtos/racao-golden-15kg   en vez de   /produtos/01924ccf-0000-...
-- El objetivo declarado del Sprint 3 es un catálogo indexable por Google, y
-- una URL con el nombre del producto pesa para SEO. El public_id sigue
-- existiendo como identificador estable e interno.
--
-- El slug lo genera la app desde `nome` (ver common/Slugify) y el admin puede
-- corregirlo. Por eso NO se deriva en la DB: es editable.
--
-- 180 > 160 (largo de `nome`) para dejar lugar al sufijo -2, -3 que se agrega
-- cuando dos productos slugifican igual.
--
-- La tabla está vacía en este punto, así que el NOT NULL entra sin default.
-- =====================================================================

ALTER TABLE products
    ADD COLUMN slug VARCHAR(180) NOT NULL;

-- Único por tenant, igual que categories y species. El índice que crea este
-- UNIQUE es además el que sirve el lookup del detalle (tenant_id + slug),
-- así que no hace falta un índice extra.
ALTER TABLE products
    ADD CONSTRAINT uq_products_tenant_slug UNIQUE (tenant_id, slug);

COMMENT ON COLUMN products.slug IS
    'Identificador legible para la URL pública. Generado desde nome, editable por el admin. Único por tenant.';
