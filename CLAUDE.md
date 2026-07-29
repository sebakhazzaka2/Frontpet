# FrontPet

## 1. Qué es

Plataforma web comercial para FrontPet, un petshop local (cliente piloto):

- **Vender productos** vía catálogo online → pedidos por WhatsApp (sin checkout con pagos)
- **Reservar turnos** de servicios (baños, peluquería) online
- **Panel admin** simple para gestionar todo

Visión futura (no MVP1): SaaS multi-tenant para negocios locales.

---

## 2. Stack

### Backend
- **Java 21** + **Spring Boot 3.x** + **Maven** + Lombok
- **PostgreSQL 16** + **Flyway** (no MySQL, no NoSQL)
- **Spring Security + JWT** en cookies HttpOnly (nunca localStorage)
- **JUnit 5 + Testcontainers** para tests de integración
- **Logback con JSON output** desde el día uno

### Frontend

> Versiones **verificadas contra `pnpm-lock.yaml`** (jul 2026). Next y React
> **pineados exactos** a propósito. Si actualizás, actualizá esta tabla.

| Paquete | Versión | Nota |
|---|---|---|
| `next` | **16.2.6** | App Router. Turbopack default. ⚠️ **Trampas de Next 16: `docs/next16-notes.md`** — leer antes de código de rutas/caching/imágenes. Lo más frecuente: `params`/`searchParams`/`cookies()` son **Promises** |
| `react` / `react-dom` | **19.2.4** | |
| `typescript` | 5.9.3 | strict mode |
| `tailwindcss` | **4.3.0** | CSS-first: tokens en `globals.css` vía `@theme`. **No existe ni se recrea `tailwind.config.ts`**. Ver ADR 014 |
| `shadcn` (CLI) | 4.7.0 | + `radix-ui`, `cva`, `clsx`, `tailwind-merge`, `tw-animate-css` |
| `framer-motion` | 12.38.0 | |
| `@tanstack/react-query` | ^5.100.10 | |
| `react-hook-form` | 7.76.0 | + `@hookform/resolvers` ^5.2.2 |
| `zod` | **4.4.3** | ⚠️ v4, API distinta a v3. No copiar snippets de v3 |
| `lucide-react` | ^1.16.0 | iconos |

**Node.js 20.9+ obligatorio.** Toolchain corre en **WSL** (ADR 015).

### Infraestructura
Un solo **VPS Hetzner CX32** (4 vCPU/8 GB, **región Ashburn**) con **Coolify** sirve
Spring Boot + Next + Postgres; **Caddy** proxy/SSL; **Cloudflare** DNS+CDN+**R2**
(imágenes); GitHub Actions CI; Sentry; Plausible. Razonamiento completo (por qué no
Vercel/Pages/AWS, por qué CX32 y Ashburn): **ADR 016**.

⚠️ Hoy todo corre en **localhost**. No comprar infra hasta el Sprint Despliegue
(post-Sprint 3, gateado por conversación de pago con el cliente — ver ROADMAP).

### Tracking
**Meta Pixel** (eventos estándar) + **Plausible**. Tracking propio (tabla `events`) es Fase 2.

---

## 3. Decisiones arquitectónicas vigentes

Detalle en `docs/decisions/`. Resumen:

- **Monolito modular** en **monorepo** (`backend/` + `frontend/`)
- **Multi-tenant ready**: toda tabla del dominio lleva `tenant_id`
- **API REST** (no GraphQL) · **JWT en cookie HttpOnly**
- **Slots de booking calculados dinámicamente**, no materializados en DB
- **Carrito en sessionStorage**, no persistido hasta el envío del pedido
- **WhatsApp click-to-chat** (`wa.me/...?text=...`), no Business API
- **UUID v7** para entidades públicas, BIGSERIAL para internas

---

## 4. Estructura del repo

