'use client'

import { Minus, Plus } from 'lucide-react'

interface QuantityStepperProps {
  quantidade: number
  onChange: (quantidade: number) => void
  /** Tope superior opcional (stock disponible). Sin límite si no se pasa. */
  max?: number
  min?: number
}

// Tarea 4.5/4.2 (issue #29): selector de cantidad reusado en product-detail.tsx
// (producto simple) y product-variant-picker.tsx (con variante elegida). No
// clampea silenciosamente contra `max` restando de golpe — el botón "+" se
// deshabilita al llegar al tope, así el usuario ve por qué no puede seguir
// sumando en vez de que el número deje de responder sin explicación.
export function QuantityStepper({ quantidade, onChange, max, min = 1 }: QuantityStepperProps) {
  const canDecrease = quantidade > min
  const canIncrease = max === undefined || quantidade < max

  return (
    <div className="flex w-fit items-center gap-3 rounded-md border border-outline">
      <button
        type="button"
        onClick={() => canDecrease && onChange(quantidade - 1)}
        disabled={!canDecrease}
        aria-label="Diminuir quantidade"
        className="flex size-9 items-center justify-center text-ink transition-opacity disabled:opacity-30"
      >
        <Minus className="size-4" />
      </button>
      <span className="min-w-4 text-center text-sm font-medium text-ink" aria-live="polite">
        {quantidade}
      </span>
      <button
        type="button"
        onClick={() => canIncrease && onChange(quantidade + 1)}
        disabled={!canIncrease}
        aria-label="Aumentar quantidade"
        className="flex size-9 items-center justify-center text-ink transition-opacity disabled:opacity-30"
      >
        <Plus className="size-4" />
      </button>
    </div>
  )
}
