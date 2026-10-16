# Working Context

> Estado efímero del proyecto — lo que cambia sprint a sprint. **No es** CLAUDE.md
> (permanente) ni ROADMAP.md (planificado). Si algo de acá deja de cambiar, se promueve
> a uno de esos dos o a un ADR; si es una decisión sin tomar, va a `pending-decisions.md`.

**Última actualización**: 2026-10-16 (doc-sync). `main` al día con `origin/main` hasta `a4f62c3` (PR #86).

---

## Verdad actual

- Branch activa: `main`. Desde 2026-10-01 entraron #83 (fix hydration en `<head>`), #84 (fotos placeholder
  de hero/servicios + `og-image.jpg`), #85 (hero con card de catálogo) y #86 (`docs/checklist-prueba-prod.md` +
  `docs/limpieza-datos-prueba.sql`). `FrontPet.md` y `docs/estado-frontpet.md` están en `.gitignore`.
- Ramas: solo `main` en local. En el remoto quedan ramas viejas ya mergeadas por squash (ver abajo si se limpian).
- **Infra**: ya está comprada y en uso (VPS Hetzner CX33, dominio, Cloudflare). La restricción de
  "no comprar infra hasta el Sprint Despliegue" ya no aplica; sí sigue vigente no sumar costos nuevos sin hablarlo.
- **El sitio está en producción**: `https://frontpet.com.br` y `https://api.frontpet.com.br`
  (verificado 2026-10-01: `sitemap.xml` y `robots.txt` → 200). Sprint Despliegue (D.1–D.10, detalle en
  `docs/deploy-runbook.md` y `ROADMAP.md`) mayormente cerrado:
  - ✅ D.1–D.2 VPS y DNS · ✅ D.3 Resend (asumido; confirmar prueba en Gmail/Outlook) · ✅ D.4 Sentry + Plausible ·
    ✅ D.5–D.6.3 Coolify, Postgres, backend y frontend · ✅ D.8 Sentry activo (falta un error orgánico real).
  - ⏳ D.7 backups automáticos — **sin evidencia**; es el hueco más concreto.
  - 🔄 D.9 security review — `pnpm audit` y headers sin confirmar end-to-end; Next subido a 16.3.8 el 2026-10-01 por CVEs.
  - 🔄 D.10 contenido — `og-image.jpg` y fotos de hero/servicios ya existen pero son **placeholders de stock**
    (#84); faltan fotos reales y `themeColor` confirmado con el cliente.
  - ⏳ Prueba de punta a punta en producción: `docs/checklist-prueba-prod.md` (incluye backup previo, que además
    cierra la duda de D.7).
- **Sprint 7 en curso**: hechos 7.1–7.4, 7.6, 7.9, 7.12–7.14. Parciales 7.10 y 7.11 (PR #81, sin confirmar pasada completa).
  Sin arrancar: 7.7 (auditoría mobile completa).
- **Google Search Console** verificada, sitemap reenviado (en observación). `GOOGLE_PLACES_API_KEY` restringida por IP al VPS.
- **Ecosistema "AI SaaS Ecosystem"**: este repo adoptó el contrato común v1 (CLAUDE.md §14) y expone su estado en
  `docs/ESTADO.md`. Fase actual del ecosistema: auditoría y diseño; no se implementa el agente todavía.

## Hallazgos post-deploy (documentados en pending-decisions.md)

- **§24** Coolify no pasa variables "secret" al build de Docker aunque tengan "Available at Buildtime".
- **§25** Cookie de sesión sin `Domain` → login no persistía entre subdominios. Arreglado (PR #77).
- **§26** Efecto del fix: cookie con `Domain=frontpet.com.br` llega a todos los subdominios. Mitigado borrando el DNS de
  `coolify.`; scope más fino de cookie pospuesto a Fase 2.
- **§27** **Sin resolver**: el stock nunca se descuenta. Propuesta: descontar al pasar a `CONFIRMED`. Decidir antes de agendar.

## Deuda técnica y documental conocida

- 🔴 `V5__seed_dev.sql` dice "não executar em produção" y nada lo impide (§4). Confirmar si corrió en prod.
- Riesgos de seguridad y brechas multi-tenant: ver `docs/ESTADO.md` (CSRF off, sin roles, rate limit en memoria,
  tenant fijo, login por email global).
- La modalidad Entrega/Retirada compara contra el literal "Retirada na loja" (§16, marcado resuelto allí; no re-verificado).
- 51 valores arbitrarios de Tailwind y ~20 spacings fuera de la escala de 4 (§17) — sin agendar.
- `SERVICES_PREVIEW` estático (§14) · `<ManualAppointmentDialog>` con inputs nativos (§13) — deuda baja.
- Paleta en `globals.css` sigue siendo placeholder pendiente del cliente.
- `docs/ui/` es un export de Stitch viejo; `docs/learnings.md` está gitignored.
- **7.12 DoD** (§19): sin evidencia de prueba real de reset en Gmail/Outlook ni de invalidación de sesión en otro navegador.
- **7.3 DoD** (§20): falta validación visual a 320/768/1024px del dashboard.
- Wizard sin protección de doble submit (§21) · `POST /admin/products` responde 500 ante UTF-8 inválido (§22) ·
  Sentry backend con `send-default-pii=false` (§23; el banner LGPD ya existe).
- **Referencias rotas**: `docs/reuse-consultorio.md` fue eliminado a propósito (recuperable con
  `git show fbd9aa0^:docs/reuse-consultorio.md`). `ROADMAP.md` y ADR 013 ya lo indican; siguen citándolo comentarios
  Javadoc de `identity`/`config` (SecurityConfig, DataInitializer, StartupEnvValidator, AuthService, JwtService,
  JwtAuthFilter, AuthController, UserDetailsServiceImpl) — es código, no se tocó. No bloquea nada.

## Cola activa / bloqueado por

- Nada bloqueado técnicamente. Hueco más concreto: **D.7 backups**.
- El hito de cobro del 05/09/2026 y la entrega del 30/09/2026 ya pasaron; esta doc no tiene información sobre el estado
  real de esa conversación con el cliente — confirmar con Sebastián.
- §27 (stock) necesita decisión de diseño antes de estimarse.
- Ver `docs/pending-decisions.md` para el detalle completo.

## Regla de actualización

Se reescribe (no se acumula) en cada `/sprint-close` y cada `/doc-sync`. Si una entrada deja de ser cierta, se borra.
Si algo permanece igual durante 2+ sprints, probablemente no es efímero: mover a CLAUDE.md o a un ADR.
