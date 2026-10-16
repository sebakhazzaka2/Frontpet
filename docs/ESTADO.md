# ESTADO — FrontPet

**Fecha**: 2026-10-16 · **Rama**: `main` (HEAD `3ba78a5`) · **Alcance**: lectura de código, migraciones, config, CI y docs. No se ejecutó build, tests, `pnpm audit` ni la app; no se tocó producción. No se lee ningún `.env` real ni se muestra ningún valor sensible.

> Auditoría detallada previa (más extensa, sin trackear): `docs/estado-frontpet.md`. Esta ficha es el resumen para el proyecto de planificación. Al cerrar sesión se actualiza junto con la sección "Estado actual" de `CLAUDE.md`.

---

## 1. HECHOS VERIFICADOS

### Funcionalidad implementada
| Funcionalidad | Evidencia |
|---|---|
| Catálogo público (productos, categorías, especies, variantes, promos, imágenes en R2) | `backend/src/main/java/com/frontpet/catalog/api/PublicCatalogController.java`, `AdminProductController.java`, migraciones V2/V6–V9 |
| Pedidos anónimos (carrito → `POST /orders` → mensaje WhatsApp armado en el cliente) | `orders/api/PublicOrderController.java`, `frontend/lib/whatsapp/templates.ts` |
| Reserva de turnos con slots dinámicos, capacidad, bloqueos, horarios, turno manual | `booking/api/*Controller.java`, `AppointmentServiceImpl`, `AvailabilityServiceImpl`, `SlotGrid` |
| Admin: productos, pedidos, turnos, servicios (solo edición), horarios/bloqueos, dashboard de conteos | `*/api/Admin*Controller.java`, `frontend/app/admin/(protected)/` |
| Auth admin: login JWT en cookie HttpOnly, logout, reset de contraseña por email | `identity/AuthController` (`/api/v1/auth/*`), `PasswordResetService`, `notifications/` |
| LGPD: anonimización por teléfono con log + banner de consentimiento + tracking gateado | `privacy/*`, `V13__privacy_anonymization.sql`, `frontend/components/consent/`, `frontend/lib/analytics/` |
| Observabilidad: logs JSON en perfil `prod`, Sentry en ambos lados, solo `/actuator/health` público | `backend/src/main/resources/application.yml`, `frontend/sentry.*.config.ts` |
| Rate limit por IP (login, reset, pedidos, turnos) + honeypot, en memoria | `identity/LoginRateLimit*`, `orders/OrderRateLimit`, ADR 019 |
| Tests backend: 38 archivos, 237 `@Test`, Postgres real vía Testcontainers | `backend/src/test/java/` |
| Tests frontend: 7 archivos (vitest) | `frontend/**/*.test.ts(x)` |
| CI: `mvn -B verify` (backend) y `pnpm build/test/lint` (frontend) en push a `main` y PRs | `.github/workflows/ci.yml` |
| Dockerfiles multi-stage de ambos servicios | `backend/Dockerfile`, `frontend/Dockerfile` |

### Endpoints disponibles (base `/api/v1`)
- **Públicos (sin auth)**: `GET /services`, `GET /availability`, `POST /appointments`, `GET /appointments/{publicId}`, `GET /products`, `GET /products/{slug}`, `GET /categories`, `GET /species`, `POST /orders`.
- **Auth**: `POST /auth/login`, `/auth/logout`, `/auth/forgot-password`, `/auth/reset-password`.
- **Admin (cookie de sesión)**: `/admin/appointments` (GET, POST manual, PATCH `/{id}/status`, PATCH `/{id}/tempo-extra`), `/admin/orders` (GET, `/counts`, `/{id}`, PATCH `/{id}/status`), `/admin/products` (CRUD, `/images/presign`, `/{id}/variants`, `/{id}/active`), `/admin/services` (GET, PUT), `/admin/business-hours`, `/admin/schedule-blocks`, `/admin/dashboard`, `/admin/privacy/preview|anonymize`.
- Fuente: `grep` de `@*Mapping` en `backend/src/main/java/**/api/`. Contratos de booking: `docs/booking-api-contracts.md`; colección en `docs/postman/`. No hay OpenAPI.

### Multi-tenant (estado real)
- Esquema listo: `tenant_id` en todas las tablas de dominio (`V1`–`V4`).
- Runtime single-tenant: `tenant/CurrentTenant.java` lee un UUID fijo de configuración (`frontpet.tenant.id`).
- Sin filtro global ni RLS: grep de `@Filter|FilterDef|@TenantId|ROW LEVEL` en `backend/src/main` → 0. El aislamiento depende de que cada query incluya `tenantId`.
- Login por email global: `AdminUserRepository.findByEmail(String)` sin tenant (usado en `AuthService`, `UserDetailsServiceImpl`, `PasswordResetService`, `DataInitializer`).
- Sin alta de negocios (el tenant se siembra por SQL en `V5__seed_dev.sql`, el admin por env en `config/DataInitializer.java`).
- Solo 3 claves en `tenants.config`: `capacidade_atendimento`, `anticipacao_min_horas`, `anticipacao_max_dias`.

