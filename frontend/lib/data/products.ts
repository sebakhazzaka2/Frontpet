// Produtos em destaque da landing. Estáticos em Sprint 2 — a tarefa 3.x troca
// esta fonte pela API real (`GET /api/v1/products`), sem reescrever <ProductCard>
// (ver CLAUDE.md §5, metodologia de implementação).
//
// `id`/`slug` são placeholders legíveis, não os `public_id` UUID v7 reais do
// backend (ADR 013) — esses só existem quando o catálogo tiver seed real.
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
  /** Sin foto real do produto ainda — `undefined` renderiza um placeholder. */
  imagemUrl?: string
  categoria: ProductCategory
  preco: number
  avaliacao: number
  maisVendido?: boolean
}

export const FEATURED_PRODUCTS: Product[] = [
  {
    id: 'racao-premium-adulto-15kg',
    slug: 'racao-premium-adulto-15kg',
    nome: 'Ração Premium Adulto 15kg',
    categoria: 'Rações',
    preco: 185.0,
    avaliacao: 4.9,
    maisVendido: true,
  },
  {
    id: 'mordedor-interativo-resistente',
    slug: 'mordedor-interativo-resistente',
    nome: 'Mordedor Interativo Resistente',
    categoria: 'Brinquedos',
    preco: 42.9,
    avaliacao: 5.0,
  },
  {
    id: 'guia-e-coleira-premium-soft',
    slug: 'guia-e-coleira-premium-soft',
    nome: 'Guia e Coleira Premium Soft',
    categoria: 'Acessórios',
    preco: 89.0,
    avaliacao: 4.8,
  },
  {
    id: 'cama-nuvem-luxo-ultra-macia',
    slug: 'cama-nuvem-luxo-ultra-macia',
    nome: 'Cama Nuvem Luxo Ultra Macia',
    categoria: 'Conforto',
    preco: 159.9,
    avaliacao: 5.0,
  },
]
