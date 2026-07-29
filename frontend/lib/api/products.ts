import { apiFetch, type PageResponse, type TaxonRef } from './client'
import type { ProductDetail } from '@/lib/data/product-detail'
import type { Product } from '@/lib/data/products'

export function getProductBySlug(slug: string) {
  return apiFetch<ProductDetail>(`/products/${slug}`)
}

interface ListProductsParams {
  page?: number
  size?: number
  categoria?: string
  busca?: string
}

export async function listProducts({
  page = 0,
  size = 24,
  categoria,
  busca,
}: ListProductsParams = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (categoria) {
    params.set('categoria', categoria)
  }
  if (busca) {
    params.set('busca', busca)
  }

  // revalidate: 60 (tarea 3.11) — ISR simple. Peor caso, un producto editado
  // por el admin tarda hasta 60s en reflejarse, no hasta el próximo deploy.
  const response = await apiFetch<PageResponse<Product>>(`/products?${params}`, {
    revalidate: 60,
  })
  // El backend nunca devuelve 404 acá (lista vacía es 200 con items: []), pero
  // apiFetch tipa como opcional por el 404 genérico — este endpoint no lo usa.
  if (!response) {
    throw new Error('GET /products no debería devolver 404')
  }
  return response
}

export async function listCategories() {
  const response = await apiFetch<TaxonRef[]>('/categories', { revalidate: 60 })
  if (!response) {
    throw new Error('GET /categories no debería devolver 404')
  }
  return response
}

// Cães, Gatos — usado por el filtro público y por el form de producto del
// admin (issue #33) para asignar espécies.
export async function listSpecies() {
  const response = await apiFetch<TaxonRef[]>('/species', { revalidate: 60 })
  if (!response) {
    throw new Error('GET /species no debería devolver 404')
  }
  return response
}