### Riesgos de seguridad visibles (sin valores)
1. CSRF deshabilitado (`config/SecurityConfig.java:89`); solo SameSite=Lax. Aceptado en docs; la cookie con `Domain` raíz amplía la superficie (`docs/pending-decisions.md` §26).
2. Sin autorización por rol: grep de `@PreAuthorize|@EnableMethodSecurity` → 0; todo es `authenticated()`.
3. Rate limit en memoria (no sobrevive a reinicios ni a varias instancias) y por IP.
4. `application.yml` trae defaults de desarrollo para credenciales; `StartupEnvValidator` solo protege con perfil `prod`.
5. El guard del admin en Next solo comprueba existencia de la cookie; la autorización real es del backend (correcto, pero la UI se renderiza con cualquier cookie).
6. `GET /appointments/{publicId}` es público (nombre + mascota, sin teléfono); el id es UUID v7.
7. El CI no incluye escaneo de dependencias (`ci.yml`).
- Bien resuelto: JWT HttpOnly+Secure, BCrypt, revocación de sesiones al cambiar contraseña, reset de un solo uso, headers de seguridad, validación `jakarta.validation`, precio/duración recalculados en servidor.

### No existe (verificado por grep/estructura)
Entidad cliente/mascota (son columnas de texto en `appointments`/`orders`), historia clínica, caja, pagos reales (solo se captura `forma_pagamento`), profesionales, jobs programados (`@Scheduled`: 0 usos reales), recordatorios, WhatsApp Business API, API key / auth máquina-a-máquina, cancelación pública de turnos, OpenAPI.

---

## 2. HIPÓTESIS / DUDAS

- **Producción**: `docs/WORKING-CONTEXT.md` (2026-10-01) dice que `frontpet.com.br` y `api.frontpet.com.br` están en vivo. No se verificó contra la URL.
- **Perfil `prod`**: si `SPRING_PROFILES_ACTIVE=prod` no está activo en el VPS, `StartupEnvValidator` no corre y rigen los defaults de desarrollo.
- **Seed de desarrollo en producción**: `V5`/`V10`/`V11` (seeds, incluido un admin y datos reales del negocio) pueden haberse ejecutado en prod; nada lo impide (`docs/pending-decisions.md` §4).
- **IP real tras Caddy**: `FORWARD_HEADERS_STRATEGY` por defecto es `NONE`; si no se configura, el rate limit vería la IP del proxy.
- **Backups (D.7)**: sin evidencia de que existan ni de restauración probada.
- **Email de reset**: no confirmado que el dominio de Resend esté verificado ni que llegue a Gmail/Outlook sin caer en spam.
- **Cuentas compartidas**: `pending-decisions.md` §18 indica una cuenta de Cloudflare personal compartida con otro cliente.
- **Cobertura numérica**: desconocida (sin JaCoCo). CI en verde según `gh run list` de la auditoría previa; no se re-ejecutó.
- **Precios de adicionais** provisorios y contenido de reseñas/trust-bar parcialmente inventado (`pending-decisions.md` §15).
- **Estimaciones** de esfuerzo para multi-tenant (3–5 semanas) y por vertical (1–2 semanas por fork) son del análisis previo, sin validar.

---

## 3. PENDIENTE / DEUDA TÉCNICA (priorizada)

1. **Backups de Postgres** (D.7): configurar y probar una restauración. Hay datos reales de clientes en producción.
2. **Aislamiento de tenant sin red de seguridad**: filtro global de Hibernate o RLS + tests con dos tenants reales.
3. **Login ambiguo con más de un tenant** (`findByEmail` global) y `CurrentTenant` fijo: resolver tenant por credencial/dominio y meter `tenantId` en el JWT.
4. **Confirmar que el seed de desarrollo no corre en producción** (separar migraciones por perfil) y que el perfil `prod` está activo.
5. **Stock nunca se descuenta** (`pending-decisions.md` §27): decidir si se descuenta al pasar a `CONFIRMED`.
6. **Roles / autorización por rol y CSRF token** (o scope más fino de la cookie, §26).
7. **Rate limit persistente y por credencial**, y verificar `FORWARD_HEADERS_STRATEGY` detrás de Caddy.
8. **Para un agente externo**: API key por negocio con scopes, cancelación/reprogramación segura, auditoría humano vs agente, OpenAPI.
9. **Marca y rubro hardcodeados** en ~41 archivos del frontend (`lib/data/site.ts`, metadata, plantillas WhatsApp, `Porte` P/M/G/GG en modelo): mover a `tenant.config`.
10. **Cobertura de pruebas**: frontend con 7 tests y sin E2E (Playwright instalado sin specs); sin JaCoCo; doble submit del wizard sin proteger (§21).

Menores (fuera del top 10): `POST /admin/products` responde 500 ante UTF-8 inválido (§22); `.githooks/` y `docs/ui/` obsoletos; hay un checklist de prueba en producción pendiente de correr (`docs/checklist-prueba-prod.md`, con `docs/limpieza-datos-prueba.sql`).

---

## Inconsistencias README ↔ código

Detectadas el 2026-10-16 y **corregidas en el mismo día** (`README.md`): script `pnpm typecheck` inexistente, módulos del backend incompletos, estado/fechas vencidas y falta de Sentry/Resend. Sin inconsistencias abiertas conocidas. Queda como cita rota solo `docs/reuse-consultorio.md` en comentarios Javadoc de `identity`/`config` (el archivo fue eliminado a propósito; recuperable del historial de git).
