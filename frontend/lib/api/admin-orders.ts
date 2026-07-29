import { apiFetch } from './client'
import type { PageResponse } from './client'
import type { OrderDetail, OrderStatus, OrderSummary } from './orders'

// Espejo de OrderStatusCounts (backend/.../orders/dto/OrderStatusCounts.java) —
// contadores de las tabs del admin, calculados en el backend (issue #34: "los
// contadores salen del backend, no se calculan en el cliente").
export interface OrderStatusCounts {
  todos: number
  pendentes: number
  confirmados: number
  cancelados: number
}

interface ListAdminOrdersParams {
  status?: OrderStatus
  page?: number
  size?: number
}

// GET /api/v1/admin/orders — ordenado por createdAt desc en el backend
// (@PageableDefault sort=createdAt,DESC — aprovecha idx_orders_tenant_created).
export async function listAdminOrders({ status, page = 0, size = 24 }: ListAdminOrdersParams = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (status) params.set('status', status)

  const response = await apiFetch<PageResponse<OrderSummary>>(`/admin/orders?${params}`)
  if (!response) {
    throw new Error('GET /admin/orders no debería devolver 404')
  }
  return response
}

export async function getOrderStatusCounts() {
  const response = await apiFetch<OrderStatusCounts>('/admin/orders/counts')
  if (!response) {
    throw new Error('GET /admin/orders/counts no debería devolver 404')
  }
  return response
}

export async function getOrder(publicId: string) {
  const response = await apiFetch<OrderDetail>(`/admin/orders/${publicId}`)
  if (!response) {
    throw new Error('GET /admin/orders/{id} no debería devolver 404 acá')
  }
  return response
}

export async function updateOrderStatus(publicId: string, status: OrderStatus) {
  const response = await apiFetch<OrderDetail>(`/admin/orders/${publicId}/status`, {
    method: 'PATCH',
    body: { status },
  })
  if (!response) {
    throw new Error('PATCH /admin/orders/{id}/status no debería devolver 404 acá')
  }
  return response
}
