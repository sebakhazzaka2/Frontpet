'use client'

import { Phone } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import type { OrderDetail, OrderStatus } from '@/lib/api/orders'
import { buildOrderMessage, freteLabel, modalidadeLabel } from '@/lib/whatsapp/templates'
import { buildWhatsAppLinkTo } from '@/lib/data/site'
import { canTransitionStatus } from '@/lib/status-transitions'
import { formatPrice } from '@/lib/utils'

interface OrderDetailPanelProps {
  order: OrderDetail
  onStatusChange: (status: OrderStatus) => void
  updating: boolean
}

const STATUS_OPTIONS: { value: OrderStatus; label: string }[] = [
  { value: 'PENDING', label: 'Pendente' },
  { value: 'CONFIRMED', label: 'Confirmado' },
  { value: 'CANCELLED', label: 'Cancelado' },
]

function initials(nome: string): string {
  const parts = nome.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase()
}

// Tarea 4.14 (issue #34) — contenido compartido entre el panel fijo de
// desktop (60/40 split) y el bottom sheet mobile de <OrderList>: una sola
// fuente de verdad para no divergir entre las dos superficies.
//
// Sin thumbnails de producto en los items ni el botón de WhatsApp genérico
// que dibuja el mock junto al nombre del cliente: `OrderItemDetail` no trae
// imagen (no se inventa), y el mock tiene DOS botones de WhatsApp con
// alcance ambiguo — el AC pide uno solo ("Enviar confirmação"), que además
// es el que manda el template real por status.
export function OrderDetailPanel({ order, onStatusChange, updating }: OrderDetailPanelProps) {
  const whatsappHref = buildWhatsAppLinkTo(order.clienteTelefone, buildOrderMessage(order))

  return (
    <div className="flex h-full flex-col">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="font-display text-h2 text-navy">Pedido #{order.publicId.slice(0, 8).toUpperCase()}</h2>
        <OrderStatusBadge status={order.status} />
      </div>

      <div className="mb-6 flex items-center gap-3 rounded-lg bg-surface p-4">
        <div className="flex size-11 shrink-0 items-center justify-center rounded-full bg-navy text-sm font-semibold text-white">
          {initials(order.clienteNome)}
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate font-semibold text-navy">{order.clienteNome}</p>
          <p className="flex items-center gap-1 text-caption text-ink-muted">
            <Phone className="size-3.5" /> {order.clienteTelefone}
          </p>
        </div>
      </div>

      <div className="mb-6">
        <h3 className="mb-2 text-label uppercase tracking-wider text-ink-muted">
          Modalidade de entrega
        </h3>
        <p className="text-sm text-ink">{modalidadeLabel(order)}</p>
        {order.modalidade === 'ENTREGA' && (
          <p className="text-sm text-ink-muted">{order.enderecoEntrega}</p>
        )}
        {order.horarioEntrega && (
          <p className="mt-1 text-caption text-ink-muted">Horário preferido: {order.horarioEntrega}</p>
        )}
      </div>

      <div className="mb-6 flex-1">
        <h3 className="mb-3 text-label uppercase tracking-wider text-ink-muted">
          Itens do pedido ({order.items.length})
        </h3>
        <div className="flex flex-col gap-3">
          {order.items.map((item, index) => (
            <div
              key={`${item.productPublicId}-${item.variantId ?? 'simple'}-${index}`}
              className="flex items-center justify-between border-b border-outline/10 py-2 text-sm"
            >
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium text-ink">{item.nomeSnapshot}</p>
                <p className="text-caption text-ink-muted">{formatPrice(item.unitPriceSnapshot)}</p>
              </div>
              <span className="shrink-0 font-medium text-ink">x{item.quantidade}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="mt-auto border-t border-outline/30 pt-4">
        <div className="mb-2 flex items-center justify-between text-sm text-ink-muted">
          <span>Subtotal</span>
          <span className="font-medium">{formatPrice(order.subtotal)}</span>
        </div>
        <div className="mb-4 flex items-center justify-between text-sm text-ink-muted">
          <span>Frete</span>
          <span className="italic">{freteLabel(order)}</span>
        </div>
        <div className="mb-1 flex items-end justify-between">
          <span className="text-h3 font-semibold text-navy">Total</span>
          <span className="text-[22px] font-semibold text-navy">{formatPrice(order.subtotal)}</span>
        </div>
        <p className="mb-6 text-caption text-ink-muted">
          * O valor do frete será confirmado pelo WhatsApp.
        </p>

        <div className="mb-4 flex gap-1 rounded-full bg-surface p-1">
          {STATUS_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              disabled={updating || !canTransitionStatus(order.status, option.value)}
              onClick={() => onStatusChange(option.value)}
              className={
                order.status === option.value
                  ? 'flex-1 rounded-full bg-white py-2 text-caption font-semibold text-navy shadow-card'
                  : 'flex-1 rounded-full py-2 text-caption text-ink-muted transition-colors hover:bg-white/60 disabled:opacity-50'
              }
            >
              {option.label}
            </button>
          ))}
        </div>

        <a
          href={whatsappHref}
          target="_blank"
          rel="noopener noreferrer"
          className="flex h-14 items-center justify-center gap-3 rounded-xl bg-wa font-semibold text-white shadow-card transition-all hover:opacity-90 active:scale-95"
        >
          <WhatsAppIcon className="size-5" />
          Enviar confirmação no WhatsApp
        </a>
      </div>
    </div>
  )
}

function OrderStatusBadge({ status }: { status: OrderStatus }) {
  const styles: Record<OrderStatus, string> = {
    PENDING: 'bg-orange/10 text-orange',
    CONFIRMED: 'bg-wa/10 text-wa',
    CANCELLED: 'bg-outline/20 text-ink-muted',
  }
  const labels: Record<OrderStatus, string> = {
    PENDING: 'Pendente',
    CONFIRMED: 'Confirmado',
    CANCELLED: 'Cancelado',
  }
  return (
    <span className={`rounded-full px-3 py-1 text-caption font-semibold ${styles[status]}`}>
      {labels[status]}
    </span>
  )
}

export { OrderStatusBadge }
