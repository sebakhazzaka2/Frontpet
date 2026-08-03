'use client'

import { useState } from 'react'
import { toast } from 'sonner'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'
import { updateService } from '@/lib/api/admin-schedule'
import type { ServiceOfferingDetail } from '@/lib/api/services'
import { ApiFetchError } from '@/lib/api/client'

interface ServiceEditDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  service: ServiceOfferingDetail
  onSaved: () => void
}

// Bloque F (issue #63) — port do modal "Editar serviço" do mock de Stitch
// "Gestão de Serviços". Desvios do mock: o nome É editável aqui (o mock o
// desenha disabled, mas o DTO real `UpdateServiceRequest` aceita `nome` e o
// AC da issue pede a edição); "Categoria" e "O que inclui" se cortam — nenhum
// DTO real tem esses campos (ADR 009 só prevê nome/descrição/active/pricing).
// O mock também edita um único preço/duração por serviço; o modelo real tem
// 4 portes (P/M/G/GG) com preço e duração próprios (ADR 011), então a grilha
// de portes substitui os dois inputs "Duração"/"Preço Base" do mock.
export function ServiceEditDialog({ open, onOpenChange, service, onSaved }: ServiceEditDialogProps) {
  const [nome, setNome] = useState(service.nome)
  const [descricao, setDescricao] = useState(service.descricao)
  const [active, setActive] = useState(service.active)
  const [pricing, setPricing] = useState(
    service.pricing.map((p) => ({
      size: p.size,
      price: String(p.price),
      durationMinutes: String(p.durationMinutes),
    }))
  )
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function updatePricingRow(index: number, patch: { price?: string; durationMinutes?: string }) {
    setPricing((rows) => rows.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  function validate(): string | null {
    if (nome.trim().length < 2) return 'Informe o nome do serviço.'
    for (const row of pricing) {
      const price = Number(row.price)
      const duration = Number(row.durationMinutes)
      if (!row.price || Number.isNaN(price) || price <= 0) {
        return `Informe um preço válido para o porte ${row.size}.`
      }
      if (!row.durationMinutes || Number.isNaN(duration) || duration <= 0) {
        return `Informe uma duração válida para o porte ${row.size}.`
      }
    }
    return null
  }

  async function handleSubmit() {
    const validationError = validate()
    if (validationError) {
      setError(validationError)
      return
    }
    setError(null)
    setSaving(true)

    try {
      await updateService(service.id, {
        nome,
        descricao: descricao || undefined,
        active,
        pricing: pricing.map((row) => ({
          size: row.size,
          price: Number(row.price),
          durationMinutes: Number(row.durationMinutes),
        })),
      })
      toast.success('Serviço atualizado.')
      onOpenChange(false)
      onSaved()
    } catch (err) {
      setError(err instanceof ApiFetchError ? err.message : 'Não foi possível salvar o serviço.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto rounded-xl sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Editar serviço</DialogTitle>
        </DialogHeader>

        <div className="flex flex-col gap-5">
          <div className="flex flex-col gap-1.5">
            <label className="text-label text-ink-muted" htmlFor="service-nome">
              Nome do serviço
            </label>
            <Input id="service-nome" value={nome} onChange={(e) => setNome(e.target.value)} />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-label text-ink-muted" htmlFor="service-descricao">
              Descrição
            </label>
            <Textarea
              id="service-descricao"
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              rows={3}
            />
          </div>

          <div className="flex flex-col gap-2">
            <span className="text-label text-ink-muted">Preço e duração por porte</span>
            <div className="grid grid-cols-[auto_1fr_1fr] items-center gap-x-3 gap-y-2">
              <span className="text-caption text-ink-muted">Porte</span>
              <span className="text-caption text-ink-muted">Preço (R$)</span>
              <span className="text-caption text-ink-muted">Duração (min)</span>
              {pricing.map((row, index) => (
                <div key={row.size} className="contents">
                  <span className="flex size-9 items-center justify-center rounded-md bg-surface text-sm font-semibold text-navy">
                    {row.size}
                  </span>
                  <Input
                    value={row.price}
                    onChange={(e) => updatePricingRow(index, { price: e.target.value })}
                    inputMode="decimal"
                    placeholder="0,00"
                  />
                  <Input
                    value={row.durationMinutes}
                    onChange={(e) => updatePricingRow(index, { durationMinutes: e.target.value })}
                    inputMode="numeric"
                    placeholder="0"
                  />
                </div>
              ))}
            </div>
          </div>

          <label className="flex items-center gap-3 rounded-lg bg-surface p-3">
            <Checkbox checked={active} onCheckedChange={(checked) => setActive(checked === true)} />
            <span className="text-sm text-ink">Serviço ativo (visível no site e no wizard)</span>
          </label>

          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button onClick={handleSubmit} disabled={saving}>
            {saving ? 'Salvando...' : 'Salvar alterações'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
