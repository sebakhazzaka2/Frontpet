# ADR 020 — Algoritmo de cálculo de disponibilidad (Sprint 5)

**Estado**: Aceptada
**Fecha**: 2026-07-29
**Sprint**: 5 (tarea 5.3 exige explícitamente este plan en prosa antes de codear)

---

## Contexto

El ADR 005 fijó **slots dinámicos, no materializados** como estrategia, con un modelo de datos
(`services`, `resources`, `schedule_rules`) que quedó reemplazado por el esquema real de
`V3__booking.sql` (`services`+`service_pricing` por porte, `business_hours`, `schedule_blocks`,
capacidade plana del ADR 009). Antes de escribir `AvailabilityServiceImpl` faltan fijar cinco
decisiones concretas que el ADR 005 no resuelve con el esquema actual:

1. Granularidad de la grilla de horarios candidatos.
2. Cómo se calcula la duración total de un turno (base + adicionais por porte, ADR 011) y qué
   pasa con `tempo_extra` (+20%).
3. La query exacta de solapamiento contra `appointments`.
4. El mecanismo de concurrencia para el último cupo quedaba abierto en ADR 013 ("capa app").
5. Qué forma tiene la respuesta del endpoint público — y qué motivo de indisponibilidad expone.

---

## Decisión

### 1. Granularidad de grilla: 30 minutos

El ADR 005 proponía 30 min; el repo consultorio origen usaba 15. Se fija **30 min**. El
servicio más corto del seed (`Banho Essencial`, porte P) dura 45 min — una grilla de 15 min no
habilita ninguna combinación nueva, solo cuadruplica las opciones que el wizard mobile de
Sprint 6 tiene que renderizar en una grilla de 4 columnas. `GRID_MINUTES = 30` es una constante
en `SlotGrid`, no un valor de configuración por tenant: cambiarlo es una decisión de producto,
no una variable operativa que el admin deba tocar.

### 2. Duración total y `tempo_extra`

```
duracaoBase  = duration(banho_base, porte) + Σ duration(adicionais, porte)
duracaoTotal = tempo_extra ? round(duracaoBase * 1.2) : duracaoBase
```

Confirma el ADR 011: `tempo_extra` afecta **solo duración**, nunca `total_price_snapshot`.

**El resultado no se alinea a la grilla.** La grilla de 30 min gobierna los horarios de
*inicio* ofrecidos al cliente, no los de fin. Un turno que arranca 10:30 y dura 108 min
(90 × 1.2) termina 12:18 exactos — redondear el fin a 12:30 regalaría 12 minutos de agenda
por cada turno con `tempo_extra`. El `end_at` persistido siempre es la suma real en minutos.

### 3. Query de solapamiento: rango semiabierto

```sql
WHERE tenant_id = :t
  AND status <> 'CANCELLED'
  AND start_at >= :lowerGuard   -- inicio del día consultado menos 1 día
  AND start_at <  :windowEnd    -- fin de la ventana consultada
  AND end_at   >  :windowStart
```

`start_at < fin AND end_at > inicio` es el test estándar de solapamiento de intervalos
semiabiertos, y sale directo de tener `end_at` persistido (ADR 013 §10) — reemplaza el hack
`inicio.minusHours(3)` del repo consultorio (que asumía ningún turno de más de 3h). `lowerGuard`
no es necesario para la corrección (`start_at < windowEnd` ya lo cubriría) pero sí para el plan
de ejecución: sin una cota inferior explícita sobre `start_at`, el índice parcial
`idx_appointments_tenant_start` degenera en escanear toda la historia del tenant en vez de solo
el rango relevante. Se fija en "inicio del día consultado menos 1 día" para cubrir turnos que
empezaron el día anterior y cruzan la medianoche en horarios extendidos futuros — hoy no ocurre
(cierre máximo 19:00), pero la cota barata evita un bug latente si el horario cambia.

