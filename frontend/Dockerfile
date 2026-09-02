# Sprint Despliegue (ADR 016) — build multi-stage con output standalone de
# Next 16 (ver next.config.ts): la imagen final no lleva node_modules
# completo, solo lo que Next empaqueta para correr el server standalone.
# Node 20.9+ requerido (CLAUDE.md §2).

FROM node:20-slim AS base
# corepack sin fijar versión resuelve el pnpm más nuevo del índice (11.x a
# la fecha), que usa `node:sqlite` — requiere Node 22.5+ y rompe en node:20.
# Fijamos 10.18.0 a mano, la misma mayor que usa el equipo en WSL (ADR 015).
RUN corepack enable && corepack prepare pnpm@10.18.0 --activate

FROM base AS deps
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# NEXT_PUBLIC_* se hornean en el bundle en build time — Coolify tiene que
# pasarlas como build args, no solo como env vars de runtime, o el build
# queda con los valores default/vacíos horneados adentro.
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_BASE_URL
ARG NEXT_PUBLIC_META_PIXEL_ID
ARG NEXT_PUBLIC_ANALYTICS_DOMAIN
ARG NEXT_PUBLIC_WHATSAPP_NUMBER
ARG NEXT_PUBLIC_SENTRY_DSN
ARG NEXT_PUBLIC_GOOGLE_MAPS_EMBED_KEY
ARG NEXT_PUBLIC_GOOGLE_PLACE_ID
# Sin prefijo NEXT_PUBLIC_ a propósito: solo las lee el plugin de webpack de
# Sentry durante `pnpm build` (subida de source maps), nunca deben terminar
# en el bundle del cliente. Sin SENTRY_AUTH_TOKEN, withSentryConfig salta la
# subida con un warning — no rompe el build.
#
# ⚠️ Docker avisa (SecretsUsedInArgOrEnv) que SENTRY_AUTH_TOKEN queda en el
# historial de capas de la imagen. Lo ideal sería `--mount=type=secret` de
# BuildKit, pero Coolify solo expone "Build Variables" simples en su UI —
# no confirmado que soporte secrets de build. Riesgo aceptado: es un token
# de Sentry con permiso único de subir source maps (no una credencial de DB
# ni de admin), y la imagen nunca se publica en un registro público, solo
# la construye Coolify localmente en el VPS.
ARG SENTRY_ORG
ARG SENTRY_PROJECT
ARG SENTRY_AUTH_TOKEN
# GOOGLE_PLACES_API_KEY es server-only (sin prefijo NEXT_PUBLIC_), pero la
# home usa <Reviews>/<TrustBar>/<Hero> con un fetch que trae `next.revalidate`
# (lib/google-places.ts) — eso hace que Next trate la página como ISR: se
# pre-renderiza en ESTE build y recién se regenera sola a las 24hs. Sin la
# key acá, el build la ejecuta con `process.env.GOOGLE_PLACES_API_KEY`
# undefined y hornea la sección vacía (EMPTY) hasta la próxima regeneración.
# Mismo riesgo aceptado que SENTRY_AUTH_TOKEN arriba (queda en el historial
# de capas, pero la imagen nunca sale del VPS) — verificado 2026-09-02 contra
# un deploy real donde "Histórias de Tutores" quedó vacía por este motivo.
ARG GOOGLE_PLACES_API_KEY
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL \
    NEXT_PUBLIC_BASE_URL=$NEXT_PUBLIC_BASE_URL \
    NEXT_PUBLIC_META_PIXEL_ID=$NEXT_PUBLIC_META_PIXEL_ID \
    NEXT_PUBLIC_ANALYTICS_DOMAIN=$NEXT_PUBLIC_ANALYTICS_DOMAIN \
    NEXT_PUBLIC_WHATSAPP_NUMBER=$NEXT_PUBLIC_WHATSAPP_NUMBER \
    NEXT_PUBLIC_SENTRY_DSN=$NEXT_PUBLIC_SENTRY_DSN \
    NEXT_PUBLIC_GOOGLE_MAPS_EMBED_KEY=$NEXT_PUBLIC_GOOGLE_MAPS_EMBED_KEY \
    NEXT_PUBLIC_GOOGLE_PLACE_ID=$NEXT_PUBLIC_GOOGLE_PLACE_ID \
    GOOGLE_PLACES_API_KEY=$GOOGLE_PLACES_API_KEY \
    SENTRY_ORG=$SENTRY_ORG \
    SENTRY_PROJECT=$SENTRY_PROJECT \
    SENTRY_AUTH_TOKEN=$SENTRY_AUTH_TOKEN
RUN pnpm build

FROM base AS runtime
WORKDIR /app
ENV NODE_ENV=production
# Docker inyecta HOSTNAME=<container_id> solo, y server.js usa
# process.env.HOSTNAME || '0.0.0.0' — sin este override, el server queda
# bindeado al hostname del container (no accesible desde otros containers
# de la red, ej. Traefik) en vez de a todas las interfaces. Gotcha conocido
# de Next standalone + Docker, verificado 2026-09-02 contra un deploy real
# (Traefik devolvía 502/connection refused pese al log diciendo "Ready").
ENV HOSTNAME="0.0.0.0"

RUN groupadd --system nextjs && useradd --system --gid nextjs nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nextjs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nextjs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000

# El output standalone genera su propio server.js — no hace falta `next start`.
CMD ["node", "server.js"]
