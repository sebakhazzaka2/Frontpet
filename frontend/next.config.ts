import type { NextConfig } from "next";

const nextConfig: NextConfig = {
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

export default nextConfig;