Régimen de ocupación de cupo: `PENDING` y `CONFIRMED` ocupan, `CANCELLED` libera (ya fijado en
ADR 013 §9, el índice parcial ya lo codifica — este ADR solo lo hereda, no lo redecide).

### 4. Concurrencia: `pg_advisory_xact_lock`, no `SELECT FOR UPDATE`

En `AppointmentService.create()`, justo antes de recontar los solapados, se toma
`pg_advisory_xact_lock(namespace, dayKey)` con granularidad **tenant + día**. Se descartan las
alternativas obvias:

- **`SELECT ... FOR UPDATE`** bloquea filas *existentes*, pero no impide el `INSERT` concurrente
  de otro thread — y el caso peligroso es justo cuando hay 0 o 1 filas todavía.
- **`UNIQUE`/`EXCLUDE` constraint** sobre `(tenant_id, start_at)` enforzaría capacidad 1. Con
  capacidad 2 (ADR 009) habría que materializar "asientos" numerados, que es volver a
  materializar slots — contra el ADR 005. El fix que el propio ADR 005 proponía
  (`UNIQUE(resource_id, start_at)`) queda así explícitamente incompatible con este modelo y se
  marca para actualizar ahí.
- **`SERIALIZABLE`** sería correcto, pero exige lógica de reintento sobre el error `40001` para
  un volumen de ~5 reservas/día — complejidad sin payoff medible.

`pg_advisory_xact_lock` se libera solo al hacer commit o rollback de la transacción: no hay
`unlock()` que olvidar ni fuga si salta una excepción a mitad de camino. La granularidad
tenant+día significa que colisiones de `dayKey` solo serializan de más (dos reservas del mismo
día esperan su turno), nunca producen un resultado incorrecto.

### 5. Zona horaria: `America/Sao_Paulo` explícito, nunca system default

`business_hours` guarda `TIME` local (naive), `appointments` guarda `TIMESTAMPTZ`. Todo el
algoritmo opera con `ZonedDateTime.of(..., ZoneId.of("America/Sao_Paulo"))` — nunca
`ZoneId.systemDefault()`, porque la JVM del contenedor corre en UTC y ese default silenciosamente
correcto en dev rompería en producción. `ZoneId.of(...)` como constante hoy; si el modelo
multi-tenant real llega a tener tenants en otro huso, se mueve a `tenant.config`, no antes.

### 6. Forma de la respuesta: la grilla completa, con motivo de indisponibilidad

`GET /api/v1/availability` devuelve **todos** los slots candidatos del día, cada uno con
`disponivel: boolean`, no solo los libres — el wizard de Sprint 6 necesita distinguir "ocupado"
de "fuera de horario" para no mostrarlos igual. Cuando el día entero no ofrece nada, la respuesta
expone un motivo, no una lista vacía muda:

| Motivo | Causa |
|---|---|
| `DIA_INATIVO` | `business_hours.activo = false` para ese `dia_semana` (ej. domingo) |
| `BLOQUEADO` | la fecha cae dentro de un `schedule_blocks` activo |
| `FORA_DA_JANELA` | fecha en el pasado, o más allá de `anticipacao_max_dias` (60, seed actual) |

Los slots que caen dentro de la pausa (`pausa_inicio`/`pausa_fin`, hoy NULL para FrontPet) y los
que violan `anticipacao_min_horas` se descartan de la grilla candidata antes de evaluarlos contra
ocupación — no se listan como `disponivel: false` por capacidad, porque no es esa la causa.

### Pasos del algoritmo (`AvailabilityServiceImpl.getAvailability`)

Cinco queries, todas por índice, ninguna proporcional al tamaño de la grilla:

