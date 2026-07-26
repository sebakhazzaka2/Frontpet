-- =====================================================================
-- V9 — marcas: unicidad case-insensitive
-- =====================================================================
-- Decisión (docs/pending-decisions.md §1): el alta de marca es inline en el
-- formulario de producto — el admin escribe el nombre y BrandService la crea
-- si no existe (findOrCreate). Sin pantalla propia de ABM.
--
-- Un findOrCreate por texto libre es la puerta de entrada de "Golden" /
-- "golden" / "GOLDEN" conviviendo como tres marcas distintas. La disciplina de
-- la UI (autocompletar) ayuda, pero no lo previene: dos altas concurrentes con
-- distinta capitalización pasan cualquier chequeo hecho en la capa Java.
--
-- El UNIQUE(tenant_id, nome) de V2 es case-SENSITIVE y no cierra ese agujero.
-- Se reemplaza por un índice único funcional sobre LOWER(nome): la garantía
-- vive en Postgres, no en disciplina de aplicación.
-- =====================================================================

ALTER TABLE brands DROP CONSTRAINT uq_brands_tenant_nome;

CREATE UNIQUE INDEX uq_brands_tenant_nome_lower ON brands (tenant_id, LOWER(nome));

COMMENT ON INDEX uq_brands_tenant_nome_lower IS
    'Unicidad case-insensitive: "Golden" y "golden" son la misma marca. Ver docs/pending-decisions.md §1.';
