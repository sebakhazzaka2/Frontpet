import { apiFetch } from './client'

// Espejos de com.frontpet.orders.domain — enums en MAYÚSCULAS porque así los
// serializa Jackson por defecto para un Java enum, sin @JsonProperty custom.
export type ModalidadeEntrega = 'ENTREGA' | 'RETIRADA'
export type FormaPagamento = 'DINHEIRO' | 'PIX' | 'CARTAO_DEBITO' | 'CARTAO_CREDITO'
export type FreteMode = 'GRATIS' | 'A_COMBINAR'
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

// Espejo de CreateOrderItemRequest (backend/.../orders/dto/CreateOrderItemRequest.java).
// `variantId` es number (id interno de product_variants), NO un UUID — las
// variantes no tienen public_id (ADR 013 §1).
export interface CreateOrderItemRequest {
  productPublicId: string
  variantId?: number
  quantidade: number
}

// Espejo de CreateOrderRequest. `enderecoEntrega` es obligatorio solo cuando
// modalidade === 'ENTREGA' (regla cruzada validada en OrderServiceImpl, no acá).
// `honeypot` debe viajar vacío siempre — es el campo señuelo de la tarea 4.15.
export interface CreateOrderRequest {
  clienteNome: string
  clienteTelefone: string
  modalidade: ModalidadeEntrega
  enderecoEntrega?: string
  formaPagamento: FormaPagamento
  horarioEntrega?: string
  consentimentoLgpd: boolean
  honeypot: string
  items: CreateOrderItemRequest[]
}

// Espejo de OrderItemDetail.
export interface OrderItemDetail {
  productPublicId: string
  variantId?: number
  nomeSnapshot: string
  unitPriceSnapshot: number
  quantidade: number
}

// Espejo de OrderDetail — response de POST /orders y del detalle admin.
// Sin `whatsappMessage`: corregido junto con el .java (Bloque B, issue #30) —
// el mensaje lo arma el frontend con buildOrderMessage() (lib/whatsapp/templates.ts),
// no el backend.
export interface OrderDetail {
  publicId: string
  status: OrderStatus
  clienteNome: string
  clienteTelefone: string
  formaPagamento: FormaPagamento
  freteMode: FreteMode
  enderecoEntrega: string
  horarioEntrega?: string
  subtotal: number
  items: OrderItemDetail[]
  createdAt: string
  confirmedAt?: string
  cancelledAt?: string
}

// Espejo de OrderSummary — lista del admin ("Gestão de Pedidos").
export interface OrderSummary {
  publicId: string
  status: OrderStatus
  clienteNome: string
  clienteTelefone: string
  itemCount: number
  subtotal: number
  createdAt: string
}

// POST /api/v1/orders — público y anónimo (4.8). Nunca devuelve 404: si no
// es 2xx, apiFetch tira ApiFetchError con message/fieldErrors en PT-BR.
export async function createOrder(request: CreateOrderRequest) {
  const response = await apiFetch<OrderDetail>('/orders', {
    method: 'POST',
    body: request,
  })
  if (!response) {
    throw new Error('POST /orders no debería devolver 404')
  }
  return response
}
