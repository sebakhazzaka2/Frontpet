'use client'

import { useMemo, useState, useSyncExternalStore } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ChevronDown, Plus, RefreshCw } from 'lucide-react'
import { Sheet, SheetContent, SheetTitle } from '@/components/ui/sheet'
import {
  AppointmentDetailPanel,
  AppointmentStatusBadge,
} from '@/components/admin/appointment-detail-panel'
import { ManualAppointmentDialog } from '@/components/admin/manual-appointment-dialog'
import {
  listAdminAppointments,
  updateAppointmentStatus,
  updateTempoExtra,
  type AdminAppointmentDetail,
} from '@/lib/api/admin-appointments'
import type { AppointmentStatus } from '@/lib/api/appointments'
import { formatPrice } from '@/lib/utils'

const STATUS_TABS: { value: AppointmentStatus | undefined; label: string }[] = [
  { value: undefined, label: 'Todos' },
  { value: 'PENDING', label: 'Pendentes' },
  { value: 'CONFIRMED', label: 'Confirmados' },
  { value: 'CANCELLED', label: 'Cancelados' },
]

const DIAS_SEMANA = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb']

function todayInSaoPaulo(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date())
}

function addDays(dateStr: string, days: number): string {
  const [y, m, d] = dateStr.split('-').map(Number)
  const date = new Date(Date.UTC(y, m - 1, d))
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

function dayKeyOf(iso: string): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date(iso))
}

function formatHora(iso: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(iso))
}

function formatDiaLabel(dateStr: string): string {
  const [y, m, d] = dateStr.split('-').map(Number)
  const date = new Date(Date.UTC(y, m - 1, d))
  return `${DIAS_SEMANA[date.getUTCDay()]}. ${d}`
}

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

// useSyncExternalStore, mismo criterio que <OrderList>: matchMedia es un
// sistema externo real.
function useIsDesktop() {
  return useSyncExternalStore(subscribeToDesktopQuery, getIsDesktopSnapshot, getIsDesktopServerSnapshot)
}

