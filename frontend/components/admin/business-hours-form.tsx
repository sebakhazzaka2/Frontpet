'use client'

import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'
import { listBusinessHours, upsertBusinessHours, type BusinessHoursDetail } from '@/lib/api/admin-schedule'
import { ApiFetchError } from '@/lib/api/client'

const DIA_LABELS: Record<number, string> = {
  1: 'Segunda-feira',
  2: 'Terça-feira',
  3: 'Quarta-feira',
  4: 'Quinta-feira',
  5: 'Sexta-feira',
  6: 'Sábado',
  7: 'Domingo',
}

interface Row {
  diaSemana: number
  activo: boolean
  abertura: string
  fechamento: string
  pausaAtiva: boolean
  pausaInicio: string
  pausaFin: string
}

function toRow(detail: BusinessHoursDetail): Row {
  return {
    diaSemana: detail.diaSemana,
    activo: detail.activo,
    abertura: detail.abertura.slice(0, 5),
    fechamento: detail.fechamento.slice(0, 5),
    pausaAtiva: detail.pausaInicio != null,
    pausaInicio: detail.pausaInicio?.slice(0, 5) ?? '12:00',
    pausaFin: detail.pausaFin?.slice(0, 5) ?? '13:00',
  }
}

// Bloque F (issue #63) — port de "Disponibilidade Semanal" (tab "Horário de
// atendimento"). Decisão D6 do plano de Sprint 6: uma única pausa opcional
// por dia, não o "+ Adicionar intervalo" (N ilimitado) que o mock desenha —
// o schema de `business_hours` só tem `pausaInicio`/`pausaFin`, migrar pra N
// intervalos exigiria rehacer o cálculo de slots do ADR 020, fora de escopo.
export function BusinessHoursForm() {
  const [rows, setRows] = useState<Row[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const detail = await listBusinessHours()
      setRows(detail.sort((a, b) => a.diaSemana - b.diaSemana).map(toRow))
    } catch {
      toast.error('Não foi possível carregar o horário de atendimento.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load()
  }, [load])

  function updateRow(diaSemana: number, patch: Partial<Row>) {
    setRows((current) => current.map((row) => (row.diaSemana === diaSemana ? { ...row, ...patch } : row)))
  }

  async function handleSave() {
    setSaving(true)
    try {
      await upsertBusinessHours(
        rows.map((row) => ({
          diaSemana: row.diaSemana,
          activo: row.activo,
          abertura: row.abertura,
          fechamento: row.fechamento,
          pausaInicio: row.pausaAtiva ? row.pausaInicio : null,
          pausaFin: row.pausaAtiva ? row.pausaFin : null,
        }))
      )
      toast.success('Horário de atendimento atualizado.')
      void load()
    } catch (err) {
      toast.error(err instanceof ApiFetchError ? err.message : 'Não foi possível salvar o horário.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return <p className="text-sm text-ink-muted">Carregando...</p>
  }

  return (
    <div className="flex flex-col gap-6 rounded-lg border border-outline/40 bg-white p-4 shadow-card">
      <h3 className="text-h3 text-navy">Disponibilidade semanal</h3>

      <div className="flex flex-col gap-4">
        {rows.map((row) => (
          <div key={row.diaSemana} className="flex flex-col gap-2 border-b border-outline/20 pb-4 last:border-0">
            <div className="flex flex-wrap items-center gap-4">
              <label className="flex w-36 shrink-0 items-center gap-2 text-label text-ink">
                <Checkbox
                  checked={row.activo}
                  onCheckedChange={(checked) => updateRow(row.diaSemana, { activo: checked === true })}
                />
                {DIA_LABELS[row.diaSemana]}
              </label>

              {row.activo ? (
                <div className="flex flex-wrap items-center gap-2">
                  <input
                    type="time"
                    value={row.abertura}
                    onChange={(e) => updateRow(row.diaSemana, { abertura: e.target.value })}
                    className="h-9 rounded-md border border-outline px-2 text-sm"
                  />
                  <span className="text-caption text-ink-muted">até</span>
                  <input
                    type="time"
                    value={row.fechamento}
                    onChange={(e) => updateRow(row.diaSemana, { fechamento: e.target.value })}
                    className="h-9 rounded-md border border-outline px-2 text-sm"
                  />
                </div>
              ) : (
                <span className="text-caption italic text-ink-muted">Fechado</span>
              )}
            </div>

            {row.activo && (
              <div className="flex flex-wrap items-center gap-2 pl-[152px]">
                <label className="flex items-center gap-2 text-caption text-ink-muted">
                  <Checkbox
                    checked={row.pausaAtiva}
                    onCheckedChange={(checked) => updateRow(row.diaSemana, { pausaAtiva: checked === true })}
                  />
                  Intervalo de almoço
                </label>
                {row.pausaAtiva && (
                  <div className="flex items-center gap-2">
                    <input
                      type="time"
                      value={row.pausaInicio}
                      onChange={(e) => updateRow(row.diaSemana, { pausaInicio: e.target.value })}
                      className="h-8 rounded-md border border-outline px-2 text-sm"
                    />
                    <span className="text-caption text-ink-muted">até</span>
                    <input
                      type="time"
                      value={row.pausaFin}
                      onChange={(e) => updateRow(row.diaSemana, { pausaFin: e.target.value })}
                      className="h-8 rounded-md border border-outline px-2 text-sm"
                    />
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      <Button onClick={handleSave} disabled={saving} className="w-fit">
        {saving ? 'Salvando...' : 'Salvar horário'}
      </Button>
    </div>
  )
}
