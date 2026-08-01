import { formatPrice } from '@/lib/utils'

interface ComboSummaryProps {
  totalPrice: number
  totalDurationMinutes: number
  ctaLabel: string
  ctaDisabled?: boolean
  onCta: () => void
}

// Resumo estimado + CTA — reusado no rodapé do Passo 1 (Bloque C) e no Passo 3
// (Bloque D). Preço/duração sempre vêm do combo calculado no wizard, nunca
// hardcodeados aqui.
export function ComboSummary({ totalPrice, totalDurationMinutes, ctaLabel, ctaDisabled, onCta }: ComboSummaryProps) {
  return (
    <div className="mt-8 border-t border-outline pt-6">
      <div className="mb-6 flex items-end justify-between">
        <div>
          <p className="text-caption font-bold tracking-wider text-ink-muted uppercase">Resumo Estimado</p>
          <p className="text-sm text-ink-muted">Duração: {totalDurationMinutes} min</p>
        </div>
        <p className="text-h2 font-display text-navy">{formatPrice(totalPrice)}</p>
      </div>
      <button
        type="button"
        onClick={onCta}
        disabled={ctaDisabled}
        className="w-full rounded-full bg-orange py-4 font-bold text-white shadow-md transition-transform active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {ctaLabel}
      </button>
    </div>
  )
}