// Bloque E (issue #62) — port de "Gestão de Agendamentos (Vista Semanal)".
// Mismo esqueleto que <OrderList> (TanStack Query, split 60/40 desktop, Sheet
// mobile) pero agrupado por día en vez de lista plana, siguiendo el mock.
//
// Un solo fetch de la ventana de 7 días SIN filtro de status — el filtro de
// status de las tabs y el toggle "Hoje" se aplican client-side sobre esos
// mismos datos. Evita 4 round-trips por cada cambio de tab y mantiene los 3
// contadores operacionales consistentes con lo que se está mostrando.
//
// Sin el estado "FECHADO" que dibuja el mock para domingo: no existe hoy un
// endpoint que exponga business_hours al admin (gap real, anotado también
// para Bloque F — GET /admin/business-hours no existe). Un día sin turnos
// muestra "Sem agendamento para este dia" tenga o no atendimento ese día.
export function AppointmentList() {
  const [statusFilter, setStatusFilter] = useState<AppointmentStatus | undefined>(undefined)
  const [rangeMode, setRangeMode] = useState<'hoje' | 'semana'>('semana')
  const [selectedId, setSelectedId] = useState<string | undefined>(undefined)
  const [expandedDays, setExpandedDays] = useState<Set<string>>(() => new Set([todayInSaoPaulo()]))
  const [sheetOpen, setSheetOpen] = useState(false)
  const [dialogOpen, setDialogOpen] = useState(false)
  const isDesktop = useIsDesktop()
  const queryClient = useQueryClient()

  const desde = todayInSaoPaulo()
  const hasta = addDays(desde, 6)

  const appointmentsQuery = useQuery({
    queryKey: ['admin-appointments', desde, hasta],
    queryFn: () => listAdminAppointments({ desde, hasta }),
  })

  const statusMutation = useMutation({
    mutationFn: ({ publicId, status }: { publicId: string; status: AppointmentStatus }) =>
      updateAppointmentStatus(publicId, status),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin-appointments'] })
      toast.success('Status atualizado.')
    },
    onError: () => toast.error('Não foi possível atualizar o status.'),
  })

  const tempoExtraMutation = useMutation({
    mutationFn: ({ publicId, tempoExtra }: { publicId: string; tempoExtra: boolean }) =>
      updateTempoExtra(publicId, tempoExtra),
    onSuccess: (result) => {
      void queryClient.invalidateQueries({ queryKey: ['admin-appointments'] })
      if (result.aviso) toast.warning(result.aviso)
    },
    onError: () => toast.error('Não foi possível atualizar o tempo extra.'),
  })

  const allAppointments = useMemo(() => appointmentsQuery.data ?? [], [appointmentsQuery.data])

  const counts = useMemo(
    () => ({
      semana: allAppointments.length,
      confirmados: allAppointments.filter((a) => a.status === 'CONFIRMED').length,
      aguardando: allAppointments.filter((a) => a.status === 'PENDING').length,
    }),
    [allAppointments]
  )

  const filtered = useMemo(
    () => allAppointments.filter((a) => !statusFilter || a.status === statusFilter),
    [allAppointments, statusFilter]
  )

  const dayKeys = rangeMode === 'hoje' ? [desde] : Array.from({ length: 7 }, (_, i) => addDays(desde, i))

  const byDay = useMemo(() => {
    const map = new Map<string, AdminAppointmentDetail[]>()
    for (const appointment of filtered) {
      const key = dayKeyOf(appointment.startAt)
      const list = map.get(key) ?? []
      list.push(appointment)
      map.set(key, list)
    }
    for (const list of map.values()) {
      list.sort((a, b) => a.startAt.localeCompare(b.startAt))
    }
    return map
  }, [filtered])

  const selected = allAppointments.find((a) => a.publicId === selectedId)

  function toggleDay(key: string) {
    setExpandedDays((current) => {
      const next = new Set(current)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  function handleSelect(appointment: AdminAppointmentDetail) {
    setSelectedId(appointment.publicId)
    if (!isDesktop) setSheetOpen(true)
  }

  function handleRefresh() {
    void queryClient.invalidateQueries({ queryKey: ['admin-appointments'] })
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <h1 className="text-h2-mobile font-display text-ink md:text-h2">Agendamentos</h1>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={handleRefresh}
            className="flex w-fit items-center gap-2 rounded-md border border-navy/20 px-4 py-2 text-sm font-medium text-navy hover:bg-navy/5"
          >
            <RefreshCw className="size-4" />
            Atualizar
          </button>
          <button
            type="button"
            onClick={() => setDialogOpen(true)}
            className="flex w-fit items-center gap-2 rounded-md bg-orange px-4 py-2 text-sm font-semibold text-white hover:opacity-90"
          >
            <Plus className="size-4" />
            Novo agendamento
          </button>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <StatCard label="Agendamentos da semana" value={counts.semana} />
        <StatCard label="Confirmados" value={counts.confirmados} tone="wa" />
        <StatCard label="Aguardando" value={counts.aguardando} tone="orange" />
      </div>

      <div className="flex items-center gap-2">
        {(['hoje', 'semana'] as const).map((mode) => (
          <button
            key={mode}
            type="button"
            onClick={() => setRangeMode(mode)}
            className={
              rangeMode === mode
                ? 'rounded-full bg-navy px-4 py-1.5 text-caption font-semibold text-white'
                : 'rounded-full bg-surface px-4 py-1.5 text-caption text-ink-muted hover:bg-surface-card'
            }
          >
            {mode === 'hoje' ? 'Hoje' : 'Próximos 7 dias'}
          </button>
        ))}
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.label}
            type="button"
            onClick={() => setStatusFilter(tab.value)}
            className={
              statusFilter === tab.value
                ? 'shrink-0 whitespace-nowrap rounded-full bg-navy px-5 py-2 text-sm font-semibold text-white'
                : 'shrink-0 whitespace-nowrap rounded-full bg-surface px-5 py-2 text-sm text-ink-muted hover:bg-surface-card'
            }
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        <div className="flex w-full flex-col gap-3 lg:w-[60%]">
          {appointmentsQuery.isLoading ? (
            <p className="text-sm text-ink-muted">Carregando...</p>
          ) : (
            dayKeys.map((dayKey) => {
              const dayAppointments = byDay.get(dayKey) ?? []
              const isExpanded = expandedDays.has(dayKey)
              return (
                <div key={dayKey} className="rounded-lg border border-outline/30 bg-surface-card">
                  <button
                    type="button"
                    onClick={() => toggleDay(dayKey)}
                    className="flex w-full items-center justify-between px-4 py-3 text-left"
                  >
                    <span className="text-sm font-semibold text-ink">{formatDiaLabel(dayKey)}</span>
                    <div className="flex items-center gap-2">
                      {dayAppointments.length > 0 && (
                        <span className="rounded-full bg-navy/10 px-2.5 py-0.5 text-caption font-semibold text-navy">
                          {dayAppointments.length}{' '}
                          {dayAppointments.length === 1 ? 'agend.' : 'agend.'}
                        </span>
                      )}
                      <ChevronDown
                        className={`size-4 text-ink-muted transition-transform ${isExpanded ? 'rotate-180' : ''}`}
                      />
                    </div>
                  </button>
                  {isExpanded && (
                    <div className="border-t border-outline/20 p-3">
                      {dayAppointments.length === 0 ? (
                        <p className="px-1 py-2 text-caption text-ink-muted">
                          Sem agendamento para este dia.
                        </p>
                      ) : (
                        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                          {dayAppointments.map((appointment) => (
                            <button
                              key={appointment.publicId}
                              type="button"
                              onClick={() => handleSelect(appointment)}
                              className={
                                selectedId === appointment.publicId
                                  ? 'flex flex-col gap-1 rounded-md border-2 border-orange bg-orange/5 p-3 text-left'
                                  : 'flex flex-col gap-1 rounded-md border border-outline/20 p-3 text-left hover:bg-surface'
                              }
                            >
                              <div className="flex items-center justify-between">
                                <span className="text-sm font-semibold text-navy">
                                  {formatHora(appointment.startAt)}
                                </span>
                                <AppointmentStatusBadge status={appointment.status} />
                              </div>
                              <span className="text-caption text-ink">
                                {appointment.petNome} · {appointment.clienteNome}
                              </span>
                              <span className="text-caption text-ink-muted">
                                {appointment.baseServiceNome}
                              </span>
                              <span className="text-caption font-medium text-ink-muted">
                                {formatPrice(appointment.totalPriceSnapshot)}
                              </span>
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            })
          )}
        </div>

        <div className="hidden w-full lg:sticky lg:top-20 lg:block lg:w-[40%]">
          <div className="rounded-lg border border-outline/30 bg-white p-6 shadow-card">
            {selected ? (
              <AppointmentDetailPanel
                appointment={selected}
                updatingStatus={statusMutation.isPending}
                updatingTempoExtra={tempoExtraMutation.isPending}
                tempoExtraAviso={tempoExtraMutation.data?.aviso}
                onStatusChange={(status) => statusMutation.mutate({ publicId: selected.publicId, status })}
                onTempoExtraChange={(tempoExtra) =>
                  tempoExtraMutation.mutate({ publicId: selected.publicId, tempoExtra })
                }
              />
            ) : (
              <p className="text-sm text-ink-muted">Selecione um agendamento para ver os detalhes.</p>
            )}
          </div>
        </div>
      </div>

      <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
        <SheetContent side="bottom" className="h-[90vh] rounded-t-xl">
          <SheetTitle className="sr-only">Detalhe do agendamento</SheetTitle>
          <div className="overflow-y-auto p-4">
            {selected && (
              <AppointmentDetailPanel
                appointment={selected}
                updatingStatus={statusMutation.isPending}
                updatingTempoExtra={tempoExtraMutation.isPending}
                tempoExtraAviso={tempoExtraMutation.data?.aviso}
                onStatusChange={(status) => statusMutation.mutate({ publicId: selected.publicId, status })}
                onTempoExtraChange={(tempoExtra) =>
                  tempoExtraMutation.mutate({ publicId: selected.publicId, tempoExtra })
                }
              />
            )}
          </div>
        </SheetContent>
      </Sheet>

      <ManualAppointmentDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onCreated={() => void queryClient.invalidateQueries({ queryKey: ['admin-appointments'] })}
      />
    </div>
  )
}

function StatCard({
  label,
  value,
  tone,
}: {
  label: string
  value: number
  tone?: 'wa' | 'orange'
}) {
  const valueColor = tone === 'wa' ? 'text-wa' : tone === 'orange' ? 'text-orange' : 'text-navy'
  return (
    <div className="rounded-lg border border-outline/30 bg-surface-card p-4">
      <p className="text-caption uppercase tracking-wider text-ink-muted">{label}</p>
      <p className={`mt-1 text-h2-mobile font-display ${valueColor}`}>{value}</p>
    </div>
  )
}
