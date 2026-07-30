# ADR 005 — Cálculo dinámico de slots de booking

**Fecha**: 2026-05-17
**Estado**: Aceptada — modelo de datos y solución de race condition actualizados, ver Actualización 2026-07-30
**Autor**: [tu nombre]

---

## Actualización 2026-07-30 (post-implementación, Sprint 5)

Este ADR se escribió **antes** de implementar el módulo (Sprint 2), cuando el modelo de datos
y la solución de concurrencia eran todavía hipótesis. Implementado en Sprint 5, dos partes
quedaron desactualizadas — el resto sigue vigente tal como está.

**Lo que SIGUE VIGENTE** (la decisión central de este ADR, sin cambios):
- **Slots dinámicos, nunca materializados.** El algoritmo real vive en `SlotGrid` +
  `AvailabilityServiceImpl`, documentado en detalle en **[ADR 020](./020-algoritmo-slots.md)**.
- Los pasos 1-6 de la sección "Decisión" siguen siendo la forma correcta de pensar el problema;
  solo cambian los nombres de tabla y algunos detalles (ver abajo).

**Lo que quedó OBSOLETO**:

1. **Modelo de datos** (líneas de la sección "Decisión" más abajo). El esquema real,
   implementado en `V3__booking.sql` (ver **ADR 011** y **ADR 013**), difiere en tres puntos:
   - No hay tablas `resources` ni `schedule_rules`. `resources` no existe — capacidade es un
     número plano en `tenant.config` (ADR 009), no profissionais nominales. `schedule_rules` se
     llama `business_hours` (una fila por `tenant_id + dia_semana`, con pausa opcional).
   - `services` no tiene `duration_minutes` propio: la duración sale de `service_pricing`
     (precio + duración por porte P/M/G/GG), y un turno es `1 base + N adicionais` (ADR 011)
     — la duración total es una suma, no un campo.
   - `appointments` no tiene `resource_id` ni `customer_id`: los datos del cliente van
     directo en la fila (`cliente_nome`, `cliente_telefone`, etc., sin tabla de clientes —
     MVP1 no tiene auth de clientes finales).

2. **Paso 5 ("filtrar slots que se solapen con `appointments` ya confirmadas")**. La regla real
   de ocupación es **`PENDING` y `CONFIRMED` ocupan cupo, `CANCELLED` lo libera** — no solo
   "confirmadas" (corregido en **ADR 013 §9**; el repo consultorio del que se portó el algoritmo
   era inconsistente en esto).

3. **La solución de race condition** (sección "Casos borde explícitos a testear", último ítem).
   Proponía `UNIQUE (resource_id, start_at)` — **incompatible con capacidad > 1**: esa constraint
   solo sirve para capacidad 1 (un solo turno por slot). Con capacidade 2 (ADR 009), la solución
   real es **`pg_advisory_xact_lock(tenant, día)`** dentro de la transacción de creación,
   recontando los solapados antes de insertar. Detalle completo, incluyendo por qué se descartó
   `SELECT FOR UPDATE` y `SERIALIZABLE`, en **ADR 020 §4**.

---

## Contexto

El módulo de booking necesita responder a la pregunta: **"¿qué horarios están
disponibles para el servicio X en la fecha Y?"**.

Hay dos formas estándar de modelar esto:

1. **Slots materializados**: pre-generar todos los slots posibles en una tabla
   (ej: una fila por cada slot de 30 min de los próximos 60 días). Marcar como
   ocupados los que tienen reserva.
2. **Slots dinámicos**: calcular en cada consulta los slots disponibles a partir
   de las reglas de horario, la duración del servicio y las reservas existentes.

## Decisión

**Slots dinámicos**. No se materializan en base de datos.

Modelo de datos (⚠️ **hipótesis inicial, ver "Actualización 2026-07-30" arriba** — el esquema
real difiere en nombres de tabla y en cómo se calcula la duración):

