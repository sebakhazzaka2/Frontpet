# Working Context

> Estado efímero del proyecto — lo que cambia sprint a sprint. **No es** CLAUDE.md
> (permanente) ni ROADMAP.md (planificado). Si algo de acá deja de cambiar, se promueve
> a uno de esos dos o a un ADR; si es una decisión sin tomar, va a `pending-decisions.md`.

**Última actualización**: 2026-08-01

---

## Verdad actual

- Branch activa: `main` (`feat/sprint6-contract` y `feat/booking-admin-manual` ya mergeadas,
  PRs #65 y #67)
- Sprint 5 (booking backend) **cerrado** 2026-07-30 — corrió ~26,5 hs vs 19 estimadas (ver
  Registro de decisiones de plan en ROADMAP.md)
- Sprint 6 (booking frontend + admin de agendamentos, ~16 hs) **en curso**:
  - ✅ Capa de contrato `frontend/lib/api/{admin-appointments,admin-schedule,appointments,availability,services}.ts`
    tipada contra los DTOs reales (ADR 013)
  - ✅ ADR 021 (turno manual admin) y ajuste de ADR 008
  - ✅ Backend del turno manual admin: `POST /api/v1/admin/appointments` (política de ADR 021 —
    salta la grilla de slots, avisa en vez de bloquear por capacidad/día bloqueado) + listado
    admin por rango `desde`/`hasta` (commit `74abdd9`, PR #67). Cierra `pending-decisions.md` §10
  - ⏳ Falta todo lo de UI: wizard `/agendamento` (6.1-6.4), pantalla de confirmación (6.5),
    vista de turnos del día/7 días y acciones Confirmar/Cancelar en admin (6.6-6.7) — no hay
    rutas `/agendamento` ni de turnos en `frontend/app/admin` todavía, solo el backend y la
    capa de contrato están listos
- Migración de DB recién reparada: `tempo_extra` en `appointments` (drift entre V3 y un
  volumen local desactualizado) — resuelto en el commit `7128ba0`, no debería volver a
  aparecer salvo que alguien reviva un volumen Docker viejo

## Restricciones vigentes (no permanentes — revisar cada sprint)

- Todo corre en localhost; sin infra comprada (gateado a Sprint Despliegue, post Sprint 4+5)
- Bloque B del roadmap tiene solo ~3,5 hs de slack — cualquier desborde en Sprint 6/7/8 le
  pega directo a la entrega del 30/09

## Deuda documental conocida

- Paleta de colores en `globals.css` (`@theme`) son placeholders — pendiente confirmación
  del cliente (no bloquea desarrollo, cambiarla es tocar 1 archivo)
- `docs/ui/` es un export de Stitch viejo y stale — no portar desde ahí, solo mirar `screen.png` offline
- `docs/learnings.md` está gitignored — no es fuente de verdad compartida, vive solo local

## Cola activa / bloqueado por

- Nada bloqueado hoy. Próximo punto de decisión externa: acuerdo por escrito del cliente
  para el hito de cobro del 05/09 (necesario a mediados de agosto, ver ROADMAP.md)
- Ver `docs/pending-decisions.md` para el detalle completo de lo que sigue sin resolver.

## Regla de actualización

Este archivo se reescribe (no se acumula) en cada corrida de `/sprint-close` y cada vez que
`/doc-sync` detecte que algo cambió. Si una entrada de "Deuda documental conocida" deja de
ser cierta, se borra en el momento — no se marca como resuelta y se deja.

Si algo permanece igual durante 2+ sprints seguidos, probablemente no es efímero: mover a
CLAUDE.md (si es regla/decisión estable) o a un ADR (si es una decisión tomada).
