# FrontPet

> Mapa para el proyecto de planificación: **Qué es el producto** → §1 · **Stack y cómo correrlo** → §2 y §12 ·
> **Arquitectura** → §3, §4 y §13 · **Convenciones** → §5 y §6 · **Contrato común** → §14 · **Estado actual** → §15
> (+ `docs/ESTADO.md`).

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
| `next` | **16.3.8** | App Router. Turbopack default. Subido desde 16.2.6 el 2026-10-01 por 3 RCE críticas (D.9) — no bajar. ⚠️ **Trampas de Next 16: `docs/next16-notes.md`** — leer antes de código de rutas/caching/imágenes. Lo más frecuente: `params`/`searchParams`/`cookies()` son **Promises** |
| `react` / `react-dom` | **19.2.4** | |
| `typescript` | 5.9.3 | strict mode |
| `tailwindcss` | **4.3.0** | CSS-first: tokens en `globals.css` vía `@theme`. **No existe ni se recrea `tailwind.config.ts`**. Ver ADR 014 |
| `shadcn` (CLI) | 4.7.0 | **devDependency** (es CLI, no runtime) + `radix-ui`, `cva`, `clsx`, `tailwind-merge`, `tw-animate-css` |
| `framer-motion` | 12.38.0 | |
| `@tanstack/react-query` | ^5.100.10 | |
| `react-hook-form` | 7.76.0 | + `@hookform/resolvers` ^5.2.2 |
| `zod` | **4.4.3** | ⚠️ v4, API distinta a v3. No copiar snippets de v3 |
| `lucide-react` | ^1.16.0 | iconos |

**Node.js 20.9+ obligatorio.** Toolchain corre en **WSL** (ADR 015).

### Infraestructura
Un solo **VPS Hetzner CX33** (4 vCPU/8 GB, ex-CX32, **región Falkenstein/Nuremberg**) con
**Coolify** sirve Spring Boot + Next + Postgres; **Caddy** proxy/SSL; **Cloudflare**
DNS+CDN+**R2** (imágenes); GitHub Actions CI; Sentry; Plausible. Razonamiento completo
(por qué no Vercel/Pages/AWS, por qué CX33): **ADR 016** — nota de enmienda 2026-09-01:
el plan original pedía Ashburn (EE.UU.) por latencia a Brasil, pero ese tier no existe
ahí; se acepta Alemania con caching de Cloudflare como mitigación (ver ADR 016 §3).

Estado de la infra (comprada, en producción): ver `docs/WORKING-CONTEXT.md` y `docs/deploy-runbook.md`.

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
│   ├── booking/         api/ + domain/ + dto/ (ServiceOffering, BusinessHours, ScheduleBlock,
│   │                    Appointment) + AvailabilityService/AppointmentService/ServiceCatalogService
│   ├── dashboard/       api/ + dto/ — GET /admin/dashboard, conteos operacionales (ADR 003/008)
│   ├── privacy/         api/ + domain/ + dto/ — eliminação/anonimização LGPD por telefone (ADR 023)
│   ├── notifications/   EmailSender (Resend/Logging) + PasswordResetEmailTemplate (ADR 022)
│   └── BackendApplication.java   (en la raíz a propósito: component scan)
├── backend/src/main/resources/db/migration/   Flyway
├── backend/Dockerfile · frontend/Dockerfile   multi-stage, deploy en Coolify (ADR 016)
├── frontend/
│   ├── app/(public)/  admin/(protected)/  admin/login/  admin/esqueci-senha/  admin/redefinir-senha/
│   │                  api/   ((admin)/ es placeholder sin uso, ver ADR 006)
│   │                  Rutas públicas: /produtos, /servicos, /agendamento, /agendamento/[publicId],
│   │                  /carrinho, /privacidade · Admin: /admin (dashboard), /admin/produtos,
│   │                  /admin/pedidos, /admin/agendamentos, /admin/servicos
│   ├── components/ui/  public/  public/booking/ (wizard, Sprint 6)  admin/  shared/  consent/ (banner LGPD, ADR 024)
│   ├── lib/             api client, hooks, analytics/ (Meta Pixel + Plausible, ADR 024), google-places.ts
│   │                     (ADR 025); datos en lib/data/
│   ├── instrumentation.ts · instrumentation-client.ts · sentry.server.config.ts ·
│   │   sentry.edge.config.ts   Sentry SDK (ADR deploy, Sprint Despliegue)
├── docs/               ADRs, design-system.md, db-model.png, next16-notes.md,
│                       stitch-implementation-workflow.md, port-landing-stitch.md,
│                       booking-api-contracts.md, pending-decisions.md, preguntas-cliente.md,
│                       deploy-runbook.md, WORKING-CONTEXT.md,
│                       ESTADO.md (estado verificado para el ecosistema),
│                       checklist-prueba-prod.md, limpieza-datos-prueba.sql,
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
- Sombras suaves con nombre; detalle completo en `docs/design-system.md` (v3.0 — guía de uso,
  ya no duplica tablas de tokens; los valores mandan siempre desde `globals.css`)

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
- ❌ **No comprar infraestructura nueva** sin hablarlo antes (la base del Sprint Despliegue ya está contratada)

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
- **Repo**: https://github.com/sebakhazzaka2/Frontpet · **Producción**: https://frontpet.com.br (API: api.frontpet.com.br)
- **Diseño**: Stitch `projects/3403942466915386698` vía MCP (ver §5)
- **Entrega final 30/09/2026** · hito de cobro **05/09/2026** (tienda vendiendo) ·
  22 hs/semana. Plan detallado: **ROADMAP.md v2.1**

