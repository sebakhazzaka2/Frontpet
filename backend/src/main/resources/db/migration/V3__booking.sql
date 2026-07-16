-- =====================================================================
-- V3 — Módulo BOOKING: servicios, tarifas por porte, horarios, bloqueos, turnos
-- =====================================================================
-- Decisiones de modelado (ver ADR 011, 012, 013):
--   · Servicio único con type (BASE|ADDON). service_pricing resuelve
--     precio+duración por porte (P/M/G/GG). Un turno = 1 base + N adicionais.
--   · La web es la autoridad de disponibilidad (ADR 012): estos datos
--     (business_hours + schedule_blocks + appointments no-canceladas) alimentan
--     el cálculo dinámico de slots (ADR 005). No se materializan slots.
--   · Regla ÚNICA de ocupación: PENDING y CONFIRMED ocupan cupo; CANCELLED lo
--     libera. (El repo consultorio era inconsistente en esto — ver ADR 013.)
--   · business_hours toma la estructura probada del repo consultorio:
--     activo por día + pausa opcional. FrontPet no cierra al mediodía → pausa
--     queda NULL, pero el algoritmo reusado ya la maneja (ver ADR 013).
--   · dia_semana en convención ISO-8601: 1=Lun .. 7=Dom (= DayOfWeek.getValue()
--     de java.time, sin el mapeo %7 frágil del origen).
-- =====================================================================

-- Enums modelados como VARCHAR + CHECK (no como TYPE ENUM nativo de Postgres):
-- agregar/quitar valores a un ENUM nativo requiere ALTER TYPE y es rígido;
-- un CHECK se evoluciona con un simple ALTER ... DROP/ADD CONSTRAINT.

CREATE TABLE services (
    id          BIGSERIAL      PRIMARY KEY,
    tenant_id   UUID           NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    type        VARCHAR(10)    NOT NULL CHECK (type IN ('BASE','ADDON')),
    nome        VARCHAR(120)   NOT NULL,
    descricao   TEXT,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,       -- soft-delete: se desactiva, no se borra
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Tarifa por porte. Estructura por filas (no columnas price_p/price_m/...) para
-- poder agregar en el futuro coat_type sin ALTER de columnas (ADR 011).
CREATE TABLE service_pricing (
    id                BIGSERIAL      PRIMARY KEY,
    service_id        BIGINT         NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    size              VARCHAR(2)     NOT NULL CHECK (size IN ('P','M','G','GG')),
    price             NUMERIC(10,2)  NOT NULL CHECK (price >= 0),
    duration_minutes  INT            NOT NULL CHECK (duration_minutes > 0),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_service_pricing_service_size UNIQUE (service_id, size)
);
-- La UNIQUE(service_id, size) ya sirve para buscar tarifas por service_id
-- (columna líder), así que no agregamos índice extra sobre service_id.

-- Horario de atención del booking (grooming). Una fila por (tenant, día).
CREATE TABLE business_hours (
    id             BIGSERIAL     PRIMARY KEY,
    tenant_id      UUID          NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    dia_semana     SMALLINT      NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),  -- 1=Lun .. 7=Dom (ISO-8601)
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,     -- día laborable sí/no (toggle del admin)
    abertura       TIME          NOT NULL,
    fechamento     TIME          NOT NULL,
    pausa_inicio   TIME,                                    -- NULLABLE (FrontPet no usa pausa)
    pausa_fin      TIME,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_business_hours_tenant_dia UNIQUE (tenant_id, dia_semana),
    CONSTRAINT chk_business_hours_rango CHECK (abertura < fechamento),
    -- Si hay pausa, ambos extremos y coherentes/dentro del horario.
    CONSTRAINT chk_business_hours_pausa CHECK (
        (pausa_inicio IS NULL AND pausa_fin IS NULL)
        OR (pausa_inicio IS NOT NULL AND pausa_fin IS NOT NULL
            AND pausa_inicio < pausa_fin
            AND pausa_inicio >= abertura AND pausa_fin <= fechamento)
    )
);

