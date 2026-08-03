'use client'

import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'
import { Plus, Trash2 } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import {
  listScheduleBlocks,
  createScheduleBlock,
  deleteScheduleBlock,
  type ScheduleBlockDetail,
} from '@/lib/api/admin-schedule'
import { ApiFetchError } from '@/lib/api/client'

function todayInSaoPaulo(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date())
}

function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}

// Bloque F (issue #63) — port de "Exceções e feriados" (Stitch: card na tab
// "Horário de atendimento"). Bloqueio é por dia completo, não por horário
// (limitação conhecida do ADR 020, mitigada pelo turno manual do Bloque A).
export function ScheduleBlocksPanel() {
  const [items, setItems] = useState<ScheduleBlockDetail[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [dataDesde, setDataDesde] = useState(todayInSaoPaulo)
  const [dataHasta, setDataHasta] = useState(todayInSaoPaulo)
  const [motivo, setMotivo] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setItems(await listScheduleBlocks())
    } catch {
      toast.error('Não foi possível carregar os bloqueios.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load()
  }, [load])

  function openDialog() {
    setDataDesde(todayInSaoPaulo())
    setDataHasta(todayInSaoPaulo())
    setMotivo('')
    setError(null)
    setDialogOpen(true)
  }

  async function handleCreate() {
    if (dataDesde > dataHasta) {
      setError('A data de início deve ser anterior ou igual à data de fim.')
      return
    }
    setError(null)
    setSaving(true)
    try {
      await createScheduleBlock({ dataDesde, dataHasta, motivo: motivo || undefined })
      toast.success('Bloqueio criado.')
      setDialogOpen(false)
      void load()
    } catch (err) {
      setError(err instanceof ApiFetchError ? err.message : 'Não foi possível criar o bloqueio.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    try {
      await deleteScheduleBlock(id)
      toast.success('Bloqueio removido.')
      void load()
    } catch {
      toast.error('Não foi possível remover o bloqueio.')
    }
  }

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-outline/40 bg-white p-4 shadow-card">
      <div className="flex items-center justify-between">
        <h3 className="text-h3 text-navy">Exceções e feriados</h3>
        <button
          type="button"
          onClick={openDialog}
          className="flex items-center gap-1 text-label font-semibold text-navy"
        >
          <Plus className="size-4" /> Adicionar
        </button>
      </div>

      <div className="border-t border-outline/20 pt-3">
        {loading ? (
          <p className="text-caption text-ink-muted">Carregando...</p>
        ) : items.length === 0 ? (
          <p className="text-caption italic text-ink-muted">Nenhuma exceção cadastrada</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {items.map((block) => (
              <li key={block.id} className="flex items-center justify-between gap-2 text-caption">
                <div className="flex flex-col">
                  <span className="font-semibold text-ink">
                    {block.dataDesde === block.dataHasta
                      ? formatDate(block.dataDesde)
                      : `${formatDate(block.dataDesde)} – ${formatDate(block.dataHasta)}`}
                  </span>
                  {block.motivo && <span className="text-ink-muted">{block.motivo}</span>}
                </div>
                <button
                  type="button"
                  onClick={() => handleDelete(block.id)}
                  aria-label="Remover bloqueio"
                  className="text-destructive"
                >
                  <Trash2 className="size-4" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="rounded-xl sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Adicionar bloqueio</DialogTitle>
          </DialogHeader>

          <div className="flex flex-col gap-4">
            <div className="grid grid-cols-2 gap-3">
              <div className="flex flex-col gap-1.5">
                <label className="text-label text-ink-muted" htmlFor="dataDesde">
                  De
                </label>
                <Input
                  id="dataDesde"
                  type="date"
                  value={dataDesde}
                  onChange={(e) => setDataDesde(e.target.value)}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-label text-ink-muted" htmlFor="dataHasta">
                  Até
                </label>
                <Input
                  id="dataHasta"
                  type="date"
                  value={dataHasta}
                  onChange={(e) => setDataHasta(e.target.value)}
                />
              </div>
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-label text-ink-muted" htmlFor="motivo">
                Motivo (opcional)
              </label>
              <Input
                id="motivo"
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                placeholder="Ex: Feriado municipal"
              />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Cancelar
            </Button>
            <Button onClick={handleCreate} disabled={saving}>
              {saving ? 'Salvando...' : 'Salvar bloqueio'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
