-- =====================================================================
-- V1 — Módulo IDENTITY: tenants + admin_users
-- =====================================================================
-- Notas Postgres (para quien viene de MySQL/Oracle):
--   · TIMESTAMPTZ = timestamp con zona (guarda UTC). Usar SIEMPRE esto,
--     nunca TIMESTAMP "naive" — evita el drift de zona horaria de MySQL.
--   · MySQL tiene "ON UPDATE CURRENT_TIMESTAMP"; Postgres NO. El updated_at
--     lo bumpea Hibernate con @UpdateTimestamp a nivel entidad (igual que el
--     repo consultorio). En DB dejamos DEFAULT now() para cubrir inserts/seeds.
--   · UUID es tipo nativo (16 bytes reales). PG16 no trae uuidv7() nativa
--     (recién PG18): el public_id/tenant id lo genera la app en Java (v7).
--   · BIGSERIAL = autoincremental (como AUTO_INCREMENT / IDENTITY de Oracle).
-- =====================================================================

-- Tenant = el negocio. Es la clave transversal de todo el sistema (viaja en
-- el JWT), por eso su PK es UUID v7 y no BIGSERIAL. Ver ADR 013.
CREATE TABLE tenants (
    id                UUID           PRIMARY KEY,          -- UUID v7 generado en la app
    nome              VARCHAR(120)   NOT NULL,
    endereco          TEXT,
    whatsapp_destino  VARCHAR(30)    NOT NULL,             -- destino de pedidos/reservas (E.164)
    config            JSONB          NOT NULL DEFAULT '{}'::jsonb,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

COMMENT ON COLUMN tenants.config IS
  'Config del negocio en JSONB: capacidade_atendimento (int), anticipacao_min/max_reserva, etc. Ver ADR 009/012.';

-- Usuarios admin (funcionarios). Sin roles diferenciados en MVP1 (todos ADMIN);
-- la columna role queda lista para Fase 2. Modelo alineado con el User reusable
-- del repo consultorio (email/password/role). Ver ADR 013.
CREATE TABLE admin_users (
    id             BIGSERIAL      PRIMARY KEY,
    tenant_id      UUID           NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    email          VARCHAR(255)   NOT NULL,
    password_hash  VARCHAR(255)   NOT NULL,                -- BCrypt
    role           VARCHAR(30)    NOT NULL DEFAULT 'ADMIN',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_admin_users_tenant_email UNIQUE (tenant_id, email)
);

-- Sin índice extra sobre admin_users: es una tabla diminuta (un puñado de
-- funcionarios). El login findByEmail resuelve por seq-scan al instante y la
-- UNIQUE(tenant_id, email) ya cubre integridad. (Regla: índice solo si una
-- query lo necesita a escala — ver decisión 4 del diseño.)
