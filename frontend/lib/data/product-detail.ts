import type { Product } from './products'

export interface TaxonRef {
  nome: string
  slug: string
}

// Espelho do DTO real ProductDetail (backend/src/main/java/com/frontpet/catalog/dto/
// ProductDetail.java, já implementado no Sprint 3.3) — nomes de campo em inglês/
// português misto porque assim os define o DTO real, não por escolha estética. O
// swap para GET /api/v1/products/{slug} (tarefa 3.9/3.11) troca getProductBySlug(),
// não <ProductDetailView>.
export interface ProductDetail {
  slug: string
  nome: string
  descricao: string
  mainImageUrl?: string
  price: number
  priceOriginal?: number
  stock: number
  brandNome?: string
  categories: TaxonRef[]
}

export function isOnSale(product: ProductDetail) {
  return product.priceOriginal != null
}

// Dado real do seed de desenvolvimento (backend/.../db/seed-dev/products.sql,
// tarefa 3.12) — não inventado, copiado do único produto em promoção do seed
// para não divergir do que o backend vai servir de verdade quando a tarefa
// 3.9/3.11 trocar isto por um fetch real.
const PRODUCT_DETAILS: Record<string, ProductDetail> = {
  'bifinho-de-frango': {
    slug: 'bifinho-de-frango',
    nome: 'Bifinho de Frango',
    descricao:
      'Petisco macio de frango para cães de todos os portes. Ideal para treino e recompensa.',
    price: 18.5,
    priceOriginal: 24.9,
    stock: 60,
    brandNome: 'Purina',
    categories: [{ nome: 'Petiscos', slug: 'petiscos' }],
  },
}

export function getProductBySlug(slug: string): ProductDetail | undefined {
  return PRODUCT_DETAILS[slug]
}

// "Produtos Relacionados" (tarefa 3.9, decisão 2026-07-27): não existe endpoint
// de relacionados ainda. Aproxima com produtos reais do mesmo seed que
// compartilham espécie (cães) — não é a lógica final, mas também não é dado
// inventado: são outros produtos reais do seed-dev.
export function getRelatedProducts(slug: string): Product[] {
  const related: Record<string, Product[]> = {
    'bifinho-de-frango': [
      {
        id: 'coleira-antipulgas-ajustavel',
        slug: 'coleira-antipulgas-ajustavel',
        nome: 'Coleira Antipulgas Ajustável',
        categoria: 'Acessórios',
        preco: 89.9,
      },
      {
        id: 'mordedor-kong-classico',
        slug: 'mordedor-kong-classico',
        nome: 'Mordedor Kong Clássico',
        categoria: 'Brinquedos',
        preco: 74.9,
      },
    ],
  }
  return related[slug] ?? []
}
