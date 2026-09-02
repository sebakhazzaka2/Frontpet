import * as Sentry from '@sentry/nextjs'

// D.8, Sprint Despliegue. Sin DSN (dev/test), el SDK queda inerte solo — no
// hace falta el patrón de placeholder de otros módulos acá.
//
// `|| undefined`: en Docker, un ARG de build sin --build-arg se resuelve a
// '' (no a undefined) — Sentry.init hace `new URL('')` con un DSN vacío y
// tira ERR_INVALID_URL, tumbando el build entero. undefined sí lo maneja
// bien (queda inerte). Verificado 2026-09-02 contra un build real.
//
// Sin performance tracing ni Session Replay: esto es solo captura de
// errores, fuera de presupuesto para MVP1 (mismo criterio del lado backend).
Sentry.init({
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN || undefined,
  environment: process.env.NODE_ENV === 'production' ? 'production' : 'development',
})

export const onRouterTransitionStart = Sentry.captureRouterTransitionStart
