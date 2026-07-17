# Roadmap FrontPet MVP1

> Plan de ejecución del MVP1 **re-baseado el 17/07/2026**, contra el plazo real de entrega:
> **30 de septiembre de 2026**. Ritmo comprometido: **22 hs/semana**.

### Por qué se re-baseó

El calendario original (19 semanas desde mayo, 8-10 hs/semana) no sobrevivió: a la semana 8
el estado real era *mitad de Sprint 1*, no Sprint 3. **El desvío no vino de tareas
subestimadas** — vino de ~55 hs de trabajo que este ROADMAP nunca presupuestó: 16 ADRs,
reunión y cuestionario con el cliente, design system v2.0, migración a Tailwind v4,
toolchain WSL, 19 pantallas de Stitch, y la landing v1.0 que se descartó.

Ese impuesto no se repite: las decisiones grandes (modelo de datos, deploy, design system,
idioma, catálogo de servicios) ya están tomadas. Queda ~15% de meta-trabajo, no 45%.

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
| Necesarias para el hito de cobro (Sprint 1 resto 8,5 + Sprint 2 24,5 + Despliegue 10 + Sprint 3 22,5 + Sprint 4 30) | 95.5 |
| **Slack del bloque A** | **~23,5** |
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
| **A** | 1 | Setup local: backend + frontend + auth + testing | 16 | 🔄 ~50% |
| **A** | 2 | Landing pública responsive (portada de Stitch) | 24.5 | 🔄 2.0b hecha |
| **A** | Despliegue | **Compra infra + despliegue inicial + landing en vivo** | 10 | |
| **A** | 3 | Catálogo + backend de productos | 22.5 | |
| **A** | 4 | **Carrito + pedidos WhatsApp + Admin productos** | 30 | ⚠️ el más pesado |
| | | 🎯 **05/09 — hito de cobro: la tienda vende** | | |
| **A→B** | 5 | Booking backend (disponibilidad + reservas) | 19 | ⏩ adelantar si hay slack |
| **B** | 6 | Booking frontend + Admin de turnos | 16 | |
| **B** | 7 | Dashboard + LGPD + Marketing + Reset de senha + Polish | 24.5 | |
| **B** | 8 | Capacitación + entrega formal | 14 | |
| | | 🎯 **30/09 — entrega final** | | |
| | Opcional | Solo si sobra tiempo (ver abajo) | 6.5 | |
| **Total** | | **169 hs restantes** (Sprints 1-8, con Sprint 1 al 50%) **+ 6,5 opcionales** | **~175,5** | |

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

**Todas las pantallas se portan leyendo de Stitch vía MCP, no del export local.**

- Proyecto: **FrontPet Design System** — `projects/3403942466915386698` (19 pantallas +
  logo + banner + `DESIGN.md`).
