import * as Sentry from '@sentry/nextjs'

// D.8, Sprint Despliegue. Ver instrumentation-client.ts para el criterio
// (sin DSN queda inerte, sin performance tracing, y por qué `|| undefined`).
Sentry.init({
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN || undefined,
  environment: process.env.NODE_ENV === 'production' ? 'production' : 'development',
})
