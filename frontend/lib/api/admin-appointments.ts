import { apiFetch } from './client'
import type { AppointmentStatus } from './appointments'
import type { Porte } from './services'

// Espejo de AdminAppointmentDetail — a diferencia de AppointmentDetail
// incluye clienteTelefone completo (requiere cookie JWT).
export interface AdminAppointmentDetail {
  publicId: string
  status: AppointmentStatus
  startAt: string
  endAt: string
  clienteNome: string
  clienteTelefone: string
  petNome: string
  baseServiceNome: string
  addonsNomes: string[]
  totalPriceSnapshot: number
  totalDurationMinutes: number
  tempoExtra: boolean
}

interface ListAdminAppointmentsParams {
  desde?: string
  hasta?: string
  data?: string
  status?: AppointmentStatus
}

// GET /api/v1/admin/appointments?desde=&hasta=&status= (Bloque A amplía el
// endpoint existente para aceptar rango, además de `data` de un solo día).
export async function listAdminAppointments({ desde, hasta, data, status }: ListAdminAppointmentsParams = {}) {
  const params = new URLSearchParams()
  if (desde) params.set('desde', desde)
  if (hasta) params.set('hasta', hasta)
  if (data) params.set('data', data)
  if (status) params.set('status', status)

  const response = await apiFetch<AdminAppointmentDetail[]>(`/admin/appointments?${params}`)
  if (!response) {
    throw new Error('GET /admin/appointments no debería devolver 404')
  }
  return response
}

export async function updateAppointmentStatus(publicId: string, status: AppointmentStatus) {
  const response = await apiFetch<AdminAppointmentDetail>(`/admin/appointments/${publicId}/status`, {
    method: 'PATCH',
    body: { status },
  })
  if (!response) {
    throw new Error('PATCH /admin/appointments/{id}/status no debería devolver 404 acá')
  }
  return response
}

// Espejo de TempoExtraResult. `aviso: null` si no hay solapamiento nuevo —
// persiste igual que haya aviso o no (ADR 011: avisar, no bloquear).
export interface TempoExtraResult {
  publicId: string
  totalDurationMinutes: number
  endAt: string
  aviso: string | null
}

export async function updateTempoExtra(publicId: string, tempoExtra: boolean) {
  const response = await apiFetch<TempoExtraResult>(`/admin/appointments/${publicId}/tempo-extra`, {
    method: 'PATCH',
    body: { tempoExtra },
  })
  if (!response) {
    throw new Error('PATCH /admin/appointments/{id}/tempo-extra no debería devolver 404 acá')
  }
  return response
}

// Espejo de CreateManualAppointmentRequest (Bloque A, ADR 021) — mismos
// campos que CreateAppointmentRequest público, sin honeypot (no aplica
// detrás del login). El admin puede saltear la grilla de slots, superar
// capacidad y agendar en día bloqueado/fuera de horário — todo eso persiste
// con un `aviso`, nunca un 400/409 duro (ADR 021).
export interface CreateManualAppointmentRequest {
  baseServiceId: number
  addonIds?: number[]
  porte: Porte
  data: string
  horario: string
  clienteNome: string
  clienteTelefone: string
  petNome: string
  petRaca?: string
  observacoes?: string
}

// Espejo de ManualAppointmentResult (Bloque A) — mismo shape que
// AdminAppointmentDetail más los avisos que el backend haya generado al
// saltear grilla/capacidad/horário.
export interface ManualAppointmentResult extends AdminAppointmentDetail {
  avisos: string[]
}

export async function createManualAppointment(request: CreateManualAppointmentRequest) {
  const response = await apiFetch<ManualAppointmentResult>('/admin/appointments', {
    method: 'POST',
    body: request,
  })
  if (!response) {
    throw new Error('POST /admin/appointments no debería devolver 404')
  }
  return response
}