```
services
  - id, name, duration_minutes, tenant_id

resources              -- ej: peluquero, sala de baños
  - id, name, tenant_id

schedule_rules         -- ej: "lunes a viernes 9-18"
  - id, resource_id, day_of_week, start_time, end_time

schedule_blocks        -- ej: feriados, vacaciones, bloqueos puntuales
  - id, resource_id, blocked_from, blocked_to, reason

appointments           -- las reservas reales
  - id, service_id, resource_id, customer_id, start_at, end_at, status
```

Cuando se consulta `GET /api/v1/availability?service=X&date=Y`:

1. Buscar `services` para conocer la `duration_minutes`
2. Buscar `schedule_rules` para el `day_of_week` de la fecha consultada
3. Generar los slots candidatos del día según las reglas (ej: cada 30 min desde 9 hasta 18)
4. Filtrar slots que caigan dentro de algún `schedule_block` activo
5. Filtrar slots que se solapen con `appointments` ya confirmadas
6. Retornar los disponibles

## Alternativas consideradas

### ❌ Slots materializados

**A favor**:
- Query simple: `SELECT * FROM slots WHERE date = X AND status = 'AVAILABLE'`
- Performance lineal con la cantidad de slots

**En contra**:
- **Explota la base de datos**: 5 recursos × 16 slots/día × 60 días = 4.800 filas por mes nuevas
- **Reagendamientos complejos**: cambiar un servicio a otro día requiere updates en cascada
- **Bloqueos puntuales** (vacaciones, feriados) requieren borrar filas
- **Cambios de horario** (ej: "ahora abrimos hasta las 19") requieren regenerar todo
- Difícil manejar servicios de duración variable
- Hay que mantener un proceso que pre-genere slots futuros

### ❌ Híbrido (materializar solo los próximos 30 días)

**A favor**: Limita el blowup de la DB.

**En contra**:
- Combina los problemas de ambos enfoques
- Lógica de transición entre slots materializados y no materializados confusa

### ❌ Servicio externo de booking (Cal.com, Calendly API)

**A favor**: Outsourcing del problema.

**En contra**:
- Costo recurrente
- Mala integración con el panel admin propio
- Dependencia externa para una pieza core del negocio

## Consecuencias

### Positivas
- Modelo de datos limpio y normalizado
- Cambios de horario se reflejan inmediatamente
- Sin proceso de mantenimiento de slots futuros
- Bloqueos puntuales son simples (INSERT en `schedule_blocks`)
- Servicios de distinta duración se manejan naturalmente
- Reagendamientos: solo update del `appointment`

### Negativas
- Cada consulta de disponibilidad requiere computación
- Query más complejo de implementar
- Casos borde a manejar con cuidado (servicios que cruzan dos slots,
  bloqueos parciales, etc.)

### Mitigaciones
- Cache de respuestas a nivel HTTP (Cache-Control con revalidation)
  para fechas en el futuro lejano que cambian poco
- Tests exhaustivos del query con casos borde antes de pasar a producción
- Limitar la ventana consultable (ej: máximo 30 días en el futuro)
  para evitar consultas masivas

## Casos borde explícitos a testear

- **Servicio de 90 min en slot de 60 min**: debe consumir 2 slots consecutivos
- **Reserva existente que termina en medio de un slot candidato**: el slot no está disponible
- **Bloqueo parcial del día** (ej: 14-15 hs feriado parcial): solo afecta slots dentro del rango
- **Cambio de regla de horario**: las reservas existentes anteriores al cambio no se afectan
- **Race condition**: dos clientes reservando el mismo slot simultáneamente — ⚠️ la solución
  propuesta acá (`UNIQUE (resource_id, start_at)` + `SELECT FOR UPDATE`) quedó **obsoleta**,
  ver "Actualización 2026-07-30": incompatible con capacidad > 1. Solución real:
  `pg_advisory_xact_lock` (ADR 020 §4)

## Notas para el futuro

Si en el futuro se requieren consultas masivas (ej: "disponibilidad de todos los
servicios para los próximos 3 meses para mostrar en un calendario"):

- Considerar cache distribuido (Redis) con invalidación al crear/cancelar appointments
- Considerar materialización selectiva solo del primer slot disponible por día
- **No volver atrás a slots materializados** salvo evidencia clara de necesidad
