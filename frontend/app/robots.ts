import type { MetadataRoute } from 'next'

// Mismo patrón que layout.tsx/sitemap.ts: `||`, no `??`.
const BASE_URL = process.env.NEXT_PUBLIC_BASE_URL || 'https://frontpet.com.br'

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: '/',
      disallow: [
        '/admin',
        '/carrinho',
        // Bare /agendamento (o wizard) queda permitido — solo se bloquean
        // las confirmações /agendamento/{publicId}, que además ya llevan
        // robots: noindex propio (docs/pending-decisions.md #11).
        '/agendamento/*',
      ],
    },
    sitemap: `${BASE_URL}/sitemap.xml`,
  }
}
