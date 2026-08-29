-- =====================================================================
-- V11 — SEED DE DESENVOLVIMENTO: valores confirmados pelo cliente (2026-08-24)
-- =====================================================================
-- NÃO executar em produção (mesma ressalva de V5/V10 — dados reais do tenant
-- entram via admin no Sprint Despliegue, tarefa 8.4/8.5). Este arquivo só
-- corrige os valores PROVISÓRIOS de V5 (duração de Banho) e V10 (Tosa
-- Completa, Carding, Tosa Higiênica) com os números reais que o cliente
-- mandou. Nunca editar V5/V10 diretamente — Flyway não permite alterar uma
-- migration já aplicada.
--
-- Fonte: docs/preguntas-cliente.md §2.1 (mensagem do cliente 2026-08-24).
--
-- ⚠️ Valores com GG marcados "(estimado)" abaixo são extrapolação do padrão
-- P→M→G que o cliente confirmou — ele não mandou o GG explicitamente.
-- Confirmar em algum momento antes de dar por definitivo; não bloqueia nada
-- porque GG já é o porte menos comum.
-- =====================================================================

-- ── Banho Essencial: duração real P/M/G, GG estimado (+30min sobre G, mesmo
--    incremento que P→M e M→G) ────────────────────────────────────────────
UPDATE service_pricing sp
SET duration_minutes = v.duration_minutes
FROM (VALUES
    ('P',  60),
    ('M',  90),
    ('G', 120),
    ('GG', 150) -- estimado
) AS v(size, duration_minutes)
WHERE sp.size = v.size
  AND sp.service_id = (
      SELECT id FROM services
      WHERE nome = 'Banho Essencial'
        AND tenant_id = '01924ccf-0000-7000-8000-000000000001'
  );

-- ── Banho Premium: Essencial + adicionais confirmados (limpeza dental 5-10min,
--    spray bucal 1min, limpeza de ouvidos 5-15min, corte de unhas 10-20min).
--    Cliente: "adicional aproximado de 30 a 45 min em todos os portes" —
--    usamos +35min (ponto médio) uniforme, não escalado por porte. ──────────
UPDATE service_pricing sp
SET duration_minutes = v.duration_minutes
FROM (VALUES
    ('P',   95), -- 60 + 35
    ('M',  125), -- 90 + 35
    ('G',  155), -- 120 + 35
    ('GG', 185)  -- 150 (estimado) + 35
) AS v(size, duration_minutes)
WHERE sp.size = v.size
  AND sp.service_id = (
      SELECT id FROM services
      WHERE nome = 'Banho Premium'
        AND tenant_id = '01924ccf-0000-7000-8000-000000000001'
  );

-- ── Tosa Completa: preço já vinha certo em V10 para P/M/G (R$40/50/60);
--    duração real do cliente + GG estimado (+30min, mesmo incremento) e
--    preço GG estimado (+R$10, mesmo incremento P→M→G) ────────────────────
UPDATE service_pricing sp
SET price = v.price, duration_minutes = v.duration_minutes
FROM (VALUES
    ('P',  40.00::numeric,  90),
    ('M',  50.00::numeric, 120),
    ('G',  60.00::numeric, 150),
    ('GG', 70.00::numeric, 180) -- estimado (preço e duração)
) AS v(size, price, duration_minutes)
WHERE sp.size = v.size
  AND sp.service_id = (
      SELECT id FROM services
      WHERE nome = 'Tosa Completa'
        AND tenant_id = '01924ccf-0000-7000-8000-000000000001'
  );

-- ── Carding: preço real P/M/G, duração real (nota: M e G têm a MESMA
--    duração, 60min — não é erro, o cliente confirmou assim). GG estimado
--    mantendo o platô de duração de M/G e o mesmo incremento de preço ─────
UPDATE service_pricing sp
SET price = v.price, duration_minutes = v.duration_minutes
FROM (VALUES
    ('P',  35.00::numeric, 30),
    ('M',  45.00::numeric, 60),
    ('G',  55.00::numeric, 60),
    ('GG', 65.00::numeric, 60) -- estimado (preço; duração segue o platô de M/G)
) AS v(size, price, duration_minutes)
WHERE sp.size = v.size
  AND sp.service_id = (
      SELECT id FROM services
      WHERE nome = 'Carding'
        AND tenant_id = '01924ccf-0000-7000-8000-000000000001'
  );

-- ── Tosa Higiênica: preço real é FIXO (R$10, não varia por porte — dado
--    novo, V10 tinha um preço provisório por porte). Duração não veio
--    confirmada: estimado 15min flat, é o adicional mais rápido do menu ───
UPDATE service_pricing sp
SET price = 10.00, duration_minutes = 15 -- duração estimada
WHERE sp.service_id = (
    SELECT id FROM services
    WHERE nome = 'Tosa Higiênica'
      AND tenant_id = '01924ccf-0000-7000-8000-000000000001'
);

-- Hidratação, Banho Antisséptico e Banho Antipulga: SEM confirmação ainda
-- (preço e duração). Ficam com os valores provisórios de V10 até o cliente
-- responder — ver docs/preguntas-cliente.md §2.1, pergunta pendente única
-- que restou depois desta migration.
