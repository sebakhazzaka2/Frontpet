# Working Context

> Estado efímero del proyecto — lo que cambia sprint a sprint. **No es** CLAUDE.md
> (permanente) ni ROADMAP.md (planificado). Si algo de acá deja de cambiar, se promueve
> a uno de esos dos o a un ADR; si es una decisión sin tomar, va a `pending-decisions.md`.

**Última actualización**: 2026-08-03

---

## Verdad actual

- Branch activa: `main`, todo mergeado — `feat/admin-services` (PR #71) fue la última rama
  de código del sprint.
- **Sprint 6 (booking frontend + admin de agendamentos) cerrado 2026-08-03.** Los 8 bloques
  (0, A-G) mergeados a `main` (PRs #65-#71 + este cierre documental). Corrió ~27-30 hs vs las
  16 hs que estimaba el ROADMAP original (~2x) — ver Registro de decisiones de plan en
  ROADMAP.md. **Hito alcanzado: primera reserva online posible.**
  - Único pendiente explícito del sprint: validación visual real a 320/768/1024px y prueba
    en celular de `/admin/servicos` — Sebastián decidió diferirla a un pase de polish de UI
    posterior, no perderla. El resto de las 8 pantallas (`/servicos`, wizard completo,
    `/admin/agendamentos`) ya está validado.
  - Durante el Bloque F apareció y se cerró un hueco de backend no anticipado: `GET
    /admin/services`, `GET /admin/business-hours` y `GET /admin/schedule-blocks` no
    existían pese a que el contrato tipado del Bloque 0 ya los llamaba.
- `pending-decisions.md` §10 (bloqueo de día vs. rango horario) quedó **resuelto
  parcialmente**: la mitigación (turno manual + bloqueo de día) ya es usable end-to-end
  desde el admin, verificado por API. La limitación de schema en sí (solo día completo, no
  rango horario) sigue vigente y no está en el alcance de ningún sprint actual.
- `docs/stitch-implementation-workflow.md` tiene los 8 pilotos documentados (1-8) — todas
  las pantallas de Sprint 2-6 portadas hoy están registradas ahí con sus desvíos.

## Restricciones vigentes (no permanentes — revisar cada sprint)

- Todo corre en localhost; sin infra comprada (gateado a Sprint Despliegue).
- **Sprint Despliegue sigue sin ejecutarse** y tiene que cerrar antes del 05/09 con hasta
  48 hs de propagación DNS — la ventana se está achicando. La conversación de pago con el
  cliente (necesaria para autorizar la compra de infra) tiene que salir ya, no a fin de
  agosto.
- Presupuesto de horas restante según ROADMAP: Sprints 4, 5 y 6 corrieron todos por encima
  de su estimado (+18%, +40%, ~+2x respectivamente) — el patrón es consistente: los sprints
  con más superficie nueva de UI/admin se subestiman. Vale aplicar el mismo criterio de
  auditoría previa (como se hizo con el plan de Sprint 6) antes de arrancar Sprint 7.

## Deuda técnica y documental conocida

- 🔴 **La landing publica testimonios inventados y métricas sin fuente** (`pending-decisions.md`
  §15, detectado 2026-08-03 en `/release-check`) — nombres ficticios en `lib/data/reviews.ts`,
  "4.9 Avaliação Média" y "500+ Pets Atendidos" en `<TrustBar>`. **Es el único hallazgo abierto
  con consecuencia legal/reputacional para el cliente, y bloquea el Sprint Despliegue** (no el
  desarrollo). Acción: pedir depoimentos reales (`preguntas-cliente.md` §6.5) o sacar las
  piezas antes de exponer la URL. Está en el checklist D.10 del ROADMAP.
- La modalidade Entrega/Retirada se reconstruye comparando contra el literal "Retirada na loja"
  en 4 puntos del frontend (`pending-decisions.md` §16) — fix cerrado de ~30 min (exponer
  `modalidade` en `OrderDetail`), candidato a Sprint 7.
- 51 valores arbitrarios de Tailwind en código portado (peor caso: `hero.tsx`) y ~20 spacings
  fuera de la escala de 4 (`pending-decisions.md` §17). La parte que **necesita decisión** es si
  los componentes shadcn quedan exentos de la escala de 4 — hoy se resuelve caso por caso.
- **`V5__seed_dev.sql` dice "não executar em produção" pero nada lo impide de verdad**
  (`pending-decisions.md` §4) — detectada en Sprint 3, **sigue sin resolver 4 sprints
  después** y impacta directo al Sprint Despliegue. Candidata real a resolverse antes de
  ese sprint, no después.
- Tercera copia del rate limit (login/orders/appointments) sin unificar
  (`pending-decisions.md` §9) — diferida a propósito a Sprint 7, que es el próximo. Vale
  confirmar que entra en su alcance real y no se vuelve a diferir.
- `SERVICES_PREVIEW` (landing) es estático y no refleja cambios hechos vía
  `/admin/servicos` (`pending-decisions.md` §14, nueva esta sesión) — deuda baja, candidata
  para cuando se generalice el swap estático→API de la home.
- `<ManualAppointmentDialog>` usa inputs nativos `type="date"`/`type="time"` sin picker
  propio (`pending-decisions.md` §13) — estético, no bloqueante.
- Hero del wizard de agendamento necesita estética propia, no genérica
  (`pending-decisions.md` §12) — pendiente de referencia de diseño.
- Paleta de colores en `globals.css` (`@theme`) siguen siendo placeholders — pendiente
  confirmación del cliente.
- `docs/ui/` es un export de Stitch viejo y stale — no portar desde ahí.
- `docs/learnings.md` está gitignored — no es fuente de verdad compartida.

## Cola activa / bloqueado por

- Nada bloqueado técnicamente hoy. Punto de decisión externa más urgente: acuerdo por
  escrito del cliente para el hito de cobro del 05/09 — necesario ya, no a mediados de
  agosto (esa ventana ya pasó).
- Ver `docs/pending-decisions.md` para el detalle completo de lo que sigue sin resolver.

## Regla de actualización

Este archivo se reescribe (no se acumula) en cada corrida de `/sprint-close` y cada vez que
`/doc-sync` detecte que algo cambió. Si una entrada de "Deuda documental conocida" deja de
ser cierta, se borra en el momento — no se marca como resuelta y se deja.

Si algo permanece igual durante 2+ sprints seguidos, probablemente no es efímero: mover a
CLAUDE.md (si es regla/decisión estable) o a un ADR (si es una decisión tomada).
