-- =====================================================================
-- V12 — Reset de contraseña del admin (tarea 7.12, ADR 022)
-- =====================================================================
-- password_changed_at (en admin_users): permite invalidar sesiones JWT ya
-- emitidas cuando la contraseña cambia (JwtAuthFilter compara contra el
-- `iat` del token). Se backfillea con created_at, NO con now(): con now()
-- este deploy mataría toda sesión admin activa sin motivo — el punto es
-- invalidar sesiones futuras que sobrevivan a un reset real, no las de hoy.
--
-- admin_password_reset_tokens: tabla nueva, no un JWT de un solo uso. El uso
-- único exige estado consultable en DB de todos modos (hay que poder marcar
-- "ya usado"), así que un JWT no ahorraría la tabla — solo agregaría una
-- segunda fuente de verdad y extendería el radio de explosión de jwt.secret
-- de "sesiones de 1h" a "la cuenta permanentemente".
--
-- En Postgres y no en memoria (a diferencia de los rate limiters de
-- common.SlidingWindowLimiter, ver ADR 016/019): un rate limit perdido en un
-- redeploy es una molestia menor; un token de reset perdido en pleno vuelo
-- deja al admin afuera del panel sin forma de reintentar hasta que vuelva a
-- pedir uno.
--
-- token_hash guarda sha256(token) en hex, explícitamente NO BCrypt: BCrypt
-- tiene salt por fila (hash no determinístico, no se puede buscar por
-- índice — habría que traer todas las filas y correr matches() contra cada
-- una, ~100ms cada vez). Y el costo computacional de BCrypt existe para
-- compensar la baja entropía de una contraseña humana; acá el valor tiene
-- 256 bits de SecureRandom, no hay diccionario que ralentizar. Se hashea
-- igual (no se guarda el token en claro) para que un leak de solo-lectura de
-- la DB no permita reconstruir el link de reset.
-- =====================================================================

ALTER TABLE admin_users ADD COLUMN password_changed_at TIMESTAMPTZ;
UPDATE admin_users SET password_changed_at = created_at WHERE password_changed_at IS NULL;
ALTER TABLE admin_users
    ALTER COLUMN password_changed_at SET NOT NULL,
    ALTER COLUMN password_changed_at SET DEFAULT now();

COMMENT ON COLUMN admin_users.password_changed_at IS
    'Último cambio de contraseña. JwtAuthFilter rechaza cualquier JWT con iat anterior a este valor — invalida sesiones activas al resetear. Ver ADR 022.';

-- VARCHAR(64), no CHAR(64): CHAR en Postgres devuelve padding a la derecha y
-- el ahorro de espacio frente a VARCHAR es nulo.
--
-- Sin tenant_id: tabla hija de admin_users, llega al tenant por el FK del
-- padre (ADR 013). Sin public_id: el identificador externo YA ES el token
-- opaco, y justamente no queremos que sea enumerable con un UUID secuencial
-- ni expuesto en ninguna URL de listado.
CREATE TABLE admin_password_reset_tokens (
    id             BIGSERIAL      PRIMARY KEY,
    admin_user_id  BIGINT         NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    token_hash     VARCHAR(64)    NOT NULL,          -- sha256(token) en hex minúscula
    expires_at     TIMESTAMPTZ    NOT NULL,
    used_at        TIMESTAMPTZ,                      -- NULL = no consumido todavía
    requested_ip   VARCHAR(45),                      -- IPv6 completa entra en 45 chars
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_admin_password_reset_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_admin_password_reset_tokens_admin_user_id
    ON admin_password_reset_tokens (admin_user_id);

COMMENT ON TABLE admin_password_reset_tokens IS
    'Tokens de un solo uso para reset de contraseña del admin (tarea 7.12). Un pedido nuevo borra los anteriores del mismo admin — ver PasswordResetService.';
COMMENT ON COLUMN admin_password_reset_tokens.token_hash IS
    'sha256(token) en hex. El token en claro nunca se persiste, solo viaja en el link del email.';
COMMENT ON COLUMN admin_password_reset_tokens.used_at IS
    'NULL = todavía no consumido. Se setea en la misma transacción que el UPDATE de password_hash.';
COMMENT ON COLUMN admin_password_reset_tokens.requested_ip IS
    'IP que originó el pedido (auditoría). No participa en ninguna decisión de negocio.';
COMMENT ON INDEX idx_admin_password_reset_tokens_admin_user_id IS
    'Soporta el DELETE por admin_user_id que corre en cada pedido nuevo (invalida pendientes + limpia usados/expirados de una).';
