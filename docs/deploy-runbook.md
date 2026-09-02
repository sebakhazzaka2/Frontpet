# Runbook de despliegue — Sprint Despliegue

> Paso a paso ejecutable para levantar `frontpet.com.br` + `api.frontpet.com.br` en el VPS.
> Arquitectura y porqués: **ADR 016**. Tareas del ROADMAP que este runbook cubre: **D.1-D.10**.
> Actualizar este archivo si algún paso cambia al ejecutarlo — es el runbook real, no un
> plan aspiracional.

**Antes de arrancar**: confirmar con el cliente el gasto (VPS ~€7.50/mes + dominio
~USD 10-15/año) y quién paga con qué tarjeta/cuenta. Sin esto no se puede ni empezar D.1.

### ⚠️ Prerequisitos de código — verificado 2026-08-24, faltan hoy

Esto **no está en el repo todavía** y D.6/D.8 lo necesitan. Hacerlo vos mismo (no es tarea
del cliente) antes o durante D.5-D.6, con tiempo de sobra:

- **No existe `backend/Dockerfile` ni `frontend/Dockerfile`.** Nixpacks de Coolify puede
  detectar Maven/pnpm solo sin Dockerfile, así que D.6 puede arrancar igual — pero un
  Dockerfile propio da control real sobre la imagen (JRE headless liviana para el backend,
  build standalone de Next). Si Nixpacks falla o el build queda pesado, este es el primer
  lugar a mirar.
- **Sentry SDK no está en `pom.xml`** (`sentry-spring-boot-starter` ausente) ni confirmado
  en el frontend. D.8 asume que ya está integrado — si no lo está, sumar ~1-1.5 hs de spike
  de instalación antes de ese paso, no en el medio.

---

## Orden de ejecución

```
D.1 VPS  ────────────────┐
D.2 Dominio ──────────────┼──► D.5 Coolify ──► D.6 Deploy apps ──► D.7 Backups ──► D.8 Sentry ──► D.9 Security review ──► D.10 Contenido ──► 🎯 URL pública
D.3 Resend (depende de D.2)┤
D.4 Sentry/Plausible (cuentas) ┘
```

D.1/D.2/D.4 se pueden hacer en paralelo (son compras/cuentas independientes); D.3 depende de que
D.2 (DNS en Cloudflare) ya exista. D.5 en adelante es secuencial: cada paso depende del anterior.

---

## D.1 — VPS Hetzner CX33

