# Working Context

> Estado efímero del proyecto — lo que cambia sprint a sprint. **No es** CLAUDE.md
> (permanente) ni ROADMAP.md (planificado). Si algo de acá deja de cambiar, se promueve
> a uno de esos dos o a un ADR; si es una decisión sin tomar, va a `pending-decisions.md`.

**Última actualización**: 2026-09-02

---

## Verdad actual

- Branch activa: `feat/sentry-integration`, checked out directamente en el working
  directory principal (no worktree separado). Mezcla dos cosas sin relacionar en el mismo
  working tree:
  1. **Integración del SDK de Sentry** (`backend/pom.xml`, `backend/src/main/resources/application.yml`,
     `frontend/instrumentation-client.ts`, `frontend/instrumentation.ts`,
     `frontend/sentry.edge.config.ts`, `frontend/sentry.server.config.ts`,
     `frontend/next.config.ts`, `frontend/package.json` — todos sin commitear todavía) +
     dos fixes reales de bugs que rompían el build de Docker, encontrados hoy: faltaban los
     `NEXT_PUBLIC_*` como build ARGs en `frontend/Dockerfile`, y `??` en vez de `||` para env
     vars vacías en `frontend/app/layout.tsx` y `frontend/lib/api/client.ts` (un ARG de Docker
     sin `--build-arg` resuelve a `''`, no a `undefined` — el fallback de `??` nunca dispara).
  2. **No generado por esta sesión** — cambios sin commitear de una sesión paralela/concurrente:
     `backend/.env.example` (documenta env vars de prod que faltaban: `JWT_SECRET`,
     `TENANT_ID`, `ADMIN_*`, `R2_*`, `RESEND_*`, `APP_BASE_URL`, `CORS_ALLOWED_ORIGINS`) y
     `docs/pending-decisions.md` (agregó §23, Sentry `send-default-pii`).
- **Sprint Despliegue en curso ahora mismo** (D.1-D.10, detalle en `docs/deploy-runbook.md` y
  la tabla actualizada de `ROADMAP.md`). Estado real al 2026-09-02:
  - ✅ D.1 (VPS): Hetzner CX33 (ex-CX32, mismos specs) creado en **Falkenstein/Nuremberg**
    (Alemania) — **no** Ashburn como preveía el plan original de ADR 016: ese tier no existe
    en Ashburn/Hillsboro (la equivalente, CPX32, sale ~USD 42/mes, fuera de presupuesto). Es
    una desviación real de ADR 016, documentada como nota de enmienda en el propio ADR 016 y
    en `deploy-runbook.md` D.1 — **verificado consistente en ambos** en esta corrida.
  - ✅ D.2 (dominio/DNS): `frontpet.com.br` comprado en `registro.br`, DNS apuntado a
    Cloudflare (SSL/TLS Full strict), A records de root/api/coolify creados.
  - ✅ D.5 (Coolify): instalado en el VPS.
  - ✅ D.6.1 (Postgres): desplegado como recurso de Coolify.
  - 🔄 D.6.2 (backend en Coolify): env vars cargándose ahora mismo — todavía no verificado en
    vivo (`https://api.frontpet.com.br/actuator/health` sin confirmar).
  - ⏳ D.6.3 (frontend en Coolify): no arrancado — el `Dockerfile` ya está listo y verificado,
    falta el deploy en sí. Incluye la tarea nueva de Cache Rules de Cloudflare para rutas
    públicas GET (mitigación de latencia por el VPS en Alemania, ADR 016 §3) — tampoco
    arrancada.
  - 🔄 D.3 (verificación de dominio de e-mail en Resend): registros DNS (DKIM/SPF/MX) ya
    cargados en Cloudflare para `frontpet.com.br`, verificación/propagación pendiente de
    confirmar.
  - ❓ D.4 (cuentas Sentry/Plausible): **estado sin confirmar** — no asumir hecho ni pendiente
    sin verificarlo con Sebastián. El SDK de Sentry se está integrando recién ahora (branch
    `feat/sentry-integration`), lo que sugiere que activar Sentry de punta a punta todavía no
    pasó, pero eso no confirma si la cuenta/proyecto ya existen.
  - ⏳ D.7, D.8, D.9: no arrancados.
  - 🔄 D.10 (checklist de contenido): el ítem de depoimentos/métricas reales ya se resolvió
    (ADR 025, 2026-09-01) — el resto de la checklist (metadataBase, og-image, search console,
    themeColor, WhatsApp definitivo, fotos reales) sigue pendiente.
- `docs/pending-decisions.md` §9 (rate limit unificado), §15 (testimonios falsos → ADR 025),
  §16 (modalidade) están resueltos y así quedan documentados en esa misma sección — no hace
  falta repetir el detalle acá.
- §18 de `pending-decisions.md` (cuenta personal de Cloudflare de Sebastián) ya cubre también
  la situación de presupuesto de Hetzner y el cambio de región a Alemania **de forma
  indirecta** (menciona la tarjeta de Sebastián como método de pago compartido), pero **no
  menciona explícitamente** el cambio de región Ashburn→Falkenstein/Nuremberg — ese detalle
  vive solo en la nota de enmienda de ADR 016 §3 y en `deploy-runbook.md` D.1. Si se retoma
  §18, vale cruzar la referencia.
