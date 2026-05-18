# FrontPet

> Plataforma web comercial para FrontPet, un petshop local. Catálogo de productos
> con pedidos por WhatsApp + agenda online de servicios + panel administrativo.

[![Status](https://img.shields.io/badge/status-en%20desarrollo-orange)]()
[![License](https://img.shields.io/badge/license-Proprietario-blue)]()

---

## Sobre el proyecto

FrontPet es una plataforma web comercial pensada para digitalizar la venta de productos
y la reserva de servicios para mascotas. El objetivo del MVP1 es validar el modelo
comercial digital con la menor fricción técnica posible y máxima velocidad de
implementación.

A largo plazo, el sistema está diseñado para evolucionar hacia un SaaS multi-tenant
configurable para múltiples negocios locales (petshops, peluquerías, barberías,
gimnasios, etc.).

### ¿Qué hace?

**Para el cliente final** (público):
- Navegar un catálogo de productos con búsqueda y filtros
- Armar un carrito multi-producto
- Enviar el pedido por WhatsApp con un mensaje pre-formateado
- Reservar turnos online para servicios (baños, peluquería)

**Para el negocio** (admin):
- Gestionar productos, categorías e imágenes
- Configurar servicios y horarios de atención
- Ver pedidos y turnos recibidos
- Mini-dashboard con métricas operativas

---

## Stack tecnológico

```
Backend         Spring Boot 3 + Java 21 + PostgreSQL 16 + Flyway
Frontend        Next.js 14 + TypeScript + Tailwind CSS + shadcn/ui
Auth            JWT en cookie HttpOnly + Spring Security
Storage         Cloudflare R2 (imágenes)
Infraestructura VPS Hetzner + Docker + Coolify + Caddy
CDN             Cloudflare Pages (frontend público)
Tracking        Meta Pixel + Plausible
```

Ver `docs/decisions/001-stack-tecnologico.md` para el razonamiento completo.

---

## Estructura del repositorio

> Este proyecto está dividido en dos repos:
> - **frontpet-backend** — Spring Boot (este repo si estás en backend)
> - **frontpet-web** — Next.js (este repo si estás en frontend)

```
.
├── README.md                  ← Este archivo
├── CLAUDE.md                  ← Contexto del proyecto para asistentes IA
├── ROADMAP.md                 ← Plan de ejecución por sprints
├── docs/
│   └── decisions/             ← Architectural Decision Records (ADRs)
│       ├── README.md
│       ├── 001-stack-tecnologico.md
│       ├── 002-multi-tenant.md
│       ├── 003-pedidos-whatsapp.md
│       ├── 004-auth-jwt-cookie.md
│       └── 005-slots-dinamicos.md
└── src/ o app/                ← Código fuente del proyecto
```

---

## Primeros pasos (desarrollo local)

### Requisitos previos

- Java 21 (JDK)
- Node.js 20+ y pnpm
- Docker y Docker Compose
- Git
- Editor recomendado: IntelliJ IDEA Community (backend) + VS Code o Cursor (frontend)

### Levantar el backend localmente

```bash
# Clonar
git clone https://github.com/[usuario]/frontpet-backend.git
cd frontpet-backend

# Levantar Postgres en Docker
docker compose up -d

# Aplicar migraciones de DB
./mvnw flyway:migrate

# Correr el backend
./mvnw spring-boot:run
```

El backend queda en `http://localhost:8080`.

### Levantar el frontend localmente

```bash
# Clonar
git clone https://github.com/[usuario]/frontpet-web.git
cd frontpet-web

# Instalar dependencias
pnpm install

# Variables de entorno (copiar el ejemplo)
cp .env.example .env.local

# Levantar el servidor de desarrollo
pnpm dev
```

El frontend queda en `http://localhost:3000`.

---

## Comandos útiles

### Backend
```bash
./mvnw spring-boot:run          # Levantar backend
./mvnw test                     # Correr tests
./mvnw flyway:migrate           # Aplicar migraciones
./mvnw flyway:info              # Ver estado de migraciones
docker compose up -d            # Levantar Postgres
docker compose down             # Detener Postgres
```

### Frontend
```bash
pnpm dev                        # Servidor de desarrollo
pnpm build                      # Build de producción
pnpm start                      # Servir build local
pnpm lint                       # ESLint
pnpm typecheck                  # Verificación TypeScript
```

---

## Documentación

| Documento | Para qué sirve |
|-----------|----------------|
| [CLAUDE.md](./CLAUDE.md) | Contexto completo del proyecto, convenciones, scope MVP1 |
| [ROADMAP.md](./ROADMAP.md) | Plan de ejecución por sprints con tareas y estimaciones |
| [docs/decisions/](./docs/decisions/) | Decisiones arquitectónicas (ADRs) |

---

## Estado del proyecto

**Versión actual**: MVP1 en desarrollo
**Inicio**: mayo 2026
**Entrega estimada**: fin de septiembre 2026
**Duración**: 19 semanas (16 activas + 3 colchón)

### Hitos principales

- [ ] Sprint 1 (sem 1-2) — Setup infraestructura
- [ ] Sprint 2 (sem 3-4) — Landing pública
- [ ] Sprint 3 (sem 5-7) — Catálogo
- [ ] **Sprint 4 (sem 8-9) — 🎯 Primera venta posible**
- [ ] Sprint 5 (sem 10-11) — Booking backend
- [ ] **Sprint 6 (sem 12-13) — 🎯 Primera reserva posible**
- [ ] Sprint 7 (sem 14-15) — Polish + Marketing
- [ ] Sprint 8 (sem 16) — Capacitación + Entrega

---

## Equipo

- **Desarrollador**: Sebastian Khazzaka
- **Cliente piloto**: FrontPet

---

## Licencia

Proprietario. La arquitectura base del sistema pertenece al desarrollador. FrontPet
obtiene licencia de uso del MVP desarrollado para su operación comercial dentro del
alcance acordado.

Ver propuesta comercial firmada para detalles.
