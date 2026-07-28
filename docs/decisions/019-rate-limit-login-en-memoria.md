# ADR 019 — Rate limit del login: en memoria, solo por IP

**Estado**: Aceptada
**Fecha**: 2026-07-28
**Sprint**: 1 (tarea 1.8, deuda cerrada en el arranque del Sprint 4)

---

## Contexto

`POST /api/v1/auth/login` acepta intentos infinitos. Dos riesgos distintos, no uno:

1. **Fuerza bruta** contra la única cuenta admin de MVP1.
2. **DoS de CPU**: `BCryptPasswordEncoder` cuesta ~100 ms de cómputo por verificación *a
   propósito* (esa lentitud es la defensa contra crackeo offline). En el CX32 de 4 vCPU del
   Sprint Despliegue (ADR 016), un atacante que dispare logins en paralelo satura el server
   completo, no solo el login — el endpoint sin límite es un vector de DoS contra toda la
   tienda.

El punto (2) exige que el bloqueo ocurra **antes** de que `authenticate()` corra, no dentro de
`AuthService`.

Tres preguntas necesitaban respuesta antes de codear:
1. ¿Contador en memoria o persistido?
2. ¿Se bloquea por IP, por cuenta, o ambas?
3. ¿Cómo se obtiene la IP real detrás de un proxy sin abrir un bypass?

---

## Decisión

### 1. En memoria (`ConcurrentHashMap`), no en DB, no Redis

Una sola instancia en un solo VPS (ADR 016), sin scaling horizontal en MVP1. Un contador
persistido en Postgres significaría un `INSERT`/`UPDATE` por login fallido — amplificar en la
misma base que sirve el catálogo un tráfico que el propio atacante controla. Sería un rate
limiter que empeora el DoS que previene.

**Caveat aceptado**: un reinicio del proceso borra todos los contadores. Es un bypass real pero
acotado — el atacante no puede provocar reinicios, solo un deploy los produce (semanal como
mucho).

**Revisar cuando**: haya una segunda instancia del backend, o un requisito de que el bloqueo
sobreviva un deploy. Ahí corresponde Redis, no antes.

### 2. Solo por IP. Sin lockout por cuenta

Con **un solo admin** en MVP1, un lockout convencional por cuenta es un vector de auto-DoS:
cualquiera que adivine `admin@frontpet.dev` puede dejar a la clienta afuera de su propio panel
para siempre, fallando 5 logins cada 15 minutos desde cualquier lado. Para un negocio que
necesita ese panel para despachar pedidos, eso es peor que la fuerza bruta que se quiere evitar.

**Presupuesto**: 10 fallos / 15 minutos por IP → bloqueo de esa IP, mismo mensaje y código que
cualquier otro rechazo (ver punto 4). Sin escalado de backoff — la ventana fija ya cubre el caso
de uso real (credential stuffing desde uno o pocos hosts).

**Escape hatch**: `frontpet.login-rate-limit.enabled` (default `true`) para poder desactivarlo
vía env var y rescatar a alguien con un redeploy, sin tocar código.

**Diferido, no descartado**: un throttle *adicional* por cuenta (que nunca escale, para no
reabrir el auto-DoS) se evalúa junto a la tarea 4.15 (`POST /orders`), cuando haya un segundo
endpoint público real con el mismo problema y la generalización se pueda hacer con evidencia en
mano, no como una apuesta sobre un caso de uso que todavía no existe.

### 3. `X-Forwarded-For`: nunca parseado a mano

Ese header lo escribe quien quiera. Confiar en él sin filtrar significa que el atacante manda un
valor distinto en cada request, recibe un balde nuevo cada vez, y el rate limit queda decorativo
— además de que el mapa de contadores crece sin límite, un segundo vector de DoS.

`server.forward-headers-strategy` queda en **`NONE` por default**. El código del filtro solo lee
`request.getRemoteAddr()`. La propiedad se sube a `FRAMEWORK` recién en el Sprint Despliegue,
cuando Caddy sea el único camino de entrada al backend y esté configurado con
`trusted_proxies` apuntando a los rangos de Cloudflare — no antes. Si se sube antes de que Caddy
sea el único ingress, cualquiera se saltea el rate limit con un header falso.

### 4. Respuesta: `429`, nunca `423`

`423 Locked` es semántica WebDAV y, usado solo para el caso de cuenta bloqueada, sería un oráculo
de enumeración ("esta cuenta existe y está bloqueada"). Con la decisión del punto 2 (solo IP) el
riesgo de enumeración por código de estado ya no aplica hoy, pero se deja `429` fijado como
precedente: si en algún momento se suma el throttle por cuenta de la 4.15, ambos casos deben
devolver el mismo código y el mismo cuerpo.

`Retry-After: <segundos>` + mensaje en PT-BR (ADR 007) deliberadamente vago, sin contar
intentos: *"Muitas tentativas de login. Tente novamente em alguns minutos."*

### Por qué no Bucket4j

Bucket4j vale por sus backends distribuidos (Redis, Hazelcast). Acá hace falta un contador y un
timestamp en una sola JVM — CLAUDE.md §6 ("¿se resuelve con vanilla?") tiene una sola respuesta.
Un `ConcurrentHashMap` con `compute()` atómico y un tope duro de claves rastreadas (evicción
perezosa) cubre el caso sin sumar una dependencia.

---

## Alternativas consideradas

### ❌ Lockout por cuenta (con o sin escalado)
Descartado por el auto-DoS del punto 2: un solo admin convierte "bloquear la cuenta que falla"
en "cualquiera puede bloquear a la dueña del negocio".

### ❌ Contador persistido en `admin_users` (columnas `failed_login_attempts`, `locked_until`)
Descartado: exigiría una migración V10 acoplada a `ddl-auto: validate`, y un write a Postgres
por cada login fallido — tráfico que controla el atacante, amplificado contra la misma DB que
sirve la tienda.

### ❌ Confiar en `X-Forwarded-For` directamente
Descartado por ser trivialmente falseable sin un proxy de confianza en el medio que lo
reescriba. Ver punto 3.

### ❌ Bucket4j / Resilience4j
Descartado por CLAUDE.md §6: la necesidad real (un mapa acotado con ventana de tiempo, una sola
JVM) no justifica una dependencia pensada para backends distribuidos.

---

## Consecuencias

### Positivas
- El bloqueo corre **antes** de `authenticate()`, así que también corta el costo de CPU de
  BCrypt bajo ataque, no solo el resultado del login.
- Cero dependencias nuevas.
- La API de `LoginAttemptService` es agnóstica de la clave (`"ip:…"` hoy, potencialmente
  `"user:…"` después) — si la 4.15 termina necesitando la misma lógica, se reusa tal cual o se
  descarta con evidencia real, no con una apuesta de hoy.

### Negativas / a vigilar
- Un reinicio borra los contadores (bypass acotado, aceptado — ver punto 1).
- `forward-headers-strategy: NONE` significa que, **detrás de un proxy real sin este ADR
  actualizado**, el rate limit bloquearía la IP del proxy entera en vez de la del cliente
  real — hay que subirlo a `FRAMEWORK` como parte explícita del checklist del Sprint Despliegue,
  no descubrirlo en producción.
- Sin throttle por cuenta, un atacante distribuido (botnet con IPs distintas) todavía puede
  acumular intentos contra el único admin más rápido que con un límite combinado. Se acepta
  porque el universo de atacantes con esa capacidad contra un petshop de Santana do Livramento
  es chico, y el costo de la alternativa (auto-DoS real, garantizado, contra la única cuenta) es
  peor que el riesgo teórico que evita.
