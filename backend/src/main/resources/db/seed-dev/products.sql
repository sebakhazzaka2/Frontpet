-- =====================================================================
-- SEED DE DESARROLLO — catálogo de productos (tarea 3.12)
-- =====================================================================
-- ⚠️ ESTO NO ES UNA MIGRACIÓN. Vive fuera de `classpath:db/migration` a
-- propósito, para que Flyway no lo vea y nunca corra solo.
--
-- Por qué fuera de Flyway: estos 10 productos son INVENTADOS (nombres,
-- precios y stock). V5 dice "não executar em produção" pero es mentira —
-- `application.yml` (frontpet.tenant.id) apunta al tenant que V5 inserta,
-- así que V5 tiene que correr en prod sí o sí. Cualquier cosa que metamos
-- en una migración termina publicada bajo la marca del cliente con precios
-- falsos, en un sitio que gente de Santana do Livramento puede encontrar.
-- Acá afuera ese problema no existe y no hace falta maquinaria de perfiles.
--
-- Cómo correrlo (desde la raíz del repo, con el compose levantado):
--   docker exec -i frontpet-postgres psql -U frontpet -d frontpet \
--     < backend/src/main/resources/db/seed-dev/products.sql
--
-- Es re-ejecutable: borra y vuelve a insertar. Si ya creaste pedidos de
-- prueba, el DELETE va a fallar contra el ON DELETE RESTRICT de
-- order_items.product_id (ADR 013 §12) — es el comportamiento correcto.
--
-- Las categorías y especies NO se insertan acá: son datos reales del
-- cliente y ya los siembra V8__catalog_reference_data.sql.
-- =====================================================================

BEGIN;

-- Cascada: se llevan variantes, product_categories y product_species (V2).
DELETE FROM products WHERE tenant_id = '01924ccf-0000-7000-8000-000000000001';
DELETE FROM brands   WHERE tenant_id = '01924ccf-0000-7000-8000-000000000001';

-- ── Marcas ───────────────────────────────────────────────────────────
-- Marcas reales que un petshop de este rubro efectivamente vende. Sirven
-- para que el filtro por marca y la búsqueda tengan contra qué probarse.
INSERT INTO brands (tenant_id, nome) VALUES
('01924ccf-0000-7000-8000-000000000001', 'Golden'),
('01924ccf-0000-7000-8000-000000000001', 'Premier'),
('01924ccf-0000-7000-8000-000000000001', 'Sanol'),
('01924ccf-0000-7000-8000-000000000001', 'Purina'),
('01924ccf-0000-7000-8000-000000000001', 'Kong');

