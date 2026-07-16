-- =====================================================================
-- V2 — Módulo CATALOG: categorías, especies, marcas, productos, variantes
-- =====================================================================
-- Decisiones de modelado (ver ADR 013):
--   · Variantes: precio/stock viven en products cuando NO hay variantes; si el
--     producto tiene variantes, la variante manda. order_items apunta a product
--     (siempre) + variant (opcional), con FK compuesta que garantiza que la
--     variante pertenece al producto.
--   · Un producto puede estar en varias categorías y varias especies (N:M).
--   · Una sola foto por producto en MVP1 (main_image_url); galería = Fase 2.
--   · NUMERIC(10,2) para dinero (nunca float). Moneda única (BRL), sin columna
--     currency en MVP1 (single-tenant, single-currency).
-- =====================================================================

CREATE TABLE categories (
    id          BIGSERIAL      PRIMARY KEY,
    tenant_id   UUID           NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    nome        VARCHAR(80)    NOT NULL,
    slug        VARCHAR(80)    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_tenant_slug UNIQUE (tenant_id, slug)
);

CREATE TABLE species (
    id          BIGSERIAL      PRIMARY KEY,
    tenant_id   UUID           NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    nome        VARCHAR(80)    NOT NULL,
    slug        VARCHAR(80)    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_species_tenant_slug UNIQUE (tenant_id, slug)
);

CREATE TABLE brands (
    id          BIGSERIAL      PRIMARY KEY,
    tenant_id   UUID           NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    nome        VARCHAR(120)   NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_brands_tenant_nome UNIQUE (tenant_id, nome)
);

CREATE TABLE products (
    id             BIGSERIAL      PRIMARY KEY,
    public_id      UUID           NOT NULL,                 -- UUID v7 (app); expuesto en URLs públicas
    tenant_id      UUID           NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    brand_id       BIGINT         REFERENCES brands(id) ON DELETE SET NULL,  -- NULLABLE
    nome           VARCHAR(160)   NOT NULL,
    descricao      TEXT,
    main_image_url TEXT,
    price          NUMERIC(10,2)  CHECK (price >= 0),       -- usado cuando NO hay variantes
    stock          INT            NOT NULL DEFAULT 0 CHECK (stock >= 0),
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_public_id UNIQUE (public_id)
);

-- Listado del catálogo: "productos activos del tenant". Índice de mayor uso.
CREATE INDEX idx_products_tenant_active ON products (tenant_id, active);
-- Filtro por marca (feature confirmada). Parcial: la mayoría de productos puede
-- no tener marca, no indexamos los NULL.
CREATE INDEX idx_products_brand ON products (brand_id) WHERE brand_id IS NOT NULL;

CREATE TABLE product_variants (
    id             BIGSERIAL      PRIMARY KEY,
    product_id     BIGINT         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    nome_variante  VARCHAR(80)    NOT NULL,                 -- ej "10kg", "60 unidades"
    price          NUMERIC(10,2)  NOT NULL CHECK (price >= 0),
    stock          INT            NOT NULL DEFAULT 0 CHECK (stock >= 0),
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    -- necesaria como destino de la FK compuesta de order_items (ver V4).
    -- Aunque id ya es único, la FK compuesta exige un UNIQUE sobre (product_id, id).
    CONSTRAINT uq_product_variants_product_id UNIQUE (product_id, id)
);

CREATE INDEX idx_product_variants_product ON product_variants (product_id);

-- ---- Relaciones N:M ------------------------------------------------
-- Tablas puras de asociación: solo created_at (una fila no se "actualiza",
-- se inserta o se borra). Por eso NO llevan updated_at.

CREATE TABLE product_categories (
    product_id  BIGINT      NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    category_id BIGINT      NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_product_categories PRIMARY KEY (product_id, category_id)
);
-- La PK cubre la dirección product→category. Este índice cubre la inversa:
-- "productos de la categoría X".
CREATE INDEX idx_product_categories_category ON product_categories (category_id);

CREATE TABLE product_species (
    product_id  BIGINT      NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    species_id  BIGINT      NOT NULL REFERENCES species(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_product_species PRIMARY KEY (product_id, species_id)
);
CREATE INDEX idx_product_species_species ON product_species (species_id);
