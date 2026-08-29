'use client'

import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
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
import { listServices } from '@/lib/api/services'
import { createManualAppointment } from '@/lib/api/admin-appointments'
import type { Porte } from '@/lib/api/services'
import { ApiFetchError } from '@/lib/api/client'

interface ManualAppointmentDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onCreated: () => void
}

const PORTE_OPTIONS: Porte[] = ['P', 'M', 'G', 'GG']

function todayInSaoPaulo(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date())
}

// Bloque E (issue #62) — "+ Novo agendamento". A diferença central do form
// público (Bloque C/D): não valida contra a grilha de slots nem chama
// /availability antes do submit — o admin pode digitar qualquer horário
// (ADR 021), o backend é quem resolve preço/duração e devolve `avisos` se
// saltear capacidade/grilha/dia bloqueado, sem bloquear a criação.
//
// useState em vez de react-hook-form, mesmo critério do <ProductFormDialog>:
// a lista de adicionais é dinâmica (depende do catálogo real) e o ganho de
// consistência não paga o schema de zod extra pra este form.
export function ManualAppointmentDialog({
  open,
  onOpenChange,
  onCreated,
}: ManualAppointmentDialogProps) {
  const servicesQuery = useQuery({
    queryKey: ['services'],
    queryFn: listServices,
    enabled: open,
  })

  const [porte, setPorte] = useState<Porte>('M')
  const [baseServiceId, setBaseServiceId] = useState<number | undefined>(undefined)
  const [addonIds, setAddonIds] = useState<number[]>([])
  const [data, setData] = useState(todayInSaoPaulo)
  const [horario, setHorario] = useState('')
  const [clienteNome, setClienteNome] = useState('')
  const [clienteTelefone, setClienteTelefone] = useState('')
  const [petNome, setPetNome] = useState('')
  const [petRaca, setPetRaca] = useState('')
  const [observacoes, setObservacoes] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const baseServices = servicesQuery.data?.filter((s) => s.type === 'BASE') ?? []
  const addons = servicesQuery.data?.filter((s) => s.type === 'ADDON') ?? []

  function resetForm() {
    setPorte('M')
    setBaseServiceId(undefined)
    setAddonIds([])
    setData(todayInSaoPaulo())
    setHorario('')
    setClienteNome('')
    setClienteTelefone('')
    setPetNome('')
    setPetRaca('')
    setObservacoes('')
    setError(null)
  }

  function toggleAddon(id: number) {
    setAddonIds((ids) => (ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]))
  }

  function validate(): string | null {
    if (!baseServiceId) return 'Selecione o banho.'
    if (!data) return 'Informe a data.'
    if (!horario) return 'Informe o horário.'
    if (clienteNome.trim().length < 2) return 'Informe o nome do cliente.'
    if (clienteNome.trim().length > 160) return 'Nome do cliente muito longo.'
    if (clienteTelefone.trim().length < 8) return 'Informe um telefone válido.'
    if (clienteTelefone.trim().length > 30) return 'Telefone muito longo.'
    if (petNome.trim().length < 1) return 'Informe o nome do pet.'
    if (petNome.trim().length > 80) return 'Nome do pet muito longo.'
    if (petRaca.trim().length > 80) return 'Raça muito longa.'
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
      const result = await createManualAppointment({
        baseServiceId: baseServiceId as number,
        addonIds,
        porte,
        data,
        horario,
        clienteNome,
        clienteTelefone,
        petNome,
        petRaca: petRaca || undefined,
        observacoes: observacoes || undefined,
      })

      toast.success('Agendamento criado.')
      result.avisos.forEach((aviso) => toast.warning(aviso))

      resetForm()
      onOpenChange(false)
      onCreated()
    } catch (err) {
      setError(err instanceof ApiFetchError ? err.message : 'Não foi possível criar o agendamento.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) resetForm()
        onOpenChange(next)
      }}
    >
      <DialogContent className="max-h-[85vh] overflow-y-auto rounded-xl sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Novo agendamento</DialogTitle>
        </DialogHeader>

        <div className="flex flex-col gap-5">
          <div className="flex flex-col gap-1.5">
            <span className="text-label text-ink-muted">Porte do pet</span>
            <div className="flex rounded-md border border-outline bg-white p-1">
              {PORTE_OPTIONS.map((option) => (
                <button
                  key={option}
                  type="button"
                  onClick={() => setPorte(option)}
                  className={
                    porte === option
                      ? 'flex-1 rounded-md bg-navy py-1.5 text-sm text-white'
                      : 'flex-1 rounded-md py-1.5 text-sm text-ink-muted'
                  }
                >
                  {option}
                </button>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="text-label text-ink-muted">Banho</span>
            <div className="flex flex-col gap-2">
              {baseServices.map((service) => (
                <label
                  key={service.id}
                  className="flex items-center gap-2 rounded-md border border-outline px-3 py-2 text-sm text-ink"
                >
                  <input
                    type="radio"
                    name="baseService"
                    checked={baseServiceId === service.id}
                    onChange={() => setBaseServiceId(service.id)}
                  />
                  {service.nome}
                </label>
              ))}
              {servicesQuery.isLoading && (
                <p className="text-sm text-ink-muted">Carregando serviços...</p>
              )}
            </div>
          </div>

          {addons.length > 0 && (
            <div className="flex flex-col gap-1.5">
              <span className="text-label text-ink-muted">Adicionais</span>
              <div className="flex flex-wrap gap-3">
                {addons.map((addon) => (
                  <label key={addon.id} className="flex items-center gap-1.5 text-sm text-ink">
                    <Checkbox
                      checked={addonIds.includes(addon.id)}
                      onCheckedChange={() => toggleAddon(addon.id)}
                    />
                    {addon.nome}
                  </label>
                ))}
              </div>
            </div>
          )}

          {/* TODO: inputs nativos date/time — pendiente reemplazar por un
              picker propio (feedback de Sebastián, 2026-08-01). Data já
              default pra hoje; o resto (UI do calendário/horário) fica pra
              depois, não bloqueia o fluxo. */}
          <div className="grid grid-cols-2 gap-4">
            <div className="flex flex-col gap-1.5">
              <label className="text-label text-ink-muted" htmlFor="data">
                Data
              </label>
              <Input id="data" type="date" value={data} onChange={(e) => setData(e.target.value)} />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-label text-ink-muted" htmlFor="horario">
                Horário
              </label>
              <Input
                id="horario"
                type="time"
                value={horario}
                onChange={(e) => setHorario(e.target.value)}
              />
            </div>
          </div>
          <p className="-mt-3 text-caption text-ink-muted">
            Não precisa cair na grade de horários — se superar a capacidade ou cair em um dia
            bloqueado, o agendamento é criado mesmo assim, com um aviso.
          </p>

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="flex flex-col gap-1.5">
              <label className="text-label text-ink-muted" htmlFor="clienteNome">
                Nome do cliente
              </label>
              <Input
                id="clienteNome"
                value={clienteNome}
                onChange={(e) => setClienteNome(e.target.value)}
                maxLength={160}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-label text-ink-muted" htmlFor="clienteTelefone">
                Telefone
              </label>
              <Input
                id="clienteTelefone"
                value={clienteTelefone}
                onChange={(e) => setClienteTelefone(e.target.value)}
                placeholder="(55) 99123-4567"
                maxLength={30}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-label text-ink-muted" htmlFor="petNome">
                Nome do pet
              </label>
              <Input
                id="petNome"
                value={petNome}
                onChange={(e) => setPetNome(e.target.value)}
                maxLength={80}
              />
            </div>
            <div className="flex flex-col gap-1.5">
              <label className="text-label text-ink-muted" htmlFor="petRaca">
                Raça (opcional)
              </label>
              <Input
                id="petRaca"
                value={petRaca}
                onChange={(e) => setPetRaca(e.target.value)}
                maxLength={80}
              />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-label text-ink-muted" htmlFor="observacoes">
              Observações (opcional)
            </label>
            <Textarea
              id="observacoes"
              value={observacoes}
              onChange={(e) => setObservacoes(e.target.value)}
              rows={3}
            />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button onClick={handleSubmit} disabled={saving}>
            {saving ? 'Criando...' : 'Criar agendamento'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
