# Guía de reutilización — `consultorio-odontologico` → FrontPet backend

> **Qué es esto**: inventario accionable, archivo por archivo, de qué traer del backend del
> consultorio odontológico (Java 17 + MySQL + Angular) al backend de FrontPet (Java 21 +
> Postgres 16 + Next). Pensado como checklist para el momento de implementar: en cada pieza
> dice si se copia casi directo, se adapta o se reescribe, y **exactamente qué cambia**.
>
> **Fuente**: `C:\Users\Usuario\dev\consultorio-odontologico` (rutas relativas a
> `backend/src/main/java/com/consultorio/`). Las **decisiones** ya están en
> [ADR 013](./decisions/013-modelo-datos-mvp1.md); esto es el "cómo portar".
>
> **Regla de oro**: nada se copia a ciegas. El paquete es por feature (no por capa), los DTOs
> van como **records** de Java 21, los IDs públicos son UUID v7, y **todo lleva `tenant_id`**.

**Leyenda**: 🟢 copiar casi directo · 🟡 adaptar · 🔵 patrón (reescribir siguiendo la idea) · 🔴 descartar

---

## 1. Seguridad / Auth — el bloque de mayor reuse

| Archivo origen | Verdicto | Notas |
|---|---|---|
| `security/JwtService.java` | 🟢 | jjwt 0.12.5, HMAC, subject=email. Casi verbatim. |
| `security/UserDetailsServiceImpl.java` | 🟢 | `findByEmail`. Verbatim contra `admin_users`. |
| `security/JwtAuthFilter.java` | 🟡 | **Cambio clave: leer cookie, no header.** |
| `security/SecurityConfig.java` | 🟡 | Rutas + CSRF + sacar register. |
| `service/AuthService.java` | 🟡 | Lógica reusable; cambia la entrega del token. |
| `controller/AuthController.java` | 🟡 | DTOs → records, login setea cookie, agregar logout. |

**Cambios concretos al portar:**

- **`JwtAuthFilter`** (líneas 34-39): hoy hace `request.getHeader("Authorization")` + `startsWith("Bearer ")`. FrontPet lee el JWT de la **cookie HttpOnly** (ADR 004). Cambio localizado: reemplazar esas líneas por leer `request.getCookies()` y buscar la cookie de sesión. El resto del filtro (validar token, poblar `SecurityContext`) queda igual.
- **`AuthService.login`**: la lógica (`authenticationManager.authenticate` + `jwtService.generateToken`) se reusa tal cual. Lo que cambia es **cómo se entrega**: hoy devuelve `{ "token": ... }` en el body; FrontPet debe **setear la cookie** `HttpOnly + Secure + SameSite=Lax` en la respuesta (se hace en el controller con `ResponseCookie`). El body ya no lleva el token.
- **Bug a corregir al portar**: `login` tira `RuntimeException("Credenciales inválidas")` → cae en el handler genérico → **HTTP 500**. Debe ser **401** con una excepción propia (ej. `BadCredentialsException` de Spring o una custom mapeada en el GlobalExceptionHandler).
- **`SecurityConfig`**:
  - Rutas: cambiar `/auth/login`, `/public/**` por las de FrontPet: `GET /api/v1/**` de catálogo/disponibilidad **público**, `POST /api/v1/bookings` y `POST /api/v1/orders` **público**, resto `/api/v1/admin/**` **autenticado**.
  - **`/auth/register` está `permitAll` → SACARLO.** FrontPet no tiene self-registration de admins; el admin se seedea (ver §4). Dejar register abierto es un agujero.
  - **CSRF**: hoy está `.csrf(disable)` y es correcto *porque el token va en header Bearer* (inmune a CSRF). Con **cookie** cambia el análisis: `SameSite=Lax` da buena protección para los POST cross-site, pero para defensa en profundidad conviene evaluar un **token CSRF** en los POST de estado. → Decisión a tomar al portar; puede ameritar actualizar el ADR 004.
  - CORS con `allowCredentials=true` ya está bien (necesario para que el browser mande la cookie).
- **`AuthController`**: DTOs (`LoginRequest`) como **records**. `login` setea la cookie. Agregar endpoint **`logout`** que limpia la cookie (el consultorio no lo tiene porque el front borraba el token del storage).
- **Multi-tenant (futuro, no MVP1)**: el JWT no lleva `tenant_id` y `UserDetailsServiceImpl` busca solo por email. En MVP1 single-tenant alcanza. Cuando haya multi-tenant, agregar claim `tenant_id` al token y `findByTenantAndEmail`.

---

## 2. Manejo de excepciones — copiar casi directo

| Archivo origen | Verdicto | Notas |
|---|---|---|
| `exception/GlobalExceptionHandler.java` | 🟢 | Mapea 404/400/422/409/500 limpio. |
| `exception/ResourceNotFoundException.java` | 🟢 | Trivial, verbatim. |
| `GlobalExceptionHandler.ErrorResponse` (record interno) | 🟢 | Verbatim. |

**Cambios al portar:**
- El mensaje de `DataIntegrityViolationException` es clínico ("historial u otros") → **genericar**.
- Los mensajes user-facing van en **PT-BR** (ADR 007), no español.
- Agregar el handler de **credenciales inválidas → 401** (ver §1).

---

## 3. Booking / disponibilidad — el core, adaptar fuerte

| Archivo origen | Verdicto | Notas |
|---|---|---|
| `service/CitaService.getDisponibilidad` | 🔵 | El algoritmo oro. Ver cambios abajo. |
| `service/DisponibilidadService.java` | 🟡 | Upsert horario + bloqueos. |
| `service/PublicReservaService.java` | 🔵 | Patrón de reserva pública. |
| `repository/CitaRepository.java` | 🔵 | Patrón de query de solapamiento. |

