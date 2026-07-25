-- =====================================================================
-- V5 — SEED DE DESENVOLVIMENTO (localhost only)
-- =====================================================================
-- NÃO executar em produção. Os dados reais do tenant e serviços são
-- inseridos pelo admin no Sprint Despliegue.
--
-- Dados confirmados com o cliente (jul/2026):
--   · Preços: tabela "Banhos Individuais" oficial (imagem jul/2026)
--   · Horários: preguntas-cliente.md §3.1
--   · Capacidade: 2 atendimentos simultâneos (§2.2)
--
-- Pendente de confirmação:
--   · whatsapp_destino: substituir pelo número real antes do deploy
--   · duration_minutes: estimativas razoáveis — confirmar com o cliente
--   · ADDONs (tosa, carding, hidratação etc.): preços e durações pendentes
-- =====================================================================

-- ── Tenant ──────────────────────────────────────────────────────────
-- UUID fixo só para dev (a app gera UUID v7 real em prod).
INSERT INTO tenants (id, nome, endereco, whatsapp_destino, config)
VALUES (
    '01924ccf-0000-7000-8000-000000000001',
    'FrontPet',
    'Santana do Livramento, RS, Brasil',
    '+555596724124',    -- FrontPet WhatsApp (E.164)
    jsonb_build_object(
        'capacidade_atendimento',   2,   -- 2 animais em simultâneo (§2.2)
        'anticipacao_min_horas',    1,   -- mínimo 1 h antes para reservar
        'anticipacao_max_dias',     60   -- máximo 60 días hacia adelante
    )
);

-- ── Serviços BASE ───────────────────────────────────────────────────
-- "Banhos Individuais" — 2 modalidades confirmadas pelo cliente.

INSERT INTO services (tenant_id, type, nome, descricao) VALUES
(
    '01924ccf-0000-7000-8000-000000000001',
    'BASE',
    'Banho Essencial',
    'Limpeza completa, secagem profissional e perfume.'
),
(
    '01924ccf-0000-7000-8000-000000000001',
    'BASE',
    'Banho Premium',
    'Banho completo, limpeza dental, spray bucal, limpeza de ouvidos e corte de unhas (se necessário).'
);

-- ── Preços por porte ─────────────────────────────────────────────────
-- Preços reais da tabela oficial do cliente (jul/2026).
-- duration_minutes: estimativas — CONFIRMAR com o cliente antes do deploy.

INSERT INTO service_pricing (service_id, size, price, duration_minutes)
SELECT s.id, v.size, v.price, v.duration_minutes
FROM services s
CROSS JOIN (VALUES
    ('P',  49.00::numeric, 45),
    ('M',  59.00::numeric, 60),
    ('G',  79.00::numeric, 90),
    ('GG', 95.00::numeric, 120)
) AS v(size, price, duration_minutes)
WHERE s.nome = 'Banho Essencial'
  AND s.tenant_id = '01924ccf-0000-7000-8000-000000000001';

INSERT INTO service_pricing (service_id, size, price, duration_minutes)
SELECT s.id, v.size, v.price, v.duration_minutes
FROM services s
CROSS JOIN (VALUES
    ('P',  65.00::numeric,  60),
    ('M',  75.00::numeric,  75),
    ('G',  95.00::numeric, 105),
    ('GG', 115.00::numeric, 135)
) AS v(size, price, duration_minutes)
WHERE s.nome = 'Banho Premium'
  AND s.tenant_id = '01924ccf-0000-7000-8000-000000000001';

-- ── Horários de atendimento ──────────────────────────────────────────
-- §3.1: seg-sex 09:00–17:00 / sáb 09:00–19:00 / dom fechado
-- Sem pausa ao meio-dia (FrontPet não fecha — pausa_inicio/fin NULL).
-- dia_semana ISO-8601: 1=Seg .. 6=Sáb .. 7=Dom

INSERT INTO business_hours (tenant_id, dia_semana, activo, abertura, fechamento) VALUES
('01924ccf-0000-7000-8000-000000000001', 1, TRUE, '09:00', '17:00'),  -- Seg
('01924ccf-0000-7000-8000-000000000001', 2, TRUE, '09:00', '17:00'),  -- Ter
('01924ccf-0000-7000-8000-000000000001', 3, TRUE, '09:00', '17:00'),  -- Qua
('01924ccf-0000-7000-8000-000000000001', 4, TRUE, '09:00', '17:00'),  -- Qui
('01924ccf-0000-7000-8000-000000000001', 5, TRUE, '09:00', '17:00'),  -- Sex
('01924ccf-0000-7000-8000-000000000001', 6, TRUE, '09:00', '19:00'),  -- Sáb
('01924ccf-0000-7000-8000-000000000001', 7, FALSE, '09:00', '17:00'); -- Dom (fechado — activo=FALSE)
