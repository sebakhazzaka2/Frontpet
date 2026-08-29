import { apiFetch } from './client'

// Espejo de AppointmentCounts (backend/.../booking/dto/AppointmentCounts.java).
export interface AppointmentCounts {
  hoje: number
  proximos7Dias: number
  aguardandoConfirmacao: number
}

// Espejo de ProductStockCounts (backend/.../catalog/dto/ProductStockCounts.java).
export interface ProductStockCounts {
  ativos: number
  semEstoque: number
}

// Espejo de AdminDashboardSummary — mini-dashboard operacional (CLAUDE.md §6/§7:
// só conteos, sem métricas analíticas).
export interface AdminDashboardSummary {
  pedidosPendentes: number
  turnos: AppointmentCounts
  produtos: ProductStockCounts
}

export async function getAdminDashboardSummary() {
  const response = await apiFetch<AdminDashboardSummary>('/admin/dashboard')
  if (!response) {
    throw new Error('GET /admin/dashboard no debería devolver 404')
  }
  return response
}
