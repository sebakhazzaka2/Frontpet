import { apiFetch } from './client'
import type { PageResponse, TaxonRef } from './client'

// Espejo de AdminProductSummary (backend/.../catalog/dto/AdminProductSummary.java,
// issue #33) — a diferencia de Product (lib/data/products.ts, catálogo público),
// expone `active` y `stock`: el admin necesita distinguir ocultos y ver estoque,
// cosas que la grilla pública no muestra.
export interface AdminProductSummary {
  publicId: string
  slug: string
  nome: string
  mainImageUrl?: string
  price?: number
  priceOriginal?: number
  brandNome?: string
  hasVariants: boolean
  active: boolean
  stock?: number
}

// Espejo de ProductVariantDto (backend/.../catalog/dto/ProductVariantDto.java).
export interface ProductVariantDetail {
  id: number
  nomeVariante: string
  price: number
  priceOriginal?: number
  stock: number
  onSale: boolean
  inStock: boolean
}

// Espejo de ProductDetail (backend/.../catalog/dto/ProductDetail.java) — el
// mismo shape que ya usa /produtos/{slug}, pero acá lo devuelve también el
// admin (create/update/replaceVariants responden ProductDetail completo).
export interface AdminProductDetail {
  publicId: string
  slug: string
  nome: string
  descricao?: string
  mainImageUrl?: string
  price?: number
  priceOriginal?: number
  stock: number
  brandNome?: string
  categories: TaxonRef[]
  species: TaxonRef[]
  variants: ProductVariantDetail[]
  onSale: boolean
  active: boolean
}

// Espejo de ProductVariantRequest (solo en el alta).
export interface ProductVariantRequest {
  nomeVariante: string
  price: number
  stock?: number
}

// Espejo de CreateProductRequest.
export interface CreateProductRequest {
  nome: string
  descricao?: string
  mainImageUrl?: string
  price?: number
  priceOriginal?: number
  stock?: number
  brandNome?: string
  categorySlugs?: string[]
  speciesSlugs?: string[]
  variants?: ProductVariantRequest[]
}

// Espejo de UpdateProductRequest. `slug` vacío/undefined = no tocar el actual.
// `price` no nulo sobre un producto CON variantes = conversión a precio simple
// (docs/pending-decisions.md §6, mode-switching — ver ProductServiceImpl).
export interface UpdateProductRequest {
  nome: string
  descricao?: string
  mainImageUrl?: string
  price?: number
  priceOriginal?: number
  stock?: number
  brandNome?: string
  categorySlugs?: string[]
  speciesSlugs?: string[]
  slug?: string
}

// Espejo de ProductVariantUpsertRequest. `id` ausente = variante nueva.
export interface ProductVariantUpsertRequest {
  id?: number
  nomeVariante: string
  price: number
  stock?: number
}

// Espejo de PresignedUploadRequest/Response (backend/.../catalog/dto/, ADR 018).
export interface PresignedUploadRequest {
  fileName: string
  contentType: string
  contentLength: number
}

export interface PresignedUploadResponse {
  uploadUrl: string
  publicUrl: string
  objectKey: string
  expiresAt: string
}

interface ListAdminProductsParams {
  page?: number
  size?: number
  categoria?: string
  busca?: string
  incluirInativos?: boolean
}

// GET /api/v1/admin/products (issue #33) — no existía ningún listado admin
// hasta este bloque, solo GET /api/v1/products (público, fuerza active=true).
export async function listAdminProducts({
  page = 0,
  size = 24,
  categoria,
  busca,
  incluirInativos = false,
}: ListAdminProductsParams = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (categoria) params.set('categoria', categoria)
  if (busca) params.set('busca', busca)
  if (incluirInativos) params.set('incluirInativos', 'true')

  const response = await apiFetch<PageResponse<AdminProductSummary>>(`/admin/products?${params}`)
  if (!response) {
    throw new Error('GET /admin/products no debería devolver 404')
  }
  return response
}

// GET /api/v1/admin/products/{publicId} (issue #33) — a diferencia de
// getProductBySlug (público), no filtra `active`: hace falta para poder
// abrir el form de edición de un producto oculto.
export async function getProductForEdit(publicId: string) {
  const response = await apiFetch<AdminProductDetail>(`/admin/products/${publicId}`)
  if (!response) {
    throw new Error('GET /admin/products/{id} no debería devolver 404 acá')
  }
  return response
}

export async function presignProductImageUpload(request: PresignedUploadRequest) {
  const response = await apiFetch<PresignedUploadResponse>('/admin/products/images/presign', {
    method: 'POST',
    body: request,
  })
  if (!response) {
    throw new Error('POST /admin/products/images/presign no debería devolver 404')
  }
  return response
}

export async function createProduct(request: CreateProductRequest) {
  const response = await apiFetch<AdminProductDetail>('/admin/products', {
    method: 'POST',
    body: request,
  })
  if (!response) {
    throw new Error('POST /admin/products no debería devolver 404')
  }
  return response
}

export async function updateProduct(publicId: string, request: UpdateProductRequest) {
  const response = await apiFetch<AdminProductDetail>(`/admin/products/${publicId}`, {
    method: 'PUT',
    body: request,
  })
  if (!response) {
    throw new Error('PUT /admin/products/{id} no debería devolver 404')
  }
  return response
}

export async function replaceProductVariants(
  publicId: string,
  variants: ProductVariantUpsertRequest[]
) {
  const response = await apiFetch<AdminProductDetail>(`/admin/products/${publicId}/variants`, {
    method: 'PUT',
    body: variants,
  })
  if (!response) {
    throw new Error('PUT /admin/products/{id}/variants no debería devolver 404')
  }
  return response
}

export async function deactivateProduct(publicId: string) {
  await apiFetch(`/admin/products/${publicId}`, { method: 'DELETE' })
}

// PUT /admin/products/{id}/active (issue #33) — a diferencia de
// deactivateProduct (atajo de apagar), este también reactiva.
export async function setProductActive(publicId: string, active: boolean) {
  await apiFetch(`/admin/products/${publicId}/active`, {
    method: 'PUT',
    body: { active },
  })
}
