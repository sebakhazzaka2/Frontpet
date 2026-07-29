-- =====================================================================
-- V10 — SEED DE DESENVOLVIMENTO: adicionais (localhost only)
-- =====================================================================
-- NÃO executar em produção. Completa o seed de V5 com os 6 adicionais
-- confirmados pelo cliente (docs/preguntas-cliente.md §2.1), cujos preços e
-- durações por porte AINDA NÃO foram confirmados (bloqueante ativo, ver
-- docs/preguntas-cliente.md linha 329: "Antes do Sprint 5").
--
-- ⚠️ TODOS os preços e durações abaixo são PROVISÓRIOS — inventados para
-- destravar o desenvolvimento do algoritmo de disponibilidade (ADR 020), que
-- é agnóstico aos valores concretos (ADR 011). NÃO usar para nenhuma decisão
-- de produto nem confirmar ao cliente. Os testes de integração do Sprint 5
-- semeiam seus próprios valores em vez de depender deste arquivo, para que
-- corrigir os números reais não quebre casos de borde já testados.
--
-- Ao confirmar os valores reais com o cliente: substituir os preços e
-- duration_minutes abaixo por uma migration nova (V11), nunca editar este
-- arquivo — Flyway não permite alterar uma migration já aplicada.
-- =====================================================================

INSERT INTO services (tenant_id, type, nome, descricao) VALUES
('01924ccf-0000-7000-8000-000000000001', 'ADDON', 'Tosa Higiênica',    'Aparação das áreas íntimas, patas e focinho.'),
('01924ccf-0000-7000-8000-000000000001', 'ADDON', 'Tosa Completa',     'Tosa do corpo todo, no estilo escolhido.'),
('01924ccf-0000-7000-8000-000000000001', 'ADDON', 'Carding',           'Remoção de pelos mortos da subpelagem.'),
('01924ccf-0000-7000-8000-000000000001', 'ADDON', 'Hidratação',        'Tratamento hidratante para pelo e pele.'),
('01924ccf-0000-7000-8000-000000000001', 'ADDON', 'Banho Antisséptico','Banho com produto antisséptico para peles sensíveis.'),
('01924ccf-0000-7000-8000-000000000001', 'ADDON', 'Banho Antipulga',   'Banho com produto antipulgas e carrapatos.');

-- Preço e duração por porte — PROVISÓRIOS, ver aviso no header do arquivo.
INSERT INTO service_pricing (service_id, size, price, duration_minutes)
SELECT s.id, v.size, v.price, v.duration_minutes
FROM services s
CROSS JOIN (VALUES
    ('P',  15.00::numeric, 15),
    ('M',  20.00::numeric, 20),
    ('G',  25.00::numeric, 25),
    ('GG', 30.00::numeric, 30)
) AS v(size, price, duration_minutes)
WHERE s.nome = 'Tosa Higiênica'
  AND s.tenant_id = '01924ccf-0000-7000-8000-000000000001';

INSERT INTO service_pricing (service_id, size, price, duration_minutes)
SELECT s.id, v.size, v.price, v.duration_minutes
FROM services s
CROSS JOIN (VALUES
    ('P',  30.00::numeric, 30),
    ('M',  40.00::numeric, 40),
    ('G',  50.00::numeric, 50),
    ('GG', 60.00::numeric, 60)
) AS v(size, price, duration_minutes)
WHERE s.nome = 'Tosa Completa'
  AND s.tenant_id = '01924ccf-0000-7000-8000-000000000001';

INSERT INTO service_pricing (service_id, size, price, duration_minutes)
SELECT s.id, v.size, v.price, v.duration_minutes
FROM services s
CROSS JOIN (VALUES
    ('P',  25.00::numeric, 20),
    ('M',  35.00::numeric, 25),
    ('G',  45.00::numeric, 30),
    ('GG', 55.00::numeric, 35)
) AS v(size, price, duration_minutes)
WHERE s.nome = 'Carding'
  AND s.tenant_id = '01924ccf-0000-7000-8000-000000000001';

INSERT INTO service_pricing (service_id, size, price, duration_minutes)
SELECT s.id, v.size, v.price, v.duration_minutes
FROM services s
CROSS JOIN (VALUES
    ('P',  20.00::numeric, 15),
    ('M',  25.00::numeric, 20),
    ('G',  30.00::numeric, 25),
    ('GG', 35.00::numeric, 30)
) AS v(size, price, duration_minutes)
WHERE s.nome = 'Hidratação'
  AND s.tenant_id = '01924ccf-0000-7000-8000-000000000001';

INSERT INTO service_pricing (service_id, size, price, duration_minutes)
SELECT s.id, v.size, v.price, v.duration_minutes
FROM services s
CROSS JOIN (VALUES
    ('P',  15.00::numeric, 10),
    ('M',  18.00::numeric, 10),
    ('G',  22.00::numeric, 15),
    ('GG', 25.00::numeric, 15)
) AS v(size, price, duration_minutes)
WHERE s.nome = 'Banho Antisséptico'
  AND s.tenant_id = '01924ccf-0000-7000-8000-000000000001';

INSERT INTO service_pricing (service_id, size, price, duration_minutes)
SELECT s.id, v.size, v.price, v.duration_minutes
FROM services s
CROSS JOIN (VALUES
    ('P',  15.00::numeric, 10),
    ('M',  18.00::numeric, 10),
    ('G',  22.00::numeric, 15),
    ('GG', 25.00::numeric, 15)
) AS v(size, price, duration_minutes)
WHERE s.nome = 'Banho Antipulga'
  AND s.tenant_id = '01924ccf-0000-7000-8000-000000000001';
