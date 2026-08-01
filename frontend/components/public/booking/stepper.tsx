import { Check } from 'lucide-react'
import { cn } from '@/lib/utils'

const STEPS = [
  { number: 1, label: 'Serviço' },
  { number: 2, label: 'Data' },
  { number: 3, label: 'Confirmação' },
] as const

export function Stepper({ currentStep }: { currentStep: 1 | 2 | 3 }) {
  return (
    <div className="relative mx-auto mb-10 flex max-w-xs items-center justify-between">
      <div className="absolute top-1/2 left-0 -z-10 h-px w-full -translate-y-1/2 bg-outline/30" />
      {STEPS.map((step) => {
        const isDone = step.number < currentStep
        const isActive = step.number === currentStep
        return (
          <div key={step.number} className="flex flex-col items-center gap-2">
            <div
              className={cn(
                'flex size-10 items-center justify-center rounded-full font-bold text-ink-muted shadow-sm ring-2 ring-white/20',
                isDone && 'bg-wa text-white',
                isActive && 'bg-orange text-white',
                !isDone && !isActive && 'bg-muted',
              )}
            >
              {isDone ? <Check className="size-[18px]" /> : step.number}
            </div>
            <span
              className={cn(
                'text-[10px] font-bold tracking-wider uppercase',
                isDone && 'text-wa',
                isActive && 'text-orange',
                !isDone && !isActive && 'text-ink-muted/60',
              )}
            >
              {step.label}
            </span>
          </div>
        )
      })}
    </div>
  )
}
