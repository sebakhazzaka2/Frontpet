# ADR 013 — Modelo de datos MVP1: decisiones de esquema y reutilización

**Estado**: Aceptada
**Fecha**: 2026-07-13
**Sprint**: 2
**Migrations**: `V1__identity`, `V2__catalog`, `V3__booking`, `V4__orders`

---

## Contexto

Con las respuestas del cliente cerradas y los ADR 011 (servicios) y 012 (booking sin
ERP) definidos, se diseñó el esquema completo de la DB MVP1. Este ADR documenta las
decisiones **transversales de modelado** que no pertenecen a un módulo puntual, para no
perderlas. Se tomaron una por una, con alternativas y justificación.

El diseño además reutiliza el modelo de agenda del repo previo `consultorio-odontologico`
(Java + MySQL). Se documenta qué se adoptó y qué no.

---

## Decisiones de esquema

### 1. `tenant_id` es UUID, y `tenants.id` es UUID v7 (PK)

El tenant es la clave transversal del sistema (viaja en el JWT, filtra toda query). Se
modela como **UUID v7 PK**, no BIGSERIAL. El costo de 16 vs 8 bytes en índices es
irrelevante a esta escala, UUID v7 es time-ordered (buena localidad), y evita exponer IDs
secuenciales de tenant en un futuro multi-tenant. La regla "BIGSERIAL para internas" del
CLAUDE.md aplica a filas de alto volumen (order_items, etc.), no al tenant.

El resto de entidades: **BIGSERIAL** interno + **`public_id` UUID v7** solo donde se
expone públicamente (`products`, `appointments`, `orders`).

### 2. Variantes: precio/stock en `products` o en `product_variants`

Producto simple → precio/stock en `products`. Producto con variantes (rações 3/10/15/20kg,
tapetes) → la variante manda. **No** se fuerza una "variante default" para productos
simples (ensuciaría la tabla y el ABM). `order_items` referencia product (siempre) +
variant (opcional).

### 3. Integridad variante-producto vía FK compuesta

`product_variants` lleva `UNIQUE(product_id, id)` y `order_items` una FK compuesta
`(product_id, product_variant_id) → product_variants(product_id, id)`. Garantiza a nivel DB
que la variante elegida pertenece al producto. Con `product_variant_id` NULL, Postgres
(MATCH SIMPLE) omite el chequeo y el `product_id` sigue validado por su FK simple.
`product_id` **siempre presente** habilita la métrica "top productos" agrupando sin joins.

### 4. Cliente embebido, sin tabla `customer`

MVP1 no tiene auth de clientes. Se guarda `cliente_nome` + `cliente_telefone` +
`cliente_telefone_norm` (normalizado) embebidos en `orders` y `appointments`. Una tabla
`customer` deduplicada por teléfono sería generalización prematura (CLAUDE.md §6): dedup
frágil y sin consumidor real en MVP1. Guardar el teléfono normalizado preserva la data para
construir "clientes recurrentes" en Fase 2 con un backfill, sin la tabla hoy.

### 5. Snapshots de precio/nombre

`order_items` congela `nome_snapshot` + `unit_price_snapshot`. `appointments` congela
`base_price_snapshot` + `total_price_snapshot`, y `appointment_addons` guarda snapshot
**por línea** (`price_snapshot`, `duration_snapshot`). Preserva el desglose histórico aunque
cambien las tarifas. El total en `appointments` se denormaliza (una vez, inmutable) para que
la lista de turnos no haga JOIN+SUM por fila.

### 6. Enums como VARCHAR + CHECK (no `TYPE ENUM` nativo)

`type`, `size`, `status`, `frete_mode`: VARCHAR + CHECK. Los ENUM nativos de Postgres son
rígidos de evolucionar (ALTER TYPE); un CHECK se cambia con DROP/ADD CONSTRAINT.

### 7. Timestamps: `DEFAULT now()` + Hibernate, sin triggers

MySQL tiene `ON UPDATE CURRENT_TIMESTAMP`; **Postgres no**. En vez de triggers, se usa el
mismo patrón que el repo consultorio: DB con `DEFAULT now()` (cubre inserts y seeds) +
Hibernate `@CreationTimestamp`/`@UpdateTimestamp` a nivel entidad para bumpear en updates.
Más simple y consistente con el código reusado.

### 8. `business_hours`: estructura probada + convención de día