1. Crear cuenta en [console.hetzner.cloud](https://console.hetzner.cloud) si no existe.
2. **New Project** → nombre `frontpet-prod`.
3. **Add Server**:
   - **Location**: `Falkenstein` o `Nuremberg` (Alemania) — ⚠️ **cambio de plan 2026-09-01**:
     ADR 016 quería Ashburn (~130ms a RS, Brasil) pero el tier CX de 4 vCPU/8 GB **no existe
     en Ashburn/Hillsboro**; la equivalente ahí (CPX32) sale ~USD 42/mes, fuera de
     presupuesto. Se acepta Alemania (~200ms) — ver la nota de ADR 016 §3 y el paso de
     caching agregado en D.6.3 para mitigar el impacto en las páginas públicas.
   - **Image**: **Ubuntu 24.04 LTS** — sí es LTS aunque Hetzner no lo etiquete en la lista
     ("Noble Numbat", soporte hasta 2029). No usar 26.04 aunque sea la LTS más nueva: el
     instalador automático de Coolify (D.5) solo soporta oficialmente 20.04/22.04/24.04 —
     con 26.04 el script falla y hay que instalar todo a mano.
   - **Type**: **CX33** (4 vCPU / 8 GB RAM / 80 GB disco, x86 Intel/AMD — Hetzner renombró
     el tier ex-CX32 a CX33, mismos specs). No bajar a CX23 (2 vCPU / 4 GB, ex-CX22) —
     Coolify + Spring Boot + Postgres + Next no entran cómodos en 4 GB (detalle en ADR 016).
   - **SSH Key**: generar una nueva si no tenés una para este proyecto:
     ```bash
     ssh-keygen -t ed25519 -C "frontpet-deploy" -f ~/.ssh/frontpet_deploy
     ```
     Pegar el contenido de `~/.ssh/frontpet_deploy.pub` en el campo de Hetzner.
   - **Networking**: dejar defaults (IPv4 + IPv6 públicas).
   - **Firewall**: crear uno nuevo, `frontpet-fw`, con reglas de entrada:
     - `22/tcp` (SSH) — idealmente restringido a tu IP, no `0.0.0.0/0`
     - `80/tcp`, `443/tcp` (HTTP/HTTPS)
     - `8000/tcp` (dashboard de Coolify — restringir a tu IP también, no dejarlo abierto al mundo)
4. Crear el servidor. Anotar la **IP pública** (v4) — la necesitás para D.2.
5. Verificar acceso:
   ```bash
   ssh -i ~/.ssh/frontpet_deploy root@<IP_DEL_VPS>
   ```

**Costo**: ~€7.50/mes. **Tiempo**: ~15 min.

---

## D.2 — Dominio + DNS (Cloudflare)

> ✅ **Ya resuelto (2026-08-24)**: dominio comprado en `registro.br`, a nombre del cliente —
> sin relación con Cloudflare, sin riesgo de facturación cruzada.
>
> ⚠️ **Decisión temporal (ver `pending-decisions.md` §18)**: el DNS y el bucket R2 de
> producción corren, por ahora, en la **cuenta personal de Cloudflare de Sebastián**
> (compartida con otro cliente) — no en una cuenta exclusiva del cliente de FrontPet. Se migra
> en la reunión de entrega, cuando se pueda usar su tarjeta. Hasta entonces, la tarjeta de
> Sebastián es la que paga cualquier excedente de R2 sobre el free tier.

1. Agregar el sitio en Cloudflare (**Add a Site**, en la cuenta personal, ver nota arriba) y
   copiar los nameservers que asigna.
2. Ir al panel de `registro.br` y cambiar los nameservers del dominio a los que dio Cloudflare.
   Puede tardar hasta 24-48 hs en propagar — **hacer esto lo antes posible en el día**, no al
   final.
3. En **DNS → Records** del dominio en Cloudflare, crear:

   | Tipo | Nombre | Contenido | Proxy status |
   |---|---|---|---|
   | A | `@` (frontpet.com.br) | `<IP_DEL_VPS>` | 🟠 Proxied |
   | A | `api` (api.frontpet.com.br) | `<IP_DEL_VPS>` | 🟠 Proxied |
   | A | `coolify` (opcional, dashboard) | `<IP_DEL_VPS>` | ⚪ DNS only |

   Dejar `coolify.frontpet.com.br` **sin proxy** (DNS only) si vas a acceder al dashboard de
   Coolify por subdominio — el proxy de Cloudflare puede interferir con el puerto 8000. Si
   accedés al dashboard por IP directa, ni hace falta este registro.

4. **SSL/TLS → Overview**: modo **Full (strict)** — Coolify/Caddy van a servir HTTPS con
   Let's Encrypt en el origin, y Cloudflare valida contra ese certificado real (no "Flexible",
   que rompe redirects y es inseguro origin→edge).

**Costo**: ~USD 10-15/año. **Tiempo**: ~20 min + espera de propagación.

---

## D.3 — Verificación del dominio de e-mail en Resend

Necesario para que `POST /forgot-password` (tarea 7.12) mande e-mails reales — sin esto, el
backend cae a `LoggingEmailSender` y ningún admin recibe el link de reset. Depende de propagación
DNS (minutos a horas): arrancarlo temprano en el día, en paralelo con otros pasos.

> ⚠️ Misma nota que D.2: el dominio de Resend usado hoy corre en la **cuenta personal de
> Cloudflare/Resend de Sebastián** (ver `pending-decisions.md` §18) — se migra en la reunión de
> entrega, junto con el resto.

