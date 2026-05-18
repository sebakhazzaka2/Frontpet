# ADR 001 — Stack tecnológico

**Fecha**: 2026-05-17
**Estado**: Aceptada
**Autor**: [tu nombre]

---

## Contexto

Tenía que elegir el stack para construir el MVP1 de FrontPet, considerando estas restricciones:

- Estudiante avanzado de sistemas con experiencia previa en Java/Spring Boot
- Cliente comercial real que paga Meta Ads y necesita una web profesional
- Presupuesto del cliente: USD 500 (MVP) + USD 47/mes (mantenimiento + infraestructura)
- Sin urgencia extrema, hay margen para invertir 2-3 semanas adicionales en una arquitectura más sólida
- Visión futura: SaaS multi-tenant para múltiples negocios locales
- Posibilidad de integrar con ERP/CRM de FrontPet más adelante

Se evaluaron dos filosofías de stack:

### Opción A — BaaS (Backend-as-a-Service)
Next.js (frontend + API routes) + Supabase (DB + Auth + Storage) + Vercel.

### Opción B — Stack tradicional auto-hosteado
Next.js (frontend) + Spring Boot (backend) + PostgreSQL + VPS Hetzner.

## Decisión

**Stack B — Tradicional auto-hosteado**:

- **Backend**: Spring Boot 3 + Java 21
- **Frontend**: Next.js 14 + React + TypeScript + Tailwind + shadcn/ui
- **Base de datos**: PostgreSQL 16
- **Auth**: JWT en cookie HttpOnly + Spring Security
- **Storage**: Cloudflare R2
- **Infraestructura**: VPS Hetzner CX22 + Coolify + Docker
- **Frontend CDN**: Cloudflare Pages

## Alternativas consideradas

### ❌ Stack A (Next.js + Supabase + Vercel)

**A favor**:
- Time-to-market 2-3 semanas más rápido
- Todo en TypeScript end-to-end
- Auth y Storage resueltos sin código

**En contra**:
- **Vercel Hobby prohíbe uso comercial** → requiere Vercel Pro (USD 20/mes)
- Supabase Free se queda corto en 3-6 meses → Pro USD 25/mes
- **Costo recurrente USD 45-50/mes** vs USD 12/mes en Stack B
- Vendor lock-in moderado con Supabase y Vercel
- Limitaciones para integrar con ERP/CRM externos en el futuro
- Aprovecha menos el conocimiento previo de Java/Spring Boot del desarrollador

### ❌ Cloudflare Pages + Workers + D1
- D1 todavía joven, menos maduro que Postgres
- Cambio de paradigma grande respecto al conocimiento previo
- Migración futura más compleja si se necesita escalar

### ❌ Vercel + PlanetScale o Neon directo
- Sigue siendo más caro que VPS auto-hosteado a mediano plazo
- Mismo problema de uso comercial en Vercel Hobby

## Consecuencias

### Positivas
- Costo recurrente bajo (USD 6 VPS + USD 1 dominio = ~USD 7/mes base)
- Control total sobre datos e infraestructura
- Reutilización del conocimiento de Java/Spring Boot
- Arquitectura preparada para integrar con ERP/CRM en el futuro sin reescribir
- Aprendizaje real de DevOps (Docker, Linux, Caddy) que suma al perfil profesional
- Sin restricciones comerciales en el deploy

### Negativas
- 2-3 semanas adicionales de desarrollo vs Stack A
- Más responsabilidad operativa (backups, monitoreo, actualizaciones de seguridad)
- Más superficie técnica a mantener
- Curva de aprendizaje inicial con Coolify y Docker

### Mitigaciones
- Coolify reduce la fricción de DevOps (UI tipo Vercel)
- Backups automatizados con cron + R2
- Sentry para error tracking
- Uptime Kuma para monitoreo básico

## Notas

- Esta decisión es **reversible**: si en el futuro el proyecto crece y la operación
  del VPS se vuelve costosa en tiempo, se puede migrar a un PaaS gestionado.
- La elección de **PostgreSQL sobre MySQL** se documenta como parte de esta decisión:
  Postgres es más moderno, mejor JSON nativo, mejor concurrencia, y es lo que usan
  los proveedores managed más relevantes (Neon, Supabase, etc.) por si se migra.
