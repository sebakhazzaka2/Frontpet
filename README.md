# FrontPet

> Plataforma web comercial para FrontPet, un petshop local. Catálogo de productos
> con pedidos por WhatsApp + agenda online de servicios + panel administrativo.

[![Status](https://img.shields.io/badge/status-en%20desarrollo-orange)]()
[![License](https://img.shields.io/badge/license-Proprietario-blue)]()

**Para el cliente final**: navegar el catálogo, armar carrito, enviar el pedido por
WhatsApp, reservar turnos online (Banho & Tosa).
**Para el negocio**: gestionar productos e imágenes, servicios y horarios, ver pedidos
y turnos, mini-dashboard operativo.

A largo plazo el sistema está diseñado para evolucionar hacia un SaaS multi-tenant para
negocios locales. MVP1 es single-tenant.

---

## Stack

```
Backend         Spring Boot 3 + Java 21 + PostgreSQL 16 + Flyway
Frontend        Next.js 16 + TypeScript + Tailwind CSS v4 + shadcn/ui
Auth            JWT en cookie HttpOnly + Spring Security
Infraestructura VPS Hetzner + Coolify + Caddy · Cloudflare DNS/CDN + R2 (imágenes)
Tracking        Meta Pixel + Plausible (gateados por consentimiento LGPD)
Errores/Email   Sentry (backend y frontend) · Resend (reset de contraseña)
```

Razonamiento: [docs/decisions/](./docs/decisions/) (ADR 001 stack, ADR 016 deploy).

---

## Estructura (monorepo)

```
.
├── backend/     Spring Boot (módulos: common, config, tenant, identity, catalog, orders, booking,
│                dashboard, privacy, notifications)
├── frontend/    Next.js 16 (App Router)
├── docs/        ADRs, design system, runbook de deploy, ESTADO.md (estado verificado)
├── CLAUDE.md    Contexto del proyecto: convenciones, scope, reglas
└── ROADMAP.md   Plan de ejecución por sprints (fuente de verdad del plan)
```

---

## Desarrollo local

Requisitos: Java 21 · Node 20.9+ y pnpm (en WSL, ver ADR 015) · Docker.

```bash
# Backend (localhost:8080)
cd backend
docker compose up -d            # Postgres
./mvnw spring-boot:run          # aplica migraciones Flyway al arrancar

# Frontend (localhost:3000)
cd frontend
pnpm install
cp .env.example .env.local
pnpm dev
```

Otros comandos: `./mvnw test` (requiere Docker, usa Testcontainers) · `pnpm build` / `pnpm test` / `pnpm lint`.
El CI corre `mvn -B verify` y `pnpm build` + `pnpm test` + `pnpm lint`.

---

## Estado

**En producción** en [frontpet.com.br](https://frontpet.com.br) (API en `api.frontpet.com.br`), con
MVP1 funcional: catálogo, pedidos por WhatsApp, agenda online, panel admin, reset de contraseña y
LGPD. Sprint 7 (polish) en curso. El runtime es **single-tenant** (un despliegue = un negocio).

Estado verificado, riesgos y deuda: [docs/ESTADO.md](./docs/ESTADO.md). Contexto efímero:
[docs/WORKING-CONTEXT.md](./docs/WORKING-CONTEXT.md). Plan e hitos: [ROADMAP.md](./ROADMAP.md).

## Equipo y licencia

**Desarrollador**: Sebastián Khazzaka · **Cliente piloto**: FrontPet (Santana do
Livramento, RS, Brasil).

Proprietario. La arquitectura base pertenece al desarrollador; FrontPet obtiene licencia
de uso del MVP para su operación comercial. Ver propuesta comercial firmada.
