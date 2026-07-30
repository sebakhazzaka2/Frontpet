# ADR 012 — Booking sin integración con el ERP del cliente: la web es la autoridad de disponibilidad

**Estado**: Aceptada — mitigación acotada al implementar, ver Actualización 2026-07-30
**Fecha**: 2026-07-09
**Sprint**: 2

---

## Actualización 2026-07-30 (post-implementación, Sprint 5)

La mitigación de la sección "Consecuencia clave" de abajo dice que el admin bloquea "los
**horarios**" ocupados por otros canales — al implementar (`V3__booking.sql`), `schedule_blocks`
solo soporta bloquear el **día completo** (`data_desde`/`data_hasta`), no un rango horario
dentro del día. La mitigación real hoy es más gruesa de lo que este ADR asumía.

El fix correcto —que el admin cargue un turno manual (`POST /admin/appointments`) que ocupe
cupo real sin pasar por el wizard público, permitiendo bloquear una hora puntual— quedó
diferido a **Sprint 6** (junto con la vista de turnos). Detalle en **ADR 020** ("Negativas / a
vigilar") y `docs/pending-decisions.md` §10, incluyendo el riesgo de sobre-oferta mientras
tanto si el cliente opera por teléfono antes de que Sprint 6 cierre.

---

## Contexto

Al relevar la operación real de FrontPet surgió un dato que no estaba en el diseño previo:
**el cliente ya usa un ERP** que tiene su propia agenda de banhos y por donde procesa los
pedidos. Su flujo actual de turno es: el cliente escribe por WhatsApp → FrontPet ofrece
horarios → el cliente elige → **FrontPet lo carga en el ERP**.

Esto plantea la pregunta de la fuente de verdad de la agenda. Tres caminos:

- **(A)** Nuestra web es la **autoridad de disponibilidad** del booking online: calcula slots
  con sus propias reservas, el cliente elige, la reserva nace `PENDING` y el admin confirma.
- **(B)** Nuestra web reemplaza la agenda del ERP para grooming.
- **(C)** Integramos/sincronizamos con el ERP.

El CLAUDE.md ya define que **"Integración con ERP/CRM de FrontPet" está fuera de MVP1**
(Fase 2+). La responsabilidad del proyecto termina en nuestra plataforma; el ERP es sistema
interno del cliente.

---

## Decisión

**Camino (A). No hay integración con el ERP. Nuestra plataforma es la autoridad de
disponibilidad para el booking online, y la reserva es una solicitud que el admin confirma.**

Concretamente:

1. La disponibilidad se calcula **solo** con los datos de nuestra DB: `appointments` +
   capacidade ([ADR 009](./009-servicos-fixos-capacidade-simples.md)) + horários +
   `schedule_blocks`, según el cálculo dinámico del [ADR 005](./005-slots-dinamicos.md).
2. La reserva se persiste `PENDING` y el admin la confirma desde el panel, sin abrir WhatsApp
   en el submit (consistente con [ADR 008](./008-agendamentos-sin-whatsapp.md)).
3. **No se lee ni se escribe nada del ERP.** Lo que el cliente haga en su ERP después de
   confirmar (facturar, registrar recepción del animal, etc.) es su operación interna y queda
   fuera de scope.

---

## Consecuencia clave (y su mitigación)

Como somos la autoridad de disponibilidad **pero no** conocemos lo que vive en el ERP ni lo
que entra por otros canales (teléfono, walk-in), **nuestra disponibilidad solo refleja las
reservas hechas por la web.** Si entra un turno por fuera, la web podría ofrecer un horario
que en la realidad ya está tomado (sobre-oferta).

**Mitigación (operativa, sin código nuevo):** el admin **bloquea manualmente** en nuestro
sistema, vía `schedule_blocks`, los horarios que se ocupan por otros canales. Es el mismo
mecanismo de los feriados. El costo es un hábito operativo del cliente, no una feature.

Esto se le comunicó y aceptó explícitamente: la web maneja la disponibilidad online, y lo
que entra por otros canales se bloquea a mano para que no se sobre-ofrezca.

---

## Por qué no (B) ni (C)

- **(B) Reemplazar el ERP**: el ERP hace más que la agenda (facturación, logística,
  recepción del animal con observaciones). No está en scope ni en el interés del cliente
  migrar todo eso. La web cubre la captación online, no la operación completa.
- **(C) Integrar**: explícitamente Fase 2+ en CLAUDE.md. Requiere conocer y acoplarse a un
  ERP de terceros (API, formato, credenciales) — riesgo y esfuerzo fuera del MVP1 de USD 500.

---

## Consecuencias

### Positivas
- Scope acotado y honesto: la plataforma no depende de un sistema externo que no controlamos.
- El diseño de slots dinámicos (ADR 005) y de reservas `PENDING` (ADR 008) se mantiene intacto
  y cobra pleno sentido: **sí** somos fuente de verdad de lo que se reserva online, así que el
  cálculo de solapamiento y la concurrencia (lock del último cupo) siguen siendo necesarios.

### Negativas
- Doble carga operativa mientras el cliente siga usando el ERP: confirma en nuestro panel y
  además registra en su ERP. Es su decisión mantener ambos.
- La disponibilidad online es tan confiable como la disciplina del admin para bloquear lo que
  entra por otros canales.

### Notas para el futuro
- Si el volumen crece y la doble carga molesta, Fase 2 puede evaluar integración real con el
  ERP (camino C) o que la web pase a ser la agenda única (camino B). No anticipar en MVP1.
