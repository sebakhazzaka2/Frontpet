# ADR 022 — Reset de contraseña del admin

**Estado**: Aceptada
**Fecha**: 2026-08-29
**Sprint**: 7 (tarea 7.12)

---

## Contexto

Hasta esta tarea, el admin de FrontPet no tenía forma de recuperar su contraseña: si la
olvidaba, había que cambiarla a mano en la DB (`frontend/components/admin/login-form.tsx`
documentaba explícitamente que "Esqueceu a senha" se omitió del mock de Stitch porque esta
tarea no existía todavía).

La implementación completa toca tres áreas nuevas para el repo — la primera tabla con token
expirable, la primera llamada HTTP saliente a un tercero, y el primer uso real del paquete
`notifications/` — y una decisión de diseño no trivial: si además de permitir setear una
contraseña nueva, el reset debe **invalidar las sesiones JWT ya emitidas**.

Preguntas a resolver antes de codear:
1. ¿Token opaco o JWT de un solo uso?
2. ¿Cómo se persiste el token sin poder reconstruirlo desde un leak de solo-lectura de la DB?
3. ¿El reset invalida sesiones activas, y si sí, cómo, dado que ADR 004 usa JWT stateless?
4. ¿Qué proveedor de email, y con qué cliente HTTP?
5. ¿Cómo se evita que el flujo sea un oráculo de enumeración de cuentas?

---

## Decisión

### 1. Token opaco de 32 bytes (`SecureRandom`), Base64URL sin padding — no JWT

43 caracteres URL-safe. El uso único exige estado consultable en DB de todos modos (hay que
poder marcar "ya usado" y purgar los viejos al pedir uno nuevo), así que un JWT no ahorraría la
tabla — solo agregaría una segunda fuente de verdad y extendería el radio de explosión de
`jwt.secret` de "sesiones de 1h" a "la cuenta permanentemente": quien tenga el secreto podría
forjar un link de reset válido para cualquier admin, sin pasar por Resend ni por la DB.

### 2. Se persiste `sha256(token)` en hex — explícitamente no BCrypt

BCrypt tiene salt por fila: el hash no es determinístico, así que no se puede indexar/buscar —
verificar un token exigiría traer todas las filas no usadas y correr `matches()` contra cada una
(~100ms cada vez, con el costo pensado para eso). Y ese costo computacional existe para
compensar la baja entropía de una contraseña *humana*; acá el valor tiene 256 bits de
`SecureRandom`, no hay diccionario que ralentizar. Se hashea igual (nunca se persiste el token en
claro) para que un leak de solo-lectura de la DB no alcance para reconstruir el link.

`VARCHAR(64)`, no `CHAR(64)`: `CHAR` en Postgres devuelve padding a la derecha y el ahorro de
espacio frente a `VARCHAR` es nulo.

**TTL 60 minutos.** 15 min es hostil si el mail tarda en llegar; 24h es una ventana de ataque
gratis sobre un link que viaja por email en texto plano. Pedir un token nuevo hace `DELETE` de
los anteriores del mismo admin — invalida pendientes, limpia usados y purga expirados en una
sola operación, sin necesitar un `@Scheduled`.

### 3. Estado en Postgres, no en memoria

A diferencia de los rate limiters de `common.SlidingWindowLimiter` (ADR 019), acá la tabla vive
en Postgres. Contraste deliberado: un rate limit perdido en un redeploy es una molestia menor
(el atacante recupera presupuesto un poco antes); un token de reset perdido en pleno vuelo deja
al admin bloqueado fuera de su propio panel sin poder reintentar hasta pedir un link nuevo — el
mismo problema que esta tarea existe para resolver.

### 4. Invalidación de sesión vía `admin_users.password_changed_at`

**Decisión explícita de esta tarea, no parte del scope original**: un reset exitoso invalida
las cookies de sesión ya emitidas. Sin esto, un atacante con una sesión robada sobrevive
tranquilamente a que la víctima "resuelva" el problema reseteando su contraseña.

La comparación vive en `JwtAuthFilter`, no en `JwtService`: `JwtService` no conoce el dominio
(`isTokenValid(token, userDetails)` es una firma genérica de Spring Security) y el filtro ya
tiene el `AdminUser` concreto que devuelve `UserDetailsServiceImpl`. `JwtService.extractIssuedAt`
solo expone el claim `iat` crudo.