```
frontpet/
├── backend/src/main/java/com/frontpet/
│   ├── common/          Auditable, UuidV7, Slugify, PageResponse, ApiError, RestExceptionHandler
│   ├── config/          SecurityConfig, CorsConfig
│   ├── tenant/          Tenant, CurrentTenant (hoy fijo, MVP1 single-tenant)
│   ├── identity/        api/ + domain/ + Auth/Jwt/LoginRateLimit services
│   ├── catalog/         api/ + domain/ + dto/ (Product, Category, Brand, Species)
│   ├── orders/          api/ + domain/ + dto/ + OrderRateLimit
│   ├── booking/         [.gitkeep]   ├── notifications/  [.gitkeep]
│   └── BackendApplication.java   (en la raíz a propósito: component scan)
├── backend/src/main/resources/db/migration/   Flyway
├── frontend/
│   ├── app/(public)/  admin/(protected)/  admin/login/  api/   ((admin)/ es placeholder sin uso, ver ADR 006)
│   ├── components/ui/  public/  admin/
│   └── lib/            api client, hooks; datos en lib/data/
├── docs/               ADRs, design-system.md, db-model.png, next16-notes.md,
│                       stitch-implementation-workflow.md, port-landing-stitch.md,
│                       pending-decisions.md, preguntas-cliente.md, reuse-consultorio.md,
│                       learnings.md [gitignored], ui/ [gitignored, export stale — ver §5]
├── CLAUDE.md · ROADMAP.md · README.md
```

> Si la estructura real difiere, actualizá esta sección. Debe ser fuente de verdad.

---

## 5. Convenciones

### Commits y branches
- Conventional Commits **en inglés**, con scope en monorepo: `feat(web): ...`, `fix(backend): ...`
- **El usuario hace los commits** — Claude solo sugiere mensaje y archivos
- Branches `feat/nombre-corto` / `fix/nombre-corto`, vida corta, squash merge a `main`

### Backend (Java)
- Packages por feature (`catalog`, `booking`), no por capa
- Cada módulo expone una `*Service`; otros módulos consumen esa interfaz, **nunca el repositorio**
- DTOs con records; validaciones con `jakarta.validation`
- Tests: `*ServiceTest` (unit), `*IntegrationTest` (Testcontainers)

### Frontend (TypeScript)
- Componentes PascalCase `.tsx`; hooks `useX`; Tailwind directo (no CSS modules)
- Server Components por defecto; `"use client"` solo con interactividad real
- Siempre `<Image>` de Next, nunca `<img>`

### Metodología (frontend-first híbrido — ADR 017)
- Superficie pública: design system → componentes → vistas con datos estáticos → deploy
- **Excepciones**: booking backend se adelanta (riesgo concentrado); admin se hace **vertical**
- Swap estático→API **por superficie**, no big-bang
- Componente inexistente → crearlo antes de usarlo; **nunca duplicar** (revisar `components/`)
- **Datos siempre en `frontend/lib/data/`, tipados contra los DTOs reales** (ADR 013), nunca hardcodeados

### API REST
- URLs kebab-case, JSON camelCase, versionado `/api/v1/...`
- Códigos HTTP estándar: 200, 201, 400, 401, 403, 404, 409, 422, 500

### Design system
- **Tokens: `frontend/app/globals.css`, bloque `@theme` — fuente canónica** (ADR 014).
  Cambios de paleta se hacen ahí, nunca hex hardcodeados en componentes.
  ⚠️ Los valores actuales son placeholders pendientes de confirmación del cliente —
  seguir codeando con ellos (ajustar después es tocar 1 archivo).
- **Diseño: proyecto Stitch `projects/3403942466915386698`, leído por MCP.**
  Workflow completo de porteo: `docs/stitch-implementation-workflow.md`. Claves:
  - `docs/ui/` es un export **viejo y stale** — solo para mirar `screen.png` offline, no se porta desde ahí
  - Stitch corre Tailwind **v3**: **ninguna clase `rounded-*` sobrevive un copy-paste** (tabla de traducción: ADR 014)
  - Login es la única pantalla DESKTOP; las otras 18 son MOBILE (780px)
  - No existen pantallas de reset de contraseña ni banner LGPD (generarlas en Stitch — tareas 7.12/7.13)
- **Paleta — 4 colores base** + 1 semántico:
  Navy `#011E5A` (identidad/estructura, botón "main system action") ·
  Orange `#F4640D` (acciones comerciales) · Green `#25D366` (solo WhatsApp) ·
  Slate `#64748B` (texto secundario/borders) · `--color-star #E0A82E` (solo rating)
