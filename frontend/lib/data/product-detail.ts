import type { Product } from './products'
import type { TaxonRef } from '@/lib/api/client'

export interface ProductVariant {
  id: number
  nomeVariante: string
  price: number
  priceOriginal?: number
  stock: number
}

// Espejo verificado del DTO real ProductDetail (backend/src/main/java/com/frontpet/
// catalog/dto/ProductDetail.java, tarea 3.3 ✅) — confirmado contra la respuesta real
// de GET /api/v1/products/{slug} en 2026-07-27, no solo leyendo el .java. Nombres de
// campo en inglés/portugués mixto porque así los define el DTO real.
export interface ProductDetail {
  publicId: string
  slug: string
  nome: string
  descricao: string
  mainImageUrl?: string
  price?: number
  priceOriginal?: number
  stock: number
  brandNome?: string
  categories: TaxonRef[]
  species: TaxonRef[]
  variants: ProductVariant[]
  onSale: boolean
}

// "Produtos Relacionados" (tarea 3.9, decisión 2026-07-27): no existe endpoint
// de relacionados todavía. Se aproxima con productos reales del mismo seed que
// comparten espécie (cães) — no es la lógica final, pero tampoco es dato
// inventado: son otros productos reales del seed-dev.
export function getRelatedProducts(slug: string): Product[] {
  const related: Record<string, Product[]> = {
    'bifinho-de-frango': [
      {
        slug: 'coleira-antipulgas-ajustavel',
        nome: 'Coleira Antipulgas Ajustável',
        price: 89.9,
      },
      {
        slug: 'mordedor-kong-classico',
        nome: 'Mordedor Kong Clássico',
        price: 74.9,
      },
    ],
  }
  return related[slug] ?? []
}
