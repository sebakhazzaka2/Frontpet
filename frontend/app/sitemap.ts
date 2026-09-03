import type { MetadataRoute } from 'next'
import { listProducts } from '@/lib/api/products'

// Mismo patrón que layout.tsx: `||`, no `??` — un ARG de Docker sin
// --build-arg resuelve a '' (no undefined), y con `??` el fallback nunca
// se dispara.
const BASE_URL = process.env.NEXT_PUBLIC_BASE_URL || 'https://frontpet.com.br'

// Rutas públicas indexables del MVP1 (CLAUDE.md §7). No incluye /carrinho
// (transacional, noindex) ni /agendamento/[publicId] (confirmación privada
// atrás de un UUID, noindex — docs/pending-decisions.md #11) ni nada bajo
// /admin.
//
// force-dynamic: sin esto, Next intenta generar /sitemap.xml en BUILD time
// (es una ruta estática por default) — el fetch a listProducts() pega al
// backend, que no está disponible durante el build de Docker (front y back
// son containers separados). Sin force-dynamic, el build entero fallaba con
// ECONNREFUSED, no solo el sitemap — verificado 2026-09-03 con un build
// real. Con esto, el fetch pasa en runtime real (backend sí disponible en
// producción), mismo criterio que /servicos y /agendamento.
export const dynamic = 'force-dynamic'

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const staticRoutes: MetadataRoute.Sitemap = [
    { url: BASE_URL, priority: 1, changeFrequency: 'weekly' },
    { url: `${BASE_URL}/produtos`, priority: 0.8, changeFrequency: 'daily' },
    { url: `${BASE_URL}/servicos`, priority: 0.8, changeFrequency: 'weekly' },
    { url: `${BASE_URL}/agendamento`, priority: 0.8, changeFrequency: 'weekly' },
  ]

  // size grande: o catálogo de um petshop MVP1 é pequeno o suficiente para
  // caber numa página só — sem paginar o sitemap por enquanto.
  const { items } = await listProducts({ size: 200 })
  const productRoutes: MetadataRoute.Sitemap = items.map((product) => ({
    url: `${BASE_URL}/produtos/${product.slug}`,
    priority: 0.6,
    changeFrequency: 'weekly',
  }))

  return [...staticRoutes, ...productRoutes]
}