**El problema del segundo**: jjwt serializa `iat` como `NumericDate`, granularidad de segundo;
`password_changed_at` tiene sub-segundo. Dentro del mismo segundo esa información de desempate
ya se perdió al firmar — no hay forma de saber si el token se emitió antes o después del reset.
Se elige **fallar del lado de invalidar**: se rechaza todo token con
`iat < floor(password_changed_at) + 1s`.

- Falso positivo (login legítimo justo en el mismo segundo del reset, tratado como inválido) =
  el admin vuelve a loguearse — molestia de segundos, y en la práctica casi inexistente: el
  flujo de reset termina redirigiendo a la pantalla de login de todas formas.
- Falso negativo (la sesión del atacante sobrevive) = exactamente el ataque que esta feature
  existe para impedir.

`password_changed_at` se backfillea en la migración con `created_at`, **no con `now()`**: con
`now()` el propio deploy de esta tarea mataría toda sesión admin activa sin ningún reset real de
por medio.

**Matiza ADR 004**: ese ADR lista como consecuencia negativa que "la revocación de token no es
inmediata (vive hasta su expiración natural)". Sigue siendo cierto para el caso general — no hay
un `POST /logout` remoto que tire abajo una sesión ajena bajo demanda — pero un reset de
contraseña ahora sí revoca de inmediato. No es gratis: `loadUserByUsername` ya corre en cada
request autenticada y ya trae la fila entera (así que no se suma una query), pero la sesión pasa
de ser puramente stateless a **semi-stateful** — en la práctica ya lo era parcialmente, porque
`JwtAuthFilter` siempre golpeó la DB en cada request para resolver el usuario.

### 5. Resend como proveedor + `java.net.http.HttpClient` sin SDK

**Resend**, no SMTP directo ni una librería tipo `starter-mail`: entrega gestionada con dominio
verificado (SPF/DKIM/DMARC), sin mantener infraestructura SMTP propia. **SES quedó descartado**
por el sandbox inicial (requiere salir de él para mandar a destinatarios no verificados, trámite
extra que no se justifica para un solo remitente/dominio de este tamaño).

Cliente a mano con `java.net.http.HttpClient` del JDK 21 + el `ObjectMapper` de Boot — sin SDK de
terceros. Mismo criterio de `common.UuidV7` y CLAUDE.md §6 ("¿se resuelve con vanilla?"): es el
primer HTTP saliente del repo, y sienta el precedente para el próximo.

- **`sendAsync`, no `send`**: un Resend colgado no puede convertir cada `forgot-password` en 10s
  de hilo bloqueado, y el envío síncrono además rompería la anti-enumeração por timing (punto 6).
- **Nunca propaga**: `EmailSender.send()` no lanza. Los errores se loguean en ERROR; el caller
  sigue como si el envío hubiera salido. Es lo que garantiza que un Resend caído no cambie la
  respuesta HTTP y por lo tanto no filtre si un email existe.
  **Limitación aceptada, declarada abierta** (mismo criterio que ADR 018 con el tope de tamaño de
  R2): con Resend caído, el token existe en la DB pero el usuario nunca recibe el link. El TTL de
  60 min lo vuelve irrelevante — el admin simplemente reintenta el pedido.
- **Sin reintentos**: un reintento sobre un endpoint de envío puede duplicar el mail, y un
  segundo mail con otro token confunde más de lo que ayuda.
- `LoggingEmailSender` cubre dev/test sin `RESEND_API_KEY` (mismo precedente que los placeholders
  de `R2Config`/`S3Presigner`) — única excepción deliberada a "nunca loguear el token en claro":
  sin eso no hay forma de probar el flujo completo en local.

### 6. Anti-enumeração: 202 constante + doble rate limit

`POST /forgot-password` responde **siempre 202 con body vacío** — email existente, inexistente,
o bloqueado por rate limit de email son indistinguibles desde afuera. Las únicas respuestas
distintas son el 400 de validación de formato (no depende de la DB) y el 429 del filtro de IP
(depende de la IP, no del email). Espejo del test de anti-enumeração que ya existía para el
login.

