import { apiFetch, type PageResponse } from './client'
import type { ProductDetail } from '@/lib/data/product-detail'
import type { Product } from '@/lib/data/products'

export function getProductBySlug(slug: string) {
  return apiFetch<ProductDetail>(`/products/${slug}`)
}

interface ListProductsParams {
  page?: number
  size?: number
}

export async function listProducts({ page = 0, size = 24 }: ListProductsParams = {}) {
  const response = await apiFetch<PageResponse<Product>>(`/products?page=${page}&size=${size}`)
  // El backend nunca devuelve 404 acá (lista vacía es 200 con items: []), pero
  // apiFetch tipa como opcional por el 404 genérico — este endpoint no lo usa.
  if (!response) {
    throw new Error('GET /products no debería devolver 404')
  }
  return response
}
