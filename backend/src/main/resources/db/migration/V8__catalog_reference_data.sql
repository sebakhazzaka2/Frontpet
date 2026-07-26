-- =====================================================================
-- V8 — Categorias e espécies (dados de referência)
-- =====================================================================
-- Estes NÃO são dados de teste: são as categorias reais confirmadas com o
-- cliente. Por isso vão numa migração normal e não no seed de dev (V5).
--
-- Categorias (CLAUDE.md §6) + "Outros" como recolhedor: todo produto que não
-- entra em nenhuma das outras precisa de um lugar onde cair.
--
-- Espécies: só Cães e Gatos. O cliente trabalha exclusivamente com esses dois
-- (confirmado jul/2026). Aves, Peixes e Roedores foram descartados.
--
-- Dependem do tenant inserido em V5. Flyway roda as migrações em ordem, então
-- o tenant sempre existe quando esta roda.
-- =====================================================================

INSERT INTO categories (tenant_id, nome, slug) VALUES
('01924ccf-0000-7000-8000-000000000001', 'Rações',     'racoes'),
('01924ccf-0000-7000-8000-000000000001', 'Acessórios', 'acessorios'),
('01924ccf-0000-7000-8000-000000000001', 'Higiene',    'higiene'),
('01924ccf-0000-7000-8000-000000000001', 'Petiscos',   'petiscos'),
('01924ccf-0000-7000-8000-000000000001', 'Conforto',   'conforto'),
('01924ccf-0000-7000-8000-000000000001', 'Brinquedos', 'brinquedos'),
('01924ccf-0000-7000-8000-000000000001', 'Outros',     'outros');

INSERT INTO species (tenant_id, nome, slug) VALUES
('01924ccf-0000-7000-8000-000000000001', 'Cães',  'caes'),
('01924ccf-0000-7000-8000-000000000001', 'Gatos', 'gatos');
