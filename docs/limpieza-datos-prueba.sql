-- ════════════════════════════════════════════════════════════════════════════
-- Limpieza de datos de PRUEBA en producción — pareja de docs/checklist-prueba-prod.md
--
-- NO es una migración Flyway (vive en docs/, Flyway no lo ve). Se corre A MANO,
-- una sola vez, contra la BD de producción (terminal de Coolify → psql).
--
-- Qué borra: SOLO filas marcadas como prueba:
--   · nombre (cliente / producto / marca / motivo de bloqueo) que empieza con 'ZZ-TEST'
--   · teléfonos de prueba 5555900000xx  (ej. "(55) 90000-0001")
--   · filas que la prueba de anonimización LGPD dejó como "Titular removido"
--     DENTRO de la ventana de prueba (>= t0, ver abajo)
-- Qué NO toca: tenants, admin_users, services, service_pricing, business_hours,
--   categories, species, ni ningún producto/pedido/turno real.
--
-- Seguro por defecto: todo corre dentro de una transacción que se deja ABIERTA.
-- Vos decidís al final: COMMIT; o ROLLBACK;
--
-- Uso:
--   1. Hacer backup ANTES de probar (ver checklist §0).
--   2. Editar t0 (abajo): hora de inicio de la prueba, con zona horaria.
--   3. Pegar el PASO 1 (vista previa) y revisar. Anotar las URLs de imagen (R2).
--   4. Pegar el PASO 2 (borrado). Revisar los conteos del PASO 3.
--   5. Si todo cuadra → COMMIT;   si algo raro → ROLLBACK;
-- ════════════════════════════════════════════════════════════════════════════

BEGIN;

-- ▼▼▼ EDITAR: inicio de la ventana de prueba (hora de Brasil) ▼▼▼
CREATE TEMP TABLE _cfg AS SELECT TIMESTAMPTZ '2026-10-16 00:00:00-03' AS t0;
-- ▲▲▲ ────────────────────────────────────────────────────── ▲▲▲

-- Teléfonos de prueba, normalizados como los guarda PhoneNormalizer (55 + DDD + número).
CREATE TEMP TABLE _tel_prueba (tel_norm TEXT);
INSERT INTO _tel_prueba VALUES ('5555900000001'), ('5555900000002'), ('5555900000003'),
                               ('5555900000004'), ('5555900000005');

-- Una sola definición de "qué es prueba" por tabla, reusada por vista previa y borrado.
CREATE TEMP VIEW _orders_prueba AS
SELECT o.id FROM orders o
WHERE o.cliente_nome LIKE 'ZZ-TEST%'
   OR o.cliente_telefone_norm IN (SELECT tel_norm FROM _tel_prueba)
   -- Anonimizadas por la prueba LGPD: perdieron nombre y teléfono, así que se
   -- reconocen solo por la ventana de tiempo. REVISAR en la vista previa.
   OR (o.anonymized_at >= (SELECT t0 FROM _cfg));

CREATE TEMP VIEW _appointments_prueba AS
SELECT a.id FROM appointments a
WHERE a.cliente_nome LIKE 'ZZ-TEST%'
   OR a.pet_nome LIKE 'ZZ-TEST%'
   OR a.cliente_telefone_norm IN (SELECT tel_norm FROM _tel_prueba)
   OR (a.anonymized_at >= (SELECT t0 FROM _cfg));

-- ────────────────────────────────────────────────────────────────────────────
-- PASO 1 — VISTA PREVIA (solo lectura). Revisar fila por fila antes de seguir.
-- ────────────────────────────────────────────────────────────────────────────

SELECT 'orders' AS tabla, o.public_id, o.created_at, o.cliente_nome, o.cliente_telefone_norm, o.status, o.anonymized_at
FROM orders o WHERE o.id IN (SELECT id FROM _orders_prueba) ORDER BY o.created_at;

SELECT 'appointments' AS tabla, a.public_id, a.start_at, a.cliente_nome, a.pet_nome, a.cliente_telefone_norm, a.status, a.anonymized_at
FROM appointments a WHERE a.id IN (SELECT id FROM _appointments_prueba) ORDER BY a.created_at;

SELECT 'products' AS tabla, p.public_id, p.nome, p.slug, p.active, p.main_image_url
FROM products p WHERE p.nome LIKE 'ZZ-TEST%' ORDER BY p.created_at;

