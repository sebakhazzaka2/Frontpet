# FrontPet
---
## 1. Qué es FrontPet

Plataforma web comercial para FrontPet, un petshop local. Permite:

- **Vender productos** vía catálogo online → pedidos por WhatsApp (sin checkout con pagos)
- **Reservar turnos** de servicios (baños, peluquería) online
- **Gestionar** todo desde un panel admin simple

FrontPet es **cliente piloto** del producto. La intención a largo plazo es que esta misma plataforma
se transforme en un SaaS multi-tenant para múltiples negocios locales (petshops, peluquerías,
barberías, gimnasios, etc.). Pero **eso es visión futura, no MVP1**.

---

## 2. Stack tecnológico

### Backend
- **Java 21** + **Spring Boot 3.x**
- **PostgreSQL 16** (no MySQL, no NoSQL)
- **Flyway** para migraciones
- **Spring Security + JWT** para auth (cookies HttpOnly, no localStorage)
- **Maven** para build
- **Lombok** OK
- **JUnit 5** + **Testcontainers** para tests de integración
- **Logback con JSON output** para structured logging desde el día uno

### Frontend

> Versiones **verificadas contra `pnpm-lock.yaml`** (julio 2026), no aspiracionales.
> Next y React están **pineados exactos** a propósito. Si actualizás, actualizá esta tabla.

| Paquete | Versión | Nota |
|---|---|---|
| `next` | **16.2.6** | App Router, no Pages Router. Turbopack por defecto |
| `react` / `react-dom` | **19.2.4** | |
| `typescript` | 5.9.3 | strict mode. Next 16 exige TS ≥ 5.1 |
| `tailwindcss` | **4.3.0** | CSS-first. Tokens en `globals.css` vía `@theme`. Ver ADR 014 |
| `shadcn` (CLI) | 4.7.0 | + `radix-ui` unificado, `cva`, `clsx`, `tailwind-merge`, `tw-animate-css` |
| `framer-motion` | 12.38.0 | |
| `@tanstack/react-query` | ^5.100.10 | |
| `react-hook-form` | 7.76.0 | + `@hookform/resolvers` ^5.2.2 |
| `zod` | **4.4.3** | ⚠️ v4, API distinta a la v3. No copiar snippets de v3 |
| `lucide-react` | ^1.16.0 | iconos |

**Node.js 20.9+ obligatorio** (Next 16 dropeó Node 18).

#### Next 16 — cambios que nos pegan directo

Trampas concretas para las tareas del ROADMAP. **No escribir código estilo Next 14**:

- **Async Request APIs (breaking).** `params`, `searchParams`, `cookies()`, `headers()` y
  `draftMode()` son **Promises**. El acceso síncrono fue eliminado del todo en la 16.
  ```tsx
  // ❌ Next 14                          // ✅ Next 16
  function Page({ params }) {            export default async function Page(
    const { slug } = params                props: PageProps<'/produtos/[slug]'>
  }                                      ) {
                                           const { slug } = await props.params
                                         }
  ```
  Pega en `/produtos/[slug]` (tarea 3.9) y en el auth guard del admin (4.11), que lee la
  cookie JWT con `cookies()`. `npx next typegen` genera los helpers `PageProps` /
  `LayoutProps` / `RouteContext` tipados por ruta.
- **`middleware.ts` → `proxy.ts`.** El nombre `middleware` está deprecado. **`proxy` corre
  solo en runtime Node, no soporta edge.** Ver la nota de Cloudflare en Infraestructura.
- **`next lint` fue eliminado.** Ya está bien en `package.json` (`"lint": "eslint"`).
  `next build` ya no linta.
- **El caching cambió.** `revalidateTag` ahora pide un segundo argumento (perfil de
  `cacheLife`). PPR salió de experimental y se activa con `cacheComponents`. **La tarea 3.11
  del ROADMAP ("ISR / revalidate 60s") está escrita para Next 14 — revisar contra la doc de
  16 antes de implementarla.**
- **`next/image`**: `images.domains` está deprecado → usar `images.remotePatterns` (necesario
  para el bucket R2 de la tarea 3.5). El default de `qualities` pasó a solo `[75]` y
  `minimumCacheTTL` de 60s a 4h.