**`CitaService.getDisponibilidad`** (ya detallado en ADR 013) — el algoritmo (bloqueados → día activo → candidatos cada 15min → descartar pausa → descartar solapados) se traduce, con estos cambios **obligatorios**:
- Capacidad: `anyMatch(solapa)` → **`count(solapan) < capacidade`** (ADR 009, =2).
- Duración: pasar `base(porte) + Σ adicionais(porte)` como duración total (ADR 011). El algoritmo es agnóstico a la duración, no cambia.
- `tenant_id` en todas las queries.
- Usar `appointments.end_at` (ya persistido) en vez de recomputar desde duración.
- **Lock de concurrencia** que el original NO tiene (`SELECT FOR UPDATE`/advisory lock) — el `create()` del consultorio tiene la race del último cupo.
- Matar los hacks: `minusHours(3)` (usar rango limpio con `end_at`) y `%7` (usar `dia_semana` ISO 1-7).

**`DisponibilidadService`**:
- `upsertDia` (upsert de horario por día) → 🟢 reusable, + `tenant_id`.
- Bloqueos: el origen usa **fecha única** (`fechas_bloqueadas`); FrontPet usa **rango** (`schedule_blocks.data_desde/data_hasta`). Adaptar `esFechaBloqueada` a "¿la fecha cae en algún rango del tenant?".
- El `@Scheduled(cron)` que limpia fechas pasadas → 🟢 buen patrón reusable.

**`PublicReservaService`**:
- Patrón a reusar: validar que el slot esté disponible **antes** de crear, y crear el turno `PENDING`.
- **Sacar la dedup por email/`Paciente`** — FrontPet no tiene tabla customer, el contacto va **embebido** (ADR 013). El cliente se guarda como `cliente_nome`/`cliente_telefone` en `appointments`.
- Agregar el **lock** al crear (el original valida-y-crea sin lock).

**`CitaRepository`**: patrón de query derivada (`findByFechaHoraInicioBetweenAndEstado`). Para FrontPet, la query de solapamiento filtra por `tenant_id` + rango de `start_at`/`end_at` + `status <> 'CANCELLED'` (aprovecha el índice parcial que ya creamos).

---

## 4. Config / startup — portar el patrón (alineado con lo de hoy)

| Archivo origen | Verdicto | Notas |
|---|---|---|
| `config/StartupEnvValidator.java` | 🟢 | Valida env vars en prod. Muy recomendable. |
| `config/DataInitializer.java` | 🟡 | Seed de admin desde env, idempotente. |
| `config/ClinicProperties.java` | 🔵 | FrontPet usa `tenant.config` JSONB, distinto. |

- **`StartupEnvValidator`** → **portar sí o sí.** Corre en perfil `prod`, valida que `JWT_SECRET` (≥32 chars), CORS y credenciales de admin estén seteados y **no sean el default inseguro**, y hace `System.exit(1)` si falta algo. Está 100% alineado con el trabajo de env vars + hook pre-push que hicimos hoy — es la contraparte en runtime. Adaptar las vars a las de FrontPet (`DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, admin).
- **`DataInitializer`** → patrón de seed de admin desde env (`ApplicationRunner`, idempotente: si el admin ya existe, no hace nada). FrontPet: seedear **tenant + admin**. Complementa el seed por migration (los datos de dominio como servicios van por migration; el admin con password hasheada conviene por `DataInitializer` para no meter hashes en SQL).
- **`ClinicProperties`** (`@ConfigurationProperties`) → FrontPet modela la config del negocio en `tenant.config` (JSONB), no en properties estáticas. El patrón aplica parcialmente; baja prioridad.

---

## 5. Descartar (confirmado)

| Origen | Por qué |
|---|---|
| `service/PagoService`, `model/Pago`, `controller/PagoController`, `SaldoController` | FrontPet no tiene pagos online. |
| `HistorialProcedimientos*` (model/service/controller/dto) | Dominio clínico, no aplica. |
| `service/GooglePlacesService`, `googleEventId` en Cita | Google Calendar/Places fuera de scope. |
| `model/Paciente`, `PacienteOrigen`, dedup por email | FrontPet sin registro de clientes; contacto embebido. |
| `controller/PublicReviewsController`, `ReviewResponse` | Reviews dinámicas = Fase 2. |
| `service/FileStorageService` | Guarda en disco local; FrontPet usa **R2** (S3 API). Reescribir. |
| Todo Angular | FrontPet es Next.js. |

---

## Checklist "cuando implementemos" (ordenado por momento)

**Sprint de auth/identity:**
- [ ] Portar `JwtService`, `UserDetailsServiceImpl` (🟢).
- [ ] Portar `JwtAuthFilter` con lectura de **cookie**.
- [ ] Portar `SecurityConfig`: rutas FrontPet, sacar register, decidir CSRF.
- [ ] Portar `AuthService`/`AuthController`: login setea cookie, logout, records, error 401.
- [ ] Portar `StartupEnvValidator` + `DataInitializer` (seed tenant + admin).
- [ ] Portar `GlobalExceptionHandler` + `ResourceNotFoundException` (mensajes PT-BR).

**Sprint de booking:**
- [ ] Traducir `getDisponibilidad` con capacidad=N, duración base+adicionais, `tenant_id`, `end_at`, **lock**.
- [ ] Adaptar `DisponibilidadService` (upsert horario + bloqueos por **rango**).
- [ ] Adaptar `PublicReservaService` sin dedup, con lock.
- [ ] Query de solapamiento en el repo (tenant + rango + status).

**Transversal:** cada pieza portada lleva `tenant_id` y test de integración del happy path (Definition of Done).