SELECT 'brands' AS tabla, b.id, b.nome FROM brands b WHERE b.nome LIKE 'ZZ-TEST%';

SELECT 'schedule_blocks' AS tabla, s.id, s.data_desde, s.data_hasta, s.motivo
FROM schedule_blocks s WHERE s.motivo LIKE 'ZZ-TEST%';

SELECT 'privacy_erasure_log' AS tabla, l.id, l.created_at, l.orders_anonymized, l.appointments_anonymized
FROM privacy_erasure_log l
WHERE l.telefone_hash IN (SELECT encode(sha256(convert_to(tel_norm, 'UTF8')), 'hex') FROM _tel_prueba)
  AND l.created_at >= (SELECT t0 FROM _cfg);

SELECT 'admin_password_reset_tokens' AS tabla, count(*) AS filas
FROM admin_password_reset_tokens WHERE created_at >= (SELECT t0 FROM _cfg);

-- 👉 Copiar de acá las main_image_url NO nulas: hay que borrar esos objetos en
--    Cloudflare R2 a mano (la BD no los borra). Ver checklist §7.

-- ⚠️  ¿Aparece algo que NO sea tuyo en las listas de arriba (un pedido real, un
--     cliente real "Titular removido")? → ROLLBACK; y ajustar t0 o los criterios.

-- ────────────────────────────────────────────────────────────────────────────
-- PASO 2 — BORRADO. Orden importa:
--   · orders → cascada a order_items (order_items.product_id es RESTRICT hacia
--     products, por eso los pedidos se borran ANTES que los productos).
--   · appointments → cascada a appointment_addons.
--   · products → cascada a product_variants / product_categories / product_species.
-- ────────────────────────────────────────────────────────────────────────────

DELETE FROM orders       WHERE id IN (SELECT id FROM _orders_prueba);
DELETE FROM appointments WHERE id IN (SELECT id FROM _appointments_prueba);

DELETE FROM products WHERE nome LIKE 'ZZ-TEST%';
DELETE FROM brands   WHERE nome LIKE 'ZZ-TEST%';

DELETE FROM schedule_blocks WHERE motivo LIKE 'ZZ-TEST%';

DELETE FROM privacy_erasure_log
WHERE telefone_hash IN (SELECT encode(sha256(convert_to(tel_norm, 'UTF8')), 'hex') FROM _tel_prueba)
  AND created_at >= (SELECT t0 FROM _cfg);

-- Tokens de reset generados durante la prueba (son del admin real; se vuelven a
-- generar cuando haga falta). Sacar esta línea si no probaste el reset.
DELETE FROM admin_password_reset_tokens WHERE created_at >= (SELECT t0 FROM _cfg);

-- ────────────────────────────────────────────────────────────────────────────
-- PASO 3 — VERIFICACIÓN. Todo debe dar 0 (columna "restantes").
-- ────────────────────────────────────────────────────────────────────────────

SELECT 'orders'          AS tabla, count(*) AS restantes FROM orders       WHERE id IN (SELECT id FROM _orders_prueba)
UNION ALL SELECT 'appointments',    count(*) FROM appointments WHERE id IN (SELECT id FROM _appointments_prueba)
UNION ALL SELECT 'products',        count(*) FROM products     WHERE nome LIKE 'ZZ-TEST%'
UNION ALL SELECT 'brands',          count(*) FROM brands       WHERE nome LIKE 'ZZ-TEST%'
UNION ALL SELECT 'schedule_blocks', count(*) FROM schedule_blocks WHERE motivo LIKE 'ZZ-TEST%';

-- Sanidad: lo real sigue ahí. Comparar con lo anotado antes de la prueba (checklist §0).
SELECT (SELECT count(*) FROM orders)       AS orders_total,
       (SELECT count(*) FROM appointments) AS appointments_total,
       (SELECT count(*) FROM products)     AS products_total;

-- ════════════════════════════════════════════════════════════════════════════
-- La transacción sigue ABIERTA. Elegir UNA:
--   COMMIT;      ← si los conteos cuadran y no hay nada ajeno en la vista previa
--   ROLLBACK;    ← ante cualquier duda (no se borra nada; se puede reintentar)
-- ════════════════════════════════════════════════════════════════════════════
