-- =====================================================================
-- V4 — Módulo ORDERS: pedidos + items
-- =====================================================================
-- Decisiones de modelado (ver ADR 003 actualizado, 013):
--   · Cliente EMBEBIDO (nome + telefone normalizado), sin tabla customer ni
--     login (MVP1 no tiene auth de clientes). Ver ADR 013 decisión de contacto.
--   · Forma de pagamento: se CAPTURA (dato offline para logística), NO es pago
--     online. frete_mode: GRATIS (<5km) / A_COMBINAR (resto). Total = Subtotal
--     (el frete no suma un número). Ver ADR 003 (actualización 2026-07-09).
--   · order_items apunta a product (siempre) + variant (opcional). FK COMPUESTA
--     garantiza a nivel DB que la variante pertenece al producto (decisión 3).
--   · Snapshot de nombre y precio por línea → los pedidos históricos no cambian
--     si el catálogo cambia de precio.
-- =====================================================================

CREATE TABLE orders (
    id                    BIGSERIAL      PRIMARY KEY,
    public_id             UUID           NOT NULL,          -- UUID v7 (app); código de pedido público
    tenant_id             UUID           NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    status                VARCHAR(12)    NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING','CONFIRMED','CANCELLED')),
    cliente_nome          VARCHAR(160)   NOT NULL,
    cliente_telefone      VARCHAR(30)    NOT NULL,
    cliente_telefone_norm VARCHAR(20)    NOT NULL,          -- normalizado para Fase 2
    forma_pagamento       VARCHAR(40)    NOT NULL,          -- cómo paga al recibir (offline). Valores TBD → sin CHECK todavía
    frete_mode            VARCHAR(12)    NOT NULL CHECK (frete_mode IN ('GRATIS','A_COMBINAR')),
    endereco_entrega      TEXT           NOT NULL,          -- entrega a domicilio (dirección obligatoria en checkout)
    horario_entrega       VARCHAR(120),                     -- preferencia de horario del cliente (texto libre)
    subtotal_snapshot     NUMERIC(10,2)  NOT NULL CHECK (subtotal_snapshot >= 0),  -- Total = Subtotal
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    confirmed_at          TIMESTAMPTZ,
    cancelled_at          TIMESTAMPTZ,
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_orders_public_id UNIQUE (public_id)
);

-- Lista de pedidos del admin, ordenada por fecha.
CREATE INDEX idx_orders_tenant_created ON orders (tenant_id, created_at);
-- NOTA: cliente_telefone_norm sin índice (misma razón que en appointments: la
-- query de "clientes recurrentes" es Fase 2).

CREATE TABLE order_items (
    id                  BIGSERIAL      PRIMARY KEY,
    order_id            BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id          BIGINT         NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    product_variant_id  BIGINT,                             -- NULLABLE: solo si el producto tenía variantes
    nome_snapshot       VARCHAR(200)   NOT NULL,            -- nombre al momento del pedido
    unit_price_snapshot NUMERIC(10,2)  NOT NULL CHECK (unit_price_snapshot >= 0),
    quantidade          INT            NOT NULL CHECK (quantidade > 0),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    -- FK compuesta: si product_variant_id no es NULL, el par (product_id, variant)
    -- DEBE existir en product_variants → la variante pertenece al producto.
    -- Con variant NULL, Postgres (MATCH SIMPLE) no chequea esta FK; el product_id
    -- sigue validado por la FK simple de arriba.
    CONSTRAINT fk_order_items_variant
        FOREIGN KEY (product_id, product_variant_id)
        REFERENCES product_variants (product_id, id) ON DELETE RESTRICT
);

-- Carga de items de un pedido.
CREATE INDEX idx_order_items_order ON order_items (order_id);
-- Métrica MVP1 "top 5 productos pedidos": agrupa por product_id (siempre presente
-- gracias a la decisión 3). Este índice la sostiene.
CREATE INDEX idx_order_items_product ON order_items (product_id);
