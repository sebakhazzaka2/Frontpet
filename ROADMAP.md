# Roadmap FrontPet MVP1

> Plan detallado de ejecución del MVP1. Sprints de 2 semanas, 8 a 10 horas semanales,
> 19 semanas totales. Total estimado: ~171 hs trabajadas + ~21 hs de buffer = ~192 hs disponibles.

---

## Resumen de fases

| Sprint | Semanas | Foco | Hs estimadas |
|--------|---------|------|-------------:|
| 0 | Previa | Pre-kickoff (GitHub Projects + modelo DB) | 3 |
| 1 | 1-2 | Setup local: backend + frontend + testing (sin deploy) | 14 |
| 2 | 3-4 | Landing pública responsive (local) | 22 |
| Despliegue | 5 | **Compra infra + despliegue inicial + landing en vivo** | 10 |
| 3 | 6-8 | Catálogo + backend de productos | 24 |
| 4 | 9-10 | **Carrito + pedidos WhatsApp + Admin productos** | 27 |
| 5 | 11-12 | Booking backend (disponibilidad + reservas) | 18 |
| 6 | 13-14 | Booking frontend + Admin de turnos | 16 |
| 7 | 15-16 | Mini-dashboard + Meta Pixel + Plausible + Polish | 23 |
| 8 | 17 | Capacitación + entrega formal | 14 |
| Colchón | 18-19 | Imprevistos, ajustes, parciales, viajes | 21 |
| **Total** | **19 sem** | | **~192 hs** |

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
- ✅ `tailwind.config.ts` con design tokens definidos
- ✅ `docs/design-system.md` con paleta, escalas y tipografía documentadas

**Pendiente**:

| # | Tarea | Hs |
|---|-------|----|
| 0.1 | Setup de GitHub Projects (Kanban con sprints como milestones, issues iniciales) | 1.5 |
| 0.2 | **Modelo de DB inicial en DBdiagram.io**: `tenants`, `users`, `categories`, `products`, `services`, `appointments`, `orders`. Exportar PNG a `docs/db-model.png` | 1.5 |

**Entregable**: Kanban operativo + modelo de DB visualizable antes de la primera migración.

> 💡 Por qué la 0.2: dibujar la DB antes de migrar te ahorra 2-3 refactors de schema
> en sprints 3-5. Cuesta 1.5 hs ahora, te ahorra 10 hs después.

---

## Sprint 1 — Setup local (semanas 1-2, ~14 hs)

**Objetivo**: backend y frontend corriendo en localhost con auth, primer endpoint conectado,
testing y observabilidad locales listos. **No hay despliegue todavía** — todo en localhost
contra Postgres en Docker.

| # | Tarea | Hs |
|---|-------|----|
| 1.1 | Setup Spring Boot 3 con estructura modular: `tenant`, `identity`, `catalog`, `booking`, `orders`, `notifications` | 4 |
| 1.2 | Postgres en Docker Compose + Flyway + primera migración (`tenants`, `users`) | 2 |
| 1.3 | Spring Security + JWT en cookie HttpOnly + endpoint `POST /api/v1/auth/login` funcional | 3 |
| 1.4 | Setup Next.js 14 + Framer Motion + TanStack Query + React Hook Form. Verificar que el design system de `tailwind.config.ts` funciona end-to-end con una pantalla mínima | 1.5 |
| 1.5 | CORS configurado, primer endpoint del frontend consumiendo backend local | 1 |
| 1.6 | **Testcontainers + primer test de integración** del endpoint de login. Sirve como template para todos los siguientes | 2 |
| 1.7 | **Logback con JSON structured output** + endpoint `/actuator/health` configurado y testeado | 0.5 |

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

## Sprint 2 — Landing pública (semanas 3-4, ~22 hs)

**Objetivo**: landing comercial pulida y mobile-first, lista para mostrar al cliente.
Sigue corriendo en local — el despliegue es la siguiente etapa.

