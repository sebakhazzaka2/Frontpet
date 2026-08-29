import { apiFetch } from './client'
import type { Porte } from './services'

// Espejo de com.frontpet.booking.domain.AppointmentStatus. Sin estados
// además de estos tres (CLAUDE.md §6).
export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

// Espejo de CreateAppointmentRequest (backend/.../booking/dto/CreateAppointmentRequest.java).
// Nunca lleva precio ni duração — el backend los recalcula server-side
// (ADR 020), jamás confía en lo que mande el cliente. `honeypot` debe viajar
// vacío siempre, mismo patrón anti-bot de CreateOrderRequest (tarea 4.15/5.8).
export interface CreateAppointmentRequest {
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
  // ADR 024 — mismo consentimento explícito que CreateOrderRequest já tinha.
  consentimentoLgpd: boolean
  honeypot: string
}

// Espejo de AppointmentAddonDetail.
export interface AppointmentAddonDetail {
  nome: string
  priceSnapshot: number
  durationSnapshot: number
}

// Espejo de AppointmentDetail — response de POST /appointments (201) y de
// GET /appointments/{publicId} (vista pública reducida, sin clienteTelefone —
// docs/booking-api-contracts.md, docs/pending-decisions.md §11).
export interface AppointmentDetail {
  publicId: string
  status: AppointmentStatus
  baseServiceNome: string
  addons: AppointmentAddonDetail[]
  porte: Porte
  startAt: string
  endAt: string
  basePriceSnapshot: number
  totalPriceSnapshot: number
  totalDurationMinutes: number
  tempoExtra: boolean
  clienteNome: string
  petNome: string
}

// POST /api/v1/appointments — público y anônimo (Bloque D). Nunca devuelve
// 404: si no es 2xx, apiFetch tira ApiFetchError con message/fieldErrors en
// PT-BR — 409 sin cupo, 400 fuera de grilla/horário/ventana, 429 rate limit.
export async function createAppointment(request: CreateAppointmentRequest) {
  const response = await apiFetch<AppointmentDetail>('/appointments', {
    method: 'POST',
    body: request,
  })
  if (!response) {
    throw new Error('POST /appointments no debería devolver 404')
  }
  return response
}

// GET /api/v1/appointments/{publicId} — pantalla de confirmação (Bloque D).
// A diferencia de createOrder/createAppointment, este SÍ puede devolver 404
// real (publicId inexistente) — se deja como undefined, el caller decide
// (notFound() de Next).
export function getAppointment(publicId: string) {
  return apiFetch<AppointmentDetail>(`/appointments/${publicId}`)
}