-- Bloqueos: feriados, vacaciones y — clave en ADR 012 — cupos ocupados por
-- otros canales (teléfono/local/ERP) que el admin bloquea a mano.
CREATE TABLE schedule_blocks (
    id          BIGSERIAL     PRIMARY KEY,
    tenant_id   UUID          NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    data_desde  DATE          NOT NULL,
    data_hasta  DATE          NOT NULL,
    motivo      VARCHAR(200),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_schedule_blocks_rango CHECK (data_desde <= data_hasta)
);
-- El cálculo de disponibilidad pregunta "¿fecha X bloqueada para el tenant?"
-- en cada consulta → índice por (tenant_id, data_desde).
CREATE INDEX idx_schedule_blocks_tenant_desde ON schedule_blocks (tenant_id, data_desde);

CREATE TABLE appointments (
    id                    BIGSERIAL      PRIMARY KEY,
    public_id             UUID           NOT NULL,          -- UUID v7 (app); código de reserva público
    tenant_id             UUID           NOT NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    base_service_id       BIGINT         NOT NULL REFERENCES services(id) ON DELETE RESTRICT,  -- debe ser type=BASE (validado en app)
    size                  VARCHAR(2)     NOT NULL CHECK (size IN ('P','M','G','GG')),
    start_at              TIMESTAMPTZ    NOT NULL,
    end_at                TIMESTAMPTZ    NOT NULL,           -- = start + duración total (base + adicionais). Evita el hack minusHours(3) del origen
    status                VARCHAR(12)    NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING','CONFIRMED','CANCELLED')),
    cliente_nome          VARCHAR(160)   NOT NULL,
    cliente_telefone      VARCHAR(30)    NOT NULL,           -- tal como lo escribió el cliente
    cliente_telefone_norm VARCHAR(20)    NOT NULL,           -- normalizado (solo dígitos + país) para agrupar en Fase 2
    pet_nome              VARCHAR(80)    NOT NULL,
    pet_raca              VARCHAR(80),                       -- opcional
    base_price_snapshot   NUMERIC(10,2)  NOT NULL,           -- precio del baño base congelado
    total_price_snapshot  NUMERIC(10,2)  NOT NULL,           -- base + adicionais, denormalizado para la lista de turnos
    total_duration_minutes INT           NOT NULL CHECK (total_duration_minutes > 0),
    observacoes           TEXT,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    confirmed_at          TIMESTAMPTZ,
    cancelled_at          TIMESTAMPTZ,
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_appointments_public_id UNIQUE (public_id),
    CONSTRAINT chk_appointments_rango CHECK (end_at > start_at)
);

-- Corazón del cálculo de slots (ADR 005): "turnos que solapan la fecha X".
-- Parcial WHERE status <> 'CANCELLED' porque los cancelados NO ocupan cupo.
CREATE INDEX idx_appointments_tenant_start
    ON appointments (tenant_id, start_at)
    WHERE status <> 'CANCELLED';
-- FK para joins de display (turno → servicio base).
CREATE INDEX idx_appointments_base_service ON appointments (base_service_id);
-- NOTA: NO indexamos cliente_telefone_norm — la única query que lo usaría
-- ("clientes recurrentes") es Fase 2. Guardamos la columna, no el índice.

-- Adicionais del turno (N:M). Snapshot por línea para preservar el desglose
-- histórico aunque cambien las tarifas (ADR 013, decisión 2).
CREATE TABLE appointment_addons (
    appointment_id     BIGINT         NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    service_id         BIGINT         NOT NULL REFERENCES services(id) ON DELETE RESTRICT,  -- debe ser type=ADDON (validado en app)
    price_snapshot     NUMERIC(10,2)  NOT NULL,
    duration_snapshot  INT            NOT NULL CHECK (duration_snapshot > 0),
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT pk_appointment_addons PRIMARY KEY (appointment_id, service_id)
);
-- La PK lidera con appointment_id; este índice sirve la FK hacia services.
CREATE INDEX idx_appointment_addons_service ON appointment_addons (service_id);