| # | Tarea | Hs |
|---|-------|----|
| 2.0 | **Estudio de referencias + moodboard**: revisar Chewy, Bond Vet, Mobbin (categoría ecommerce mobile), Awwwards. Capturar 10-15 screenshots en `docs/moodboard/`. Definir 3 anclas visuales claras antes de codear | 2 |
| 2.1 | Estructura general de la landing en componentes React | 3 |
| 2.2 | `<Hero>` con animaciones Framer Motion (fade-in, slide-up) | 3 |
| 2.3 | `<TrustBar>` y `<AnnouncementBar>` superior | 1 |
| 2.4 | `<ServiceCard>` (preview, sin booking todavía, link a `/turnos`) | 2 |
| 2.5 | `<ProductCard>` con CTA WhatsApp directo (variante para landing) | 2 |
| 2.6 | `<Reviews>` con 3-4 testimonios estáticos | 1 |
| 2.7 | `<FinalCTA>` y `<FloatingWA>` con pulse animation | 2 |
| 2.8 | Footer con links, contacto, redes | 1 |
| 2.9 | Optimización Lighthouse: imágenes, fuentes, Core Web Vitals > 90 | 3 |
| 2.10 | Responsive completo: 320px / 768px / 1024px / 1440px | 2 |

**Entregable**: landing pulida en `localhost:3000`, lista para deployar la próxima semana.

**Hito**: aprobación visual del cliente (screenshots + Loom).

> 💡 La tarea 2.0 es la palanca más grande de calidad por hora invertida. Arrancar la
> landing sin moodboard es el camino más rápido a "queda como hecho con IA".

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

## Sprint 3 — Catálogo + Backend de productos (semanas 6-8, ~24 hs)

**Objetivo**: catálogo dinámico desde DB con detalle por producto y CRUD vía API.

| # | Tarea | Hs |
|---|-------|----|
| 3.1 | Migración SQL: `categories`, `products`, `product_images` con `tenant_id`. **Actualizar `docs/db-model.png`** | 2 |
| 3.2 | Modelos JPA + repositorios + servicios | 2 |
| 3.3 | Endpoints públicos: `GET /api/v1/products`, `GET /api/v1/products/{slug}`, `GET /api/v1/categories` | 3 |
| 3.4 | Endpoints admin protegidos: `POST/PUT/DELETE /api/v1/admin/products` | 2 |
| 3.5 | Integración Cloudflare R2 SDK en backend + endpoint de upload firmado | 3 |
| 3.6 | Frontend: página `/productos` con grid responsive | 2 |
| 3.7 | Filtro por categoría (chips horizontales scrolleables) | 2 |
| 3.8 | Búsqueda por nombre con debounce (300ms) | 2 |
| 3.9 | Página `/productos/[slug]` con galería de imágenes y descripción | 3 |
| 3.10 | Skeletons de carga, estados vacíos, error boundaries | 1 |
| 3.11 | ISR / `revalidate` en Next.js para catálogo (60s) | 1 |
| 3.12 | Seed con 10 productos de prueba | 1 |

**Entregable**: catálogo navegable real en producción, indexable por Google, optimizado.

**Hito**: cliente puede empezar a planificar sus fotos y descripciones reales.

**Riesgos**:
- Upload de imágenes a R2 con presigned URLs puede tomar 2 hs extra la primera vez
- Decidir el tamaño/formato de imágenes (recomendado: WebP, max 1200x1200, < 200KB)

---

## Sprint 4 — Carrito + Pedidos WhatsApp + Admin productos (semanas 9-10, ~27 hs)

**Objetivo**: cierre del flujo de venta + autonomía del cliente sobre el catálogo.

| # | Tarea | Hs |
|---|-------|----|
| 4.1 | Migración SQL: `orders`, `order_items`, `customers`. **Actualizar `docs/db-model.png`** | 1 |
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

**Entregable**: **FrontPet puede vender por WhatsApp y gestionar su catálogo.**

**Hito** 🎯: **primera venta real posible** (semana 10). Avisale al cliente, es buen
momento para validar el modelo con tráfico real.

---

## Sprint 5 — Booking backend (semanas 11-12, ~18 hs)

