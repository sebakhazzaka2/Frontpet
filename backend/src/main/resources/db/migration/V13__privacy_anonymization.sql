-- =====================================================================
-- V13 — Direito de eliminação LGPD (tarea 7.14, ADR 023)
-- =====================================================================
-- Decisiones de modelado (ADR 023):
--   · Anonimização in-place, não DELETE: orders/appointments não têm tabela
--     customer própria (ADR 013 §4) — um DELETE levaria junto os snapshots
--     de preço/items (dado contábil) e reduziria os conteos do dashboard
--     operativo (7.1/7.3). Os dados PESSOAIS são limpos; o registro comercial
--     fica, anônimo.
--   · anonymized_at marca a anonimização — não existe deletedAt/soft-delete
--     nestas tabelas hoje (só products/services têm `active`).
--   · cliente_telefone_norm passa a ser NULLABLE: a anonimização a apaga.
--   · Reversão de ADR 013 §11 (que decidiu NÃO indexar cliente_telefone_norm
--     porque a única query que precisaria era Fase 2): esta é essa query. O
--     índice é PARCIAL (WHERE ... IS NOT NULL) — não indexa as linhas já
--     anonimizadas, que por definição nunca mais são buscadas por telefone.
--   · privacy_erasure_log audita QUEM apagou O QUÊ. Guarda o HASH do
--     telefone (SHA-256), não o telefone em si — não faria sentido reter o
--     identificador que acabou de ser removido. admin_user_id é um BIGINT
--     solto, sem FK: nenhuma tabela do repo referencia admin_users (os
--     testes constroem AdminUser em memória, sem persistir — ver
--     AdminOrderControllerTest.adminFor), então uma FK quebraria todo teste
--     que chamasse este endpoint.
-- =====================================================================

ALTER TABLE orders
    ALTER COLUMN cliente_telefone_norm DROP NOT NULL,
    ADD COLUMN anonymized_at TIMESTAMPTZ;

ALTER TABLE appointments
    ALTER COLUMN cliente_telefone_norm DROP NOT NULL,
    ADD COLUMN anonymized_at TIMESTAMPTZ;

CREATE INDEX idx_orders_cliente_telefone_norm
    ON orders (tenant_id, cliente_telefone_norm)
    WHERE cliente_telefone_norm IS NOT NULL;

CREATE INDEX idx_appointments_cliente_telefone_norm
    ON appointments (tenant_id, cliente_telefone_norm)
    WHERE cliente_telefone_norm IS NOT NULL;

CREATE TABLE privacy_erasure_log (
    id                      BIGSERIAL      PRIMARY KEY,
    tenant_id               UUID           NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    admin_user_id           BIGINT         NOT NULL,          -- sem FK, ver nota acima
    telefone_hash           VARCHAR(64)    NOT NULL,          -- SHA-256 hex do telefone normalizado
    orders_anonymized       INT            NOT NULL,
    appointments_anonymized INT            NOT NULL,
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_privacy_erasure_log_tenant ON privacy_erasure_log (tenant_id, created_at);
