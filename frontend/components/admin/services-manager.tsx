'use client'

import { useState } from 'react'
import { ServicesTab } from '@/components/admin/services-tab'
import { BusinessHoursForm } from '@/components/admin/business-hours-form'
import { ScheduleBlocksPanel } from '@/components/admin/schedule-blocks-panel'

const TABS = [
  { id: 'servicos', label: 'Serviços' },
  { id: 'horario', label: 'Horário de atendimento' },
] as const

type TabId = (typeof TABS)[number]['id']

// Bloque F (issue #63) — port de "Gestão de Serviços (Admin Interativo)".
// Sidebar/topbar já existem (<AdminShell>, Bloque D do Sprint 4); aqui só o
// conteúdo das duas tabs do mock. A terceira tab do mock ("Profissionais")
// não existe no modelo real (capacidade = número, ADR 009) e não se porta.
export function ServicesManager() {
  const [tab, setTab] = useState<TabId>('servicos')

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-h2-mobile font-display text-ink md:text-h2">Serviços e horários</h1>
      </div>

      <div className="flex gap-6 border-b border-outline/30">
        {TABS.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => setTab(item.id)}
            className={
              tab === item.id
                ? 'border-b-2 border-orange pb-3 text-label font-semibold text-ink'
                : 'pb-3 text-label text-ink-muted hover:text-ink'
            }
          >
            {item.label}
          </button>
        ))}
      </div>

      {tab === 'servicos' ? (
        <ServicesTab />
      ) : (
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
          <div className="lg:col-span-8">
            <BusinessHoursForm />
          </div>
          <div className="lg:col-span-4">
            <ScheduleBlocksPanel />
          </div>
        </div>
      )}
    </div>
  )
}
