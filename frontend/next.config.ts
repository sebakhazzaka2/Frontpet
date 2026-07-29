import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Verificado con Lighthouse (2026-07-26): sin esto, next-devtools carga
  // ~218 KB (138 KB sin usar) incluso en `next start` — no es exclusivo de
  // `next dev`. Explicaba buena parte de mainthread-work-breakdown y
  // bootup-time en la auditoría mobile. Deshabilitado para producción.
  devIndicators: false,

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
    ],
  },
};

export default nextConfig;
