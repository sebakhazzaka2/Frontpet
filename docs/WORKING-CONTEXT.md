# Working Context

> Estado efímero del proyecto — lo que cambia sprint a sprint. **No es** CLAUDE.md
> (permanente) ni ROADMAP.md (planificado). Si algo de acá deja de cambiar, se promueve
> a uno de esos dos o a un ADR; si es una decisión sin tomar, va a `pending-decisions.md`.

**Última actualización**: 2026-10-01

---

## Verdad actual

- Branch activa: `main`, working tree limpio. La rama `feat/sentry-integration` que figuraba
  acá como "en curso" ya se mergeó a `main` vía PR #75 (commit `253efb5f`) — **pero** la rama
  remota `origin/feat/sentry-integration` sigue existiendo con un commit propio
  (`1d2087e3 docs: sync CLAUDE.md, ROADMAP, deploy runbook...`) que nunca llegó a `main`.
  Revisar ese commit antes de borrar la rama, por si tiene contenido documental que todavía
  falta aplicar.
- **El sitio está en producción**: `https://frontpet.com.br` y `https://api.frontpet.com.br`
  están en vivo (verificado 2026-10-01: `sitemap.xml` y `robots.txt` responden 200 OK, incluso
  simulando el user-agent de Googlebot — sin bloqueo de Cloudflare). El Sprint Despliegue
  (D.1-D.10, detalle en `docs/deploy-runbook.md` y la tabla de `ROADMAP.md`) está mayormente
  cerrado:
  - ✅ D.1-D.2 (VPS Hetzner CX33 en Falkenstein/Nuremberg, dominio + DNS en Cloudflare).
  - ✅ D.3 (Resend) — asumido hecho porque 7.12 (reset de contraseña) está marcado hecho en
    Sprint 7, lo que requiere el dominio verificado. **Confirmar con Sebastián** que se probó
    de punta a punta en Gmail/Outlook (pending-decisions.md §19 lo pedía explícito).
  - ✅ D.4 (cuentas Sentry + Plausible).
  - ✅ D.5-D.6.3 (Coolify, Postgres, backend y frontend deployados).
  - ⏳ D.7 (backups automáticos) — **sin evidencia de que se haya hecho**. Sigue siendo el
    hueco más concreto del Sprint Despliegue.
  - ✅ D.8 (Sentry activo) — SDK integrado (PR #75), error de prueba confirmado en el
    dashboard, endpoint de prueba removido (PR #80). Falta solo el primer error orgánico real.
  - 🔄 D.9 (security review pre-exposición) — se corrió sobre los PRs del deploy (#75/#76/#77)
    y encontró 2 bugs reales, ya mitigados (ver "Hallazgos post-deploy" abajo). El checklist
    completo de D.9 (`pnpm audit`, headers de seguridad, etc.) no está confirmado end-to-end.
  - 🔄 D.10 (checklist de contenido) — depoimentos reales (ADR 025) y SEO básico/sitemap
    (tarea 7.9, PR #81) ya resueltos. Sigue pendiente: `og-image.jpg` real, `themeColor`
    confirmado con el cliente, fotos reales de producto/servicio/hero.
- **Hoy, 2026-10-01, se cerraron 3 tareas operativas post-deploy** (fuera del runbook, a
  pedido de Sebastián):
  1. Google Search Console: property de `frontpet.com.br` verificada, sitemap reenviado.
     Hubo un estado transitorio "no se puede obtener" al reenviarlo — diagnosticado como
     timing (el servidor respondía 200 normal), no bloqueo de Cloudflare. Sebastián lo dejó
     en observación, sin confirmar aún que haya pasado a "Success".
  2. Sentry: DSNs confirmados cargados en Coolify (backend y frontend). Sin error real
     disparado todavía — queda a la espera de uno orgánico.
  3. `GOOGLE_PLACES_API_KEY` restringida por IP en Google Cloud Console a `167.235.134.150/32`
     (la IP del VPS, recuperada de `~/.ssh/known_hosts` local porque no había acceso a Hetzner
     a mano). Cierra el pendiente que dejaba ADR 025 abierto ("sin restricción hasta que
     exista el VPS de producción").
- **Sprint 7 en curso**: 7.1-7.4, 7.6, 7.9, 7.12, 7.13, 7.14 confirmados hechos en el código
  (dashboard admin con `<KPICard>`, Meta Pixel + Plausible gateados por LGPD, SEO/sitemap,
  reset de contraseña, banner de consentimiento, derecho de eliminación). 7.10 (optimización
  de imágenes) y 7.11 (testing cross-browser) parcialmente cubiertos por PR #81, sin
  confirmar si la pasada completa ya se hizo. 7.7 (auditoría mobile completa) no arrancada.

## Hallazgos post-deploy (nuevos desde el 2026-09-02, documentados en pending-decisions.md)

Estos 4 ítems no existían la última vez que este archivo se actualizó — vale tenerlos
presentes porque son reales, no hipotéticos:

- **§24** — Coolify no pasa variables marcadas como "secret" al build de Docker aunque tengan
  el toggle de "Available at Buildtime" activo (comportamiento de la plataforma, no bug propio;
  costó un rato de debugging con `GOOGLE_PLACES_API_KEY`).
- **§25** — Bug real de código: la cookie de sesión del admin no tenía `Domain` explícito, así
  que el login nunca persistía entre `frontpet.com.br` y `api.frontpet.com.br`. Arreglado
  (PR #77), existía desde que existe el login.
- **§26** — Efecto secundario del fix de §25: la cookie ampliada a `Domain=frontpet.com.br` se
  manda también a `coolify.frontpet.com.br`. Mitigado borrando ese registro DNS (ver nota en
  `deploy-runbook.md` D.2); la opción B (scope más fino de cookie) queda pospuesta a Fase 2.
- **§27** — **Sin resolver, con decisión de diseño pendiente**: el stock de productos nunca se
  descuenta automáticamente, ni al crear el pedido ni al confirmarlo. Sebastián propuso
  descontar recién al pasar a `CONFIRMED` (evita stock negativo por varios `PENDING`
  simultáneos) — es trabajo nuevo a estimar, no estaba en el Sprint 7 original. Decidir antes
  de agendarlo.

## Deuda técnica y documental conocida

- 🔴 `V5__seed_dev.sql` dice "não executar em produção" pero nada lo impide de verdad
  (`pending-decisions.md` §4) — sigue sin resolver. Como el backend ya está deployado (D.6.2
  hecho), confirmar si terminó corriendo en prod o si se separaron las migraciones por perfil.
- La modalidade Entrega/Retirada se reconstruye comparando contra el literal "Retirada na
  loja" en 4 puntos del frontend (`pending-decisions.md` §16, marcado resuelto en ese doc —
  verificar si sigue siendo cierto antes de asumirlo).
- 51 valores arbitrarios de Tailwind en código portado y ~20 spacings fuera de la escala de 4
  (`pending-decisions.md` §17) — sin agendar.
- `SERVICES_PREVIEW` (landing) es estático y no refleja cambios vía `/admin/servicos`
  (`pending-decisions.md` §14) — deuda baja.
- `<ManualAppointmentDialog>` usa inputs nativos `type="date"`/`type="time"` (`pending-decisions.md` §13).
- Paleta de colores en `globals.css` sigue siendo placeholder, pendiente confirmación del
  cliente — `design-system.md` sigue sin drift respecto a `globals.css` (es la fuente
  canónica por diseño, v3.0 no duplica tablas de valores).
- `docs/ui/` es un export de Stitch viejo y stale — no portar desde ahí.
- `docs/learnings.md` está gitignored — no es fuente de verdad compartida.
- **7.12 DoD** (`pending-decisions.md` §19): confirmar prueba real de reset en Gmail/Outlook
  sin caer en spam, y la invalidación de sesión en otro navegador — no hay evidencia de que
  se haya corrido ese checklist ya en producción real.
- **7.3 DoD** (`pending-decisions.md` §20): mini-dashboard ya tiene código, falta validación
  visual a 320/768/1024px.
- **El wizard de agendamento no protege contra doble submit** (`pending-decisions.md` §21).
- **`POST /admin/products` responde 500 en vez de 400** ante JSON con bytes UTF-8 inválidos
  (`pending-decisions.md` §22).
- **Sentry backend corre con `send-default-pii` en `false`** (`pending-decisions.md` §23) —
  atado a que exista banner LGPD (ya existe, 7.13) antes de reconsiderarlo.
- **Stock no se descuenta automáticamente** (`pending-decisions.md` §27, nuevo) — ver arriba.
- **Referencias rotas a `docs/reuse-consultorio.md`**: sigue sin existir, sigue citado en
  `ROADMAP.md` (tareas 1.3 y 5.3) y en comentarios Javadoc de varios archivos de
  `identity`/`config` en el backend. No bloquea nada — Sprints 1 y 5 ya cerrados — pero son
  links/citas rotas que quedan en el repo.

## Cola activa / bloqueado por

- Nada bloqueado técnicamente en desarrollo. El hueco más concreto del Sprint Despliegue es
  **D.7 (backups automáticos)** — sin evidencia de que se haya configurado.
- Punto de decisión externa más urgente: el hito de cobro del 05/09/2026 ya pasó respecto a
  la fecha de esta actualización (2026-10-01) — confirmar con Sebastián el estado real de esa
  conversación con el cliente, este archivo no tiene información para asumir nada.
- §27 (stock) necesita una decisión de diseño antes de poder estimarse y agendarse.
- Ver `docs/pending-decisions.md` para el detalle completo de lo que sigue sin resolver.

## Regla de actualización

Este archivo se reescribe (no se acumula) en cada corrida de `/sprint-close` y cada vez que
`/doc-sync` detecte que algo cambió. Si una entrada de "Deuda documental conocida" deja de
ser cierta, se borra en el momento — no se marca como resuelta y se deja.

Si algo permanece igual durante 2+ sprints seguidos, probablemente no es efímero: mover a
CLAUDE.md (si es regla/decisión estable) o a un ADR (si es una decisión tomada).
