# FrontPet — Contexto del proyecto para Claude

> Este archivo es el punto de entrada para cualquier asistente de IA que trabaje en este repo.
> Leelo completo antes de generar código. Si vas a tomar una decisión técnica que contradiga
> lo que está acá, primero confirmá con el desarrollador.

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

### Frontend
- **Next.js 14** (App Router, no Pages Router)
- **React 18** + **TypeScript** (strict mode)
- **Tailwind CSS** + **shadcn/ui**
- **Framer Motion** para animaciones
- **TanStack Query** para data fetching
- **React Hook Form** + **Zod** para validación de formularios

### Infraestructura
- **VPS Hetzner CX22** (Ubuntu 24.04)
- **Coolify** como PaaS auto-hosteado
- **Docker** + **Docker Compose**
- **Caddy** (reverse proxy + SSL automático)
- **Cloudflare R2** para storage de imágenes
- **Cloudflare Pages** para deploy del frontend
- **GitHub Actions** para CI
- **Sentry** para error tracking
- **Plausible** para analytics de visitas

### Tracking
- **Meta Pixel** instalado en el frontend público (eventos estándar: Contact, ViewContent, Schedule, Purchase)
- Tracking propio (tabla `events`) **NO va en MVP1**, queda para Fase 2

---

## 3. Decisiones arquitectónicas vigentes

Las decisiones detalladas están en `docs/decisions/`. Resumen:

- **Monolito modular**, no microservicios
- **Multi-tenant ready**: toda tabla del dominio lleva `tenant_id` desde el día uno
- **API REST** (no GraphQL)
- **Auth con JWT en cookie HttpOnly**, no en localStorage
- **Slots de booking calculados dinámicamente**, no materializados en DB
- **Carrito en sessionStorage**, no persistido en backend hasta el envío del pedido
- **WhatsApp click-to-chat** (`wa.me/...?text=...`), no WhatsApp Business API
- **IDs en UUID v7** para entidades públicas, BIGSERIAL para internas

---

## 4. Estructura del repositorio

El proyecto está dividido en dos repos:

```
frontpet-backend/          (Spring Boot)
├── src/main/java/com/frontpet/
│   ├── tenant/             Configuración del negocio
│   ├── identity/           Usuarios admin, auth
│   ├── catalog/            Productos, categorías
│   ├── booking/            Servicios, recursos, reservas
│   ├── orders/             Pedidos, items
│   └── notifications/      Generación de mensajes WhatsApp
├── src/main/resources/
│   └── db/migration/       Migraciones Flyway
└── docker-compose.yml

frontpet-web/              (Next.js)
├── app/
│   ├── (public)/           Rutas públicas (landing, catálogo, booking)
│   ├── (admin)/            Rutas del panel admin
│   └── api/                API routes mínimas (proxy a backend si hace falta)
├── components/
│   ├── ui/                 Componentes shadcn/ui
│   ├── public/             Componentes de la web pública
│   └── admin/              Componentes del admin
├── lib/                    Utilidades, API client, hooks
└── tailwind.config.ts
```

---

## 5. Convenciones de código

### Commits
Conventional Commits **en español**:
- `feat: agrega flujo de checkout multi-producto`
- `fix: corrige cálculo de slots para servicios largos`
- `refactor: extrae servicio de cálculo de disponibilidad`
- `docs: actualiza README de setup local`
- `chore: actualiza dependencias menores`

### Branches
- `main` siempre desplegable
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

---

## 7. Scope MVP1 — qué entra y qué no

### ✅ Incluido en MVP1 (USD 500 one-time)

**Público**
- Landing comercial responsive
- Catálogo con búsqueda y filtro por categoría
- Detalle de producto con galería
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

## 8. Cómo Claude debería trabajar en este repo

Cuando Claude (Code o web) reciba pedidos en este proyecto, debería:

1. **Primero leer este archivo** antes de generar código
2. **Respetar las decisiones de la sección 3 y 6** sin debatirlas (a menos que el desarrollador
   abra explícitamente la discusión)
3. **Si una tarea cae fuera del scope MVP1** (sección 7), avisar antes de implementar:
   "Esta tarea parece ser Fase 2 según `CLAUDE.md`. ¿Confirmás que querés que la haga?"
4. **Para preguntas de "qué library uso"**: revisar primero la sección 2. Si la decisión
   no está tomada, proponer 2-3 opciones con trade-offs claros.
5. **Para queries SQL**: priorizar legibilidad sobre cleverness. Comentar el por qué de joins
   complejos.
6. **Para tests**: priorizar tests de integración del happy path antes que coverage exhaustivo.

---

## 9. Comandos útiles del proyecto

```bash
# Backend
./mvnw spring-boot:run                  # Levantar backend local
./mvnw test                             # Correr tests
./mvnw flyway:migrate                   # Aplicar migraciones DB
docker compose up -d                    # Levantar Postgres local

# Frontend
pnpm dev                                # Servidor de desarrollo
pnpm build                              # Build de producción
pnpm lint                               # Linter
pnpm typecheck                          # Verificación de tipos TS

# Deploy (automático vía push a main)
git push origin main                    # Trigger del CI/CD
```

---

## 10. Contactos y referencias

- **Desarrollador**: [tu nombre]
- **Cliente piloto**: FrontPet (Pehuajó, Buenos Aires)
- **Repos**: [pendiente — completar al crearlos]
- **Producción**: [pendiente — completar al desplegar]
- **Diseño de referencia**: ver `FrontPet.jsx` y `frontpet-landing.html` en `/prototypes/`
  (los prototipos son **referencia visual**, no se copian literalmente)

---

**Última actualización**: mayo 2026
**Versión del documento**: 1.0
