// Espejo del DTO real ProductSummary (backend/src/main/java/com/frontpet/catalog/dto/
// ProductSummary.java, tarea 3.3 ✅) — mismos nombres de campo, para que el swap de
// fuente (estática → API real) no requiera tocar <ProductCard> (CLAUDE.md §5).
//
// Sin `avaliacao`/rating a propósito: ProductSummary no tiene ese campo — no existe
// tabla de reviews en MVP1 (ADR 013, CLAUDE.md §7). Decidido 2026-07-26.
//
// Sin `maisVendido`: decidido 2026-07-27 (issue #16) — no hay pedidos reales todavía
// (orders recién existe con la tarea 4.8), mostrar un "más vendido" inventado sería
// mentir en la pantalla. El "top productos" real queda para el dashboard admin (7.3).
export interface Product {
  slug: string
  nome: string
  mainImageUrl?: string
  price: number
  priceOriginal?: number
  brandNome?: string
  hasVariants?: boolean
}

export const PRODUCT_CATEGORIES = [
  'Rações',
  'Acessórios',
  'Higiene',
  'Petiscos',
  'Conforto',
  'Brinquedos',
  'Outros',
] as const

export type ProductCategory = (typeof PRODUCT_CATEGORIES)[number]

// Productos destacados de la landing. Estáticos en Sprint 2 por diseño (ADR 017,
// la landing no consume backend) — a diferencia de /produtos (tarea 3.6), que sí
// fetchea la API real vía lib/api/products.ts.
export const FEATURED_PRODUCTS: Product[] = [
  {
    slug: 'racao-premium-adulto-15kg',
    nome: 'Ração Premium Adulto 15kg',
    price: 185.0,
  },
  {
    slug: 'mordedor-interativo-resistente',
    nome: 'Mordedor Interativo Resistente',
    price: 42.9,
  },
  {
    slug: 'guia-e-coleira-premium-soft',
    nome: 'Guia e Coleira Premium Soft',
    price: 89.0,
  },
  {
    slug: 'cama-nuvem-luxo-ultra-macia',
    nome: 'Cama Nuvem Luxo Ultra Macia',
    price: 159.9,
  },
]
