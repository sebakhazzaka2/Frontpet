// Productos destacados de la landing. Estáticos en Sprint 2 — la tarea 3.x
// cambia esta fuente por la API real (`GET /api/v1/products`), sin reescribir
// <ProductCard> (ver CLAUDE.md §5, metodología de implementación).
//
// `id`/`slug` son placeholders legibles, no los `public_id` UUID v7 reales del
// backend (ADR 013) — esos solo existen cuando el catálogo tenga seed real.
//
// Sin `avaliacao`/rating a propósito: el DTO real (`ProductSummary`, backend
// Sprint 3) no tiene ese campo — no existe tabla de reviews en MVP1 (ADR
// 013, CLAUDE.md §7). Mostrar un número sin dato real detrás sería mentir
// en la pantalla (decidido 2026-07-27).
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

export interface Product {
  id: string
  slug: string
  nome: string
  /** Sin foto real del producto todavía — `undefined` renderiza un placeholder. */
  imagemUrl?: string
  categoria: ProductCategory
  preco: number
  maisVendido?: boolean
}

export const FEATURED_PRODUCTS: Product[] = [
  {
    id: 'racao-premium-adulto-15kg',
    slug: 'racao-premium-adulto-15kg',
    nome: 'Ração Premium Adulto 15kg',
    categoria: 'Rações',
    preco: 185.0,
    maisVendido: true,
  },
  {
    id: 'mordedor-interativo-resistente',
    slug: 'mordedor-interativo-resistente',
    nome: 'Mordedor Interativo Resistente',
    categoria: 'Brinquedos',
    preco: 42.9,
  },
  {
    id: 'guia-e-coleira-premium-soft',
    slug: 'guia-e-coleira-premium-soft',
    nome: 'Guia e Coleira Premium Soft',
    categoria: 'Acessórios',
    preco: 89.0,
  },
  {
    id: 'cama-nuvem-luxo-ultra-macia',
    slug: 'cama-nuvem-luxo-ultra-macia',
    nome: 'Cama Nuvem Luxo Ultra Macia',
    categoria: 'Conforto',
    preco: 159.9,
  },
]
