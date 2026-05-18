# Roadmap FrontPet MVP1

> Plan detallado de ejecución del MVP1. Sprints de 2 semanas, 8 a 10 horas semanales,
> 19 semanas totales (16 activas + 3 colchón). Total estimado: ~166 hs trabajadas
> + ~26 hs de buffer = ~192 hs disponibles.

---

## Resumen de fases

| Sprint | Semanas | Foco | Hs estimadas |
|--------|---------|------|-------------:|
| 0 | Previa | Pre-kickoff (compras, accesos) | 6 |
| 1 | 1-2 | Setup infraestructura completa | 18 |
| 2 | 3-4 | Landing pública responsive | 20 |
| 3 | 5-7 | Catálogo + backend de productos | 24 |
| 4 | 8-9 | **Carrito + pedidos WhatsApp + Admin productos** | 27 |
| 5 | 10-11 | Booking backend (disponibilidad + reservas) | 18 |
| 6 | 12-13 | Booking frontend + Admin de turnos | 16 |
| 7 | 14-15 | Mini-dashboard + Meta Pixel + Plausible + Polish | 23 |
| 8 | 16 | Capacitación + entrega formal | 14 |
| Colchón | 17-19 | Imprevistos, ajustes, parciales, viajes | 26 |
| **Total** | **19 sem** | | **~192 hs** |

**Hitos clave**:
- 🎯 Sprint 4 (semana 9): Primera venta posible
- 🎯 Sprint 6 (semana 13): Primera reserva online posible
- 🎯 Sprint 8 (semana 16): MVP completo entregado

---

## Sprint 0 — Pre-kickoff (semana previa, ~6 hs)

**Objetivo**: Tener toda la infraestructura comprada y accesos listos antes de
arrancar oficialmente el contrato.

| # | Tarea | Hs |
|---|-------|----|
| 0.1 | Compra de VPS Hetzner CX22 (€4.50/mes) y SSH key inicial | 1 |
| 0.2 | Compra de dominio y configuración DNS apuntando al VPS | 1 |
| 0.3 | Cuenta Cloudflare: R2 + Pages + DNS proxy | 1 |
| 0.4 | Cuenta Sentry (tier gratis), Plausible o Umami | 0.5 |
| 0.5 | Creación de los 2 repos en GitHub (backend + frontend) | 0.5 |
| 0.6 | Subir `CLAUDE.md`, `ROADMAP.md` y `docs/decisions/001-005.md` a ambos repos | 1 |
| 0.7 | Setup de GitHub Projects (Kanban con las fases como milestones) | 1 |

**Entregable**: VPS pingueando, dominio resolviendo, repos creados y documentados.

---

## Sprint 1 — Setup completo (semanas 1-2, 18 hs)

**Objetivo**: Backend "hello world" andando, frontend con primera pantalla, deploys
automatizados via push a main.

| # | Tarea | Hs |
|---|-------|----|
| 1.1 | Instalar Coolify en el VPS, configurar HTTPS automático | 3 |
| 1.2 | Setup Spring Boot 3 con estructura modular: `tenant`, `identity`, `catalog`, `booking`, `orders`, `notifications` | 4 |
| 1.3 | Configurar Postgres en Docker Compose, Flyway, primera migración con tablas `tenants` y `users` | 2 |
| 1.4 | Spring Security + JWT en cookie HttpOnly, endpoint `POST /api/v1/auth/login` funcional | 3 |
| 1.5 | Setup Next.js 14 + TypeScript strict + Tailwind + shadcn/ui + Framer Motion | 2 |
| 1.6 | Configurar Cloudflare Pages con auto-deploy desde GitHub | 1 |
| 1.7 | Configurar CORS, primer endpoint conectado frontend-backend | 1 |
| 1.8 | Backups automáticos: cron + `pg_dump` + upload a Cloudflare R2 | 2 |

**Entregable**:
- `https://frontpet.com` (frontend) y `https://api.frontpet.com` (backend) accesibles
- Página "hello world" en el frontend
- Login funcional contra base de datos real
- Push a `main` → deploy automático

**Hito**: Infraestructura operativa end-to-end.

**Riesgos**:
- Coolify es nuevo para vos → revisá su doc antes de empezar (1 hora extra fuera del sprint)
- Postgres + Flyway dentro de Docker puede dar problemas de conexión la primera vez

---

## Sprint 2 — Landing pública (semanas 3-4, 20 hs)

**Objetivo**: Landing comercial pulida y mobile-first, lista para que FrontPet la apruebe
visualmente. Basada en el espíritu del prototipo `frontpet-landing.html`, no copiada literal.

| # | Tarea | Hs |
|---|-------|----|
| 2.1 | Estructura general de la landing en componentes React | 3 |
| 2.2 | `<Hero>` con animaciones (Framer Motion: fade-in, slide-up) | 3 |
| 2.3 | `<TrustBar>` y `<AnnouncementBar>` superior | 1 |
| 2.4 | `<ServiceCard>` (preview, sin booking todavía, link a `/turnos`) | 2 |
| 2.5 | `<ProductCard>` con CTA WhatsApp directo (variante para landing) | 2 |
| 2.6 | `<Reviews>` con 3-4 testimonios estáticos | 1 |
| 2.7 | `<FinalCTA>` y `<FloatingWA>` con pulse animation | 2 |
| 2.8 | Footer con links, contacto, redes | 1 |
| 2.9 | Optimización Lighthouse: imágenes, fuentes, Core Web Vitals > 90 | 3 |
| 2.10 | Responsive completo: 320px / 768px / 1024px / 1440px | 2 |

