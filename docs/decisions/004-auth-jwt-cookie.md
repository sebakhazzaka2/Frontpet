# ADR 004 — Autenticación con JWT en cookie HttpOnly

**Fecha**: 2026-05-17
**Estado**: Aceptada
**Autor**: [tu nombre]

---

## Contexto

El MVP1 tiene un solo tipo de autenticación: el **administrador del negocio**
(FrontPet) accediendo al panel admin.

Los clientes finales NO se loguean en MVP1 (decisión separada — ver scope MVP1 en
`CLAUDE.md`).

Hay que elegir cómo almacenar el token de autenticación en el navegador del admin.

## Decisión

**JWT en cookie HttpOnly + Secure + SameSite=Lax**.

Detalles técnicos:

- El token se emite en `POST /api/v1/auth/login`
- Se envía al cliente via `Set-Cookie` header (no en el body)
- Atributos de la cookie:
  - `HttpOnly` — inaccesible desde JavaScript
  - `Secure` — solo HTTPS
  - `SameSite=Lax` — protección CSRF razonable
  - `Path=/`
  - `Max-Age=3600` (1 hora)
- El frontend nunca toca el token; el navegador lo manda automáticamente
- Para invalidar sesión: `POST /api/v1/auth/logout` que setea cookie con `Max-Age=0`
- Refresh token (opcional, considerar para Fase 2 si las sesiones son muy cortas)

**Payload del JWT**:
```json
{
  "sub": "<user_id>",
  "tenantId": "<tenant_id>",
  "roles": ["ADMIN"],
  "iat": 1700000000,
  "exp": 1700003600
}
```

## Alternativas consideradas

### ❌ JWT en localStorage

**A favor**:
- Simple de implementar en el frontend
- Funciona en cualquier dominio sin configurar CORS de cookies

**En contra**:
- **Vulnerable a XSS** — cualquier script puede leer el token
- Es la fuente número uno de ataques en aplicaciones SPA
- Mala práctica reconocida en la industria

### ❌ JWT en sessionStorage

Mismo problema que localStorage. Solo cambia que se borra al cerrar la pestaña.

### ❌ Session ID en cookie con sesión server-side

**A favor**:
- Revocación inmediata posible
- Token "tonto" (no contiene información)

**En contra**:
- Requiere store de sesiones (Redis o tabla en DB)
- Más complejidad para un MVP1 con un admin único

### ❌ OAuth con proveedor externo (Google, etc.)

**A favor**: Outsourcing del problema de auth.

**En contra**:
- Fuerza al admin a tener cuenta de Google
- Más fricción para FrontPet
- Sobreingeniería para un MVP

## Consecuencias

### Positivas
- Inmune a XSS (atacker no puede leer la cookie)
- Mitigación natural de CSRF con SameSite=Lax
- El frontend no maneja tokens — código más simple
- Sin necesidad de Redis ni store de sesiones

### Negativas
- Configurar CORS con `credentials: include` requiere atención
- Backend y frontend deben compartir el mismo dominio padre (o configurar CORS estrictamente)
- Revocación de token no es inmediata (vive hasta su expiración natural)

### Mitigaciones
- Sentry alerta si se detectan patrones de uso anómalos (Fase 2)
- Sesiones cortas (1 hora) limitan el daño de un token comprometido
- HTTPS obligatorio en todos los entornos (incluido desarrollo via tunnels)

## Notas

- En desarrollo local, usar `localhost` consistentemente en frontend y backend
  para que las cookies funcionen
- En producción, configurar `Domain=.frontpet.com` si frontend y backend están en
  subdominios distintos (`frontpet.com` y `api.frontpet.com`)
- Cuando se sume auth de clientes finales (Fase 2+), revisitar esta decisión —
  puede tener sentido un store de sesiones con Redis para permitir logout global
