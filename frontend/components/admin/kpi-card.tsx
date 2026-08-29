import type { LucideIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'

const TONE_CLASSES = {
  navy: 'bg-navy/10 text-navy',
  orange: 'bg-orange/10 text-orange',
} as const

interface KPICardProps {
  label: string
  badge: string
  value: number | string
  sublabel?: string
  icon: LucideIcon
  tone?: keyof typeof TONE_CLASSES
}

// Card reutilizable del mini-dashboard admin (ROADMAP tarea 7.2). Solo
// presenta un conteo operacional + contexto — no interpreta el número
// (CLAUDE.md §6: nada de trend pills acá, esa lógica ni existe).
export function KPICard({ label, badge, value, sublabel, icon: Icon, tone = 'navy' }: KPICardProps) {
  return (
    <div className="flex flex-col gap-3 rounded-lg border border-outline/30 bg-surface-card p-5 shadow-card">
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-col gap-1.5">
          <span className="text-sm text-ink-muted">{label}</span>
          <Badge variant="outline" className="w-fit">
            {badge}
          </Badge>
        </div>
        <span className={`flex size-9 shrink-0 items-center justify-center rounded-md ${TONE_CLASSES[tone]}`}>
          <Icon className="size-4" />
        </span>
      </div>
      <span className="text-h2 font-display text-navy">{value}</span>
      {sublabel && <span className="text-caption text-ink-muted">{sublabel}</span>}
    </div>
  )
}