-- ── Productos ────────────────────────────────────────────────────────
-- public_id hardcodeado con forma de UUID v7 (la app genera v7 reales;
-- para un fixture fijo alcanza con respetar la forma). Mismo criterio que
-- el tenant de V5.
--
-- main_image_url NULL en todos: no hay fotos reales todavía y no se
-- hotlinkean imágenes ajenas. El <ProductCard> ya renderiza el placeholder
-- de PawPrint cuando falta.
--
-- Acentos a propósito en los nombres (Ração, Higiênico, Clássico,
-- Ajustável): la búsqueda de la 3.8 usa LOWER(nome) LIKE sin unaccent, así
-- que buscar "racao" NO va a matchear "Ração". Es un hallazgo esperado —
-- mejor que aparezca acá y no en producción.
INSERT INTO products (
    public_id, tenant_id, brand_id, nome, slug, descricao,
    price, price_original, stock, active
) VALUES
-- 1 — CON VARIANTES: price NULL a propósito. El precio del listado sale
--     del MIN de las variantes activas (ver ProductRepository.findSummaries).
(
    '01924ccf-0001-7000-8000-000000000001',
    '01924ccf-0000-7000-8000-000000000001',
    (SELECT id FROM brands WHERE tenant_id = '01924ccf-0000-7000-8000-000000000001' AND LOWER(nome) = 'golden'),
    'Ração Golden Fórmula Frango e Carne Adulto',
    'racao-golden-formula-frango-e-carne-adulto',
    'Alimento completo para cães adultos de todos os portes. Fórmula com frango e carne, sem corantes artificiais.',
    NULL, NULL, 0, TRUE
),
-- 2 — producto simple con marca.
(
    '01924ccf-0001-7000-8000-000000000002',
    '01924ccf-0000-7000-8000-000000000001',
    (SELECT id FROM brands WHERE tenant_id = '01924ccf-0000-7000-8000-000000000001' AND LOWER(nome) = 'premier'),
    'Ração Premier Gatos Castrados Frango',
    'racao-premier-gatos-castrados-frango',
    'Ração seca para gatos castrados adultos. Ajuda no controle de peso e na saúde do trato urinário.',
    129.90, NULL, 18, TRUE
),
-- 3 — N:M REAL (Acessórios + Higiene, Cães + Gatos) y SIN MARCA.
(
    '01924ccf-0001-7000-8000-000000000003',
    '01924ccf-0000-7000-8000-000000000001',
    NULL,
    'Coleira Antipulgas Ajustável',
    'coleira-antipulgas-ajustavel',
    'Proteção contra pulgas e carrapatos por até 6 meses. Ajustável, resistente à água.',
    89.90, NULL, 25, TRUE
),
-- 4
(
    '01924ccf-0001-7000-8000-000000000004',
    '01924ccf-0000-7000-8000-000000000001',
    (SELECT id FROM brands WHERE tenant_id = '01924ccf-0000-7000-8000-000000000001' AND LOWER(nome) = 'sanol'),
    'Shampoo Neutro para Filhotes',
    'shampoo-neutro-para-filhotes',
    'Fórmula suave para a pele sensível dos filhotes. Não irrita os olhos.',
    34.90, NULL, 40, TRUE
),
-- 5 — EN PROMOÇÃO: price_original > price (CHECK de V6). Único producto
--     que devuelve ?promocoes=true.
(
    '01924ccf-0001-7000-8000-000000000005',
    '01924ccf-0000-7000-8000-000000000001',
    (SELECT id FROM brands WHERE tenant_id = '01924ccf-0000-7000-8000-000000000001' AND LOWER(nome) = 'purina'),
    'Bifinho de Frango',
    'bifinho-de-frango',
    'Petisco macio de frango para cães de todos os portes. Ideal para treino e recompensa.',
    18.50, 24.90, 60, TRUE
),
-- 6
(
    '01924ccf-0001-7000-8000-000000000006',
    '01924ccf-0000-7000-8000-000000000001',
    NULL,
    'Cama Iglu Toca',
    'cama-iglu-toca',
    'Toca fechada em material acolchoado. Oferece abrigo e sensação de segurança.',
    199.00, NULL, 7, TRUE
),
-- 7
(
    '01924ccf-0001-7000-8000-000000000007',
    '01924ccf-0000-7000-8000-000000000001',
    (SELECT id FROM brands WHERE tenant_id = '01924ccf-0000-7000-8000-000000000001' AND LOWER(nome) = 'kong'),
    'Mordedor Kong Clássico',
    'mordedor-kong-classico',
    'Brinquedo de borracha natural resistente. Pode ser recheado com petiscos.',
    74.90, NULL, 15, TRUE
),
-- 8 — N:M (Brinquedos + Conforto).
(
    '01924ccf-0001-7000-8000-000000000008',
    '01924ccf-0000-7000-8000-000000000001',
    NULL,
    'Arranhador Torre de Sisal',
    'arranhador-torre-de-sisal',
    'Torre com três níveis e sisal natural. Estimula o arranhar sem danificar os móveis.',
    249.00, NULL, 4, TRUE
),
-- 9 — cubre "Outros", el recolector.
(
    '01924ccf-0001-7000-8000-000000000009',
    '01924ccf-0000-7000-8000-000000000001',
    NULL,
    'Tapete Higiênico 30 unidades',
    'tapete-higienico-30-unidades',
    'Pacote com 30 tapetes absorventes com gel. Camada externa impermeável.',
    59.90, NULL, 22, TRUE
),
-- 10 — INACTIVO: no debe aparecer en GET /products, y GET /products/{slug}
--      tiene que devolver 404 (ProductServiceImpl filtra por active).
(
    '01924ccf-0001-7000-8000-000000000010',
    '01924ccf-0000-7000-8000-000000000001',
    NULL,
    'Comedouro Duplo Inox',
    'comedouro-duplo-inox',
    'Comedouro duplo com base antiderrapante. Fora de linha — mantido inativo.',
    45.00, NULL, 0, FALSE
);

