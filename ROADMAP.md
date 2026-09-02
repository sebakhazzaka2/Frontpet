# Roadmap FrontPet MVP1

> Plan de ejecución del MVP1 **re-baseado el 17/07/2026**, contra el plazo real de entrega:
> **30 de septiembre de 2026**. Ritmo comprometido: **22 hs/semana**.

> El porqué del re-baseo y las demás decisiones de plan fechadas viven en el
> **[Registro de decisiones de plan](#registro-de-decisiones-de-plan)**, al final.

### El plazo y el cobro son dos cosas distintas

| | Fecha | Qué |
|---|---|---|
| 🎯 **Hito de cobro** | **05/09/2026** | **Tienda vendiendo online y desplegada.** Landing + catálogo + carrito + pedidos por WhatsApp + admin de productos y pedidos. Es el hito contra el que se pide el segundo pago |
| 🎯 **Entrega final** | **30/09/2026** | MVP1 completo: lo anterior + agendamento online + dashboard + capacitación |

El 05/09 **no es una entrega**: es la fecha en la que se necesita el pago. Se factura contra
valor real entregado (la tienda vendiendo), no contra una promesa. **Requiere acuerdo
explícito del cliente por escrito antes de mediados de agosto** — no el 1/09.

### Presupuesto de horas

| | Hs |
|---|---:|
| Disponibles 17/07 → 05/09 (7 sem × 22, menos 6 días de panza) | ~119 |
| Necesarias para el hito de cobro (Sprint 1 resto 8,5 + Sprint 2 24,5 + Sprint 3 24 + Despliegue 10 + Sprint 4 30) | 97 |
| **Slack del bloque A** | **~22** |
| Disponibles 06/09 → 30/09 (3,5 sem × 22) | ~77 |
| Necesarias para la entrega (Sprint 5 19 + Sprint 6 16 + Sprint 7 24,5 + Sprint 8 14) | 73.5 |
| **Slack del bloque B** | **~3,5** ⚠️ |

**Los 6 días de panza van en el bloque A, no en el B.** El bloque B tiene 3,5 hs de margen:
un solo mal día ahí se come la entrega.

**Usá el slack del bloque A para adelantar el Sprint 5** (booking backend, 19 hs). Es la
parte más riesgosa del MVP y la única que no depende de nada del bloque B. Si entra antes
del 05/09, el bloque B pasa de 3,5 hs de margen a ~22 hs. Si no lo adelantás, estás
apostando la entrega a que septiembre salga perfecto.

> Estas horas **no incluyen colchón**: el slack del bloque A *es* el colchón. Y asumen
> 22 hs/semana sostenidas — si una semana caés a 10, se recupera del slack de A, no de B.

---

## Resumen de fases

| Bloque | Sprint | Foco | Hs | Estado |
|---|--------|------|---:|--------|
| | 0 | Pre-kickoff (GitHub Projects + modelo DB) | 3 | ✅ hecho |
| **A** | 1 | Setup local: backend + frontend + auth + testing | 16 | ✅ **cerrado** — gap de 1.6/1.7/1.8 mergeado (PR #27) |
| **A** | 2 | Landing pública responsive (portada de Stitch) | 24.5 | 🔄 ~94% — 2.0b-2.9 hechas (incluye perf/Lighthouse), falta confirmar 2.10 (responsive formal a 320/768/1024/1440) |
| **A** | 3 | Catálogo + backend de productos | 24 | ✅ **cerrado (2026-07-27)** — 3.1-3.12 hechas, mergeado a `main` (PR #26) |
| **A** | 4 | **Carrito + pedidos WhatsApp + Admin productos** | 30 | ✅ **cerrado (2026-07-29)** — Bloques 0/A/B/C/D/E/F mergeados a `main` (PRs #36-42) + G (este cierre documental), incluye §6/§7 de `pending-decisions.md`. Ver Registro de decisiones de plan |
| **A→B** | 5 | Booking backend (disponibilidad + reservas) | 19 | ✅ **cerrado (2026-07-30)** — Bloques 0/A/B/C/D/E/F/G mergeados a `main` (PRs #52-56), corrió ~26,5 hs vs 19 estimadas. Ver Registro de decisiones de plan |
| **A** | Despliegue | **Compra infra + despliegue inicial** — landing + catálogo + venta + booking backend en vivo | 10 | ⏸️ pospuesta otra vez: ahora va **después del Sprint 4 y 5**, no inmediatamente post-Sprint 3 (decisión 2026-07-27, ver nota abajo) |
| | | 🎯 **05/09 — hito de cobro: la tienda vende** (requiere Sprint 4 + Despliegue cerrados; el orden 4→5→Despliegue debe dejar margen para que Despliegue cierre antes de esta fecha) | | |
| **B** | 6 | Booking frontend + Admin de turnos | 16 | |
| **B** | 7 | Dashboard + LGPD + Marketing + Reset de senha + Polish | 24.5 | |
| **B** | 8 | Capacitación + entrega formal | 14 | |
| | | 🎯 **30/09 — entrega final** | | |
| | Opcional | Solo si sobra tiempo (ver abajo) | 6.5 | |
| **Total** | | **~85 hs restantes** (Sprints 1-8: 1, 3, 4 y 5 cerrados —4 y 5 corrieron por encima de su estimado, ver Registro de decisiones de plan—, 2 al ~94%) **+ 6,5 opcionales** | **~91,5** | |

> 🔁 Despliegue movido después del Sprint 3 — ver Registro de decisiones de plan (2026-07-27).

> ⚠️ **Sprint 4 (30 hs) es el más pesado del plan** y cae justo contra el hito de cobro.
> Conviene partirlo en dos mitades entregables — venta pública (4.1-4.10) y admin
> (4.11-4.14) — para que un desborde no se lleve puesta la fecha del 05/09.

### Cambios de scope — julio 2026

**Opcional — solo si sobra tiempo** (6,5 hs). No se borran: se hacen al final si el
colchón sigue vivo, y si no, se cotizan como Fase 2. **Ninguna es requisito de entrega.**

| Tarea | Hs | Por qué es opcional |
|---|---:|---|
| 7.15 Sentry: tuning de rate limits y alertas | 1.5 | El Sentry básico (D.8) ya captura errores |
| 7.16 Uptime Kuma | 1 | Monitoreo de uptime en un negocio de este tamaño puede esperar |
| 7.17 Simulacro de restore de backups | 1 | El backup corre (D.7); verificarlo formalmente es lo ideal, no lo mínimo |
| 7.18 Meta Pixel server-side (Conversions API) | 1 | Ya estaba marcada "opcional" en el plan original |
| 7.8 Pulido de animaciones y micro-interacciones | 2 | Es lo que separa "funciona" de "se siente pro" — pero no bloquea la entrega |

**Corregido** (no es un recorte de tiempo, es un error del plan):

| Tarea | Hs | Por qué |
|---|---:|---|
| 3.9 Galería de imágenes en detalle de producto | −1 | **Contradecía `CLAUDE.md` §7**: MVP1 es una foto por producto, galería es Fase 2 |
| 2.0 Moodboard | −2 | Stitch ya es la referencia visual (ver abajo). Se reemplaza por 2.0b, sync desde el MCP |

**Agregado al scope** (+13,5 hs) — seguridad y LGPD, ver detalle en cada sprint.
**No son opcionales**: son la diferencia entre entregar algo funcional y entregar algo
funcional y seguro.

| Tarea | Hs | Por qué |
|---|---:|---|
| 1.8 Rate limit + lockout en login | 2 | La 1.3 dejaba el login sin protección de fuerza bruta |
| 3.5b Validación de mime/tamaño en presigned URLs de R2 | 1.5 | Sin esto el bucket es de subida libre a costa nuestra |
| 4.15 Rate limit + honeypot en `POST /orders` | 2 | Endpoint público anónimo |
| 4.16 Política de privacidad + aviso en checkout (LGPD) | 2 | Sprint 4 es donde empieza a entrar dato personal real |
| 5.8 Rate limit en `POST /appointments` | 1 | Sin esto cualquiera llena la agenda con turnos falsos |
| 7.12 Recuperación de contraseña del admin (Resend) | 5 | No existía flujo de reset |
| 7.13 Banner de consentimiento LGPD + gating del Pixel | 2 | El Pixel no puede disparar antes del consentimiento |
| 7.14 Derecho de eliminación (endpoint admin) | 1 | Exigido por LGPD |

**Neto: +5 hs.** El scope no creció: se cambió observabilidad opcional por seguridad no negociable.

---

## Stitch es la fuente de verdad del diseño

**Todas las pantallas se portan leyendo de Stitch vía MCP** (`projects/3403942466915386698`),
no del export local. Reglas de porteo (docs/ui stale, traducción de radios v3→v4, pantallas
faltantes): **`CLAUDE.md` §5** y **`docs/stitch-implementation-workflow.md`**.

- El prototipo de landing v1.0 (commit `010f8f87^`) **queda descartado**: está contra el
  design system v1.0 muerto, en español, y sus tipos de `data.ts` contradicen el ADR 013
  (mezclan especie y categoría en un campo plano). No se reutiliza nada.

**Hitos clave**:
- 🎯 Sprint Despliegue (fecha flexible, después del Sprint 3 — ver nota en "Resumen de
  fases"): primera URL pública en vivo
- 🎯 Sprint 4 (semana 10): primera venta posible
- 🎯 Sprint 6 (semana 14): primera reserva online posible
- 🎯 Sprint 8 (semana 17): MVP completo entregado

---

## Prácticas recurrentes (aplican a todo el proyecto)

Estas no son tareas de un sprint puntual: son hábitos que se ejecutan **durante todo el
proyecto**. El costo está distribuido y no se contabiliza como tareas separadas.

> 🏗️ **Estilo de ejecución (ADR 017 — frontend-first híbrido)**: la superficie pública se
> construye UI-primero con datos estáticos (design system → componentes → vistas → deploy).
> **Excepciones**: booking backend se adelanta (no se difiere), y el admin se hace vertical.
> Swap por superficie, no big-bang. Datos siempre en `frontend/lib/data/` **tipados contra
> los DTOs reales** (ADR 013), nunca hardcodeados.

Las prácticas por feature/decisión/bug/dependencia (plan en prosa, referencias visuales,
tests del happy path, mobile real, ADRs, `learnings.md`, test "¿vanilla?") viven en
**`CLAUDE.md` §9** — acá solo lo que es de calendario:

### Por cada semana
- **Demo en Loom** (3-5 min) los viernes mostrando lo nuevo. Aunque nadie la mire al
  principio, queda registro para el cliente y para tu portfolio.
- **Una sesión de "no escribir código"**: leer docs, blog posts, repos open source en
  el stack. La diferencia entre junior y mid es saber qué *no* hay que hacer, y eso solo
  se aprende leyendo.

### Cada 2 sprints (demo formal al cliente)
- Video Loom + mensaje con pedido explícito de feedback:
  > "Te dejo el avance del mes. ¿Hay algo que quieras cambiar antes de seguir?
  > Si no me respondés en 3 días, sigo con el plan original."

---

## Sprint 0 — Pre-kickoff (semana previa, ~3 hs)

**Objetivo**: cerrar lo pendiente del setup base antes de arrancar el desarrollo.

**Ya completado** (no consume horas):
- ✅ Repo en GitHub creado (monorepo)
- ✅ `CLAUDE.md`, `ROADMAP.md` subidos al repo
- ✅ Design tokens definidos — **en `app/globals.css` vía `@theme`, no en `tailwind.config.ts`**
  (ese archivo no existe y no debe recrearse — ver ADR 014)
- ✅ `docs/design-system.md` v2.0 con paleta, escalas y tipografía documentadas

**Pendiente**:

| # | Tarea | Hs |
|---|-------|----|
| 0.1 | Setup de issues en GitHub (milestones por sprint) para trackear las tareas de este ROADMAP | 1.5 |
| 0.2 | **Modelo de DB inicial en DBdiagram.io**: `tenants`, `users`, `categories`, `products`, `services`, `appointments`, `orders`. Exportar PNG a `docs/db-model.png` | 1.5 |

**Entregable**: issues del sprint creadas en GitHub + modelo de DB visualizable antes de la primera migración.

> El **plan y la secuencia viven acá, en `ROADMAP.md`** — no hay tablero Kanban aparte que
> mantener sincronizado. Los milestones de GitHub Issues son solo para trackear el estado
> (abierta/cerrada) de cada tarea; el AC de cada una vive en su issue (ver `CLAUDE.md` §9).

> 💡 Por qué la 0.2: dibujar la DB antes de migrar te ahorra 2-3 refactors de schema
> en sprints 3-5. Cuesta 1.5 hs ahora, te ahorra 10 hs después.

---

## Sprint 1 — Setup local (~16 hs, ~50% hecho)

**Objetivo**: backend y frontend corriendo en localhost con auth, primer endpoint conectado,
testing y observabilidad locales listos. **No hay despliegue todavía** — todo en localhost
contra Postgres en Docker.

> ♻️ **Auth (1.3/1.6) se porta del repo consultorio**, no se escribe de cero: JWT, security
> config, exception handler y validador de env vars. Qué copiar y qué cambiar (Bearer→cookie,
> login que setea cookie, sacar register) está en **[docs/reuse-consultorio.md](docs/reuse-consultorio.md)** §1-2-4.

| # | Tarea | Hs | Estado |
|---|-------|---:|---|
| 1.1 | Setup Spring Boot 3 con estructura modular: `tenant`, `identity`, `catalog`, `booking`, `orders`, `notifications` | 4 | 🔄 `tenant` y `catalog` con entidades JPA reales; `identity`/`booking`/`orders`/`notifications` siguen en `.gitkeep` |
| 1.2 | Postgres en Docker Compose + Flyway + primera migración | 2 | ✅ **adelantado**: V1–V4 cubren identity, catalog, booking y orders (tareas 3.1 / 4.1 / 5.1 ya hechas) |
| 1.3 | Spring Security + JWT en cookie HttpOnly + endpoint `POST /api/v1/auth/login` funcional | 3 | ✅ **hecha** (`9f16dc8`, PR #24) — portado del repo consultorio como estaba previsto |
| 1.4 | Setup Next 16 + Framer Motion + TanStack Query + React Hook Form. Verificar que los tokens de `@theme` en `globals.css` funcionan end-to-end (**no hay `tailwind.config.ts` — ver ADR 014**) | 1.5 | ✅ |
| 1.5 | CORS configurado, primer endpoint del frontend consumiendo backend local | 1 | ✅ **hecha** — se cerró con la 3.6a (`ed5fc19`): el cliente de API ya consume `/produtos` real |
| 1.6 | **Testcontainers + primer test de integración** del endpoint de login. Sirve como template para todos los siguientes | 2 | ⚠️ **pendiente** — Testcontainers sigue sin estar en el `pom.xml`; los tests de catálogo (3.2/3.4) corren sin él. Es la deuda que la nota original advertía: ya llevamos 3 sprints de tests sin el template |
| 1.7 | **Logback con JSON structured output** + endpoint `/actuator/health` configurado y testeado | 0.5 | ⏳ pendiente |
| 1.8 | **Rate limit + lockout en el login**: máx. N intentos por IP/usuario en ventana, backoff. Un solo usuario admin y sin protección de fuerza bruta es un login de juguete | 2 | ⏳ pendiente |

**Entregable**:
- Backend en `localhost:8080`, frontend en `localhost:3000`, ambos conectados
- Login funcional contra DB real (Postgres en Docker)
- Test de integración del login corriendo verde
- Estructura de proyecto lista para empezar a sumar features

**Hito**: stack local operativo end-to-end.

**Riesgos**:
- Postgres + Flyway dentro de Docker puede dar problemas de conexión la primera vez
- Testcontainers tarda la primera vez si descarga imágenes (~10 min)
- JWT en cookie HttpOnly + CORS local puede requerir ajuste fino con SameSite=Lax

> 💡 Por qué el test de integración va acá y no después: si no instalás testing en el
> sprint 1, no lo vas a instalar nunca. El costo marginal ahora es 2 hs; después de
> tener 10 features, son 15 hs y media de refactor.

---

## Sprint 2 — Landing pública (~24,5 hs)

**Objetivo**: landing comercial pulida y mobile-first, lista para mostrar al cliente.
Sigue corriendo en local — el despliegue es la siguiente etapa.

> **Fuente**: pantalla `FrontPet - Landing Page com Rodapé Sincronizado`
> (`projects/3403942466915386698/screens/5ce9a4555404479fb7e807816cda053c`), MOBILE 780px.
> Se lee del MCP de Stitch, **no** de `docs/ui/` (stale) ni del prototipo v1.0 (descartado).

| # | Tarea | Hs |
|---|-------|----|
| 2.0b | **Sync desde Stitch**: leer la pantalla vía MCP, traducir radios, mapear la paleta, listar divergencias. **Reemplaza al moodboard** — la referencia visual ya existe | 1 | ✅ **hecha** → `docs/port-landing-stitch.md`. Las 8 decisiones **resueltas** (§5); tokens ya en `globals.css` |
| 2.0c | **Mapear los 53 iconos de Material Symbols a `lucide-react`** — no estaba estimado; salió de la 2.0b | 1.5 |
| 2.0d | **`<BottomNav>` mobile** (`md:hidden fixed bottom-0`) — existe en Stitch, no estaba en el ROADMAP. Ojo con la colisión a 320px contra el FAB del carrito y el FloatingWA | 2 |
| 2.1 | Estructura general de la landing en componentes React | 3 |
| 2.2 | `<Hero>` con animaciones Framer Motion (fade-in, slide-up) | 3 |
| 2.3 | `<TrustBar>`. `<AnnouncementBar>` descartado (ver Registro, 2026-07-25) | 1 |
| 2.4 | `<ServiceCard>` (preview, sin booking todavía, link a `/agendamento`) | 2 |
| 2.5 | `<ProductCard>` con CTA WhatsApp directo (variante para landing) | 2 |
| 2.6 | `<Reviews>` con 3-4 testimonios estáticos | 1 |
| 2.7 | `<FloatingWA>` con pulse animation. `<FinalCTA>` descartado (ver Registro, 2026-07-25) | 2 |
| 2.8 | Footer con links, contacto, redes | 1 |
| 2.9 | Optimización Lighthouse: imágenes, fuentes, Core Web Vitals > 90 | 3 |
| 2.10 | Responsive completo: 320px / 768px / 1024px / 1440px | 2 |

**Entregable**: landing pulida en `localhost:3000`, lista para deployar la próxima semana.

**Hito**: aprobación visual del cliente (screenshots + Loom).

> ⚠️ **Antes de codear hay que cerrar dos cosas abiertas del design system**:
> 1. La paleta de `@theme` sigue siendo **placeholder** pendiente de confirmación del cliente.
> 2. El radio de las cards: la prosa de `DESIGN.md` dice 16px, las pantallas de Stitch las
>    dibujan a 12px. Comparar contra el `screen.png` al portar y fijar `--radius-lg`.
>
> Las dos se resuelven mirando la landing real. Si el cliente no confirma la paleta,
> **no bloquear el sprint**: codear con los tokens actuales — cambiarlos después es tocar
> `@theme`, no los componentes. Ese es justamente el punto de tener tokens.

---

## Sprint 3 — Catálogo + Backend de productos (~24 hs)

**Objetivo**: catálogo dinámico desde DB con detalle por producto y CRUD vía API.

| # | Tarea | Hs |
|---|-------|----|
| 3.1 | ~~Migración SQL: `categories`, `products`, `product_images`~~ — **ya hecha en `V2__catalog.sql`** | ~~2~~ 0 |
| 3.2 | Modelos JPA + repositorios + servicios | 2 | ✅ **hecha**. `Product/Category/Brand/Species/ProductVariant` + repos + `ProductService`/`CategoryService`/`BrandService`. Suma `slug` (V7, no estaba en el V2 original) y alta inline de marcas (`findOrCreate`, V9) — no estimados en el ROADMAP original |
| 3.3 | Endpoints públicos: `GET /api/v1/products`, `GET /api/v1/products/{slug}`, `GET /api/v1/categories` | 3 | ✅ **hecha**, + `GET /api/v1/species`. Requirió adelantar la base de `SecurityConfig` (sin ella, `starter-security` bloqueaba todo con 401 — no estaba en el scope de esta tarea, es la 1.3) |
| 3.4 | Endpoints admin protegidos: `POST/PUT/DELETE /api/v1/admin/products` | 2 | ✅ **hecha** |
| 3.4b | **Gestión de variantes vía admin**: `PUT /admin/products/{id}/variants`, upsert por id + soft-delete. Hueco encontrado al cerrar 3.4 (docs/pending-decisions.md §5, ahora resuelta) — no estaba en el ROADMAP original | 2 | ✅ **hecha (2026-07-27)**. Sin mode-switching precio-simple↔variantes (§6 de pending-decisions, ~3-4hs si se agenda) |
| 3.5 | Integración Cloudflare R2 SDK en backend + endpoint de upload firmado. ✅ **Prerequisito resuelto (2026-07-27)**: cuenta Cloudflare + bucket R2 (`frontpet-products`) creados, credenciales del API Token (`R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET_NAME`, `R2_ENDPOINT`) guardadas fuera del repo, listas para cargarlas como env vars cuando se implemente esta tarea | 3 | ✅ **hecha (2026-07-27)**. `POST /api/v1/admin/products/images/presign`. ⚠️ Suma env var `R2_PUBLIC_URL` (no estaba en la lista original — necesaria para armar `mainImageUrl` después del upload) |
| 3.5b | **Restricción del presigned URL**: mime type permitido, tamaño máximo y expiración corta, firmados en la política. Sin esto el bucket es de subida libre a costa nuestra | 1.5 | ✅ **hecha (2026-07-27)**. ⚠️ `Content-Type` sí queda firmado/aplicado por R2; el tamaño máximo **no** — R2 no soporta `content-length-range` (a diferencia de S3). Queda como chequeo sobre lo declarado, aceptado porque el endpoint es admin-only (docs/pending-decisions.md §8) |
| 3.6a | **Cliente de API en el frontend**: wrapper de `fetch` (`frontend/lib/api/`), base URL desde `NEXT_PUBLIC_API_URL`, manejo de 404/error. Sin librería nueva (TanStack Query entra cuando 3.7/3.8 necesiten cache/refetch). Agregada al plan — ver Registro, 2026-07-27 | 1.5 | ✅ **hecha** (`ed5fc19`) |
| 3.6 | Frontend: página `/produtos` con grid responsive | 2 | ✅ **hecha** (`dfeb1f2`), con paginación |
| 3.7 | Filtro por categoría (chips horizontales scrolleables) | 2 | ✅ **hecha** (`e74c200`) |
| 3.8 | Búsqueda por nombre con debounce (300ms) | 2 | ✅ **hecha** (`73cb5dc`) |
| 3.9 | Página `/produtos/[slug]` con **una** imagen y descripción (galería es Fase 2, ver `CLAUDE.md` §7). **`params` es una Promise en Next 16** — usar `PageProps<'/produtos/[slug]'>` de `next typegen` | 2 | ✅ **hecha** — shell + fetch real (`ed5fc19`) + variant picker/gating de CTA (`67d6cbf`) |
| 3.10 | Skeletons de carga, estados vacíos, error boundaries | 1 | ✅ **hecha** (`c3ed00a`) |
| 3.11 | Caching del catálogo en Next 16. ⚠️ **Esta tarea está escrita para Next 14**: `revalidateTag` ahora exige un segundo argumento (perfil de `cacheLife`) y PPR se activa con `cacheComponents`. Revisar contra la doc de 16 antes de implementar | 1 | ✅ **hecha** — ISR vía `next.revalidate` (`a80df15`) |
| 3.12 | Seed con 10 productos de prueba | 1 | ✅ **hecha** — `backend/.../db/seed-dev/products.sql`, fuera de Flyway a propósito (no es apto para producción, ver comentario del archivo) |

**Entregable**: catálogo navegable real en producción, indexable por Google, optimizado.

**Hito**: cliente puede empezar a planificar sus fotos y descripciones reales.

**Riesgos**:
- Upload de imágenes a R2 con presigned URLs puede tomar 2 hs extra la primera vez
- Decidir el tamaño/formato de imágenes (recomendado: WebP, max 1200x1200, < 200KB)

---

## Sprint Despliegue (~10,5 hs, fecha flexible)

**Objetivo**: comprar la infraestructura, configurar todo, **dejar lo construido hasta
ahora en vivo** (landing + catálogo) con dominio propio. Este sprint dura 1 semana sola.

> 🔁 Se ejecuta cuando el cliente confirme el pago/gasto de infra, no en una semana fija —
> pero siempre antes del 05/09. Rationale completo: Registro de decisiones de plan (2026-07-27).
> La D.3 (cuenta Cloudflare + R2) ya no depende de este sprint — se adelantó a la 3.5.

> **Arquitectura de deploy definida en el [ADR 016](docs/decisions/016-deploy-frontend-vps-coolify.md)**:
> un solo VPS sirve backend + frontend vía Coolify. Cloudflare queda como DNS + CDN + R2.
> No hay Cloudflare Pages ni adapter de OpenNext.

### Compras y cuentas (~2.5 hs — D.3 se adelantó a la tarea 3.5)

| # | Tarea | Hs |
|---|-------|----|
| D.1 | Compra de VPS Hetzner **CX33** (ex-CX32, 4 vCPU / 8 GB, ~€10/mes) **región Falkenstein/Nuremberg** (Ashburn no tiene este tier — ver ADR 016 §3, cambio 2026-09-01) + SSH key inicial | 1 |
| D.2 | Compra de dominio + configuración DNS apuntando al VPS | 1 |
| D.4 | Cuenta Sentry (tier gratis) + Plausible o Umami | 0.5 |

### Configuración y deploy (~7,5 hs)

| # | Tarea | Hs |
|---|-------|----|
| D.5 | Instalar Coolify en el VPS, HTTPS automático con Caddy | 3 |
| D.6 | Configurar Coolify para buildear y servir el **frontend Next 16** con auto-deploy desde GitHub (junto al backend) | 1 |
| D.7 | Backups automáticos: cron + `pg_dump` + upload a Cloudflare R2 | 2 |
| D.8 | Sentry activado en backend y frontend, probar primer error capturado intencionalmente | 0.5 |
| D.9 | Correr `/security-review` sobre el branch antes de exponer la URL pública: rate limiting en login, headers de seguridad (Caddy), CORS restrictivo al dominio real, cookies `Secure`, validación de upload de imágenes (tipo/tamaño), `pnpm audit` / dependencias | 0.5 |
| D.10 | **Checklist de contenido antes de exponer la URL** (agregada 2026-08-03, auditoría de release) — ver detalle abajo | 0.5 |

#### D.10 — Checklist de contenido pre-público

Cosas que hoy son placeholders inofensivos en `localhost` y dejan de serlo con una URL
pública. Ninguna se detecta con `/security-review` ni con el DoD, por eso van listadas:

- [x] **Depoimentos reales** — `<Reviews>` y `<TrustBar>` leen datos reales de la Places API
      (ADR 025, 2026-09-01), sin fallback a testimonios inventados. Falta solo confirmar
      periódicamente que el Google Business siga teniendo reviews (si llegan a cero, la sección
      se queda sin cards sola). Detalle: `pending-decisions.md` §15
- [ ] **`metadataBase` / dominio real** en `app/layout.tsx:93` — hoy TODO con dominio
      placeholder; afecta a todas las URLs absolutas de OG y canonical
- [ ] **`/public/og-image.jpg` real** (1200x630) — `app/layout.tsx:63`, hoy sin imagen: los
      links compartidos por WhatsApp salen sin preview
- [ ] **Código de verificación de search console** — `app/layout.tsx:99`, hoy placeholder
- [ ] **`themeColor` de `app/layout.tsx:117`** — hex hardcodeado (`#F8F9FF`) que la API
      `Viewport` de Next no deja leer de `@theme`; si el cliente confirmó paleta (§6.1),
      sincronizarlo a mano acá
- [ ] **Número de WhatsApp definitivo** en el seed (`preguntas-cliente.md` §7.3) y en
      `lib/data/site.ts` (hoy TODO)
- [ ] **Restringir `GOOGLE_PLACES_API_KEY` por IP** en Google Cloud Console a la IP del VPS
      (ADR 025) — hoy sin restricción de aplicación porque en dev la IP local cambia; ya está
      restringida a la Places API únicamente, falta la restricción de IP una vez que exista la
      IP fija de producción (Sprint Despliegue)
- [ ] **Fotos reales** de productos, serviços y hero (`preguntas-cliente.md` §6.3) — o asumir
      explícitamente que se sale con los gradientes placeholder

**Entregable**:
- `https://frontpet.com` con landing + catálogo en vivo
- `https://api.frontpet.com` respondiendo (health check OK)
- Push a `main` dispara deploy automático (frontend y backend)
- Backups corriendo
- Sentry recibiendo errores

**Hito** 🎯: primera URL pública. Es buen momento para enviar Loom al cliente con el link
en vivo y empezar a recibir feedback con tráfico real.

**Riesgos**:
- Coolify es nuevo → invertir 1 hora extra fuera del sprint para leer su doc antes de arrancar
- DNS puede tardar hasta 48hs en propagar
- Primer deploy de Spring Boot a contenedor puede requerir ajuste de memoria del VPS
- **Los 3 servicios (Spring Boot + Postgres + Next) comparten los 8 GB del CX33.** Vigilar
  RAM en el primer deploy; el CX23 (ex-CX22) de 4 GB del plan original directamente no alcanzaba
- **Postergarlo demasiado también es un riesgo**: si la conversación de pago con el cliente
  se estira, este sprint (10 hs, con DNS de hasta 48hs de propagación) se termina apretando
  contra el 05/09 igual. No es un colchón infinito.

---

## Sprint 4 — Carrito + Pedidos WhatsApp + Admin productos (~30 hs) 🎯 hito de cobro

**Objetivo**: cierre del flujo de venta + autonomía del cliente sobre el catálogo.
**Este sprint es el que habilita el segundo pago del 05/09.**

> ⚠️ **30 hs es el sprint más pesado del plan.** Partirlo en dos mitades entregables:
> **4A venta pública** (4.1-4.10, 4.15, 4.16 — ~20 hs) y **4B admin** (4.11-4.14 — ~10 hs).
> Si algo desborda, 4A sola ya es un hito defendible ante el cliente: la tienda vende.

| # | Tarea | Hs |
|---|-------|----|
| 4.1 | ~~Migración SQL: `orders`, `order_items`~~ — **ya hecha en `V4__orders.sql`** (sin tabla `customers`: el cliente va embebido en `orders`, ADR 013 §4) | ~~1~~ 0 |
| 4.2 | Hook `useCart()` con sessionStorage: add, remove, update qty, clear | 3 | ✅ **hecha** (`d1b8d16`, PR #36) |
| 4.3 | `<CartButton>` flotante con contador animado | 1 | ✅ **hecha**, junto con 4.5 (`07b0414`, PR #37) |
| 4.4 | `<CartDrawer>` o `/carrito` con lista editable | 3 | ✅ **hecha** — fusionada con el checkout (4.6) en una sola vista `/carrinho` (`29b4e2e`, PR #38), sigue el mock de Stitch "Sua Sacola". No existe `/carrito` ni `/checkout` como rutas separadas — ver Piloto 2 en `docs/stitch-implementation-workflow.md` |
| 4.5 | Botón "Agregar al carrito" en `<ProductCard>` con feedback visual | 1 | ✅ **hecha**, junto con 4.3 (`07b0414`, PR #37) |
| 4.6 | Página `/checkout` con formulario (nombre, WA, modalidad, dirección, notas) | 2 | ✅ **hecha** — fusionada dentro de `/carrinho` (ver 4.4), no es ruta separada. Form real de 6 campos, no 3 (ADR 003 act. 2026-07-28) |
| 4.7 | Validación con Zod + React Hook Form | 1 | ✅ **hecha**, parte del form de `/carrinho` (`29b4e2e`) |
| 4.8 | Endpoint `POST /api/v1/orders`: persiste pedido + items, retorna ID | 2 | ✅ **hecha** (`230de00`, `a98f422`, `2cfdee5`, PR #40) |
| 4.9 | Generación del mensaje WhatsApp con loop sobre items | 2 | ✅ **hecha** — en el frontend, no en el backend (`0bbdc46`, `frontend/lib/whatsapp/templates.ts`, ver ADR 010 act.) |
| 4.10 | Redirect a `wa.me/...?text=...` después del POST exitoso | 1 | ✅ **hecha**, parte de `29b4e2e` |
| 4.11 | Admin: shell del panel (sidebar, layout, auth guard) | 3 | ✅ **hecha** (`e7873a3`, PR #39) |
| 4.12 | Admin: CRUD de productos (tabla + form + upload imagen) | 4 | ✅ **hecha** (`0b09411`, `995e111`, `989f858`, PR #41) |
| 4.13 | Admin: CRUD de categorías (simple, inline) | 1 | ✅ **reinterpretada** — las categorías son lista cerrada sin CRUD de admin (CLAUDE.md §6, decidido 2026-07-25, previo a este sprint). Lo implementado es la asignación categoría/espécie (N:M) vía checkboxes dentro del form de producto (`989f858`), no un CRUD |
| 4.14 | Admin: vista de pedidos con cambio de estado (`PENDING/CONFIRMED/CANCELLED`) | 2 | ✅ **hecha** (`2cfdee5`, `764c0cd`, `24f7d35`, PR #40/#42) |
| 4.15 | **Rate limit + honeypot en `POST /api/v1/orders`**: es un endpoint público y anónimo. Sin esto, cualquiera con curl inunda la bandeja del cliente | 2 | ✅ **hecha** (`5478fe3`, `ff86cb7`) |
| 4.16 | **Política de privacidad + aviso de tratamiento de datos en el checkout (LGPD)**: página `/privacidade` en PT-BR + checkbox de consentimiento. Sprint 4 es donde empieza a entrar dato personal real (nome, WhatsApp, endereço embebidos en `orders`, sin tabla `customers` — ADR 013 §4) | 2 | ✅ **hecha**, parte de `29b4e2e` |

**Entregable**: **FrontPet puede vender por WhatsApp y gestionar su catálogo.**

**Hito** 🎯 **05/09 — primera venta real posible.** Es el hito contra el que se pide el
segundo pago. Avisale al cliente y validá el modelo con tráfico real.

> ⚠️ **Interino hasta el Sprint 7**: el reset de contraseña del admin (7.12) todavía no
> existe en esta etapa. Si el cliente se bloquea entre el Sprint 4 y el 7, lo resolvés a
> mano vos. Es aceptable por unas semanas — no como estado de entrega.

---

## Sprint 5 — Booking backend (~19 hs) ⏩ adelantar al bloque A si hay slack

> ✅ **Cerrado 2026-07-30.** Los 8 bloques (0, A-G) mergeados a `main` (PRs #52-56, issues
> #44-51 cerradas). Corrió ~26,5 hs vs las 19 estimadas — detalle del desvío y de la decisión
> de diferir el turno manual del admin a Sprint 6 en el Registro de decisiones de plan.

**Objetivo**: modelar la agenda y resolver el query difícil de slots disponibles.

> ♻️ **El algoritmo de slots (5.3) se traduce del repo consultorio** (agnóstico a la duración,
> encaja con base+adicionais×porte). Cambios obligatorios —capacidad=N, `tenant_id`, `end_at`,
> **lock de concurrencia** que el original no tiene— en **[docs/reuse-consultorio.md](docs/reuse-consultorio.md)** §3.

> ⏩ **Este es el sprint a adelantar con el slack del bloque A** (~24 hs libres antes del
> 05/09). Es la parte más riesgosa del MVP y no depende de nada del bloque B. Adelantarlo
> lleva el margen del bloque B de 3,5 hs a ~22 hs.

| # | Tarea | Hs |
|---|-------|----|
| 5.1 | ~~Migración SQL: `services`, `resources`, `schedule_rules`, `appointments`~~ — **ya hecha en `V3__booking.sql`** | ~~2~~ 0 |
| 5.2 | Modelos JPA + repositorios | 2 |
| 5.3 | **Servicio de cálculo de slots disponibles** (el query difícil). Antes de codear: plan en prosa en un ADR. ⚠️ **Se complicó desde que se escribió**: con el ADR 011 la duración no es fija — sale de banho base + N adicionais, con duración por porte (P/M/G/GG). El cálculo depende del combo elegido, no del servicio | 7 |
| 5.4 | Endpoint `GET /api/v1/availability?service=X&date=Y` | 2 |
| 5.5 | Endpoint `POST /api/v1/appointments` con validación de solapamientos | 3 |
| 5.6 | Tests de integración: race conditions, slots de borde, servicios largos | 3 |
| 5.7 | Admin: **edición** del catálogo de serviços + configuración de `schedule_rules`. **No es CRUD completo**: el admin edita, no crea ni borra (ADR 011) | 1 |
| 5.8 | **Rate limit en `POST /api/v1/appointments`**: endpoint público y anónimo contra una agenda con capacidad real. Sin esto, cualquiera llena el día de turnos falsos y el cliente se entera atendiendo a nadie | 1 |

**Entregable**: API de booking funcional (sin frontend público todavía).

**Hito**: punto técnicamente más complejo del MVP superado.

**Riesgos**:
- El query de slots es el riesgo #1 de todo el plan → leé el ADR 005 y el 011 antes de arrancar
- La duración depende de porte + adicionais (ADR 011), no del servicio → **resolver con tests
  primero**: casos de borde, servicios largos que cruzan el cierre, race conditions
- La capacidad es un número único del tenant (`tenant.config.capacidade_atendimento`, ADR 009),
  no profesionales nominales → los slots se cuentan contra ese número

---

## Sprint 6 — Booking frontend + Admin de agendamentos (semanas 13-14, ~16 hs estimadas → ~27-30 hs reales)

**Objetivo**: cierre del flujo de reservas end-to-end.

> ✅ **Cerrado 2026-08-03.** La estimación original de 16 hs / 7 tareas (6.1-6.7) resultó
> incompleta: el ROADMAP no listaba el admin de serviços/horários, la página pública
> `/servicos` ni el turno manual del admin. El plan de ejecución completo (auditado contra
> Stitch, ADRs y código real) vive en `C:\Users\admin\.claude\plans\glimmering-wandering-tide.md`
> y reemplazó la tabla 6.1-6.7 original por 8 bloques (0/A-G). Ver Registro de decisiones de
> plan.

| Bloque | Contenido | Hs estimadas | Estado |
|---|---|---:|---|
| 0 | Contrato tipado (`lib/api/*`) + ADR 021 (turno manual) + ADR 008 (naming) | 2,5 | ✅ cerrado (PR #65) |
| A | Backend: turno manual admin + listado por rango + huecos de lectura de horários/bloqueios | 5,5 | ✅ cerrado (PR #67, commit `74abdd9`) |
| B | `/servicos` público | 3 | ✅ cerrado (PR #68) |
| C+D | Wizard `/agendamento`: passos 1-2 (serviço/porte/adicionais/data/slot) + passo 3 (form) + confirmação | 9,5 | ✅ cerrado (PRs #68, #69) |
| E | Admin `/admin/agendamentos`: vista semanal, turno manual, cambio de status, tempo-extra | 6 | ✅ cerrado (PR #70) |
| F | Admin `/admin/servicos`: tabs Serviços (CRUD de preço/duração por porte) y Horário (7 días + bloqueios). Incluyó cerrar un hueco de backend no listado (3 GETs admin que el contrato de Bloque 0 ya exigía) | 5 | ✅ cerrado (PR #71, issue #63) |
| G | Cierre: checklist Stitch, pilotos 5-8 documentados, DoD, doc sync | 2 | ✅ cerrado (issue #64) — **validación visual 320/768/1024px y prueba en celular real de `/admin/servicos` diferidas explícitamente a un pase de polish de UI posterior** (decisión de Sebastián, 2026-08-03); el resto de las 8 pantallas del sprint ya está validado |

**Entregable**: **FrontPet puede recibir reservas online.**

**Hito** 🎯: **primera reserva online posible** (semana 14) — ✅ alcanzado.

---

## Sprint 7 — Dashboard + LGPD + Marketing + Reset de senha + Polish (~24,5 hs)

**Objetivo**: sumar visibilidad de métricas, cumplir LGPD, cerrar el hueco de seguridad del
login y llevar todo a calidad de entrega.

| # | Tarea | Hs |
|---|-------|----|
| 7.1 | Endpoint `GET /api/v1/admin/dashboard` con queries agregados | 2 |
| 7.2 | Admin: componente `<KPICard>` reutilizable | 1 |
| 7.3 | Admin: mini-dashboard con **conteos operacionales** — pedidos día/mes/total, turnos día/próximos, top productos, top servicios. ⚠️ **Sin métricas analíticas** (ADR 003/008): nada de conversão, faturamento, ticket médio ni trend pills | 3 |
| 7.12 | ✅ **Recuperación de contraseña del admin**: token de un solo uso + expiración, email vía **Resend**, invalidación de sesiones JWT activas al resetear (ADR 022). Pantallas derivadas del login existente, sin pasar por Stitch — la estimación original (5h, con generación de pantalla en Stitch) quedó baja por ~2x: la invalidación de sesión, el cliente HTTP a mano contra Resend y el doble rate limit (IP + email) no estaban en el estimado inicial | ~11,75 |
| 7.13 | ✅ **Banner de consentimiento LGPD + gating del Pixel**: el Meta Pixel **no puede disparar antes del consentimiento**. Hecho **antes** de la 7.4. Pantalla derivada del design system, no generada en Stitch (ADR 024) | 2 |
| 7.14 | ✅ **Derecho de eliminación (LGPD)**: endpoint admin que anonimiza (no borra) los datos de un titular por telefone, con preview + log de auditoría (ADR 023) | 1 |
| 7.4 | ✅ Meta Pixel: instalación base + eventos estándar (Contact, ViewContent, Schedule, Purchase). Gateado por 7.13 | 2 |
| 7.6 | ✅ Plausible: instalación con script tag en `<head>`, leyendo `NEXT_PUBLIC_ANALYTICS_DOMAIN` | 0.5 |
| 7.7 | Auditoría mobile completa, ajustes finos | 3 |
| 7.9 | SEO básico: meta tags, Open Graph, sitemap.xml, robots.txt | 2 |
| 7.10 | Optimización de imágenes: WebP, lazy loading, srcset. ⚠️ Next 16: `images.domains` está deprecado → `images.remotePatterns` para el bucket R2 | 1 |
| 7.11 | Testing manual en navegadores reales (Chrome mobile, Safari iOS, Firefox) | 2 |

### Opcional — solo si sobra tiempo (6,5 hs, no bloquean la entrega)

| # | Tarea | Hs |
|---|-------|----|
| 7.8 | Pulido de animaciones y micro-interacciones | 2 |
| 7.15 | Sentry: revisar dashboard, ajustar rate limits y alertas | 1.5 |
| 7.16 | Uptime Kuma o equivalente, monitorear endpoints clave | 1 |
| 7.17 | Verificación de backups: simulación de restore en otro entorno | 1 |
| 7.18 | Meta Pixel server-side (Conversions API) | 1 |

**Entregable**: sistema en estado de entrega.

---

## Sprint 8 — Capacitación + Entrega formal (semana 17, ~14 hs)

**Objetivo**: que FrontPet pueda operar de forma autónoma.

| # | Tarea | Hs |
|---|-------|----|
| 8.1 | Documentación de uso para FrontPet: PDF corto + screenshots | 3 |
| 8.2 | Video Loom de 10-15 min recorriendo el admin paso a paso | 2 |
| 8.3 | Sesión de capacitación con FrontPet (presencial o videollamada) | 2 |
| 8.4 | Carga de productos reales con el cliente (acompañado) | 2 |
| 8.5 | Configuración de horarios reales con el cliente | 1 |
| 8.6 | Setup de Meta Pixel con el ID real del cliente | 0.5 |
| 8.7 | Última pasada general: links, fotos, textos | 1 |
| 8.8 | Entrega formal: documento de cierre, accesos, contactos | 1 |
| 8.9 | Activación del primer mes de mantenimiento | 0.5 |
| 8.10 | Comunicación a redes de FrontPet del lanzamiento (apoyo) | 1 |

**Entregable**: MVP1 entregado y operando en producción.

**Hito** 🎯: **segundo pago de USD 250 liberado.**

---

## Colchón — Sprint 9 (semanas 18-19, hasta 21 hs)

Buffer para:

- Bugs que aparezcan en producción durante uso real
- Feedback de FrontPet en las últimas semanas
- Parciales académicos o semanas de menor disponibilidad
- Viajes o imprevistos personales
- Si todo va perfecto: pequeños extras no comprometidos

**Importante**: si se consume el colchón completo, **no se agrega scope adicional**. Las
features extras que el cliente pida durante el colchón se documentan y se cotizan como Fase 2.

---

## Definition of Done por tarea

La DoD canónica vive en **`CLAUDE.md` §10** (main sin warnings, mobile real, test de
integración, validación visual, screenshot, ADR, db-model, build/lint/test desde WSL).

**Desde Sprint Despliegue en adelante también aplica:**
- ✅ Deployado y verificado en producción
- ✅ Sin errores nuevos en Sentry post-deploy

> **No hay entorno de staging.** El CX33 ya reparte 8 GB entre Spring Boot, Postgres y Next;
> un segundo stack completo no entra sin riesgo de OOM, y un VPS aparte es infra extra en un
> proyecto de USD 500. Los cambios de frontend se validan con **preview deploys de Coolify**
> (efímeros y livianos); el backend se valida con los tests de integración de Testcontainers
> antes del push. Staging no afecta el rendimiento del sitio en vivo — es un entorno de
> prueba, no está en el camino del usuario.

---

## Demos al cliente

**Cada 2 sprints** (1 vez por mes), enviar al cliente:
- Video Loom de 3-5 min mostrando lo nuevo
- Mensaje con resumen escrito y pedido explícito de feedback:
  > "Te dejo el avance del mes. ¿Hay algo que quieras cambiar antes de seguir?
  > Si no me respondés en 3 días, sigo con el plan original."

Calendario de demos (re-baseado a julio 2026; Sprint Despliegue con fecha flexible desde
el 2026-07-27, ver nota en "Resumen de fases"):
- **Demo 1**: fin de Sprint 2 → landing pulida (Loom + screenshots)
- **Demo 2**: fin de Sprint 3 → catálogo navegable (todavía en local)
- **Demo Despliegue**: cuando se resuelva la conversación de pago con el cliente y se
  ejecute el sprint → primera URL pública en vivo 🎯 (landing + catálogo)
- **Demo 3**: fin de Sprint 4 → **primera venta posible** 🎯 **← 05/09, hito de cobro**
- **Demo 4**: fin de Sprint 6 → primera reserva posible 🎯
- **Entrega final**: **30/09/2026**

> ⚠️ **La conversación sobre el pago del 05/09 va a mediados de agosto, no al final.**
> El pedido es adelantar el segundo pago contra un hito real (la tienda vendiendo), no
> contra una promesa. Necesita OK por escrito antes de que la fecha esté encima.

---

## Registro de decisiones de plan

Decisiones fechadas que cambiaron el plan (no son ADRs — son de planificación, no de
arquitectura). El cuerpo del ROADMAP refleja solo el estado resultante; el porqué vive acá.

**2026-08-03 — Re-scoping de Sprint 6 (en cierre, Bloque G).** La estimación original del
ROADMAP (16 hs / 7 tareas, 6.1-6.7) se auditó contra Stitch, los ADRs y el código real antes
de codear (plan completo en `C:\Users\admin\.claude\plans\glimmering-wandering-tide.md`) y
resultó **~2x subestimada**: faltaban 3 piezas enteras que ningún sprint tenía asignadas —
`/servicos` público, el admin de serviços/horários/bloqueios, y el turno manual del admin (este
último ya señalado como diferido en el cierre de Sprint 5). Se repartió en 8 bloques (0, A-G,
ver tabla del sprint) en vez de las 7 tareas originales. Mismo patrón que Sprints 4 y 5: el
ROADMAP subestima consistentemente los sprints con más superficie nueva de UI/admin.

Un hueco de backend no anticipado ni por el ROADMAP ni por el plan de Sprint 6 apareció recién
al ejecutar el Bloque F: `GET /admin/services`, `GET /admin/business-hours` y
`GET /admin/schedule-blocks` no existían, aunque el contrato tipado del Bloque 0
(`lib/api/admin-schedule.ts`) ya los llamaba — se cerró dentro del mismo bloque (repo→service→
controller, sin lógica nueva, con tests). Detalle: `docs/stitch-implementation-workflow.md`,
Piloto 8.

**2026-07-30 — Cierre de Sprint 5.** Los 8 bloques (0, A-G) mergeados a `main` (PRs #52-56).
Dos decisiones quedan fijadas para consulta futura:

- **El turno manual del admin (`POST /admin/appointments`) queda diferido a Sprint 6**, junto
  con la vista de turnos (tarea 6.6) — decisión tomada al planificar el sprint, no un desvío
  de ejecución. Consecuencia real mientras tanto: `schedule_blocks` solo bloquea el día
  completo, no por rango horario (el ADR 012 pedía esto último); si el cliente empieza a
  operar por teléfono antes de que Sprint 6 cierre, la disponibilidad web puede sobre-ofertar.
  Detalle en `docs/pending-decisions.md` §10 y ADR 020.
- **La unificación del rate limit (login/orders/appointments, ahora 3 copias casi idénticas)
  se difiere a Sprint 7.** No tenía sentido tocar 3 módulos dentro del sprint marcado como
  "el riesgo #1 de todo el plan". Ver `docs/pending-decisions.md` §9.

**Horas**: estimado en 19 hs, corrió ~26,5 hs (+~40%). La diferencia se concentra en tres
puntos: 5.7 (admin de serviços/horarios/bloqueios, 1h estimada → 3h reales — son 3
controllers, 2 services y ~8 DTOs, no un simple CRUD), 5.6 (el test de concurrencia real sin
`@Transactional` con limpieza manual, y los casos de borde que se sumaron al verificarlo por
mutación), y 5.2 (2h → 3,5h con el lector de `tenant.config`). A esto se suman dos tareas que
el ROADMAP nunca presupuestó pero el proceso de esta issue exige: el ADR 020 (plan en prosa
antes de codear el query difícil) y el seed de adicionais provisorio — ambos en el Bloque 0.
Mismo patrón que el Sprint 4: el sprint más riesgoso del plan volvió a correr por encima de
su estimado, consistente con que "el riesgo #1" rara vez se subestima solo en la dirección
optimista.

**2026-07-29 — Cierre de Sprint 4.** Los 7 bloques (0, A-F ejecución, G este cierre)
mergeados a `main`. Dos decisiones de ejecución quedan fijadas para consulta futura:

- **Carrito y checkout se fusionaron en una sola vista `/carrinho`**, no las dos rutas
  separadas que sugería la numeración original (4.4 carrito / 4.6 checkout). Sigue el mock
  de Stitch ("Sua Sacola"), que ya dibuja lista + resumen + form + CTA como una sola
  pantalla — desdoblarla en dos rutas habría sido inventar una separación que el diseño no
  tiene. No existe `/checkout` en el código.
- **Los dos huecos de `docs/pending-decisions.md` (§6 mode-switching, §7 compresión de
  imagen) se incorporaron al Bloque E, no se cortaron.** Ver esa sección para el detalle de
  qué se implementó.

**Horas**: estimado originalmente en 30 hs, ajustado a ~35,5 hs al planificar cuando se
decidió incorporar §6/§7. La implementación real encontró además varios huecos de backend
no presupuestados ni en esa cifra ajustada — sin listado admin de productos, sin forma de
reactivar uno oculto, sin detalle admin para productos inactivos, `ProductDetail` sin
`active`, mismo patrón en orders (contadores por status, ver Bloque F) — todos chicos mirado
uno por uno, pero sistemáticos: cada bloque destapó al menos un endpoint que hacía falta y
nadie había notado hasta construir la pantalla real. No se llevó cronómetro exacto; la
lectura razonable es que el sprint corrió por encima de los 35,5 hs ajustados, en línea con
que el propio ROADMAP ya lo marcaba como "el más pesado del plan".

**2026-07-27 — Cierre de Sprint 3, arranque de Sprint 4, Despliegue pospuesto otra vez
(ahora después del Sprint 5).** Sprint 3 se da por cerrado: 3.1-3.12 completas, mergeadas
a `main` (PR #26). El estado real (verificado contra `git log`, no solo contra las
anotaciones de esta tabla) también dejó a Sprint 1 en ~60% y Sprint 2 en ~94% — las cifras
de ambos se corrigieron acá porque habían quedado desactualizadas respecto al trabajo hecho
de paso al avanzar el catálogo (p. ej. 1.5 se cerró sola con el cliente de API de la 3.6a).
Se sigue sin comprar infra: la conversación de pago con el cliente todavía no se dio, y se
decide adelantar **Sprint 4 (carrito + pedidos + admin) y Sprint 5 (booking backend)** antes
del Sprint Despliegue, en vez de desplegar solo landing+catálogo. Esto coincide con la
recomendación original del ROADMAP de adelantar el Sprint 5 con el slack del bloque A — solo
formaliza el orden. **Riesgo a vigilar**: el hito de cobro del 05/09 exige la tienda
"vendiendo y desplegada"; el Despliegue (10 hs) tiene que cerrar antes de esa fecha sin
importar qué tan tarde se ejecute — cuanto más se lo pisa contra septiembre, menos margen
queda para las hasta 48 hs de propagación DNS y el ajuste fino de Coolify. Arranca en la
branch `feat/cart-checkout` (Sprint 4A: carrito + checkout + pedidos por WhatsApp).

**2026-07-27 — Sprint Despliegue movido después del Sprint 3.** Comprar VPS y dominio
depende de que el cliente pague/apruebe el gasto, y esa conversación todavía no se dio —
no tiene sentido frenar el avance esperándola; se sigue con el Sprint 3 (catálogo)
mientras tanto. Sigue en el Bloque A y sigue teniendo que cerrar antes del 05/09 (el hito
de cobro exige "desplegada"), pero ya no está atado a "justo después de la landing". La
única dependencia real que generaba —la 3.5 necesita un bucket R2 para el upload firmado—
se desacopla: R2 tiene free tier y no requiere plata ni aprobación, la cuenta se creó sola
(prerequisito de 3.5 ya resuelto); VPS + dominio + Coolify sí esperan la conversación de pago.

**2026-07-27 — Tarea 3.6a agregada (cliente de API, 1.5 hs).** No estaba estimada en el
plan original; bloquea 3.6, 3.7, 3.8, 3.9 y 3.11 por igual. Encontrada al hacer 3.9.

**2026-07-25 — `<AnnouncementBar>` (2.3) y `<FinalCTA>` (2.7) descartados.** No existen
en la pantalla de Stitch (entre Reviews y Footer va directo) — no se inventa UI sin
referencia visual.

**2026-07-17 — Re-baseo completo del plan (v2.0).** El calendario original (19 semanas
desde mayo, 8-10 hs/semana) no sobrevivió: a la semana 8 el estado real era mitad de
Sprint 1. El desvío no vino de tareas subestimadas, sino de ~55 hs que el plan nunca
presupuestó: 16 ADRs, reunión y cuestionario con el cliente, design system v2.0, migración
a Tailwind v4, toolchain WSL, 19 pantallas de Stitch y la landing v1.0 descartada. Ese
impuesto no se repite: las decisiones grandes ya están tomadas — queda ~15% de
meta-trabajo, no 45%. Se re-basea contra 22 hs/semana y entrega 30/09.

---

**Última actualización**: 2026-07-30
**Versión del documento**: 2.3
