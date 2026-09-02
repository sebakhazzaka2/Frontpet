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
ARG NEXT_PUBLIC_META_PIXEL_ID
ARG NEXT_PUBLIC_ANALYTICS_DOMAIN
ARG NEXT_PUBLIC_WHATSAPP_NUMBER
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL \
    NEXT_PUBLIC_META_PIXEL_ID=$NEXT_PUBLIC_META_PIXEL_ID \
    NEXT_PUBLIC_ANALYTICS_DOMAIN=$NEXT_PUBLIC_ANALYTICS_DOMAIN \
    NEXT_PUBLIC_WHATSAPP_NUMBER=$NEXT_PUBLIC_WHATSAPP_NUMBER
RUN pnpm build

FROM base AS runtime
WORKDIR /app
ENV NODE_ENV=production

RUN groupadd --system nextjs && useradd --system --gid nextjs nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nextjs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nextjs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000

# El output standalone genera su propio server.js — no hace falta `next start`.
CMD ["node", "server.js"]