Se adopta la estructura de `disponibilidad_semanal` del repo consultorio: una fila por
`(tenant, dia_semana)`, con `activo` (toggle del admin) y `pausa_inicio`/`pausa_fin`
NULLABLE. **No es anticipación**: el algoritmo de slots reusado ya maneja la pausa; sacar
las columnas obligaría a modificar el algoritmo y re-agregarlas después. FrontPet no cierra
al mediodía → pausa en NULL.

`dia_semana` en **ISO-8601 (1=Lun..7=Dom)** = `DayOfWeek.getValue()` de `java.time` directo,
eliminando el mapeo `%7` frágil del origen.

### 9. Regla única de ocupación de cupo

**PENDING y CONFIRMED ocupan cupo; CANCELLED lo libera.** El origen era inconsistente
(availability contaba PENDIENTE+CONFIRMADA, pero el guard de creación solo CONFIRMADA). El
índice parcial `idx_appointments_tenant_start WHERE status <> 'CANCELLED'` codifica la regla.

### 10. `end_at` explícito en `appointments`

Se guarda `end_at` (= start + duración total), no solo la duración. Habilita una query de
solapamiento limpia por rango y elimina el hack `inicio.minusHours(3)` del origen (que asumía
que ningún turno dura más de 3h).

### 11. Política de índices

Índice solo si una query MVP1 lo usa a escala. Se indexan: listado de catálogo
`(tenant_id, active)`, filtro por marca (parcial), FKs de join a escala (variants→product,
order_items→order/product, appointment→base_service, addons→service), disponibilidad
`schedule_blocks(tenant_id, data_desde)` y el índice parcial de slots. Se **omiten**
deliberadamente: índice sobre `cliente_telefone_norm` (query de Fase 2) e índices sobre
tablas diminutas (`admin_users`, catálogo de servicios) donde el seq-scan es instantáneo.

### 12. Política de `ON DELETE`

- Composición real → `CASCADE`: product→variants, order→items, appointment→addons,
  service→pricing, product↔category/species.
- Referencias → `RESTRICT`: `tenant_id` en todo (evita borrado masivo accidental de un
  tenant), `base_service_id`/addon `service_id` (los servicios se soft-deletean con
  `active=false`), `order_items.product_id`.
- `products.brand_id` → `SET NULL` (borrar una marca no borra el producto).

---

## Mapa de reutilización desde `consultorio-odontologico`

> El detalle accionable archivo-por-archivo (qué copiar, qué adaptar, qué cambia exactamente)
> vive en **[docs/reuse-consultorio.md](../reuse-consultorio.md)**. Acá va el resumen.

| Del origen | En FrontPet | Estado |
|---|---|---|
| `disponibilidad_semanal` | `business_hours` (+ `tenant_id`, ISO día) | Adaptado |
| `fechas_bloqueadas` | `schedule_blocks` (+ `tenant_id`, rango) | Adaptado |
| `citas` | `appointments` (+ `tenant_id`, UUID, `end_at`, N:M adicionais, snapshots) | Adaptado |
| `User` (email/password/role) | `admin_users` | Directo (+ tenant_id) |
| Algoritmo `getDisponibilidad` | Blueprint del cálculo de slots | Capa app (no DB) |
| Seguridad JWT | Reusar con cambio Bearer→cookie HttpOnly | Capa app (no DB) |
| `Servicio` (precio/duración plano) | `services` + `service_pricing` por porte | **Rediseñado** (ADR 011) |
| `Paciente` (con dedup) | — (cliente embebido, sin login) | Descartado |
| `pagos`, `historial_clinico`, Google Calendar | — | Descartado (fuera de scope) |

### Hacks del origen que NO se copian
- `haySolapamiento()` con ventana `inicio.minusHours(3)` → reemplazado por query de rango
  limpia usando `end_at`.
- Mapeo `dia_semana` con `%7` → convención ISO explícita.
- **Sin control de concurrencia** en `create()` (race del "último cupo"): el origen no lo
  resuelve. FrontPet lo cubre con `SELECT FOR UPDATE` / advisory lock al codear el service
  de booking (capa app; el esquema ya lo soporta).

---

## Consecuencias

- Esquema listo para las 4 migrations Flyway; Hibernate corre con `ddl-auto: validate` (el
  esquema es fuente de verdad, las entidades solo validan).
- Falta un seed inicial (tenant FrontPet, servicios base + adicionais con tarifas por porte,
  business_hours de grooming, usuario admin). Va en una migration/inicializador posterior,
  cuando el cliente entregue precios y duraciones (pendiente conocido).
- El `docs/db-model.png` debe regenerarse a partir de este esquema (Definition of Done).