- **Tipografía**: Fredoka (headlines) + Plus Jakarta Sans (body), pesos 400/500/600 (nunca 700+)
- **Spacing**: solo múltiplos de 4 (`4…80`). **Prohibidos los valores arbitrarios** (`mt-[13px]` no)
- **Radius (canónico, definido en `@theme`)**: `sm` 4px badges · `md` 8px **botones/inputs/chips** ·
  `lg` 16px **cards** (cerrado 2026-07-17, ver ADR 014) · `xl` 24px modales · `full` pills.
- Sombras suaves con nombre; detalle completo en `docs/design-system.md` (v2.0 —
  ⚠️ sus secciones 6-7 y la referencia rápida tienen drift v1.0, verificar contra `globals.css`)

### Idioma
- **UI y copy en PT-BR** (ADR 007): mensajes de WhatsApp, validaciones, errores visibles.
  Nombres de servicios como en el Instagram del cliente ("Banho & Tosa").
- Docs internas, ADRs, comentarios y commits en español/inglés.
- Sin selector de idioma en MVP1.

---

## 6. Reglas estrictas — qué NO hacer

No violarlas sin discutir primero:

- ❌ **No abstracciones genéricas** hasta tener 3 casos de uso reales
- ❌ **No microservicios** · ❌ **No JWT en localStorage** · ❌ **No materializar slots en DB**
- ❌ **No hardcodear lógica de petshop** — lo específico de FrontPet va en flag de `tenant.config`
- ❌ **No estados de pedido más allá de `PENDING/CONFIRMED/CANCELLED`**
- ❌ **No auth de clientes finales** — solo admin tiene login en MVP1
- ❌ **No lazy loading de ORM sin pensar** — preferir queries explícitos
- ❌ **No persistir el carrito en backend** hasta el checkout
- ❌ **No instalar librerías sin preguntarse si se resuelve con vanilla**
- ❌ **No mover el Nav al root layout** — vive en `(public)/layout.tsx`; admin tiene layout propio (ADR 006)
- ❌ **No aceptar código generado que no se entiende línea por línea**
- ❌ **No comprar infraestructura** hasta el Sprint Despliegue

### Decisiones operacionales del admin (MVP1)

- ❌ **Admin desktop-first**: sin bottom tab bar, FAB ni swipe; mobile = hamburger + drawer
- ❌ **Sin métricas analíticas** (conversão, faturamento, ticket médio, trend pills) —
  solo **conteos operacionales**. Ver ADR 003/008
- ⚠️ **Forma de pagamento se CAPTURA, no hay pago online** — dato operativo, sin pasarela (ADR 003)
- ⚠️ **Frete: modalidad `GRATIS`/`A_COMBINAR`**, sin valor numérico; `Total = Subtotal` (ADR 003)
- ❌ **No "Imprimir Pedido"** · ❌ **No selector de loja** (single-location en MVP1)
- ❌ **No CRUD completo de servicios**: admin **edita**, no crea/borra. Catálogo real:
  2 banhos base + adicionais, preço/duração por porte P/M/G/GG (ADR 011)
- ❌ **No profesionales nominales**: capacidad = un número (`tenant.config.capacidade_atendimento`, ADR 009)
- ✅ **Templates de WhatsApp versionados en código**, variantes por status (ADR 010)
- ✅ **Categorías (lista cerrada, sin CRUD, seed en `V8__catalog_reference_data.sql`)**:
  `Rações, Acessórios, Higiene, Petiscos, Conforto, Brinquedos, Outros`.
  **Espécies: `Cães, Gatos` y nada más** (confirmado jul/2026). Producto↔especie es N:M.

---

## 7. Scope MVP1 (USD 500 one-time)

**✅ Público**: landing responsive · catálogo con búsqueda y filtro · detalle de producto
(**una foto**, galería es Fase 2) · carrito multi-producto (sessionStorage) · checkout con
mensaje WhatsApp dinámico · página de servicios · booking 3 pasos + confirmación WhatsApp ·
floating WhatsApp button

**✅ Admin**: login JWT · CRUD productos con upload de imágenes · CRUD servicios/horarios ·
pedidos con cambio de estado · turnos del día/próximos 7 días · mini-dashboard operativo

**✅ Marketing**: Meta Pixel (eventos estándar) + Plausible