**Objetivo**: modelar la agenda y resolver el query difícil de slots disponibles.

| # | Tarea | Hs |
|---|-------|----|
| 5.1 | Migración SQL: `services`, `resources`, `schedule_rules`, `appointments`, `schedule_blocks`. **Actualizar `docs/db-model.png`** | 2 |
| 5.2 | Modelos JPA + repositorios | 2 |
| 5.3 | **Servicio de cálculo de slots disponibles** (el query difícil). Antes de codear: plan en prosa en `docs/decisions/006-calculo-slots.md` | 5 |
| 5.4 | Endpoint `GET /api/v1/availability?service=X&date=Y` | 2 |
| 5.5 | Endpoint `POST /api/v1/appointments` con validación de solapamientos | 3 |
| 5.6 | Tests de integración: race conditions, slots de borde, servicios largos | 3 |
| 5.7 | Admin: CRUD de servicios y configuración de `schedule_rules` | 1 |

**Entregable**: API de booking funcional (sin frontend público todavía).

**Hito**: punto técnicamente más complejo del MVP superado.

**Riesgos**:
- El query de slots puede tomar 1-2 hs más de lo estimado → revisá el ADR-005 antes de arrancar
- Servicios de distinta duración (1h vs 2.5h) complican el cálculo → resolver con tests primero

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

## Sprint 7 — Dashboard + Marketing + Polish (semanas 15-16, ~23 hs)

**Objetivo**: sumar visibilidad de métricas, marketing tools y llevar todo a calidad de entrega.

| # | Tarea | Hs |
|---|-------|----|
| 7.1 | Endpoint `GET /api/v1/admin/dashboard` con queries agregados | 2 |
| 7.2 | Admin: componente `<KPICard>` reutilizable | 1 |
| 7.3 | Admin: mini-dashboard con pedidos día/mes/total, turnos día/próximos, top productos, top servicios | 3 |
| 7.4 | Meta Pixel: instalación base + eventos estándar (Contact, ViewContent, Schedule) | 2 |
| 7.5 | Meta Pixel: evento Purchase al completar pedido (server-side opcional vía Conversions API) | 1 |
| 7.6 | Plausible: instalación con script tag en `<head>` | 0.5 |
| 7.7 | Auditoría mobile completa, ajustes finos | 3 |
| 7.8 | Pulido de animaciones y micro-interacciones | 2 |
| 7.9 | SEO básico: meta tags, Open Graph, sitemap.xml, robots.txt | 2 |
| 7.10 | Optimización de imágenes: WebP, lazy loading, srcset | 1 |
| 7.11 | Testing manual en navegadores reales (Chrome mobile, Safari iOS, Firefox) | 2 |
| 7.12 | Sentry: revisar dashboard, ajustar rate limits y alertas | 1.5 |
| 7.13 | Uptime Kuma o equivalente, monitorear endpoints clave | 1 |
| 7.14 | Verificación de backups: simulación de restore en otro entorno | 1 |

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
- ✅ Deployado en preview/staging
- ✅ Sin errores nuevos en Sentry post-deploy

---

## Demos al cliente

**Cada 2 sprints** (1 vez por mes), enviar al cliente:
- Video Loom de 3-5 min mostrando lo nuevo
- Mensaje con resumen escrito y pedido explícito de feedback:
  > "Te dejo el avance del mes. ¿Hay algo que quieras cambiar antes de seguir?
  > Si no me respondés en 3 días, sigo con el plan original."

Calendario tentativo de demos:
- **Demo 1**: fin de Sprint 2 (semana 4) → landing pulida (Loom + screenshots)
- **Demo 2**: fin de Sprint Despliegue (semana 5) → landing en vivo con URL pública 🎯
- **Demo 3**: fin de Sprint 4 (semana 10) → primera venta posible 🎯
- **Demo 4**: fin de Sprint 6 (semana 14) → primera reserva posible 🎯
- **Demo 5**: fin de Sprint 7 (semana 16) → sistema completo
- **Entrega final**: semana 17

---

**Última actualización**: mayo 2026
**Versión del documento**: 1.2