1. Resend → **Domains → Add Domain** → `frontpet.com.br`.
2. Cargar los 3 registros en Cloudflare, zona `frontpet.com.br`, todos **DNS only** (nube gris —
   nunca proxied, Cloudflare no debe intermediar tráfico de verificación de email):
   - **DKIM** — `TXT` en `resend._domainkey`, valor `p=MIGfMA0GCSq...` (lo da Resend).
   - **SPF** — `TXT` en `send`, `v=spf1 include:amazonses.com ~all`.
     ⚠️ Si ya existiera un `TXT` SPF en ese nombre, **combinar los `include:` en un solo
     registro** — dos registros SPF en el mismo nombre invalidan los dos.
   - **MX** — `send.frontpet.com.br` → `feedback-smtp.<region>.amazonses.com`, prioridad 10.
     ⚠️ **Va en el subdominio `send`, NUNCA en la raíz.** Pisar los MX de la raíz deja al
     cliente sin recibir correo en `@frontpet.com.br` — es el paso más peligroso de todos.
   - Recomendado: **DMARC** — `TXT` en `_dmarc`, `v=DMARC1; p=none; rua=mailto:...` (modo
     observación, no rechaza nada).
3. **Verify** en Resend, esperar estado *Verified*.
4. Crear un API key con permiso **solo de envío** (Sending access, no Full access) →
   `RESEND_API_KEY` (🔒, ver tabla de D.6.2).
5. Probar con `curl` contra `https://api.resend.com/emails` antes de dar el paso por cerrado —
   no depender solo del check verde de Resend.

**Costo**: free tier de Resend alcanza para el volumen de MVP1 (un solo admin, resets
esporádicos). **Tiempo**: ~45 min + espera de propagación DNS.

---

## D.4 — Cuentas de observabilidad