---

## 12. Cómo correrlo (comandos reales)

Requisitos: Java 21 · Node 20.9+ y pnpm 10 (en WSL, ADR 015) · Docker. Variables: plantillas en
`backend/.env.example` y `frontend/.env.example` (nunca commitear `.env*` reales).

```bash
# Backend (localhost:8080)
cd backend
docker compose up -d              # Postgres 16
./mvnw spring-boot:run            # Flyway migra al arrancar
./mvnw test                       # tests; los de integración levantan Postgres con Testcontainers (requiere Docker)
mvn -B verify                     # lo que corre el CI

# Frontend (localhost:3000)
cd frontend
pnpm install
cp .env.example .env.local
pnpm dev
pnpm build && pnpm test && pnpm lint   # DoD; CI idéntico (vitest, eslint). No existe script `typecheck`
```

CI: `.github/workflows/ci.yml` (backend: `mvn -B verify`; frontend: `pnpm build/test/lint`).

---

## 13. Arquitectura (flujo de datos y multi-tenant)

- **Flujo público**: navegador → Next 16 (SSR/Server Components; datos estáticos en `lib/data/`
  o fetch al backend vía `lib/api/client.ts`) → Spring Boot `/api/v1/*` → PostgreSQL.
  Pedidos y turnos se crean anónimos (con rate limit por IP + honeypot) y quedan en `PENDING`;
  el mensaje de WhatsApp lo arma el **frontend** (`lib/whatsapp/templates.ts`), el backend nunca envía mensajes.
- **Flujo admin**: login → cookie HttpOnly `frontpet_session` con JWT → `/api/v1/admin/*`.
  El guard de Next solo mira que exista la cookie; la autorización real es del backend.
- **Imágenes**: subida directa a Cloudflare R2 con URL prefirmada (`POST /admin/products/images/presign`, ADR 018).
- **Disponibilidad**: slots calculados al vuelo (`SlotGrid`, `AvailabilityServiceImpl`) y reserva atómica con lock por tenant+día.
- **Multi-tenant**: el **esquema** está listo (`tenant_id` en toda tabla de dominio) pero el **runtime es single-tenant**:
  `CurrentTenant` lee un UUID fijo de configuración, no hay filtro global ni RLS, el login busca por email sin tenant
  y el JWT no lleva `tenantId`. Un despliegue = un negocio. Detalle y brechas: `docs/ESTADO.md`.
- **Errores**: `RestExceptionHandler` + `ApiError` en `common/`; códigos HTTP estándar (ver §5 API REST).

---

## 14. Contrato común del ecosistema