**Entregable**: Landing en producción, accesible y pulida.

**Hito**: Aprobación visual del cliente (mostrar y pedir feedback explícito).

---

## Sprint 3 — Catálogo + Backend de productos (semanas 5-7, 24 hs)

**Objetivo**: Catálogo dinámico desde DB con detalle por producto y CRUD vía API.

| # | Tarea | Hs |
|---|-------|----|
| 3.1 | Migración SQL: `categories`, `products`, `product_images` con `tenant_id` | 2 |
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

**Entregable**: Catálogo navegable real, indexable por Google, optimizado.

**Hito**: Cliente puede empezar a planificar sus fotos y descripciones reales.

**Riesgos**:
- Upload de imágenes a R2 con presigned URLs puede tomar 2 hs extra la primera vez
- Decidir el tamaño/formato de imágenes (recomendado: WebP, max 1200x1200, < 200KB)

---

## Sprint 4 — Carrito + Pedidos WhatsApp + Admin productos (semanas 8-9, 27 hs)

**Objetivo**: Cierre del flujo de venta + autonomía del cliente sobre el catálogo.

| # | Tarea | Hs |
|---|-------|----|
| 4.1 | Migración SQL: `orders`, `order_items`, `customers` | 1 |
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

**Hito** 🎯: **Primera venta real posible** (semana 9). Avisale al cliente, es buen
momento para validar el modelo con tráfico real.

---

## Sprint 5 — Booking backend (semanas 10-11, 18 hs)

**Objetivo**: Modelar la agenda y resolver el query difícil de slots disponibles.

| # | Tarea | Hs |
|---|-------|----|
| 5.1 | Migración SQL: `services`, `resources`, `schedule_rules`, `appointments`, `schedule_blocks` | 2 |
| 5.2 | Modelos JPA + repositorios | 2 |
| 5.3 | **Servicio de cálculo de slots disponibles** (el query difícil) | 5 |
| 5.4 | Endpoint `GET /api/v1/availability?service=X&date=Y` | 2 |
| 5.5 | Endpoint `POST /api/v1/appointments` con validación de solapamientos | 3 |
| 5.6 | Tests de integración: race conditions, slots de borde, servicios largos | 3 |
| 5.7 | Admin: CRUD de servicios y configuración de `schedule_rules` | 1 |

**Entregable**: API de booking funcional (sin frontend público todavía).

**Hito**: Punto técnicamente más complejo del MVP superado.

**Riesgos**:
- El query de slots puede tomar 1-2 hs más de lo estimado → revisá el ADR-005 antes de arrancar
- Servicios de distinta duración (1h vs 2.5h) complican el cálculo → resolver con tests primero

---

## Sprint 6 — Booking frontend + Admin de turnos (semanas 12-13, 16 hs)

**Objetivo**: Cierre del flujo de reservas end-to-end.

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

**Hito** 🎯: **Primera reserva online posible** (semana 13).

---

## Sprint 7 — Dashboard + Marketing + Polish (semanas 14-15, 23 hs)

**Objetivo**: Sumar visibilidad de métricas, marketing tools y llevar todo a calidad de entrega.

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
| 7.12 | Sentry: configurar para frontend y backend, probar primer error capturado | 1.5 |
| 7.13 | Uptime Kuma o equivalente, monitorear endpoints clave | 1 |
| 7.14 | Verificación de backups: simulación de restore en otro entorno | 1 |

**Entregable**: Sistema en estado de entrega.

---

## Sprint 8 — Capacitación + Entrega formal (semana 16, 14 hs)

**Objetivo**: Que FrontPet pueda operar de forma autónoma.

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

**Hito** 🎯: **Segundo pago de USD 250 liberado.**

---

## Colchón — Sprint 9 (semanas 17-19, hasta 26 hs)

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
- ✅ Funciona en mobile real (probado en celular, no solo DevTools)
- ✅ Si toca backend: test de integración del happy path
- ✅ Si toca UI: validación visual a 320px, 768px, 1024px
- ✅ Deployado en preview/staging
- ✅ Si afecta UX visible: screenshot guardado en la issue de GitHub
- ✅ Sin errores nuevos en Sentry post-deploy

---

## Demos al cliente

**Cada 2 sprints** (1 vez por mes), enviar al cliente:
- Video Loom de 3-5 min mostrando lo nuevo
- Mensaje con resumen escrito y pedido explícito de feedback:
  > "Te dejo el avance del mes. ¿Hay algo que quieras cambiar antes de seguir?
  > Si no me respondés en 3 días, sigo con el plan original."

Calendario tentativo de demos:
- **Demo 1**: fin de Sprint 2 (semana 4) → landing pulida
- **Demo 2**: fin de Sprint 4 (semana 9) → primera venta posible 🎯
- **Demo 3**: fin de Sprint 6 (semana 13) → primera reserva posible 🎯
- **Demo 4**: fin de Sprint 7 (semana 15) → sistema completo
- **Entrega final**: semana 16

---

**Última actualización**: mayo 2026
**Versión del documento**: 1.0