- **Turbopack es el default** en `dev` y `build`. Los scripts ya están correctos.

Doc oficial: https://nextjs.org/docs/app/guides/upgrading/version-16

### Infraestructura (a desplegar cuando haya algo funcional para hostear)

**Un solo VPS sirve backend y frontend.** Ver ADR 016.

- **VPS Hetzner CX32** (4 vCPU / 8 GB, Ubuntu 24.04) — **región US East (Ashburn)**
- **Coolify** como PaaS auto-hosteado: buildea y sirve **Spring Boot + Next 16 + Postgres**
- **Docker** + **Docker Compose**
- **Caddy** (reverse proxy + SSL automático)
- **Cloudflare**: DNS + CDN (proxy activado) + **R2** para storage de imágenes
- **GitHub Actions** para CI
- **Sentry** para error tracking
- **Plausible** para analytics de visitas

> **Por qué no Cloudflare Pages / Workers / Vercel / AWS**: ver ADR 016. Resumen:
> `next-on-pages` está deprecado, el adapter de OpenNext tiene un conflicto sin confirmar
> con el `proxy.ts` de Next 16 ([issue #13755](https://github.com/cloudflare/workers-sdk/issues/13755)),
> Vercel Hobby es de uso no comercial (Pro = USD 240/año contra un proyecto de USD 500), y
> AWS son USD 60-100/mes + IAM/VPC que no entran en las 10 hs del Sprint Despliegue.

> ⚠️ **CX32, no CX22.** Coolify solo pide ~2 GB (Docker + su Postgres + Redis + Soketi).
> Con la JVM de Spring Boot + Postgres + Next encima, 4 GB no alcanzan.

> ⚠️ **Región Ashburn, no Alemania.** El cliente está en Rio Grande do Sul: Alemania son
> ~200 ms, Ashburn ~130 ms. Solo afecta a las llamadas de API — landing, catálogo e imágenes
> los sirve el PoP de Cloudflare en **Porto Alegre**, así que el camino de conversión no
> depende del origin.

> Durante Sprint 0, 1 y 2 todo corre en **localhost**. Las compras de VPS, dominio,
> Cloudflare y Sentry se hacen recién en el **Sprint Despliegue** (entre Sprint 2 y 3),
> cuando ya hay landing terminada para mostrar al cliente.

### Tracking
- **Meta Pixel** instalado en el frontend público (eventos estándar: Contact, ViewContent, Schedule, Purchase)
- Tracking propio (tabla `events`) **NO va en MVP1**, queda para Fase 2

---

## 3. Decisiones arquitectónicas vigentes

Las decisiones detalladas están en `docs/decisions/`. Resumen:

- **Monolito modular**, no microservicios
- **Monorepo** (un solo repo con `backend/` y `web/`), no dos repos separados
- **Multi-tenant ready**: toda tabla del dominio lleva `tenant_id` desde el día uno
- **API REST** (no GraphQL)
- **Auth con JWT en cookie HttpOnly**, no en localStorage
- **Slots de booking calculados dinámicamente**, no materializados en DB
- **Carrito en sessionStorage**, no persistido en backend hasta el envío del pedido
- **WhatsApp click-to-chat** (`wa.me/...?text=...`), no WhatsApp Business API
- **IDs en UUID v7** para entidades públicas, BIGSERIAL para internas

---

## 4. Estructura del repositorio (monorepo)

```
frontpet/
├── backend/                    (Spring Boot)
│   ├── src/main/java/com/frontpet/
│   │   ├── tenant/             Configuración del negocio
│   │   ├── identity/           Usuarios admin, auth
│   │   ├── catalog/            Productos, categorías
│   │   ├── booking/            Servicios, recursos, reservas
│   │   ├── orders/             Pedidos, items
│   │   └── notifications/      Generación de mensajes WhatsApp
│   ├── src/main/resources/
│   │   └── db/migration/       Migraciones Flyway
│   ├── docker-compose.yml      Postgres local
│   └── pom.xml
├── frontend/                        (Next.js)
│   ├── app/
│   │   ├── (public)/           Rutas públicas (landing, catálogo, booking)
│   │   ├── (admin)/            Rutas del panel admin
│   │   └── api/                API routes mínimas (proxy a backend si hace falta)
│   ├── components/
│   │   ├── ui/                 Componentes shadcn/ui
│   │   ├── public/             Componentes de la web pública
│   │   └── admin/              Componentes del admin
│   ├── lib/                    Utilidades, API client, hooks
│   └── package.json            (sin tailwind.config.ts — los tokens viven en
│                                app/globals.css vía @theme. Ver ADR 014)
├── docs/
│   ├── decisions/              Architectural Decision Records (ADRs)
│   ├── design-system.md        Tokens, escalas, paleta documentadas
│   ├── db-model.png            Modelo de DB visual (DBdiagram.io export)
│   ├── learnings.md            Bugs >2h con causa raíz  [gitignored]
│   └── ui/                     ← Export VIEJO de Stitch  [gitignored]
│       │                       ⚠️ NO es fuente de verdad — ver sección 5.
│       │                       Sirve para mirar `screen.png` sin conexión.
│       ├── frontpet_Publico/   10 pantallas (nombres en español, versiones stale)
│       ├── frontpet_Admin/     9 pantallas + design_system
│       └── */*/                Cada pantalla: `code.html` (HTML + Tailwind CDN v3) +
│                               `screen.png` (referencia visual)
├── CLAUDE.md
├── ROADMAP.md
└── README.md
```

> Si la estructura real del repo difiere de esta, actualizá esta sección para que refleje
> lo que está en disco. Esta sección debe ser fuente de verdad.

---

## 5. Convenciones de código

### Commits
Conventional Commits **en ingles**:
- `feat: agrega flujo de checkout multi-producto`
- `fix: corrige cálculo de slots para servicios largos`
- `refactor: extrae servicio de cálculo de disponibilidad`
- `docs: actualiza README de setup local`
- `chore: actualiza dependencias menores`

En monorepo, prefijar con scope cuando ayude a separar contextos:
`feat(web): ...`, `fix(backend): ...`.

El usuario es quien se encarga de los commits, solamente sugiere nombre y archivos a commitear

### Branches
- `main` siempre desplegable (una vez que haya despliegue activo)
- `feat/nombre-corto`, `fix/nombre-corto`
- Vida corta (1-3 días máximo)
- Squash merge a main

### Backend (Java)
- Packages por feature (`catalog`, `booking`), no por capa (`controllers`, `services`)
- Cada módulo expone una `*Service` con interfaz pública
- Otros módulos consumen esa interfaz, **nunca el repositorio directamente**
- Tests: `*ServiceTest` (unit) y `*IntegrationTest` (con Testcontainers)
- DTOs con records de Java 21 (`public record CreateProductRequest(...)`)
- Validaciones con `jakarta.validation` (`@NotBlank`, `@Positive`, etc.)

### Frontend (TypeScript)
- Componentes en **PascalCase**, archivos `.tsx`
- Hooks custom en `camelCase` empezando con `use`
- Types/interfaces en PascalCase
- Tailwind directo, evitar CSS modules
- Server Components por defecto, `"use client"` solo cuando hace falta (interactividad, hooks)
- Imágenes siempre con `<Image>` de Next.js, nunca `<img>`

### API REST
- URLs en **kebab-case**: `/api/products`, `/api/order-items`
- JSON con keys en **camelCase**: `{ "productId": "abc", "createdAt": "..." }`
- Versionado en URL: `/api/v1/products` (preparado para v2 futura)
- Códigos HTTP estándar: 200, 201, 400, 401, 403, 404, 409, 422, 500

### Design system (frontend)
- **Fuente canónica de los tokens: `frontend/app/globals.css`, bloque `@theme`.**
  Es lo único que compila, así que es lo único que manda. Esta sección lo describe.
- **Fuente canónica del diseño: el proyecto de Stitch, leído por MCP.**
  `projects/3403942466915386698` — "FrontPet Design System" (19 pantallas + logo +
  banner + `DESIGN.md`). **Las pantallas se portan leyendo del MCP, no de `docs/ui/`.**
- ⚠️ **`docs/ui/` es un export viejo y stale.** Sus carpetas están en español contra
  títulos PT-BR en Stitch, y su markup es de versiones anteriores ("Landing Page" local
  vs "Landing Page com Rodapé Sincronizado" en Stitch). **Se usa para mirar `screen.png`
  sin conexión. No se porta desde ahí.**
- `DESIGN.md` (Stitch) es la **referencia de intención de diseño** — de ahí salen la
  paleta, la tipografía y las guardrails. Pero **no es autoridad sobre las clases**: se
  contradice a sí mismo en los radios (ver ADR 014) y sus pantallas corren en Tailwind
  v3, no v4.
- **El login es la única pantalla DESKTOP** (2560px); las otras 18 son MOBILE (780px).
- **No existen pantallas de reset de contraseña ni de banner LGPD.** Hay que generarlas
  en Stitch antes de codearlas (tareas 7.12 y 7.13 del ROADMAP).
- Documentado en `docs/design-system.md` (v2.0).
- **Los tokens viven en `frontend/app/globals.css`, en el bloque `@theme`.**
  Tailwind v4 lee la config desde el CSS. **No existe `tailwind.config.ts`** y no hay
  que recrearlo (ver ADR 014). Para cambiar la paleta se tocan los `--color-*` de
  `@theme` y se actualiza toda la app — nunca hex hardcodeados en componentes.
- ⚠️ **Los valores actuales de `@theme` son placeholders** tomados de `DESIGN.md`,
  pendientes de que el cliente confirme la paleta final.
- ✅ **El prototipo de landing v1.0 fue borrado** (commit `010f8f87`) y **queda
  descartado**: estaba contra el design system v1.0 muerto, en español, con "Pehuajó"
  como ciudad, y los tipos de su `lib/data.ts` contradecían el ADR 013 (mezclaban
  especie y categoría en un campo plano). **No se reutiliza nada de ahí** — sigue
  recuperable en `git show 010f8f87^:...` si hace falta consultarlo, pero la landing
  se regenera desde Stitch.
- **Prohibido usar valores arbitrarios** fuera de la escala (ej. `mt-[13px]` no entra)
- Escala de spacing: `4, 8, 12, 16, 20, 24, 32, 40, 48, 56, 64, 80` (y nada más — múltiplos de 4)
- **Paleta: 4 colores base**:
  - Primary Navy `#011E5A` — identidad, estructural (sidebar admin, footer), botão "main system action"
  - Secondary Orange `#F4640D` — ações comerciais (Adicionar à sacola, Agendar, pills, prices)
  - Tertiary Green `#25D366` — WhatsApp CTAs exclusivamente
  - Neutral Slate `#64748B` — texto secundário, borders, structural elements
- **Color semántico (no cuenta como acento)**: `--color-star #E0A82E` — estrellas de rating
  (reviews, detalle de producto), exclusivamente. Se sumó en la tarea 2.0b; es el equivalente
  a un good/warning: rol semántico, no un 5º color comercial. Ver `docs/port-landing-stitch.md`.
- **Tipografia**: Fredoka (headlines) + Plus Jakarta Sans (body) — pesos solo 400/500/600 (nunca 700+)
- **Escala de radius — canónica, 4 pasos. Definida en `@theme`, no en Stitch**:

  | Clase | Valor | Uso |
  |---|---|---|
  | `rounded-sm` | 4px | chips chicos, badges |
  | `rounded-md` | 8px | **botones, inputs, chips, icon buttons** |
  | `rounded-lg` | 16px | **cards** |
  | `rounded-xl` | 24px | modales, bottom sheets |
  | `rounded-full` | pill | status pills, avatars |

  No hay paso de 12px: `DESIGN.md` lo menciona solo para "icon buttons 10-12px" y no
  justifica un token propio — usan `rounded-md`.

  ⚠️ **Las clases `rounded-*` de Stitch NO coinciden con las del repo.** Las 18
  pantallas usan los defaults del CDN de Tailwind **v3**, donde `rounded-lg` = 8px.
  En el repo `rounded-lg` = 16px. **Ninguna clase de radius sobrevive un copy-paste.**
  Tabla de traducción y resolución del conflicto: **ADR 014**.

  🔍 **A revisar**: cards quedaron en 16px (lo dicen la prosa y el frontmatter de
  `DESIGN.md`), pero las pantallas de Stitch los dibujan a **12px**. Es decisión
  estética abierta — al portar la primera pantalla, comparar contra el `screen.png` y
  confirmar si se ven mejor a 12 o 16. Cambiar `--radius-lg` en `globals.css`.
- Sombras suaves: `0 1px 2px rgba(0,0,0,0.04), 0 4px 12px rgba(0,0,0,0.03)` default / hover más diffuse

### Idioma del producto
- **UI y copy del producto en portugués brasileño (PT-BR)**. Cliente piloto opera en Santana do Livramento (RS, Brasil) y su Instagram (@frontpet.br) ya publica en PT-BR. Ver ADR 007.
- **Documentación interna, ADRs, comentarios de código, commits y CLAUDE.md siguen en español/inglés** como hasta ahora. Solo cambia la capa de presentación al usuario final.
- Mensajes pre-cargados de WhatsApp (botones "Pedir pelo WhatsApp", confirmaciones, etc.): en PT-BR.
- Validaciones de formulario y mensajes de error visibles: en PT-BR.
- Nombres de servicios respetan los del Instagram del cliente: "Banho & Tosa", no "Baño y Corte".
- Si en Fase 2+ aparece demanda real de español para clientes uruguayos (Rivera), se evalúa selector de idioma con `next-intl` o similar. **No anticipar en MVP1**.

---

## 6. Reglas estrictas — qué NO hacer

Estas son lecciones aprendidas y decisiones tomadas. **No las violes sin discutir primero**.

- ❌ **No abstracciones genéricas hasta tener 3 casos de uso reales.** Si solo FrontPet usa una
  feature, no la generalices "por las dudas".
- ❌ **No microservicios.** Es un monolito modular y así se queda en MVP.
- ❌ **No JWT en localStorage.** Solo en cookies HttpOnly + SameSite=Lax.
- ❌ **No materialices slots de booking en DB.** Se calculan dinámicamente.
- ❌ **No hardcodees lógica específica de petshop.** El producto tiene que ser reusable.
  Si necesitás algo "para FrontPet", usá una flag en `tenant.config`.
- ❌ **No agregues estados al pedido más allá de `PENDING/CONFIRMED/CANCELLED`** en MVP1.
- ❌ **No metas auth de clientes finales.** Solo admin tiene login en MVP1.
- ❌ **No useses ORM lazy loading sin pensar.** Preferí queries explícitos.
- ❌ **No persistas el carrito en backend** hasta el momento del checkout.
- ❌ **No instales librerías que no aporten valor real.** Antes de `npm install X`,
  preguntate si el problema se resuelve con código vanilla.
- ❌ **No uses valores arbitrarios fuera del design system** (ver sección 5).
- ❌ **No muevas el Nav al root layout.** Vive en `(public)/layout.tsx`. El admin tiene
  su propio layout independiente. Ver ADR 006.
- ❌ **No aceptes código generado por IA que no entendés línea por línea.**
- ❌ **No compres infraestructura (VPS, dominio, Cloudflare, Sentry) hasta tener algo concreto
  que hostear.** El despliegue está planificado entre Sprint 2 y 3.

### Decisiones operacionales del admin (MVP1)

Reglas surgidas del diseño de las pantallas admin, alineadas con MVP1:

- ❌ **Admin es desktop-first.** No usar patrones mobile-native como bottom tab bar, floating
  action button (FAB), swipe gestures. Mobile usa hamburger + drawer desde la top bar.
- ❌ **No mostrar métricas analíticas en el admin MVP1.** Sin porcentajes de conversão,
  sin "Faturamento", sin "Novos Clientes", sin "Ticket médio", sin trend pills verdes/rojos
  (verde +X%, rojo -X%). El admin de MVP1 muestra solo **conteos operacionales** (cuántos
  pedidos hoy, cuántos turnos próximos). Las métricas reales viven en Fase 2 con tracking
  propio. Ver ADR 003 y ADR 008.
- ⚠️ **Forma de pagamento: se CAPTURA, pero NO hay pago online.** El cliente indica en el
  checkout cómo va a pagar al recibir (dato operativo para logística). No hay pasarela, no
  hay cobro online. No crear UI que sugiera cobro en el momento. Ver ADR 003 (actualización).
- ⚠️ **Frete: grátis até 5km, mais de 5km "a combinar".** No se guarda un valor numérico
  calculado (el sistema no calcula distancia en MVP1); se guarda la modalidad
  (`GRATIS` / `A_COMBINAR`), la determina el admin/logística. En totals: `Total = Subtotal`
  (el frete no suma un número al total). Ver ADR 003 (actualización).
- ❌ **No "Imprimir Pedido"** en MVP1. Negócio chico, no usa papel. Evaluar en Fase 2.
- ❌ **No selector de loja / branch indicator** en el top bar admin. MVP1 é single-tenant
  single-location. Multi-branch es Fase 2+.
- ❌ **No CRUD completo de servicios** en MVP1 — admin **edita** el catálogo de serviços
  pero **no cria nem deleta** desde la UI. El catálogo real son **2 banhos base**
  (Esencial, Premium) **+ adicionais** (tosa higiênica, tosa completa, carding, hidratação,
  banho antisséptico, banho antipulga), con **preço/duração por porte** (P/M/G/GG). Un turno
  = 1 banho base + N adicionais. Ver ADR 011 (reemplaza el catálogo del ADR 009).
- ❌ **No "Profissionais nominais"** en MVP1 — capacidade do tenant se modela como un único
  número (`tenant.config.capacidade_atendimento`). Ver ADR 009.
- ✅ **Templates de WhatsApp pré-formatados** para confirmação de pedidos e agendamentos,
  versionados em código, com variantes por status (Pendente / Confirmado / Cancelado).
  Ver ADR 010.
- ✅ **Categorias canônicas (confirmadas con el cliente)**: `Rações, Acessórios, Higiene,
  Petiscos, Conforto, Brinquedos` (Conforto = casas, camas, colchonetes). **Espécies**:
  `Cães, Gatos, Aves, Peixes, Roedores`. Un producto puede estar en varias (N:M).

---

## 7. Scope MVP1 — qué entra y qué no

### ✅ Incluido en MVP1 (USD 500 one-time)

**Público**
- Landing comercial responsive
- Catálogo con búsqueda y filtro por categoría
- Detalle de producto (una foto por producto en MVP1; galería queda para Fase 2)
- **Carrito multi-producto** en sessionStorage
- Checkout con mensaje WhatsApp dinámico (todos los items del carrito)
- Página de servicios
- **Booking de 3 pasos**: servicio → fecha/hora → datos
- Confirmación de reserva vía WhatsApp
- Floating WhatsApp button

**Admin**
- Login JWT
- CRUD de productos con upload de imágenes
- CRUD de servicios y horarios
- Vista de pedidos con cambio de estado
- Vista de turnos del día / próximos 7 días
- **Mini-dashboard operativo**: pedidos del día/mes/total, turnos del día/próximos,
  top 5 productos pedidos, servicio más reservado

**Marketing**
- **Meta Pixel** con eventos estándar (Contact, Purchase, Schedule, ViewContent)
- **Plausible** para analytics básicos de visitas

### ❌ Fuera de MVP1 (Fase 2+, recotizable)

- Sistema de tracking propio con tabla `events`
- Dashboard de embudo (visitas → clicks WA → pedidos)
- Tasa de conversión visible en panel
- Métricas de productos vistos (vs pedidos)
- A/B testing de landings
- Landing pages dinámicas editables
- Sistema de likes / wishlist
- Reviews dinámicas (usuarios cargan)
- Login de clientes con historial
- Carrito persistente cross-device
- WhatsApp Business API
- Recordatorios automáticos pre-turno
- Confirmación automática de reservas
- Pagos online (Mercado Pago)
- Multi-tenant real (subdominios, signup de negocios)
- Integración con ERP/CRM de FrontPet

---

## 8. Cómo deberia trabajar en esta repo Claude

Cuando Claude reciba pedidos debería:

1. **Primero leer este archivo** antes de generar código.
2. **Respetar las decisiones de la sección 3 y 6** sin debatirlas (a menos que el desarrollador
   abra explícitamente la discusión).
3. **Si una tarea cae fuera del scope MVP1** (sección 7), avisar antes de implementar:
   "Esta tarea parece ser Fase 2 según `CLAUDE.md`. ¿Confirmás que querés que la haga?"
4. **Para preguntas de "qué library uso"**: revisar primero la sección 2. Si la decisión
   no está tomada, proponer 2-3 opciones con trade-offs claros.
5. **Para queries SQL**: priorizar legibilidad sobre cleverness. Comentar el por qué de joins
   complejos.
6. **Para tests**: priorizar tests de integración del happy path antes que coverage exhaustivo.

### Preferencias de interacción

- **Pedir plan antes de código** cuando la feature sea no-trivial. Sebastián prefiere ver el
  approach en prosa (qué query, qué edge cases, qué tests) antes de recibir 200 líneas.
- **Dar pushback activo.** Si una decisión parece frágil, decilo. Sebastián no busca
  confirmación, busca el mejor diseño posible.
- **Para UI / diseño**: usar referencias visuales. Si Sebastián manda screenshots, comparar
  contra ellos. Si no manda, pedirlos antes de codear estilos.
- **Iterar.** El primer output rara vez es el bueno. Después de generar algo, ofrecer
  variantes ("¿lo querés más minimal? ¿más denso?") en vez de dar por cerrado.
- **Explicar línea por línea cuando se pida.** Si Sebastián pregunta "¿qué hace esto?",
  explicar el código *real*, no una descripción genérica.
- **Validar facts contra docs oficiales.** Las APIs de Spring Boot y Next.js cambian; si hay
  duda, decirlo y proponer verificar en la doc en vez de inventar.

---

## 9. Prácticas que Claude debe sugerirme proactivamente

Esta sección lista hábitos que Sebastián quiere internalizar. Claude debe **sugerirlos
activamente** cuando el contexto lo amerite, no esperar a que se los pida. Funcionan como
triggers: "si pasa X, recordame Y".

### Al arrancar un sprint (obligatorio, no opcional)

**Ningún sprint arranca sin acceptance criteria.** Antes de escribir la primera línea de
código de un sprint, se escriben los AC de cada tarea de ese sprint. Claude los propone,
Sebastián los corrige. Reglas:

- **Un sprint por vez.** Se escriben los AC del sprint que arranca, **no** los de los
  siguientes. Escribir los AC del Sprint 7 mientras arranca el 2 es documentación
  anticipada — el anti-patrón de la sección 6.
- **Un AC es verificable o no es un AC.** "El catálogo funciona bien" no sirve.
  "Filtrar por `Rações` muestra solo productos de esa categoría y la URL refleja el filtro"
  sí. Si no se puede responder sí/no mirando la pantalla o corriendo un test, reescribirlo.
- **Los AC no reemplazan la Definition of Done** (sección 10): son el *qué* de cada tarea,
  la DoD es el *cómo se cierra*. Ambos aplican.
- **Viven en la issue de GitHub** de cada tarea, no en un doc aparte que se desactualiza.
- Si durante el sprint una tarea no cumple sus AC y el scope crece → ver el trigger de
  "feature que crece más allá del estimado" abajo. No se estiran los AC para que entre.

### Antes de codear

- **Al arrancar una feature con lógica no trivial** (booking, checkout, queries agregados):
  → "¿Querés que escriba primero el plan en prosa antes de tirar código?"
- **Al modelar una nueva tabla o entidad**:
  → "¿Actualizaste el diagrama de DB en `docs/db-model.png`? Si no, conviene hacerlo
  antes de la migración."
- **Al tomar una decisión técnica que no esté en `docs/decisions/`**:
  → "Esto amerita un ADR nuevo en `docs/decisions/00X-titulo.md`. ¿Lo armamos antes
  de implementar?"
- **Antes de tocar UI nueva**:
  → "¿Tenés referencias visuales (Mobbin, Awwwards, screenshots de productos similares)?
  Es más fácil iterar con anclas concretas."

### Durante el desarrollo

- **Al sugerir `npm install` / nueva dependencia**:
  → Aplicar el test de la sección 6: "¿esto se resuelve con vanilla?" Si la respuesta no
  es clara, decirlo explícitamente.
- **Si Claude genera código que parece denso o tricky**:
  → "Te recomiendo que pidas que te lo explique línea por línea antes de mergear."
- **Si Sebastián lleva más de 2 horas con un mismo bug**:
  → "Esto vale para `docs/learnings.md` cuando lo resolvamos."
- **Si una feature crece más allá del estimado del sprint**:
  → "Esto se está expandiendo. ¿Querés acotar el scope o lo movemos a colchón / Fase 2?"

### Al cerrar trabajo

- **Antes de cada PR / merge a main**:
  → Recordatorio de Definition of Done (sección 10): tests del happy path, mobile real,
  sin warnings. Deploy a preview aplica desde Sprint Despliegue en adelante.
- **Al cerrar un sprint**:
  → "Toca Loom de 3-5 min mostrando lo nuevo. ¿Te ayudo a armar el guión?"
- **Al cerrar una decisión que se discutió largo**:
  → "Conviene capturarla como ADR aunque sea corto."

### Anti-patrones a interrumpir

Claude debe **detener el flujo y avisar** si parece estar:

- Aceptando código generado sin entenderlo (señal: pedir "ahora hacelo funcionar" sin haber
  preguntado qué hace).
- Skippeando tests para ir más rápido (señal: feature merged sin test de integración).
- Hardcodeando algo específico de petshop en código que debería ser genérico.
- Instalando librerías sin justificación clara.
- Postergando ADRs ("después lo documento" — casi nunca pasa).
- Avanzando hacia compras de infraestructura antes del Sprint Despliegue.

---

## 10. Definition of Done

Antes de marcar una tarea como completa:

- ✅ Código en `main` sin warnings de compilación
- ✅ Funciona en mobile real (probado en celular vía red local, no solo DevTools)
- ✅ Si toca backend: test de integración del happy path
- ✅ Si toca UI: validación visual a 320px, 768px, 1024px
- ✅ Si afecta UX visible: screenshot guardado en la issue de GitHub
- ✅ Si introdujo decisión técnica: ADR creado o actualizado
- ✅ Si tocó schema de DB: `docs/db-model.png` regenerado
- ✅ **Si tocó dependencias o config del build: `pnpm build`, `pnpm test` y `pnpm lint`
  en verde — corridos desde WSL** (ver ADR 015). El `dev` server no alcanza: la migración
  a Tailwind v4 estuvo rota ~6 semanas sin que nadie lo notara porque nunca se buildeó.


## 12. Contactos y referencias

- **Desarrollador**: Sebastián Khazzaka
- **Cliente piloto**: FrontPet (Santana do Livramento, Brasil)
- **Repo**: https://github.com/sebakhazzaka2/Frontpet
- **Producción**: [pendiente — se completa al final del Sprint Despliegue]
- **Diseño (fuente de verdad)**: proyecto de Stitch `projects/3403942466915386698`
  ("FrontPet Design System"), leído **por MCP**. 19 pantallas + logo + banner + `DESIGN.md`.
  El HTML de Stitch usa Tailwind v3 por CDN y su propio config inline: **se porta a los
  tokens del repo, no se copia literal**. `docs/ui/` es un export viejo — ver sección 5.
- **Entrega final**: **30/09/2026**. Hito de cobro intermedio: **05/09/2026** (tienda
  vendiendo). Ritmo comprometido: 22 hs/semana. Ver ROADMAP v2.0.

---

**Última actualización**: 2026-07-17
**Versión del documento**: 1.8