<!-- BEGIN COMMON -->
## Contrato común del ecosistema (v2 · 2026-10-16)

Fuente de verdad: proyecto "AI SaaS Ecosystem" (claude.ai), documento CONTRATO-COMUN.md. No editar este bloque dentro de un repo: se cambia en la fuente y se copia idéntico a TurnosUy y FrontPet.

### 1. Contexto
- Dos productos SaaS comercialmente independientes que comparten criterios y, cuando convenga, componentes:
  - TurnosUy: repo `consultorio-odontologico`, neodentalmaster.turnosuy.com, clínicas odontológicas (Uruguay).
  - FrontPet: repo `frontpet`, frontpet.com.br, pet shops (Brasil).
- Objetivo: ingresos recurrentes con agentes comerciales de IA (consultas, disponibilidad real, turnos o pedidos, derivación a una persona), sin implementaciones manuales por cliente.
- Prioridad: validar con clientes reales antes de construir plataforma. No construir portal central, suscripciones ni multitenancy completa sin demanda validada.

### 2. Convención de evidencia
- HECHO: verificado en el código (citar archivo). HIPÓTESIS: inferido o no verificable desde el repo. RECOMENDACIÓN: propuesta sin decidir. DECIDIDO / PENDIENTE: estado de una decisión.
- Nunca presentar una hipótesis como hecho. La documentación puede ir por delante del código: comprobar antes de afirmar.

### 3. Reglas de trabajo
- Cambios pequeños, con pruebas automatizadas y rollback documentado.
- No ejecutar comandos destructivos, migraciones ni despliegues sin aprobación explícita.
- No modificar producción ni acceder a sistemas ajenos sin autorización.
- No commitear secretos, tokens ni datos de clientes. No leer `.env` reales.
- Revisar licencia y seguridad de cualquier dependencia o código externo antes de incorporarlo.
- No introducir varias herramientas nuevas a la vez; cada una debe justificar un caso de uso concreto.

### 4. Seguridad
- Un agente nunca tiene acceso directo a la base de datos: solo a herramientas acotadas que llaman a APIs autorizadas.
- Credenciales, datos y conversaciones separados por cliente.
- Operaciones sensibles requieren confirmación humana.
- Verificar el resultado de cada operación comercial antes de comunicarlo al usuario final.

### 5. Decisiones tomadas (DECIDIDO)
- Los productos siguen siendo independientes comercialmente.
- No se decide de antemano microservicios ni base compartida: se compara con evidencia de los repos.
- El agente se valida primero en un producto y después se reutiliza en el otro.
- Fase actual: auditoría y diseño; no se implementan funcionalidades del agente todavía.
- TurnosUy: no migrar a multitenancy real hasta ~5 clientes pagando; la opción anotada es un schema MySQL por tenant (fuente: `ROADMAP.md` y `backend/CLAUDE.md` de TurnosUy).

### 6. Decisiones abiertas (PENDIENTE: no asumir)
- Multitenancy en una instancia vs. una instancia por cliente para FrontPet y para el núcleo del agente (TurnosUy ya tiene decisión vigente, ver §5).
- Núcleo de agente común (librería o servicio) vs. implementación por producto.
- Producto en el que se valida primero el agente.
- Canal WhatsApp (API oficial o proveedor), proveedor de LLM y política de datos personales hacia el LLM (LGPD en Brasil, Ley 18.331 en Uruguay).
- Vocabulario de dominio común (Negocio, Cliente, Recurso/Profesional, Servicio, Turno y sus estados).

### 7. Requisitos candidatos para el agente (RECOMENDACIÓN, validar con clientes)
- Credencial de máquina por negocio con scopes mínimos (leer servicios, leer disponibilidad, crear turno); el negocio se resuelve desde la credencial, no desde configuración global.
- Los turnos que crea el agente nacen en estado pendiente y los confirma una persona.
- Cancelar o reprogramar solo con identidad verificada del cliente (código o enlace por turno), nunca con credencial de admin.
- Idempotencia en reservas, límites por credencial, por teléfono y por negocio, y validación de fechas pasadas y horizonte máximo.
- Registro de auditoría con origen de la acción (humano, web, agente).
- Contrato OpenAPI y errores semánticos para que el agente interprete resultados.
- Aviso al negocio cuando llega un turno pendiente.

