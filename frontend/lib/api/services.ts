import { apiFetch } from './client'

// Espejo de com.frontpet.booking.domain — enums en MAYÚSCULAS, serialización
// default de Jackson para un Java enum (mismo criterio que orders.ts).
export type ServiceType = 'BASE' | 'ADDON'
export type Porte = 'P' | 'M' | 'G' | 'GG'

// Espejo de ServicePricingDetail (backend/.../booking/dto/ServicePricingDetail.java).
export interface ServicePricingDetail {
  size: Porte
  price: number
  durationMinutes: number
}

// Espejo de ServiceOfferingDetail — GET /api/v1/services (público, sin auth).
// `pricing` trae las 4 filas de porte siempre que el serviço esté completo
// (ADR 011); el wizard resuelve el precio por porte indexando este array.
export interface ServiceOfferingDetail {
  id: number
  type: ServiceType
  nome: string
  descricao: string
  active: boolean
  pricing: ServicePricingDetail[]
}

// GET /api/v1/services — catálogo de banhos base + adicionais. Solo
// active=true (filtrado en backend). revalidate corto: el admin (Bloque F)
// puede tocar precios y el wizard necesita reflejarlo pronto.
export async function listServices() {
  const response = await apiFetch<ServiceOfferingDetail[]>('/services', { revalidate: 60 })
  if (!response) {
    throw new Error('GET /services no debería devolver 404')
  }
  return response
}
