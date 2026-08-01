'use client'

import { Phone, Clock, Calendar as CalendarIcon } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { Checkbox } from '@/components/ui/checkbox'
import type { AdminAppointmentDetail } from '@/lib/api/admin-appointments'
import type { AppointmentStatus } from '@/lib/api/appointments'
import { appointmentCode, buildAppointmentMessage } from '@/lib/whatsapp/templates'
import { buildWhatsAppLinkTo } from '@/lib/data/site'
import { formatPrice } from '@/lib/utils'

interface AppointmentDetailPanelProps {
  appointment: AdminAppointmentDetail
  onStatusChange: (status: AppointmentStatus) => void
  onTempoExtraChange: (tempoExtra: boolean) => void
  updatingStatus: boolean
  updatingTempoExtra: boolean
  tempoExtraAviso?: string | null
}

const STATUS_OPTIONS: { value: AppointmentStatus; label: string }[] = [
  { value: 'PENDING', label: 'Pendente' },
  { value: 'CONFIRMED', label: 'Confirmado' },
  { value: 'CANCELLED', label: 'Cancelado' },
]

function initials(nome: string): string {
  const parts = nome.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase()
}

function formatDia(iso: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(iso))
}

function formatHora(iso: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(iso))
}

// Bloque E (issue #62) — mesmo padrão de <OrderDetailPanel>: conteúdo
// compartilhado entre o painel fixo de desktop e o bottom sheet mobile de
// <AppointmentList>.
//
// Sem "Reagendar" nem "Nota Interna" (o mock de Stitch "Detalhe do
// Agendamento" traz os dois): nenhum tem backing real — reagendar é Fase 2
// (ADR 008), não existe coluna de nota interna em `appointments`. Sem idade
// do pet também ("3 anos" no mock): `petRaca` é o único dado extra que existe
// no DTO, não há campo de idade.
export function AppointmentDetailPanel({
  appointment,
  onStatusChange,
  onTempoExtraChange,
  updatingStatus,
  updatingTempoExtra,
  tempoExtraAviso,
}: AppointmentDetailPanelProps) {
  const whatsappHref = buildWhatsAppLinkTo(
    appointment.clienteTelefone,
    buildAppointmentMessage(appointment)
  )

  return (
    <div className="flex h-full flex-col">
      <div className="mb-6 flex items-center justify-between">
        <h2 className="font-display text-h2 text-navy">
          Agendamento #{appointmentCode(appointment.publicId)}
        </h2>
        <AppointmentStatusBadge status={appointment.status} />
      </div>

      <div className="mb-6 flex items-center gap-2 text-sm text-ink">
        <CalendarIcon className="size-4 text-navy" />
        <span className="font-medium">{formatDia(appointment.startAt)}</span>
      </div>
      <div className="mb-6 flex items-center gap-2 text-sm text-ink-muted">
        <Clock className="size-4" />
        {formatHora(appointment.startAt)} · Duração {appointment.totalDurationMinutes}min
      </div>

      <div className="mb-6 flex items-center gap-3 rounded-lg bg-surface p-4">
        <div className="flex size-11 shrink-0 items-center justify-center rounded-full bg-navy text-sm font-semibold text-white">
          {initials(appointment.clienteNome)}
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate font-semibold text-navy">{appointment.clienteNome}</p>
          <p className="flex items-center gap-1 text-caption text-ink-muted">
            <Phone className="size-3.5" /> {appointment.clienteTelefone}
          </p>
        </div>
      </div>

      <div className="mb-6">
        <h3 className="mb-2 text-label uppercase tracking-wider text-ink-muted">Pet</h3>
        <p className="text-sm text-ink">{appointment.petNome}</p>
      </div>

      <div className="mb-6 flex-1">
        <h3 className="mb-2 text-label uppercase tracking-wider text-ink-muted">
          Serviço selecionado
        </h3>
        <p className="text-sm font-medium text-ink">{appointment.baseServiceNome}</p>
        {appointment.addonsNomes.length > 0 && (
          <p className="text-caption text-ink-muted">+ {appointment.addonsNomes.join(', ')}</p>
        )}
      </div>

      <label className="mb-6 flex items-start gap-3 rounded-lg bg-surface p-3">
        <Checkbox
          checked={appointment.tempoExtra}
          disabled={updatingTempoExtra}
          onCheckedChange={(checked) => onTempoExtraChange(checked === true)}
        />
        <span className="flex-1">
          <span className="block text-sm text-ink">Tempo extra (+20% duração)</span>
          {tempoExtraAviso && (
            <span className="mt-1 block text-caption text-orange">{tempoExtraAviso}</span>
          )}
        </span>
      </label>

      <div className="mt-auto border-t border-outline/30 pt-4">
        <div className="mb-4 flex items-end justify-between">
          <span className="text-h3 font-semibold text-navy">Total</span>
          <span className="text-[22px] font-semibold text-navy">
            {formatPrice(appointment.totalPriceSnapshot)}
          </span>
        </div>

        <div className="mb-4 flex gap-1 rounded-full bg-surface p-1">
          {STATUS_OPTIONS.map((option) => (
            <button
              key={option.value}
              type="button"
              disabled={updatingStatus}
              onClick={() => onStatusChange(option.value)}
              className={
                appointment.status === option.value
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
          Confirmar pelo WhatsApp
        </a>
      </div>
    </div>
  )
}

export function AppointmentStatusBadge({ status }: { status: AppointmentStatus }) {
  const styles: Record<AppointmentStatus, string> = {
    PENDING: 'bg-orange/10 text-orange',
    CONFIRMED: 'bg-wa/10 text-wa',
    CANCELLED: 'bg-outline/20 text-ink-muted',
  }
  const labels: Record<AppointmentStatus, string> = {
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
