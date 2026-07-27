import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Verificado con Lighthouse (2026-07-26): sin esto, next-devtools carga
  // ~218 KB (138 KB sin usar) incluso en `next start` — no es exclusivo de
  // `next dev`. Explicaba buena parte de mainthread-work-breakdown y
  // bootup-time en la auditoría mobile. Deshabilitado para producción.
  devIndicators: false,
};

export default nextConfig;