1. **Sentry**: [sentry.io](https://sentry.io) → cuenta free tier → crear 2 proyectos:
   `frontpet-backend` (Java/Spring Boot) y `frontpet-frontend` (Next.js). Anotar los 2 DSN.
2. **Plausible** (o Umami self-hosted si preferís no pagar): [plausible.io](https://plausible.io)
   → agregar sitio `frontpet.com.br`. Anotar el script tag.

**Tiempo**: ~10 min.

---

## D.5 — Instalar Coolify

```bash
ssh -i ~/.ssh/frontpet_deploy root@<IP_DEL_VPS>
curl -fsSL https://cdn.coollabs.io/coolify/install.sh | bash
```

- Instala Docker + Coolify. Tarda unos minutos.
- Al terminar, accedé a `http://<IP_DEL_VPS>:8000` (o `https://coolify.frontpet.com.br:8000` si
  configuraste el subdominio) y creá el usuario admin de Coolify.
- Coolify trae su propio proxy (Traefik) que maneja HTTPS automático vía Let's Encrypt para
  cada app que despliegues — no hace falta instalar Caddy a mano aparte.
- En **Settings → Servers**, verificar que el servidor local esté "Reachable" y "Healthy".

**Riesgo conocido** (ya anotado en el ROADMAP): Coolify es nuevo — si algo no cierra, su doc
está en [coolify.io/docs](https://coolify.io/docs). No perder más de 1h trabada en un paso de
esta sección sin consultar la doc primero.

**Tiempo**: ~30-45 min.

---

## D.6 — Deploy de las 3 piezas

### 6.1 — Postgres (managed service de Coolify)

1. **New Resource → Database → PostgreSQL 16**.
2. Nombre: `frontpet-db`. Generar usuario/password (Coolify lo hace solo, guardalos).
3. **No** exponer el puerto de Postgres a internet — solo red interna de Coolify.
4. Deploy. Anotar el **connection string interno** (Coolify te lo muestra, algo como
   `postgres-frontpet-db:5432`).

### 6.2 — Backend (Spring Boot)

1. **New Resource → Application → Public Repository** (o conectar el GitHub del proyecto
   con la GitHub App de Coolify para auto-deploy en push).
2. Repo: `sebakhazzaka2/Frontpet`, branch `main`, **Base directory**: `backend`.
3. **Build pack**: Dockerfile si existe uno en `backend/`, si no, Nixpacks (Coolify detecta
   Java/Maven solo). Verificar que exista `backend/Dockerfile`; si no existe, crear uno
   simple multi-stage antes de este paso (avisar si hace falta, no está en el repo hoy).
4. **Domain**: `api.frontpet.com.br`.
5. **Environment variables** (Production, marcar como *secret* las sensibles):

   | Variable | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | `jdbc:postgresql://<host-interno-postgres>:5432/frontpet` |
   | `DB_USERNAME` | el que generó Coolify |
   | `DB_PASSWORD` | 🔒 el que generó Coolify |
   | `CORS_ALLOWED_ORIGINS` | `https://frontpet.com.br` |
   | `TENANT_ID` | `01924ccf-0000-7000-8000-000000000001` (o el real si se regeneró) |
   | `ADMIN_EMAIL` | email real del admin |
   | `ADMIN_PASSWORD` | 🔒 **generar uno nuevo fuerte — nunca `admin123`** (el `StartupEnvValidator` corta el arranque si lo detecta) |
   | `R2_ACCOUNT_ID` | de la cuenta Cloudflare ya creada (tarea 3.5) |
   | `R2_ACCESS_KEY_ID` | 🔒 ídem |
   | `R2_SECRET_ACCESS_KEY` | 🔒 ídem |
   | `R2_BUCKET_NAME` | ⚠️ el bucket real hoy es **`frontpet-dev`** (único que existe, cuenta personal de Sebastián — ver D.2 y `pending-decisions.md` §18), no `frontpet-products` como decía el default viejo del `application.yml`. Usar ese nombre hasta que se migre a la cuenta del cliente |
   | `R2_ENDPOINT` | endpoint de la cuenta R2 |
   | `R2_PUBLIC_URL` | dominio público del bucket (r2.dev o custom) |
   | `JWT_SECRET` | 🔒 generar con `openssl rand -base64 48`, mín. 32 caracteres |
   | `JWT_EXPIRATION` | `3600000` (1h, o ajustar) |
   | `LOGIN_RATE_LIMIT_ENABLED` | `true` |
   | `ORDER_RATE_LIMIT_ENABLED` | `true` |
   | `APPOINTMENT_RATE_LIMIT_ENABLED` | `true` |
   | `PASSWORD_RESET_RATE_LIMIT_ENABLED` | `true` (tarea 7.12) |
   | `RESEND_API_KEY` | 🔒 el API key "solo envío" creado en D.3 |
   | `RESEND_FROM` | `FrontPet <nao-responda@frontpet.com.br>` (o el remitente real que se confirme) |
   | `APP_BASE_URL` | `https://frontpet.com.br` — ⚠️ debe empezar con `https://`: `StartupEnvValidator` corta el arranque si no, porque un link de reset por http manda el token en claro por la red |
   | `FORWARD_HEADERS_STRATEGY` | `FRAMEWORK` — ⚠️ **solo** si Coolify/Traefik es el único camino de entrada (lo es, en este setup). Ver el comentario de `application.yml` y ADR 019 antes de tocar esto |

   Todas las que faltan tiran el arranque (ver `StartupEnvValidator.java`) — mejor: si falta
   una, el contenedor no levanta y Coolify te lo muestra en los logs del deploy.

6. Deploy. Verificar `https://api.frontpet.com.br/actuator/health` → `{"status":"UP"}`.
7. Confirmar que **Flyway corrió las migraciones** (mirar logs del contenedor) — incluye
   V5/V10, que son seeds de dev marcados "não executar em produção" (ver pendiente #4 de
   `docs/pending-decisions.md`, todavía sin resolver). **Decisión a tomar antes de este
   paso**: ¿se acepta que V5/V10 corran en prod igual (datos reales, solo el WhatsApp es
   placeholder — ver ese pendiente), o se separan las migraciones por perfil antes de
   deployar? Si no se resolvió, correrán igual — Flyway no filtra por ambiente hoy.

### 6.3 — Frontend (Next 16)

1. **New Resource → Application → Public Repository**, mismo repo, **Base directory**: `frontend`.
2. **Build pack**: Nixpacks (detecta Node/pnpm) o Dockerfile si se crea uno.
3. **Domain**: `frontpet.com.br` (+ `www.frontpet.com.br` con redirect, opcional).
4. **Environment variables**:

   ⚠️ **Todas las `NEXT_PUBLIC_*` (y `SENTRY_ORG`/`SENTRY_PROJECT`/`SENTRY_AUTH_TOKEN`) se
   hornean en el bundle durante `pnpm build`** — con el `Dockerfile` (`frontend/Dockerfile`),
   Coolify tiene que pasarlas como **Build Variables**, no solo como env vars de runtime, o
   quedan vacías en el JS servido sin ningún error visible en logs. Si el build pack termina
   siendo Nixpacks en vez de Dockerfile, confirmar que Coolify las exponga igual en build time.

   | Variable | Valor |
   |---|---|
   | `NEXT_PUBLIC_API_URL` | `https://api.frontpet.com.br` |
   | `NEXT_PUBLIC_BASE_URL` | `https://frontpet.com.br` (metadataBase, OG, canonical — `app/layout.tsx`) |
   | `NEXT_PUBLIC_META_PIXEL_ID` | Pixel ID real (si ya lo dio el cliente — si no, dejar vacío, se agrega en Sprint 7) |
   | `NEXT_PUBLIC_ANALYTICS_DOMAIN` | `frontpet.com.br` (Plausible) |
   | `NEXT_PUBLIC_WHATSAPP_NUMBER` | `555596724124` (sin `+`, confirmar si cambia — pendiente 7.3) |
   | `NEXT_PUBLIC_SENTRY_DSN` | DSN del proyecto `frontpet-frontend` en Sentry (D.4/D.8) — sin esto, Sentry del lado cliente no se inicializa, sin ningún error visible |
   | `NEXT_PUBLIC_GOOGLE_MAPS_EMBED_KEY` | 🔒 API key de Maps Embed API (ADR 025) — es pública igual (sale en el `src` del iframe), pero restringida por HTTP referrer en Google Cloud Console |
   | `NEXT_PUBLIC_GOOGLE_PLACE_ID` | Place ID de FrontPet en Google Business (ADR 025) — sin esto, `<Reviews>`/`<TrustBar>` quedan sin cards, indistinguible de "el negocio no tiene reviews" |
   | `SENTRY_ORG` / `SENTRY_PROJECT` / `SENTRY_AUTH_TOKEN` | 🔒 (el token) — solo para subir source maps en build time; sin ellos el build sigue funcionando, solo salta ese paso con un warning |

5. Deploy. Verificar `https://frontpet.com.br` carga la landing y `https://frontpet.com.br/produtos`
   trae productos reales del backend.
6. **Cache de las rutas públicas GET** (nuevo, 2026-09-01 — mitigación del VPS en Alemania,
   ver ADR 016 §3): `/produtos`, `/produtos/[slug]` y `/servicos` hoy renderizan dinámico en
   cada request (`ƒ Dynamic` en el build). Sin cachear, cada pageview paga el viaje completo
   Cloudflare↔Falkenstein (~200ms). Configurar **Cloudflare Cache Rules** (Rules → Cache
   Rules) para cachear esas rutas por unos minutos, o agregar `revalidate` (ISR) en esos
   `page.tsx` si el dato no necesita estar al segundo. **No cachear** `/carrinho`,
   `/admin/**` ni ningún POST (`/agendamento`, checkout) — ahí el dato tiene que ser real.

### 6.4 — Auto-deploy

Si conectaste la GitHub App de Coolify (recomendado sobre "public repository" a secas):
cada push a `main` dispara build+deploy automático de ambas apps. Confirmarlo con un commit
de prueba trivial después de que todo esté verde.

**Tiempo**: ~2-3 hs (la mayor parte es debugging de env vars y primer build).

---

## D.7 — Backups

1. En el VPS, crear script de backup:
   ```bash
   mkdir -p /root/scripts
   cat > /root/scripts/backup-db.sh <<'EOF'
   #!/bin/bash
   set -euo pipefail
   TIMESTAMP=$(date +%Y%m%d_%H%M%S)
   BACKUP_FILE="/tmp/frontpet_${TIMESTAMP}.sql.gz"
   docker exec <container-postgres> pg_dump -U frontpet frontpet | gzip > "$BACKUP_FILE"
   # Requiere aws-cli configurado con las credenciales R2 (S3-compatible)
   aws s3 cp "$BACKUP_FILE" s3://frontpet-backups/ --endpoint-url "$R2_ENDPOINT"
   rm "$BACKUP_FILE"
   EOF
   chmod +x /root/scripts/backup-db.sh
   ```
   Reemplazar `<container-postgres>` por el nombre real del contenedor (`docker ps` para verlo).
   Crear el bucket `frontpet-backups` en R2 (separado de `frontpet-products`) antes de esto.
2. Instalar `aws-cli` en el VPS (`apt install awscli` o `pip install awscli`) y configurar
   credenciales R2 (mismas del backend, o un token separado solo de escritura — mejor).
3. Cron diario:
   ```bash
   crontab -e
   # agregar:
   0 3 * * * /root/scripts/backup-db.sh >> /var/log/frontpet-backup.log 2>&1
   ```
4. Verificar manualmente corriendo el script una vez y confirmando que el archivo aparece en R2.

**Tiempo**: ~1-1.5 hs.

---

## D.8 — Activar Sentry

1. Backend: agregar el DSN de `frontpet-backend` como env var (`SENTRY_DSN`) y la dependencia
   `sentry-spring-boot-starter` si todavía no está en `pom.xml` — confirmar antes de este paso.
2. Frontend: `NEXT_PUBLIC_SENTRY_DSN` + wizard de `@sentry/nextjs` si no está instalado.
3. Provocar un error intencional en cada uno (ej. un endpoint temporal que tira excepción) y
   confirmar que aparece en el dashboard de Sentry. Borrar el endpoint de prueba después.

**Tiempo**: ~30 min (si el SDK ya estaba integrado en el código; si no, sumar el spike de
instalarlo — no estaba confirmado como hecho en el ROADMAP).

---

## D.9 — Security review pre-exposición

Antes de anunciar la URL al cliente o a nadie:

- [ ] Correr `/security-review` sobre `main` (skill del proyecto).
- [ ] Rate limiting activo en login/orders/appointments/password-reset (env vars de D.6, ya en `true`).
- [ ] Dominio de e-mail *Verified* en Resend (D.3), con una prueba real de que el link de reset
      **no cae en spam** (probar Gmail y Outlook) y apunta a `https://frontpet.com.br`, no a
      `localhost`.
- [ ] MX de la raíz de `frontpet.com.br` intactos (el registro de Resend va en `send`, ver D.3) —
      confirmar que el cliente sigue recibiendo correo normal en `@frontpet.com.br`.
- [ ] Un reset de prueba de punta a punta: pedir el link, usarlo, y confirmar que una sesión
      abierta en **otro navegador** queda deslogueada (invalidación vía `password_changed_at`,
      ADR 022) — no alcanza con probar que el login nuevo funciona.
- [ ] Cookies con `Secure` + `HttpOnly` + `SameSite` correctos en prod (JWT en cookie).
- [ ] CORS restringido a `https://frontpet.com.br` (no `*`, no localhost en prod).
- [ ] Headers de seguridad (Coolify/Traefik o agregar vía `next.config` / Spring Security
      headers) — al menos `X-Content-Type-Options`, `X-Frame-Options`, HSTS.
- [ ] `pnpm audit` en frontend y `mvn dependency-check` (o equivalente) en backend sin
      vulnerabilidades críticas sin resolver.
- [ ] Puerto 8000 de Coolify **no accesible públicamente** sin restricción de IP (firewall D.1).
- [ ] `ADMIN_PASSWORD` no es el default de dev (`StartupEnvValidator` ya lo bloquea, pero
      confirmar que arrancó).
- [ ] Dashboard de Coolify con contraseña fuerte, no la que quedó del setup inicial.

**Tiempo**: ~30-45 min si no aparece nada nuevo; más si el `/security-review` encuentra algo.

---

## D.10 — Checklist de contenido pre-público

(copiado del ROADMAP, tarea D.10 — completar antes de compartir el link con nadie)

- [ ] Depoimentos y métricas reales en `<TrustBar>`/`<Reviews>`, o sacarlas si no llegaron
      (`docs/pending-decisions.md` §15) — **decisión legal, no estética**.
- [ ] `metadataBase` con el dominio real en `app/layout.tsx:93`.
- [ ] `/public/og-image.jpg` real (1200x630).
- [ ] Código de verificación de Search Console en `app/layout.tsx:99`.
- [ ] `themeColor` de `app/layout.tsx:117` sincronizado con la paleta confirmada (si el
      cliente ya confirmó colores — sigue pendiente §6.1 de `preguntas-cliente.md`).
- [ ] Número de WhatsApp definitivo en seed y `lib/data/site.ts`.
- [ ] Fotos reales de productos/serviços/hero, o asumir explícitamente que se sale con los
      gradientes placeholder.

---

## Verificación final end-to-end

Una vez todo desplegado:

1. `https://frontpet.com.br` — landing carga, imágenes se ven, Lighthouse > 90 en producción real
   (puede variar contra lo medido en local).
2. `https://frontpet.com.br/produtos` — catálogo real, filtros y búsqueda funcionan.
3. Agregar un producto al carrito → `/carrinho` → completar form → confirmar redirect a
   WhatsApp con el mensaje armado correctamente (número real).
4. `https://frontpet.com.br/agendamento` — wizard completo, reservar un turno de prueba, borrar
   después desde el admin.
5. `https://frontpet.com.br/admin/login` — login con el `ADMIN_PASSWORD` real, no el de dev.
6. Admin: crear/editar un producto de prueba con imagen (confirma que R2 + presign funcionan
   en prod, con el dominio real en las policies de CORS del bucket si aplica).
7. Provocar el error de prueba de Sentry (D.8) una vez más ya en el flujo real, confirmarlo,
   y esta vez sí borrar el endpoint/código de prueba.
8. Confirmar `git push` a `main` dispara redeploy automático (D.6.4) con un cambio trivial.

**Hito** 🎯: si los 8 puntos pasan, es la primera URL pública en vivo. Recién ahí se manda el
Loom al cliente con el link.

---

## Qué NO hacer en este sprint

- No tocar `FORWARD_HEADERS_STRATEGY` a `FRAMEWORK` si hay algún camino de acceso al backend
  que no pase por el proxy de Coolify (rompe el rate limiting real, ver ADR 019).
- No dejar el dashboard de Coolify (puerto 8000) abierto a `0.0.0.0/0` en el firewall.
- No saltear D.10 (contenido) por apuro — testimonios falsos en producción es el único punto
  con riesgo legal real de todo este runbook.
- No comprar São Paulo/AWS "para después" — la decisión de VPS Hetzner (hoy Alemania,
  Falkenstein/Nuremberg — ver ADR 016 §3, cambio 2026-09-01) + Cloudflare ya está tomada
  y es reversible si hace falta más adelante.

---

**Última actualización**: 2026-08-29 (agregado D.3 — Resend, tarea 7.12). Escribir en este archivo cualquier ajuste real durante
la ejecución — es el runbook operativo, no debe quedar desactualizado como una foto del plan.
