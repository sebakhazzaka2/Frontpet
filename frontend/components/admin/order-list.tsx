'use client'

import { useState, useSyncExternalStore } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { RefreshCw } from 'lucide-react'
import { Sheet, SheetContent, SheetTitle } from '@/components/ui/sheet'
import { OrderDetailPanel, OrderStatusBadge } from '@/components/admin/order-detail-panel'
import {
  listAdminOrders,
  getOrderStatusCounts,
  getOrder,
  updateOrderStatus,
} from '@/lib/api/admin-orders'
import type { OrderStatus, OrderSummary } from '@/lib/api/orders'
import { formatPrice } from '@/lib/utils'

const TABS = [
  { value: undefined, label: 'Todos', key: 'todos' as const },
  { value: 'PENDING' as const, label: 'Pendentes', key: 'pendentes' as const },
  { value: 'CONFIRMED' as const, label: 'Confirmados', key: 'confirmados' as const },
  { value: 'CANCELLED' as const, label: 'Cancelados', key: 'cancelados' as const },
]

function timeAgo(iso: string): string {
  const minutes = Math.floor((Date.now() - new Date(iso).getTime()) / 60000)
  if (minutes < 1) return 'agora'
  if (minutes < 60) return `há ${minutes} min`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `há ${hours}h`
  return new Date(iso).toLocaleDateString('pt-BR')
}

// Evita abrir el Sheet mobile en desktop: el detalle ya se ve en el panel fijo
// de la derecha, y como Radix monta el overlay (dimming) independiente de las
// clases `lg:hidden` que van en el Content, abrir el Sheet en desktop dejaría
// una pantalla oscurecida sin nada visible detrás (el Content sí se oculta,
// el overlay no).
const DESKTOP_QUERY = '(min-width: 1024px)'

function subscribeToDesktopQuery(onChange: () => void) {
  const query = window.matchMedia(DESKTOP_QUERY)
  query.addEventListener('change', onChange)
  return () => query.removeEventListener('change', onChange)
}

function getIsDesktopSnapshot() {
  return window.matchMedia(DESKTOP_QUERY).matches
}

function getIsDesktopServerSnapshot() {
  return false
}

// useSyncExternalStore, no useEffect+useState: matchMedia es un sistema
// externo de verdad (mismo criterio que useCart() en hooks/use-cart.ts) —
// evita el "calling setState in an effect" que el linter marcaría con el
// patrón useEffect ingenuo.
function useIsDesktop() {
  return useSyncExternalStore(subscribeToDesktopQuery, getIsDesktopSnapshot, getIsDesktopServerSnapshot)
}