- §19 de `pending-decisions.md` (DoD incompleto del reset de contraseña) sigue describiendo
  el bloqueo correctamente: la verificación del dominio de Resend (Paso 0) es exactamente lo
  que está "en curso" en D.3 ahora mismo, todavía sin confirmar. No está stale — cuando D.3
  se confirme completo, esa sección hay que volver a tocarla.

## Deuda técnica y documental conocida

- 🔴 `V5__seed_dev.sql` dice "não executar em produção" pero nada lo impide de verdad
  (`pending-decisions.md` §4) — sigue sin resolver y ya impacta directo a D.6.2 (backend en
  deploy ahora mismo): hay que decidir antes de confirmar ese paso si se acepta que V5/V10
  corran en prod igual (datos reales, solo el WhatsApp es placeholder) o se separan las
  migraciones por perfil.
- La modalidade Entrega/Retirada se reconstruye comparando contra el literal "Retirada na
  loja" en 4 puntos del frontend (`pending-decisions.md` §16) — fix cerrado de ~30 min, sigue
  sin agendarse en ningún sprint.
- 51 valores arbitrarios de Tailwind en código portado (peor caso: `hero.tsx`) y ~20 spacings
  fuera de la escala de 4 (`pending-decisions.md` §17) — falta decidir si shadcn queda exento
  de la escala de 4.
- `SERVICES_PREVIEW` (landing) es estático y no refleja cambios hechos vía `/admin/servicos`
  (`pending-decisions.md` §14) — deuda baja.
- `<ManualAppointmentDialog>` usa inputs nativos `type="date"`/`type="time"` sin picker propio
  (`pending-decisions.md` §13) — estético, no bloqueante.
- Hero del wizard de agendamento necesita estética propia (`pending-decisions.md` §12) —
  pendiente de fotos reales del cliente.
- Paleta de colores en `globals.css` (`@theme`) sigue siendo placeholder — pendiente
  confirmación del cliente. Verificado en esta corrida: `design-system.md` §6-7 (sombras y
  patrones de componente) ya **no** están stale — coinciden con `globals.css` (radius,
  shadows, colores) tal como quedaron documentados en la v3.0 del doc.
- `docs/ui/` es un export de Stitch viejo y stale — no portar desde ahí.
- `docs/learnings.md` está gitignored — no es fuente de verdad compartida.
- **7.12 (reset de contraseña) y 7.1-7.3 (mini-dashboard)** siguen con el DoD incompleto
  (`pending-decisions.md` §19 y §20 respectivamente) — el bloqueo de 7.12 (verificación de
  Resend) es literalmente D.3, en curso ahora mismo.
- **El wizard de agendamento no protege contra doble submit** (`pending-decisions.md` §21) —
  fix propuesto, no implementado.
- **`POST /admin/products` responde 500 en vez de 400 ante JSON con bytes UTF-8 inválidos**
  (`pending-decisions.md` §22) — impacto bajo, no implementado.
- **Sentry backend corre con `send-default-pii` en `false`** (`pending-decisions.md` §23,
  agregado por la sesión paralela hoy) — atado a que exista banner LGPD antes de reconsiderarlo.
- **Referencias rotas a `docs/reuse-consultorio.md`**: `ROADMAP.md` lo enlaza 2 veces (tarea
  1.3, línea ~193, y tarea 5.3, línea ~429) y varios comentarios de código backend lo citan
  como fuente (`JwtService.java` y otros, ver §1). El archivo **no existe en el repo**. No
  bloquea nada hoy — Sprint 1 y 5 (los que lo referenciaban para el porteo desde el repo
  consultorio) ya están cerrados — pero son links/citas rotas que quedan en el repo.

## Cola activa / bloqueado por

- Nada bloqueado técnicamente hoy en desarrollo. El Sprint Despliegue tiene 3 pasos activos en
  paralelo (D.3 Resend, D.6.2 backend en Coolify) y uno listo para arrancar apenas D.6.2 cierre
  (D.6.3 frontend).
- La rama `feat/sentry-integration` tiene cambios sin commitear que mezclan Sentry + fixes de
  Docker + archivos de otra sesión (`.env.example`, `pending-decisions.md` §23) — separar antes
  de mergear, no asumir que todo es del mismo commit lógico.
- Punto de decisión externa más urgente: acuerdo por escrito del cliente para el hito de cobro
  del 05/09 — la fecha ya está encima.
- Ver `docs/pending-decisions.md` para el detalle completo de lo que sigue sin resolver.

## Regla de actualización

Este archivo se reescribe (no se acumula) en cada corrida de `/sprint-close` y cada vez que
`/doc-sync` detecte que algo cambió. Si una entrada de "Deuda documental conocida" deja de
ser cierta, se borra en el momento — no se marca como resuelta y se deja.

Si algo permanece igual durante 2+ sprints seguidos, probablemente no es efímero: mover a
CLAUDE.md (si es regla/decisión estable) o a un ADR (si es una decisión tomada).
