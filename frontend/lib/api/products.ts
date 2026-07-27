import { apiFetch } from './client'
import type { ProductDetail } from '@/lib/data/product-detail'

export function getProductBySlug(slug: string) {
  return apiFetch<ProductDetail>(`/products/${slug}`)
}
