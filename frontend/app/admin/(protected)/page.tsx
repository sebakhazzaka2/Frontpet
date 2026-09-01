'use client'

import { useQuery } from '@tanstack/react-query'
import { Archive, Calendar, ShoppingBag } from 'lucide-react'
import { KPICard } from '@/components/admin/kpi-card'
import { Skeleton } from '@/components/ui/skeleton'
import { getAdminDashboardSummary } from '@/lib/api/admin-dashboard'

// Mini-dashboard operacional (ROADMAP tarea 7.1-7.3). Só conteos que já
// existen en orders/booking/catalog — nada de conversão, faturamento, ticket
// médio ou ranking (CLAUDE.md §6, ADR 003/008).
export default function AdminDashboardPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: getAdminDashboardSummary,
  })

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-h2-mobile font-display text-ink md:text-h2">Dashboard</h1>

      {isError && <p className="text-sm text-destructive">Não foi possível carregar o dashboard.</p>}

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-[168px] rounded-lg" />
          ))}
        </div>
      ) : (
        data && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
            <KPICard
              label="Pedidos pendentes"
              badge="Pedidos"
              value={data.pedidosPendentes}
              icon={ShoppingBag}
              tone="orange"
              featured
            />
            <KPICard
              label="Turnos hoje"
              badge="Hoje"
              value={data.turnos.hoje}
              sublabel={`Próximos 7 dias: ${data.turnos.proximos7Dias} · ${data.turnos.aguardandoConfirmacao} aguardando confirmação`}
              icon={Calendar}
              tone="navy"
            />
            <KPICard
              label="Produtos ativos"
              badge="Catálogo"
              value={data.produtos.ativos}
              sublabel={`${data.produtos.semEstoque} sem estoque`}
              icon={Archive}
              tone="orange"
            />
          </div>
        )
      )}
    </div>
  )
}