### 8. Brechas comunes verificadas (auditoría 2026-10-15, ajustada 2026-10-16; revisar tras cada hito)
- Ambos son single-tenant en ejecución (TurnosUy sin `tenant_id`; FrontPet con `tenant_id` en el esquema pero tenant fijo por configuración).
- Ninguno tiene autorización por rol, autenticación de máquina, cancelación segura para el cliente final, ni contrato OpenAPI.
- Ninguno envía recordatorios ni mensajes de WhatsApp desde el backend (solo enlace `wa.me`), y no hay jobs programados de notificación.
- Marca (logo, paleta), moneda, locale y vocabulario del rubro siguen hardcodeados; TurnosUy ya permite configurar parte de los textos; no hay flags de módulos por negocio.
- Backups: TurnosUy declara restore probado (2026-10-15, `tasks/lessons.md`; no reproducible desde el repo). FrontPet no tiene evidencia de backups automáticos ni de restore (D.7).

### 9. Diferencias a respetar (no unificar a ciegas)
- Stacks distintos: TurnosUy = Java 17, Spring Boot 3.2, Angular 19, MySQL; FrontPet = Java 21, Spring Boot 3.5, Next 16, PostgreSQL. No se asume stack común.
- Despliegue: TurnosUy = una instancia Docker Compose por cliente, deploy por SSH desde CI; FrontPet = VPS Hetzner + Coolify (producción declarada en `docs/WORKING-CONTEXT.md`, no verificada).
- Sesión: TurnosUy = JWT en localStorage; FrontPet = JWT en cookie HttpOnly.
- Modelo: TurnosUy tiene Paciente, historial y pagos; FrontPet no tiene entidad cliente y agrega catálogo y pedidos.
- Estados de turno: TurnosUy PENDIENTE/CONFIRMADA/CANCELADA/COMPLETADA; FrontPet PENDING/CONFIRMED/CANCELLED.

### Cambios respecto de v1
- §5: se agrega la decisión vigente de multitenancy de TurnosUy. §6: se ajusta el ítem de multitenancy.
- §8: backups por producto (TurnosUy restore declarado; FrontPet sin evidencia); marca/textos más preciso.
- §9: se quita el proveedor del VPS de TurnosUy (dato contradictorio entre auditorías, sin verificar).
<!-- END COMMON -->

---

## 15. Estado actual

> Se actualiza al cerrar cada sesión (junto con `docs/ESTADO.md`), **sin tocar el bloque COMMON**.
> Detalle verificado, hipótesis y deuda: `docs/ESTADO.md`.

**Última actualización de esta sección**: 2026-10-16

- MVP1 implementado: catálogo + pedidos por WhatsApp, booking con slots dinámicos, panel admin, reset de contraseña,
  LGPD (anonimización + consentimiento), Meta Pixel + Plausible, Sentry. Next 16.3.8 (parche de seguridad del 2026-10-01).
- Según `docs/WORKING-CONTEXT.md` (2026-10-01) el sitio está en producción en `frontpet.com.br`; no verificado en esta revisión.
- Sprint 7 en curso (polish/mobile/imágenes); abierto: backups (D.7), stock que no se descuenta (§27 de `pending-decisions.md`),
  checklist de contenido (D.10).
- Sin clientes/mascotas como entidad, sin recordatorios ni WhatsApp Business API, sin roles ni API key para agentes.
- Hoy (2026-10-16): documentación de ecosistema — se agregaron §12–§15 (con el bloque COMMON v1 pegado) y `docs/ESTADO.md`;
  doc-sync reescribió `docs/WORKING-CONTEXT.md` y sacó de este archivo el estado efímero de infra/producción;
  se corrigieron README, deploy-runbook y las citas a `reuse-consultorio.md`. Sin cambios de código.

---

**Última actualización**: 2026-07-27 · **Versión**: 2.0 (condensada; detalle movido a
`docs/next16-notes.md`, ADR 016 y `docs/stitch-implementation-workflow.md`)