1. `TenantSettingsService.booking(tenantId)` → capacidad, anticipación mín/máx.
2. Corte temprano si la fecha pedida cae fuera de la ventana de anticipación → `FORA_DA_JANELA`.
3. Precio y duración del combo (base + Σ adicionais, por porte) en una query con join.
4. `business_hours` por `dia_semana` ISO (`data.getDayOfWeek().getValue()`, sin `%7`) → corte si
   `activo = false` → `DIA_INATIVO`.
5. `schedule_blocks` para la fecha → corte si hay uno activo → `BLOQUEADO`.
6. Generar candidatos en memoria: desde `abertura`, paso de `GRID_MINUTES`, mientras
   `start + duracaoTotal <= fechamento`.
7. Descartar los que intersectan la pausa y los que violan `anticipacao_min_horas`.
8. Una única query de intervalos ocupados del día (la del punto 3 de esta sección) + conteo de
   capacidad en memoria por slot candidato.

---

## Alternativas consideradas

### ❌ Grilla de 15 minutos
Descartada por el punto 1: no habilita ninguna combinación real con las duraciones actuales
(mínimo 45 min) y cuadruplica el trabajo de render del wizard mobile.

### ❌ Alinear `round(duracaoBase * 1.2)` a la grilla de 30 min
Descartada por el punto 2: regala minutos de agenda que la capacidad real del negocio no tiene.

### ❌ `SELECT ... FOR UPDATE` para el último cupo
Descartada por el punto 4: no cubre el caso de 0-1 filas existentes, que es exactamente el caso
peligroso de una carrera por el último cupo.

### ❌ Respuesta con solo los slots libres (sin motivo de indisponibilidad)
Descartada por el punto 6: un día sin nada disponible es ambiguo para el usuario ("¿está cerrado,
bloqueado, o simplemente lleno?") sin un motivo explícito, y Sprint 6 necesita ese dato para el
copy del wizard.

---

## Consecuencias

### Positivas
- El query de solapamiento no requiere ninguna tabla ni columna nueva — reutiliza `end_at`
  (ADR 013 §10) y el índice parcial ya existente.
- El lock de concurrencia no agrega dependencias (nativo de Postgres) ni tablas de "asientos".
- La respuesta con motivo de indisponibilidad evita que Sprint 6 tenga que inferir la causa
  desde una lista vacía.

### Negativas / a vigilar
- **`schedule_blocks` es por rango de días, no de horas.** El ADR 012 pide que el admin bloquee
  los *horarios* que se ocupan por otros canales (ERP, teléfono, walk-in); con el esquema actual
  solo puede bloquear el día completo. La solución correcta — que el admin cargue un turno manual
  que ocupe cupo sin pasar por el wizard público — queda **diferida a Sprint 6** (decisión de
  planificación, no de este ADR). Hasta entonces, la disponibilidad online puede sobre-ofertar
  horarios puntuales si el cliente ya está operando por otros canales el mismo día.
- El test de concurrencia (6 threads contra 2 cupos) es el más frágil del sprint: exige commits
  reales (sin `@Transactional` en el test) y limpieza manual de datos para no contaminar otras
  clases que comparten el contenedor Testcontainers singleton.
- `ZoneId.of("America/Sao_Paulo")` fijo en código: si el negocio multi-tenant real cruza husos,
  se mueve a `tenant.config` — no antes, no hay evidencia de esa necesidad hoy.

---

## Notas para el futuro

- El ADR 005 debe actualizarse: su modelo de datos (`resources`, `schedule_rules`) no es el que
  terminó implementado, dice "appointments confirmadas" cuando la regla real es
  PENDING+CONFIRMED (ADR 013 §9), y su mitigación de concurrencia sugerida
  (`UNIQUE(resource_id, start_at)`) es incompatible con capacidad > 1. Se marca para el cierre
  de este sprint, no bloquea el bloque 0.
- Cuando el turno manual del admin exista (Sprint 6), reevaluar si `schedule_blocks` sigue
  siendo necesario para bloqueos de día completo (feriados) o si todo bloqueo puntual migra a
  turnos manuales sin servicio asociado.
