import type { NextConfig } from "next";
import { withSentryConfig } from "@sentry/nextjs/config";

const nextConfig: NextConfig = {
  // Standalone output (Sprint Despliegue, ADR 016): el Dockerfile de
  // producción copia solo `.next/standalone` + `.next/static` + `public`,
  // sin `node_modules` completo en la imagen final.
  output: "standalone",

  // Verificado con Lighthouse (2026-07-26): sin esto, next-devtools carga
  // ~218 KB (138 KB sin usar) incluso en `next start` — no es exclusivo de
  // `next dev`. Explicaba buena parte de mainthread-work-breakdown y
  // bootup-time en la auditoría mobile. Deshabilitado para producción.
  devIndicators: false,

  // Next 15+ bloquea con 403 los chunks de `next dev` cuando el `Origin` no
  // es localhost (protección CSRF contra sitios externos embebiendo el dev
  // server). Sin esto, probar en celular real por la IP LAN sirve el HTML
  // inicial pero nunca hidrata — confirmado con curl: `_buildManifest.js`
  // devuelve 403 con `Origin: http://192.168.1.2:3001`. Solo afecta `next
  // dev`; no aplica a `next build`/`next start` (producción).
  allowedDevOrigins: ["192.168.1.2"],

  // Headers de seguridad estándar (D.9, checklist pre-lanzamiento): mismo
  // criterio que SecurityConfig del backend — nosniff, sin iframes, HSTS.
  // Next no distingue HTTP/HTTPS acá, pero el sitio es HTTPS-only en prod.
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "X-Frame-Options", value: "DENY" },
          {
            key: "Strict-Transport-Security",
            value: "max-age=31536000; includeSubDomains",
          },
        ],
      },
    ];
  },

  images: {
    // Bucket R2 de fotos de producto (ADR 018) — `images.domains` está
    // deprecado en Next 16, hay que usar remotePatterns. `**.r2.dev` cubre
    // el subdominio público que Cloudflare genera por bucket
    // (pub-<hash>.r2.dev); el hash es distinto en cada cuenta/ambiente.
    remotePatterns: [
      {
        protocol: "https",
        hostname: "**.r2.dev",
      },
      {
        // Fotos de autor de reviews de Google (ADR 025). Wildcard, no host
        // exacto: lh3/lh4/lh5.googleusercontent.com son subdominios válidos
        // de Google y un host fijo rompería la card ante cualquiera de ellos.
        protocol: "https",
        hostname: "**.googleusercontent.com",
      },
    ],
  },
};

// D.8, Sprint Despliegue. org/project/authToken vía env vars (cuenta de
// Sentry todavía no existe — D.4) — sin SENTRY_AUTH_TOKEN, el plugin sube
// el build igual pero salta la subida de source maps con un warning, no
// falla. `silent: true` para no ensuciar el log de build en dev/CI mientras
// no esté configurado.
export default withSentryConfig(nextConfig, {
  org: process.env.SENTRY_ORG,
  project: process.env.SENTRY_PROJECT,
  authToken: process.env.SENTRY_AUTH_TOKEN,
  silent: true,
  widenClientFileUpload: false,
  // disableLogger está deprecado (reemplazo webpack.treeshake.removeDebugLogging
  // no soportado con Turbopack, que es el default de este proyecto — CLAUDE.md §2).
});