// Tarea 4.14 (issue #34) — port de "Gestão de Pedidos": tabs de filtro con
// contadores del backend, split 60/40 lista+detalle en desktop, bottom sheet
// en mobile. Primer uso real de TanStack Query en el repo (ya estaba
// instalado, sin uso, desde el Bloque 0 del Sprint 4) — refetch/invalidación
// tras el PATCH de status, en vez de recargar la página.
export function OrderList() {
  const [statusFilter, setStatusFilter] = useState<OrderStatus | undefined>(undefined)
  const [selectedId, setSelectedId] = useState<string | undefined>(undefined)
  const [sheetOpen, setSheetOpen] = useState(false)
  const isDesktop = useIsDesktop()
  const queryClient = useQueryClient()

  const countsQuery = useQuery({
    queryKey: ['admin-orders-counts'],
    queryFn: getOrderStatusCounts,
  })

  const ordersQuery = useQuery({
    queryKey: ['admin-orders', statusFilter],
    queryFn: () => listAdminOrders({ status: statusFilter, size: 48 }),
  })

  const orderQuery = useQuery({
    queryKey: ['admin-order', selectedId],
    queryFn: () => getOrder(selectedId as string),
    enabled: selectedId != null,
  })

  const statusMutation = useMutation({
    mutationFn: ({ publicId, status }: { publicId: string; status: OrderStatus }) =>
      updateOrderStatus(publicId, status),
    onSuccess: (updated) => {
      queryClient.setQueryData(['admin-order', updated.publicId], updated)
      void queryClient.invalidateQueries({ queryKey: ['admin-orders'] })
      void queryClient.invalidateQueries({ queryKey: ['admin-orders-counts'] })
      toast.success('Status atualizado.')
    },
    onError: () => toast.error('Não foi possível atualizar o status.'),
  })

  function handleSelect(order: OrderSummary) {
    setSelectedId(order.publicId)
    if (!isDesktop) {
      setSheetOpen(true)
    }
  }

  function handleRefresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin-orders'] })
    void queryClient.invalidateQueries({ queryKey: ['admin-orders-counts'] })
  }

  const orders = ordersQuery.data?.items ?? []
  const counts = countsQuery.data

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-h2-mobile font-display text-ink md:text-h2">Gestão de Pedidos</h1>
          {counts && <p className="text-body-sm text-ink-muted">{counts.pendentes} pendente(s)</p>}
        </div>
        <button
          type="button"
          onClick={handleRefresh}
          className="flex w-fit items-center gap-2 rounded-md border border-navy/20 px-4 py-2 text-sm font-medium text-navy hover:bg-navy/5"
        >
          <RefreshCw className="size-4" />
          Atualizar
        </button>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1">
        {TABS.map((tab) => {
          const isActive = statusFilter === tab.value
          const count = counts?.[tab.key]
          return (
            <button
              key={tab.label}
              type="button"
              onClick={() => setStatusFilter(tab.value)}
              className={
                isActive
                  ? 'shrink-0 whitespace-nowrap rounded-full bg-navy px-5 py-2 text-sm font-semibold text-white'
                  : 'shrink-0 whitespace-nowrap rounded-full bg-surface px-5 py-2 text-sm text-ink-muted hover:bg-surface-card'
              }
            >
              {tab.label}
              {count != null ? ` (${count})` : ''}
            </button>
          )
        })}
      </div>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <div className="flex w-full flex-col gap-3 lg:w-[60%]">
          {ordersQuery.isLoading ? (
            <p className="text-sm text-ink-muted">Carregando...</p>
          ) : orders.length === 0 ? (
            <p className="text-sm text-ink-muted">Nenhum pedido encontrado.</p>
          ) : (
            orders.map((order) => (
              <button
                key={order.publicId}
                type="button"
                onClick={() => handleSelect(order)}
                className={
                  selectedId === order.publicId
                    ? 'flex flex-col gap-3 rounded-lg border-2 border-orange bg-orange/5 p-4 text-left shadow-card'
                    : 'flex flex-col gap-3 rounded-lg border border-outline/30 bg-surface-card p-4 text-left shadow-card transition-shadow hover:shadow-card-hover'
                }
              >
                <div className="flex items-start justify-between">
                  <div>
                    <span className="block text-caption font-semibold text-ink-muted">
                      #{order.publicId.slice(0, 8).toUpperCase()}
                    </span>
                    <span className="text-caption text-outline">{timeAgo(order.createdAt)}</span>
                  </div>
                  <OrderStatusBadge status={order.status} />
                </div>
                <div>
                  <p className="font-semibold text-navy">{order.clienteNome}</p>
                  <p className="text-caption text-ink-muted">{order.clienteTelefone}</p>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-caption text-ink-muted">
                    {order.itemCount} {order.itemCount === 1 ? 'produto' : 'produtos'}
                  </span>
                  <span className="text-h3 font-semibold text-navy">{formatPrice(order.subtotal)}</span>
                </div>
              </button>
            ))
          )}
        </div>

        <div className="hidden w-full lg:sticky lg:top-20 lg:block lg:w-[40%]">
          <div className="rounded-lg border border-outline/30 bg-white p-6 shadow-card">
            {orderQuery.data ? (
              <OrderDetailPanel
                order={orderQuery.data}
                updating={statusMutation.isPending}
                onStatusChange={(status) =>
                  statusMutation.mutate({ publicId: orderQuery.data.publicId, status })
                }
              />
            ) : (
              <p className="text-sm text-ink-muted">Selecione um pedido para ver os detalhes.</p>
            )}
          </div>
        </div>
      </div>

      <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
        <SheetContent side="bottom" className="h-[90vh] rounded-t-xl">
          <SheetTitle className="sr-only">Detalhe do pedido</SheetTitle>
          <div className="overflow-y-auto p-4">
            {orderQuery.data && (
              <OrderDetailPanel
                order={orderQuery.data}
                updating={statusMutation.isPending}
                onStatusChange={(status) =>
                  statusMutation.mutate({ publicId: orderQuery.data.publicId, status })
                }
              />
            )}
          </div>
        </SheetContent>
      </Sheet>
    </div>
  )
}
