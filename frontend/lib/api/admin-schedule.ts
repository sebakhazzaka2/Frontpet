import { apiFetch } from './client'
import type { Porte, ServiceOfferingDetail } from './services'

// Espejo de ServicePricingRequest.
export interface ServicePricingRequest {
  size: Porte
  price: number
  durationMinutes: number
}

// Espejo de UpdateServiceRequest — reemplazo completo, mismo criterio que
// UpdateProductRequest: el admin edita, nunca crea/borra filas de pricing
// (ADR 009). Cada entrada de `pricing` debe corresponder a un porte que el
// serviço ya tiene sembrado — 405 en POST/DELETE de /admin/services.
export interface UpdateServiceRequest {
  nome: string
  descricao?: string
  active: boolean
  pricing: ServicePricingRequest[]
}

// GET /admin/services — todos os serviços, incluídos os inativos (o admin
// precisa poder reativá-los). Distinto de `listServices` de `./services.ts`,
// que é o catálogo público (`active=true` apenas).
export async function listAdminServices() {
  const response = await apiFetch<ServiceOfferingDetail[]>('/admin/services')
  if (!response) {
    throw new Error('GET /admin/services no debería devolver 404')
  }
  return response
}

export async function updateService(id: number, request: UpdateServiceRequest) {
  const response = await apiFetch<ServiceOfferingDetail>(`/admin/services/${id}`, {
    method: 'PUT',
    body: request,
  })
  if (!response) {
    throw new Error('PUT /admin/services/{id} no debería devolver 404 acá')
  }
  return response
}

// Espejo de BusinessHoursDetail / UpsertBusinessHoursRequest. `diaSemana` en
// convención ISO-8601 (1=Lun..7=Dom, ADR 013 §8). `pausaInicio`/`pausaFin`
// son una única pausa opcional por día (D6 del plan de Sprint 6) — el schema
// no soporta N intervalos, no se porta el "+ Adicionar intervalo" de Stitch.
export interface BusinessHoursDetail {
  diaSemana: number
  activo: boolean
  abertura: string
  fechamento: string
  pausaInicio: string | null
  pausaFin: string | null
}

export type UpsertBusinessHoursRequest = BusinessHoursDetail

export async function listBusinessHours() {
  const response = await apiFetch<BusinessHoursDetail[]>('/admin/business-hours')
  if (!response) {
    throw new Error('GET /admin/business-hours no debería devolver 404')
  }
  return response
}

export async function upsertBusinessHours(rows: UpsertBusinessHoursRequest[]) {
  const response = await apiFetch<BusinessHoursDetail[]>('/admin/business-hours', {
    method: 'PUT',
    body: rows,
  })
  if (!response) {
    throw new Error('PUT /admin/business-hours no debería devolver 404')
  }
  return response
}

// Espejo de ScheduleBlockDetail / CreateScheduleBlockRequest. Bloqueo por día
// completo, no por rango horario (limitación conocida, ver ADR 020 y
// pending-decisions.md §10 — la mitigación real es el turno manual, Bloque A).
export interface ScheduleBlockDetail {
  id: number
  dataDesde: string
  dataHasta: string
  motivo: string | null
}

export interface CreateScheduleBlockRequest {
  dataDesde: string
  dataHasta: string
  motivo?: string
}

export async function listScheduleBlocks() {
  const response = await apiFetch<ScheduleBlockDetail[]>('/admin/schedule-blocks')
  if (!response) {
    throw new Error('GET /admin/schedule-blocks no debería devolver 404')
  }
  return response
}

export async function createScheduleBlock(request: CreateScheduleBlockRequest) {
  const response = await apiFetch<ScheduleBlockDetail>('/admin/schedule-blocks', {
    method: 'POST',
    body: request,
  })
  if (!response) {
    throw new Error('POST /admin/schedule-blocks no debería devolver 404')
  }
  return response
}

export async function deleteScheduleBlock(id: number) {
  await apiFetch<void>(`/admin/schedule-blocks/${id}`, { method: 'DELETE' })
}
