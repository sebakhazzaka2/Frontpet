# ADR 021 — Turno manual del admin: qué puede saltear

**Estado**: Aceptada
**Fecha**: 2026-07-30
**Sprint**: 6 (Bloque 0, tarea diferida desde el Registro de decisiones de plan del Sprint 5)

---

## Contexto

`schedule_blocks` (V3__booking.sql) solo bloquea el **día completo**, no un horario puntual.
El ADR 012 pedía poder bloquear franjas ocupadas por otros canales (teléfono, local) mientras
el negocio opera en paralelo a la web — el esquema real no lo permite (limitación conocida,
documentada en `docs/pending-decisions.md` §10 y en el ADR 020 §"Negativas / a vigilar").

La mitigación real, diferida a Sprint 6 al cerrar el Sprint 5 (2026-07-30), es que el admin
pueda cargar un turno manual (`POST /api/v1/admin/appointments`) que ocupe cupo real sin pasar
por el wizard público. Ese endpoint necesita una política explícita: ¿qué reglas del wizard
público le aplican al admin y cuáles no?

Sin esta decisión escrita, el Bloque A (backend, Sprint 6) no tiene contra qué validar.

---

## Decisión

**El turno manual del admin reusa el mismo cálculo de precio/duración server-side que el
turno público (`AvailabilityService#resolveCombo`, ADR 020), pero se le relajan tres
restricciones — todas con el mismo criterio que ya existe para `tempo_extra` (ADR 011:
"avisar, no bloquear").**

| Restricción del wizard público | Turno manual del admin |
|---|---|
| Horário debe estar en la grilla de 30 min (`SlotGrid`, ADR 020) | **No aplica.** El caso de uso típico es capturar un turno que ya existe por teléfono, en cualquier horário real |
| `409` si supera `capacidade_atendimento` | **Persiste igual.** Devuelve `aviso` en la response, mismo patrón que `PATCH .../tempo-extra` |
| `400` si el día está `BLOQUEADO` o fuera de `business_hours` | **Persiste igual.** Devuelve `aviso`. Un cliente fiel en un feriado es decisión de negocio, no algo que el software deba impedir |
| Rate limit (`AppointmentRateLimitFilter`, tarea 5.8) | **No aplica.** El endpoint vive detrás del login admin (`anyRequest().authenticated()`), mismo criterio que el resto de `/admin/*` |
| Honeypot anti-bot | **No aplica.** No tiene sentido detrás de un login |

**Lo que NO se relaja**: el precio y la duración siguen calculándose server-side a partir del
combo (base + adicionais + porte) — el admin nunca puede mandar un precio arbitrario, mismo
principio que el turno público ("nunca confía en lo que mande el cliente", ADR 020). Tampoco
se relaja la validación de que `baseServiceId`/`addonIds` existan y sean del tenant correcto.

### Forma de la respuesta

`ManualAppointmentResult` — mismo shape que `AdminAppointmentDetail` más un array `avisos: []`
(no `aviso: string | null` como `TempoExtraResult`, porque acá pueden coexistir varios: fuera
de grilla + supera capacidad + día bloqueado, a diferencia del toggle de `tempo_extra` que solo
puede generar un tipo de solapamiento).

---

## Alternativas consideradas

### ❌ El admin usa el mismo endpoint público (`POST /appointments`) con un flag `isManual`

Mezclar las dos políticas en un solo endpoint obliga a que cada validación pregunte "¿es
manual?" antes de aplicar. Un endpoint separado (`/admin/appointments`) mantiene las reglas
públicas simples y las del admin explícitas, sin condicionales cruzados. Coherente con que
ya existe `AdminAppointmentController` separado del público.

### ❌ Bloqueo duro en vez de aviso (mismo 409/400 que el público)

Rompería el propósito del turno manual: si ya hay un turno telefónico confirmado a esa hora,
el admin necesita poder registrarlo igual — bloquearlo duro dejaría la agenda real
desincronizada de la agenda del sistema, que es exactamente el problema que este endpoint
existe para resolver.

### ❌ Extender `schedule_blocks` a rango horario en vez de agregar el turno manual

Es la solución "correcta" a largo plazo (lo que pedía el ADR 012 originalmente), pero exige
migración de schema + rehacer el cálculo de slots del ADR 020, que el propio ROADMAP marca
como "el riesgo #1 de todo el plan". El turno manual da el mismo resultado práctico (el slot
deja de aparecer disponible en el wizard, porque ocupa cupo real) sin tocar el query difícil.
Se revisa en Fase 2 si el volumen de turnos por teléfono lo justifica.

---

## Consecuencias

### Positivas
- Cierra `docs/pending-decisions.md` §10 sin tocar el algoritmo de disponibilidad.
- Reusa el núcleo de cálculo de `AppointmentServiceImpl.create` — sin duplicar la lógica de
  snapshot de precio/duración.
- Extiende el patrón ya validado de "avisar, no bloquear" (ADR 011) en vez de inventar uno
  nuevo.

### Negativas
- El admin puede, en teoría, cargar turnos que se solapan entre sí sin límite — el único
  freno es el aviso, no una restricción dura. Aceptado: es una sola persona operando de
  buena fe, no un endpoint público expuesto a abuso.
- Si en el futuro hay más de un admin, esto podría generar sobre-agenda real por error
  humano sin que el sistema lo impida. Revisar si MVP1 deja de ser single-admin
  (mismo criterio de revisión que `docs/pending-decisions.md` §8).
