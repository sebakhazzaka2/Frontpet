import { apiFetch } from './client'
import type { Porte } from './services'

// Espejo de Indisponibilidade (backend/.../booking/dto/Indisponibilidade.java).
// Distingue por qué un día no ofrece horarios — el wizard necesita copy
// distinto para cada caso (ADR 020 §6), una lista vacía muda no alcanza.
export type Indisponibilidade = 'DIA_INATIVO' | 'BLOQUEADO' | 'FORA_DA_JANELA'

// Espejo de SlotDto. `horario` es HH:mm local de America/Sao_Paulo — no
// reconvertir zona, el backend ya resuelve la hora local.
export interface SlotDto {
  horario: string
  disponivel: boolean
}

// Espejo de AvailabilityResponse. `duracaoTotalMinutes`/`precoTotal` vienen
// siempre, incluso con indisponibilidade != null (el combo es válido aunque
// ese día puntual no ofrezca nada — el wizard puede mostrar el precio
// mientras el usuario prueba otras fechas).
export interface AvailabilityResponse {
  data: string
  duracaoTotalMinutes: number
  precoTotal: number
  indisponibilidade: Indisponibilidade | null
  slots: SlotDto[]
}

interface GetAvailabilityParams {
  baseServiceId: number
  porte: Porte
  addonIds?: number[]
  data: string
  /** AbortSignal de TanStack Query — ver useAvailability (Bloque C). */
  signal?: AbortSignal
}

// GET /api/v1/availability?baseServiceId=&porte=&addonIds=&data= — público,
// sin auth. Se re-dispara ante cualquier cambio del combo (Bloque C) — sin
// revalidate: el resultado depende de reservas concurrentes, no es cacheable
// entre requests distintos de forma segura.
export async function getAvailability({ baseServiceId, porte, addonIds, data, signal }: GetAvailabilityParams) {
  const params = new URLSearchParams({
    baseServiceId: String(baseServiceId),
    porte,
    data,
  })
  if (addonIds && addonIds.length > 0) {
    params.set('addonIds', addonIds.join(','))
  }

  const response = await apiFetch<AvailabilityResponse>(`/availability?${params}`, { signal })
  if (!response) {
    throw new Error('GET /availability no debería devolver 404')
  }
  return response
}