El envío del email **tiene que ser asíncrono** o la anti-enumeração se rompe por timing: síncrono,
un email existente respondería en ~300ms (BCrypt + INSERT) y uno inexistente en ~2ms, filtrable
con un cronómetro. Queda una diferencia residual de 1-3ms por el `DELETE`+`INSERT` del camino
"existe" — ruido dentro de la varianza de red, no se iguala con sleeps artificiales (eso agregaría
latencia real por una protección teatral).

Dos rate limits, no uno, porque protegen cosas distintas:

| Rate limit | Clave | Dónde | Presupuesto |
|---|---|---|---|
| IP | `"ip:" + remoteAddr` | `PasswordResetRateLimitFilter` (cubre ambos endpoints) | 5 / 15 min |
| Email | `"email:" + normalizado` | `PasswordResetService` (el filtro no puede leer el body sin envolver el request) | 3 / 1 h |

El de IP protege el server y es la única defensa contra fuerza bruta del token opaco en
`/reset-password`. El de email protege la casilla del cliente del bombardeo desde IPs rotativas
— sin él, un atacante con una botnet chica podría llenar la bandeja de un admin con decenas de
links por hora, cada uno desde una IP distinta.

**Limitación aceptada, documentada**: la clave de email se puede llenar con direcciones
inventadas hasta `SlidingWindowLimiter.MAX_TRACKED_KEYS` (10.000) y desalojar la entrada real. El
límite por IP es la primera línea que hace caro llegar a ese punto.

---

## Alternativas consideradas

### ❌ JWT de un solo uso como token de reset
Descartada por el punto 1: no ahorra la tabla (el uso único exige estado igual) y extiende el
radio de explosión de `jwt.secret`.

### ❌ BCrypt para el hash del token
Descartada por el punto 2: no determinístico (no indexable) y su costo computacional resuelve un
problema (baja entropía humana) que acá no existe.

### ❌ SMTP directo / Amazon SES
Descartadas por el punto 5: SMTP propio suma infraestructura a mantener sin necesidad; SES exige
salir del sandbox para un volumen que no lo justifica hoy.

### ❌ Invalidación de sesión con blacklist de JTIs
Guardar cada JWT emitido (o su `jti`) y consultar una blacklist en cada request sería más preciso
que comparar contra `password_changed_at` — resuelve el problema del segundo sin ambigüedad.
Descartada: exige una tabla/cache adicional con su propio TTL de limpieza, y vuelve el JWT
completamente stateful otra vez, perdiendo la ventaja que ADR 004 buscaba (sin store de sesiones).
`password_changed_at` da el 95% del beneficio (revoca TODAS las sesiones activas al resetear) sin
esa complejidad — el 5% que falta es el caso límite del mismo segundo, ya aceptado en el punto 4.

---

## Consecuencias

### Positivas
- El link de reset no puede reconstruirse desde un leak de solo lectura de la DB.
- Un reset exitoso corta cualquier sesión robada, no solo cambia la contraseña — mitiga
  parcialmente la consecuencia negativa que ADR 004 dejaba abierta.
- Cero dependencias nuevas: token, hash y cliente HTTP son todos JDK estándar.
- `forgot-password` es indistinguible en tiempo y respuesta entre cuenta existente, inexistente
  y bloqueada por rate limit.

### Negativas / a vigilar
- Con Resend caído, un pedido de reset legítimo no llega nunca — sin alertar a nadie de que pasó
  (es justamente lo que garantiza la anti-enumeração). Mitigado por el TTL corto + reintento
  manual del admin; se vuelve visible recién si el admin se queja de no recibir el link.
- La sesión JWT deja de ser puramente stateless (ver matización de ADR 004 en el punto 4).
- El límite de email es vulnerable a saturar `MAX_TRACKED_KEYS` con direcciones inventadas (punto
  6) — aceptado porque el de IP ya encarece mucho llegar a ese volumen.
- Dominio `frontpet.com.br` y sus registros DNS de envío (Resend) corren, por ahora, sobre la
  misma cuenta personal de Cloudflare de Sebastián que ya cargaba `pending-decisions.md` §18 —
  ese pendiente crece con esta tarea, no se resuelve acá.