**❌ Fase 2+ (recotizable)**: tracking propio/embudo/conversión, A/B testing, landings
editables, wishlist, reviews dinámicas, login de clientes, carrito cross-device, WhatsApp
Business API, recordatorios y confirmaciones automáticas, pagos online, multi-tenant real,
integración ERP/CRM.

> Si una tarea cae acá, avisar antes de implementar: "Esto parece Fase 2 según CLAUDE.md. ¿Confirmás?"

---

## 8. Cómo trabajar en este repo

1. Respetar §3 y §6 sin debatirlas (salvo que Sebastián abra la discusión).
2. Tarea fuera de scope MVP1 → avisar antes (ver §7).
3. Librerías: revisar §2 primero; si no está decidido, proponer 2-3 opciones con trade-offs.
4. SQL: legibilidad sobre cleverness; comentar el porqué de joins complejos.
5. Tests: happy path de integración antes que coverage exhaustivo.

**Preferencias de interacción**: plan en prosa antes de código no-trivial · pushback
activo (busca el mejor diseño, no confirmación) · UI siempre con referencias visuales
(pedirlas si no hay) · iterar y ofrecer variantes · explicar línea por línea cuando se
pida · validar APIs contra docs oficiales en vez de inventar.

---

## 9. Prácticas a sugerir proactivamente

**Secuencia obligatoria por sprint**: **AC del sprint → plan en prosa por feature → código → DoD (§10)**.

- **AC**: al arrancar cada sprint (uno por vez, nunca sprints futuros), verificables sí/no,
  viven en la issue de GitHub. Claude los propone, Sebastián corrige. No se estiran para
  que una tarea "entre" — si el scope crece, ver trigger abajo.
- **Plan en prosa**: justo antes de cada feature no trivial — componentes, query,
  Server/Client, edge cases, tests.

**Triggers** (sugerir sin que se pida):

| Si pasa… | Sugerir… |
|---|---|
| Nueva tabla/entidad | Actualizar `docs/db-model.png` antes de la migración |
| Decisión técnica sin ADR | Crear ADR en `docs/decisions/` antes de implementar |
| UI nueva | Pedir referencias visuales primero |
| Nueva dependencia | Test "¿se resuelve con vanilla?" |
| Código denso/tricky | Ofrecer explicación línea por línea |
| Bug >2 hs | Documentar en `docs/learnings.md` al resolverlo |
| Feature que desborda el estimado | Acotar scope o mover a colchón/Fase 2 |
| Pre-PR | Recordar DoD (§10) |
| Cierre de sprint | Loom 3-5 min; ofrecer guión |
| Decisión discutida largo | Capturarla como ADR aunque sea corto |

**Anti-patrones a interrumpir**: código aceptado sin entenderlo · features sin test de
integración · lógica de petshop hardcodeada · librerías sin justificación · ADRs
postergados · compras de infra anticipadas.

---

## 10. Definition of Done

- ✅ Código en `main` sin warnings de compilación
- ✅ Funciona en mobile real (celular vía red local, no solo DevTools)
- ✅ Backend → test de integración del happy path
- ✅ UI → validación visual a 320/768/1024px
- ✅ UX visible → screenshot en la issue
- ✅ Decisión técnica → ADR creado/actualizado
- ✅ Schema DB → `docs/db-model.png` regenerado
- ✅ Dependencias o build → **`pnpm build` + `pnpm test` + `pnpm lint` en verde, desde WSL**
  (ADR 015 — el dev server no alcanza)

---

## 11. Contactos y referencias

- **Dev**: Sebastián Khazzaka · **Cliente**: FrontPet (Santana do Livramento, RS, Brasil)
- **Repo**: https://github.com/sebakhazzaka2/Frontpet · **Producción**: pendiente
- **Diseño**: Stitch `projects/3403942466915386698` vía MCP (ver §5)
- **Entrega final 30/09/2026** · hito de cobro **05/09/2026** (tienda vendiendo) ·
  22 hs/semana. Plan detallado: **ROADMAP.md v2.1**

---

**Última actualización**: 2026-07-27 · **Versión**: 2.0 (condensada; detalle movido a
`docs/next16-notes.md`, ADR 016 y `docs/stitch-implementation-workflow.md`)