-- ── Variantes ────────────────────────────────────────────────────────
-- Solo el producto 1. La variante de 1 kg va INACTIVA y es la más barata a
-- propósito: si la query del listado dejara de filtrar por active, el
-- precio del catálogo pasaría de 79,90 a 34,90. Es el chequeo más filoso
-- que este seed puede hacer sobre COALESCE(price, MIN(variantes activas)).
INSERT INTO product_variants (product_id, nome_variante, price, stock, active)
SELECT p.id, v.nome_variante, v.price, v.stock, v.active
FROM products p
CROSS JOIN (VALUES
    ('1 kg',     34.90::numeric,  0, FALSE),
    ('3 kg',     79.90::numeric, 12, TRUE),
    ('10,1 kg', 199.90::numeric,  8, TRUE),
    ('15 kg',   269.90::numeric,  5, TRUE)
) AS v(nome_variante, price, stock, active)
WHERE p.tenant_id = '01924ccf-0000-7000-8000-000000000001'
  AND p.slug = 'racao-golden-formula-frango-e-carne-adulto';

-- ── Categorías (N:M) ─────────────────────────────────────────────────
-- Por slug, no por id: products.id es BIGSERIAL y no lo conocemos acá.
-- Las 7 categorías canónicas quedan cubiertas.
INSERT INTO product_categories (product_id, category_id)
SELECT p.id, c.id
FROM products p
JOIN (VALUES
    ('racao-golden-formula-frango-e-carne-adulto', 'racoes'),
    ('racao-premier-gatos-castrados-frango',       'racoes'),
    ('coleira-antipulgas-ajustavel',               'acessorios'),
    ('coleira-antipulgas-ajustavel',               'higiene'),
    ('shampoo-neutro-para-filhotes',               'higiene'),
    ('bifinho-de-frango',                          'petiscos'),
    ('cama-iglu-toca',                             'conforto'),
    ('mordedor-kong-classico',                     'brinquedos'),
    ('arranhador-torre-de-sisal',                  'brinquedos'),
    ('arranhador-torre-de-sisal',                  'conforto'),
    ('tapete-higienico-30-unidades',               'outros'),
    ('comedouro-duplo-inox',                       'acessorios')
) AS m(product_slug, category_slug) ON m.product_slug = p.slug
JOIN categories c ON c.slug = m.category_slug AND c.tenant_id = p.tenant_id
WHERE p.tenant_id = '01924ccf-0000-7000-8000-000000000001';

-- ── Espécies (N:M) ───────────────────────────────────────────────────
INSERT INTO product_species (product_id, species_id)
SELECT p.id, s.id
FROM products p
JOIN (VALUES
    ('racao-golden-formula-frango-e-carne-adulto', 'caes'),
    ('racao-premier-gatos-castrados-frango',       'gatos'),
    ('coleira-antipulgas-ajustavel',               'caes'),
    ('coleira-antipulgas-ajustavel',               'gatos'),
    ('shampoo-neutro-para-filhotes',               'caes'),
    ('bifinho-de-frango',                          'caes'),
    ('cama-iglu-toca',                             'gatos'),
    ('mordedor-kong-classico',                     'caes'),
    ('arranhador-torre-de-sisal',                  'gatos'),
    ('tapete-higienico-30-unidades',               'caes'),
    ('comedouro-duplo-inox',                       'caes'),
    ('comedouro-duplo-inox',                       'gatos')
) AS m(product_slug, species_slug) ON m.product_slug = p.slug
JOIN species s ON s.slug = m.species_slug AND s.tenant_id = p.tenant_id
WHERE p.tenant_id = '01924ccf-0000-7000-8000-000000000001';

COMMIT;

-- ── Verificación ─────────────────────────────────────────────────────
-- Esperado: 10 productos (9 activos), 4 variantes (3 activas), 12 filas de
-- categorías, 12 de especies, 5 marcas.
SELECT
    (SELECT COUNT(*) FROM products WHERE tenant_id = '01924ccf-0000-7000-8000-000000000001')                AS produtos,
    (SELECT COUNT(*) FROM products WHERE tenant_id = '01924ccf-0000-7000-8000-000000000001' AND active)     AS ativos,
    (SELECT COUNT(*) FROM product_variants v JOIN products p ON p.id = v.product_id
      WHERE p.tenant_id = '01924ccf-0000-7000-8000-000000000001')                                           AS variantes,
    (SELECT COUNT(*) FROM product_categories pc JOIN products p ON p.id = pc.product_id
      WHERE p.tenant_id = '01924ccf-0000-7000-8000-000000000001')                                           AS categorias,
    (SELECT COUNT(*) FROM product_species ps JOIN products p ON p.id = ps.product_id
      WHERE p.tenant_id = '01924ccf-0000-7000-8000-000000000001')                                           AS especies,
    (SELECT COUNT(*) FROM brands WHERE tenant_id = '01924ccf-0000-7000-8000-000000000001')                  AS marcas;
