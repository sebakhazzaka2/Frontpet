'use client'

import { CalendarX2, RefreshCw } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { AvailabilityResponse, Indisponibilidade } from '@/lib/api/availability'

interface SlotGridProps {
  availability: AvailabilityResponse | undefined
  isLoading: boolean
  isError: boolean
  onRetry: () => void
  selectedSlot: string | null
  onSelectSlot: (horario: string) => void
}

// Copy propio por motivo (AC del issue #60 — Stitch no dibuja ninguno de los
// tres). El backend distingue estos casos justamente para que el wizard no
// muestre una grilla vacía muda; colapsarlos en un mensaje genérico tiraría
// esa información a la basura.
const MOTIVO_COPY: Record<Indisponibilidade, string> = {
  DIA_INATIVO: 'A loja não atende nesse dia. Escolha outra data na lista acima.',
  BLOQUEADO: 'Essa data está indisponível na nossa agenda. Escolha outra data na lista acima.',
  FORA_DA_JANELA: 'Essa data está fora do período de agendamento. Escolha uma data mais próxima.',
}

// ⚠️ Cuarto estado vacío, que el AC no lista y el enum no cubre:
// `indisponibilidade: null` con `slots: []`. Lo devuelve AvailabilityServiceImpl
// cuando el día está aberto y sem bloqueio pero nenhum início cabe — o combo
// não termina antes do fechamento, ou já passaram todos os horários de hoje.
// Sem copy próprio o usuário vê uma grade vazia sem explicação.
const SEM_ENCAIXE_COPY =
  'Nenhum horário desse dia comporta o serviço escolhido. Tente outra data ou remova um adicional.'

export function SlotGrid({
  availability,
  isLoading,
  isError,
  onRetry,
  selectedSlot,
  onSelectSlot,
}: SlotGridProps) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-3 gap-3" aria-busy="true" aria-label="Carregando horários">
        {Array.from({ length: 6 }, (_, i) => (
          <div key={i} className="h-11 animate-pulse rounded-md bg-muted" />
        ))}
      </div>
    )
  }

  if (isError) {
    return (
      <div className="rounded-lg border border-dashed border-outline p-6 text-center">
        <p className="mb-4 text-sm text-ink-muted">Não foi possível carregar os horários.</p>
        <button
          type="button"
          onClick={onRetry}
          className="inline-flex items-center gap-2 rounded-md border border-navy px-4 py-2 text-label font-semibold text-navy transition-transform active:scale-95"
        >
          <RefreshCw className="size-4" aria-hidden />
          Tentar novamente
        </button>
      </div>
    )
  }

  if (!availability) {
    return null
  }

  if (availability.slots.length === 0) {
    const message = availability.indisponibilidade
      ? MOTIVO_COPY[availability.indisponibilidade]
      : SEM_ENCAIXE_COPY

    return (
      <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-outline p-6 text-center">
        <CalendarX2 className="size-8 text-outline" aria-hidden />
        <p className="text-sm text-ink-muted">{message}</p>
      </div>
    )
  }

  return (
    <div role="group" aria-label="Horários disponíveis" className="grid grid-cols-3 gap-3">
      {availability.slots.map((slot) => {
        const isSelected = slot.horario === selectedSlot
        return (
          <button
            key={slot.horario}
            type="button"
            disabled={!slot.disponivel}
            onClick={() => onSelectSlot(slot.horario)}
            aria-pressed={isSelected}
            className={cn(
              'rounded-md border py-3 text-sm font-medium transition-colors',
              isSelected && 'border-navy bg-navy text-white',
              !isSelected && slot.disponivel && 'border-outline text-ink hover:bg-hover',
              !slot.disponivel && 'cursor-not-allowed border-outline/50 text-ink-muted/40 line-through',
            )}
          >
            {slot.horario}
          </button>
        )
      })}
    </div>
  )
}