- `docs/ui/` es un **export viejo y stale**: nombres de carpeta en español contra títulos
  PT-BR en Stitch, y versiones anteriores del markup ("Landing Page" local vs "Landing Page
  com Rodapé Sincronizado" en Stitch). **Sirve para mirar `screen.png` offline. No es fuente.**
- El prototipo de landing v1.0 (commit `010f8f87^`) **queda descartado**: está contra el
  design system v1.0 muerto, en español, y sus tipos de `data.ts` contradicen el ADR 013
  (mezclan especie y categoría en un campo plano). No se reutiliza nada.
- **Ninguna clase `rounded-*` de Stitch se copia**: corre en Tailwind v3 vía CDN, donde
  `rounded-lg` = 8px; en el repo = 16px. Tabla de traducción en el ADR 014.
- **El login es la única pantalla DESKTOP** (2560px). Las otras 18 son MOBILE (780px).
- **No hay pantalla de reset de contraseña ni de banner LGPD.** Hay que generarlas en
  Stitch antes de codearlas (contemplado en las 7.12 y 7.13).

**Hitos clave**:
- 🎯 Sprint Despliegue (semana 5): primera URL pública con landing en vivo
- 🎯 Sprint 4 (semana 10): primera venta posible
- 🎯 Sprint 6 (semana 14): primera reserva online posible
- 🎯 Sprint 8 (semana 17): MVP completo entregado

---

## Prácticas recurrentes (aplican a todo el proyecto)

Estas no son tareas de un sprint puntual: son hábitos que se ejecutan **durante todo el
proyecto**. El costo está distribuido y no se contabiliza como tareas separadas.

### Por cada feature no trivial
1. **Plan antes de código**: escribir en prosa qué se va a hacer (query, edge cases, tests)
   antes de generar implementación. Especialmente con asistencia de IA.
2. **Referencias visuales antes de UI**: si la tarea toca diseño, abrir Mobbin / Awwwards /
   capturas de productos similares antes de tirar Tailwind.
3. **Tests del happy path**: nada se mergea a `main` sin al menos 1 test de integración
   del camino feliz (cuando aplique).
4. **Validación mobile real**: probar en celular físico vía red local, no solo DevTools.

### Por cada decisión técnica
- Si se elige una tecnología, patrón o approach que **no estaba en `docs/decisions/`**,
  se crea un ADR corto antes de implementar. Plantilla: contexto → opciones → decisión →
  consecuencias.

### Por cada semana
- **Demo en Loom** (3-5 min) los viernes mostrando lo nuevo. Aunque nadie la mire al
  principio, queda registro para el cliente y para tu portfolio.
- **Una sesión de "no escribir código"**: leer docs, blog posts, repos open source en
  el stack. La diferencia entre junior y mid es saber qué *no* hay que hacer, y eso solo
  se aprende leyendo.

### Por cada bug que tome más de 2 horas
- Documentarlo en `docs/learnings.md` (causa raíz + cómo se detectó + cómo se resolvió).
  No es opcional, es la práctica más subestimada de toda la carrera.

### Por cada nueva dependencia
- Aplicar el test del CLAUDE.md sección 6: "¿esto se resuelve con vanilla?". Si la respuesta
  es sí, no se instala.

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
| 0.1 | Setup de GitHub Projects (Kanban con sprints como milestones, issues iniciales) | 1.5 |
| 0.2 | **Modelo de DB inicial en DBdiagram.io**: `tenants`, `users`, `categories`, `products`, `services`, `appointments`, `orders`. Exportar PNG a `docs/db-model.png` | 1.5 |

**Entregable**: Kanban operativo + modelo de DB visualizable antes de la primera migración.

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
| 1.1 | Setup Spring Boot 3 con estructura modular: `tenant`, `identity`, `catalog`, `booking`, `orders`, `notifications` | 4 | 🔄 packages creados pero vacíos (`.gitkeep`); faltan entidades JPA |
| 1.2 | Postgres en Docker Compose + Flyway + primera migración | 2 | ✅ **adelantado**: V1–V4 cubren identity, catalog, booking y orders (tareas 3.1 / 4.1 / 5.1 ya hechas) |
| 1.3 | Spring Security + JWT en cookie HttpOnly + endpoint `POST /api/v1/auth/login` funcional | 3 | |
| 1.4 | Setup Next 16 + Framer Motion + TanStack Query + React Hook Form. Verificar que los tokens de `@theme` en `globals.css` funcionan end-to-end (**no hay `tailwind.config.ts` — ver ADR 014**) | 1.5 | ✅ |
| 1.5 | CORS configurado, primer endpoint del frontend consumiendo backend local | 1 | |
| 1.6 | **Testcontainers + primer test de integración** del endpoint de login. Sirve como template para todos los siguientes | 2 | ⚠️ Testcontainers **no está en el `pom.xml`** todavía |
| 1.7 | **Logback con JSON structured output** + endpoint `/actuator/health` configurado y testeado | 0.5 | |
| 1.8 | **Rate limit + lockout en el login**: máx. N intentos por IP/usuario en ventana, backoff. Un solo usuario admin y sin protección de fuerza bruta es un login de juguete | 2 | |

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
| 2.3 | `<TrustBar>`. ⚠️ **El `<AnnouncementBar>` no existe en la pantalla de Stitch** — o se descarta, o se diseña primero | 1 |
| 2.4 | `<ServiceCard>` (preview, sin booking todavía, link a `/turnos`) | 2 |
| 2.5 | `<ProductCard>` con CTA WhatsApp directo (variante para landing) | 2 |
| 2.6 | `<Reviews>` con 3-4 testimonios estáticos | 1 |
| 2.7 | `<FinalCTA>` y `<FloatingWA>` con pulse animation | 2 |
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

## Sprint Despliegue (semana 5, ~10 hs)

**Objetivo**: comprar la infraestructura, configurar todo, **dejar la landing en vivo**
con dominio propio. Este sprint dura 1 semana sola.

> Este sprint se ejecuta cuando ya hay algo concreto que hostear (la landing del Sprint 2).
> Las compras y configuraciones se agrupan acá para no fragmentar gastos ni atención.

> **Arquitectura de deploy definida en el [ADR 016](docs/decisions/016-deploy-frontend-vps-coolify.md)**:
> un solo VPS sirve backend + frontend vía Coolify. Cloudflare queda como DNS + CDN + R2.
> No hay Cloudflare Pages ni adapter de OpenNext.

### Compras y cuentas (~3.5 hs)

| # | Tarea | Hs |
|---|-------|----|
| D.1 | Compra de VPS Hetzner **CX32** (4 vCPU / 8 GB, ~€7.50/mes) **región US East (Ashburn)** + SSH key inicial | 1 |
| D.2 | Compra de dominio + configuración DNS apuntando al VPS | 1 |
| D.3 | Cuenta Cloudflare: **R2 + DNS proxy activado** (sin Pages — ver ADR 016) | 1 |
| D.4 | Cuenta Sentry (tier gratis) + Plausible o Umami | 0.5 |

### Configuración y deploy (~6.5 hs)

| # | Tarea | Hs |
|---|-------|----|
| D.5 | Instalar Coolify en el VPS, HTTPS automático con Caddy | 3 |
| D.6 | Configurar Coolify para buildear y servir el **frontend Next 16** con auto-deploy desde GitHub (junto al backend) | 1 |
| D.7 | Backups automáticos: cron + `pg_dump` + upload a Cloudflare R2 | 2 |
| D.8 | Sentry activado en backend y frontend, probar primer error capturado intencionalmente | 0.5 |

**Entregable**:
- `https://frontpet.com` con landing en vivo
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
- **Los 3 servicios (Spring Boot + Postgres + Next) comparten los 8 GB del CX32.** Vigilar
  RAM en el primer deploy; el CX22 de 4 GB del plan original directamente no alcanzaba

---

## Sprint 3 — Catálogo + Backend de productos (~22,5 hs)

**Objetivo**: catálogo dinámico desde DB con detalle por producto y CRUD vía API.

| # | Tarea | Hs |
|---|-------|----|
| 3.1 | ~~Migración SQL: `categories`, `products`, `product_images`~~ — **ya hecha en `V2__catalog.sql`** | ~~2~~ 0 |
| 3.2 | Modelos JPA + repositorios + servicios | 2 |
| 3.3 | Endpoints públicos: `GET /api/v1/products`, `GET /api/v1/products/{slug}`, `GET /api/v1/categories` | 3 |
| 3.4 | Endpoints admin protegidos: `POST/PUT/DELETE /api/v1/admin/products` | 2 |
| 3.5 | Integración Cloudflare R2 SDK en backend + endpoint de upload firmado | 3 |
| 3.5b | **Restricción del presigned URL**: mime type permitido, tamaño máximo y expiración corta, firmados en la política. Sin esto el bucket es de subida libre a costa nuestra | 1.5 |
| 3.6 | Frontend: página `/productos` con grid responsive | 2 |
| 3.7 | Filtro por categoría (chips horizontales scrolleables) | 2 |
| 3.8 | Búsqueda por nombre con debounce (300ms) | 2 |
| 3.9 | Página `/produtos/[slug]` con **una** imagen y descripción (galería es Fase 2, ver `CLAUDE.md` §7). **`params` es una Promise en Next 16** — usar `PageProps<'/produtos/[slug]'>` de `next typegen` | 2 |
| 3.10 | Skeletons de carga, estados vacíos, error boundaries | 1 |
| 3.11 | Caching del catálogo en Next 16. ⚠️ **Esta tarea está escrita para Next 14**: `revalidateTag` ahora exige un segundo argumento (perfil de `cacheLife`) y PPR se activa con `cacheComponents`. Revisar contra la doc de 16 antes de implementar | 1 |
| 3.12 | Seed con 10 productos de prueba | 1 |

**Entregable**: catálogo navegable real en producción, indexable por Google, optimizado.

**Hito**: cliente puede empezar a planificar sus fotos y descripciones reales.

**Riesgos**:
- Upload de imágenes a R2 con presigned URLs puede tomar 2 hs extra la primera vez
- Decidir el tamaño/formato de imágenes (recomendado: WebP, max 1200x1200, < 200KB)

---

## Sprint 4 — Carrito + Pedidos WhatsApp + Admin productos (~30 hs) 🎯 hito de cobro

**Objetivo**: cierre del flujo de venta + autonomía del cliente sobre el catálogo.
**Este sprint es el que habilita el segundo pago del 05/09.**

> ⚠️ **30 hs es el sprint más pesado del plan.** Partirlo en dos mitades entregables:
> **4A venta pública** (4.1-4.10, 4.15, 4.16 — ~20 hs) y **4B admin** (4.11-4.14 — ~10 hs).
> Si algo desborda, 4A sola ya es un hito defendible ante el cliente: la tienda vende.

| # | Tarea | Hs |
|---|-------|----|
| 4.1 | ~~Migración SQL: `orders`, `order_items`, `customers`~~ — **ya hecha en `V4__orders.sql`** | ~~1~~ 0 |
| 4.2 | Hook `useCart()` con sessionStorage: add, remove, update qty, clear | 3 |
| 4.3 | `<CartButton>` flotante con contador animado | 1 |
| 4.4 | `<CartDrawer>` o `/carrito` con lista editable | 3 |
| 4.5 | Botón "Agregar al carrito" en `<ProductCard>` con feedback visual | 1 |
| 4.6 | Página `/checkout` con formulario (nombre, WA, modalidad, dirección, notas) | 2 |
| 4.7 | Validación con Zod + React Hook Form | 1 |
| 4.8 | Endpoint `POST /api/v1/orders`: persiste pedido + items, retorna ID | 2 |
| 4.9 | Generación del mensaje WhatsApp con loop sobre items | 2 |
| 4.10 | Redirect a `wa.me/...?text=...` después del POST exitoso | 1 |
| 4.11 | Admin: shell del panel (sidebar, layout, auth guard) | 3 |
| 4.12 | Admin: CRUD de productos (tabla + form + upload imagen) | 4 |
| 4.13 | Admin: CRUD de categorías (simple, inline) | 1 |
| 4.14 | Admin: vista de pedidos con cambio de estado (`PENDING/CONFIRMED/CANCELLED`) | 2 |
| 4.15 | **Rate limit + honeypot en `POST /api/v1/orders`**: es un endpoint público y anónimo. Sin esto, cualquiera con curl inunda la bandeja del cliente | 2 |
| 4.16 | **Política de privacidad + aviso de tratamiento de datos en el checkout (LGPD)**: página `/privacidade` en PT-BR + checkbox de consentimiento. Sprint 4 es donde empieza a entrar dato personal real (`customers`: nome, WhatsApp, endereço) | 2 |

**Entregable**: **FrontPet puede vender por WhatsApp y gestionar su catálogo.**

**Hito** 🎯 **05/09 — primera venta real posible.** Es el hito contra el que se pide el
segundo pago. Avisale al cliente y validá el modelo con tráfico real.

> ⚠️ **Interino hasta el Sprint 7**: el reset de contraseña del admin (7.12) todavía no
> existe en esta etapa. Si el cliente se bloquea entre el Sprint 4 y el 7, lo resolvés a
> mano vos. Es aceptable por unas semanas — no como estado de entrega.

---

## Sprint 5 — Booking backend (~19 hs) ⏩ adelantar al bloque A si hay slack

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

## Sprint 6 — Booking frontend + Admin de turnos (semanas 13-14, ~16 hs)

**Objetivo**: cierre del flujo de reservas end-to-end.

| # | Tarea | Hs |
|---|-------|----|
| 6.1 | Página `/turnos` con wizard de 3 pasos | 5 |
| 6.2 | Paso 1: selector de servicio con cards | 1 |
| 6.3 | Paso 2: selector de fecha (scroll horizontal) + selector de slot (grid 4 cols) | 3 |
| 6.4 | Paso 3: formulario de datos del cliente y mascota | 2 |
| 6.5 | Pantalla de confirmación con resumen + redirect a WhatsApp | 2 |
| 6.6 | Admin: vista de turnos del día + próximos 7 días | 2 |
| 6.7 | Admin: botón "confirmar por WhatsApp" con mensaje pre-formateado | 1 |

**Entregable**: **FrontPet puede recibir reservas online.**

**Hito** 🎯: **primera reserva online posible** (semana 14).

---

## Sprint 7 — Dashboard + LGPD + Marketing + Reset de senha + Polish (~24,5 hs)

**Objetivo**: sumar visibilidad de métricas, cumplir LGPD, cerrar el hueco de seguridad del
login y llevar todo a calidad de entrega.

| # | Tarea | Hs |
|---|-------|----|
| 7.1 | Endpoint `GET /api/v1/admin/dashboard` con queries agregados | 2 |
| 7.2 | Admin: componente `<KPICard>` reutilizable | 1 |
| 7.3 | Admin: mini-dashboard con **conteos operacionales** — pedidos día/mes/total, turnos día/próximos, top productos, top servicios. ⚠️ **Sin métricas analíticas** (ADR 003/008): nada de conversão, faturamento, ticket médio ni trend pills | 3 |
| 7.12 | **Recuperación de contraseña del admin**: token de un solo uso + expiración, email vía **Resend** (free tier, requiere el dominio verificado de D.2). Incluye **generar la pantalla en Stitch primero** — no existe hoy | 5 |
| 7.13 | **Banner de consentimiento LGPD + gating del Pixel**: el Meta Pixel **no puede disparar antes del consentimiento**. Hacer esta tarea **antes** de la 7.4. Pantalla a generar en Stitch — tampoco existe | 2 |
| 7.14 | **Derecho de eliminación (LGPD)**: endpoint admin para borrar los datos de un `customer` a pedido | 1 |
| 7.4 | Meta Pixel: instalación base + eventos estándar (Contact, ViewContent, Schedule, Purchase). **Gateado por 7.13** | 2 |
| 7.6 | Plausible: instalación con script tag en `<head>` | 0.5 |
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

Antes de marcar una tarea como completa:

- ✅ Código en `main` sin warnings de compilación
- ✅ Funciona en mobile real (probado en celular vía red local, no solo DevTools)
- ✅ Si toca backend: test de integración del happy path
- ✅ Si toca UI: validación visual a 320px, 768px, 1024px
- ✅ Si afecta UX visible: screenshot guardado en la issue de GitHub
- ✅ Si introdujo decisión técnica: ADR creado o actualizado
- ✅ Si tocó schema de DB: `docs/db-model.png` regenerado

**Desde Sprint Despliegue en adelante también aplica:**
- ✅ Deployado y verificado en producción
- ✅ Sin errores nuevos en Sentry post-deploy

> **No hay entorno de staging.** El CX32 ya reparte 8 GB entre Spring Boot, Postgres y Next;
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

Calendario de demos (re-baseado a julio 2026):
- **Demo 1**: fin de Sprint 2 → landing pulida (Loom + screenshots)
- **Demo 2**: fin de Sprint Despliegue → landing en vivo con URL pública 🎯
- **Demo 3**: fin de Sprint 4 → **primera venta posible** 🎯 **← 05/09, hito de cobro**
- **Demo 4**: fin de Sprint 6 → primera reserva posible 🎯
- **Entrega final**: **30/09/2026**

> ⚠️ **La conversación sobre el pago del 05/09 va a mediados de agosto, no al final.**
> El pedido es adelantar el segundo pago contra un hito real (la tienda vendiendo), no
> contra una promesa. Necesita OK por escrito antes de que la fecha esté encima.

---

**Última actualización**: 2026-07-17
**Versión del documento**: 2.0
