'use client'

import { cn } from '@/lib/utils'
import { dayOfMonth, fullDate, weekdayShort } from '@/lib/booking-dates'

interface DateStripProps {
  dates: string[]
  selectedDate: string | null
  onSelectDate: (date: string) => void
}

// Tira horizontal de días. No pre-carga disponibilidad de los 7/14 días (AC
// del issue #60): cada día se descubre al tocarlo, 1 request por interacción.
// Por eso acá no hay ningún indicador de "tiene cupo" — sería mentira hasta
// que el usuario toque el día.
export function DateStrip({ dates, selectedDate, onSelectDate }: DateStripProps) {
  return (
    <div
      role="group"
      aria-label="Selecione o dia"
      className="-mx-1 flex gap-3 overflow-x-auto px-1 pb-4 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
    >
      {dates.map((date) => {
        const isSelected = date === selectedDate
        return (
          <button
            key={date}
            type="button"
            onClick={() => onSelectDate(date)}
            aria-pressed={isSelected}
            aria-label={fullDate(date)}
            className={cn(
              'flex h-20 w-16 shrink-0 flex-col items-center justify-center rounded-lg border transition-colors',
              isSelected ? 'border-orange bg-orange text-white' : 'border-outline hover:border-orange',
            )}
          >
            <span className="text-caption uppercase">{weekdayShort(date)}</span>
            <span className="text-h2-mobile font-bold">{dayOfMonth(date)}</span>
          </button>
        )
      })}
    </div>
  )
}
