'use client'

import { useCallback, useEffect, useState } from 'react'
import { toast } from 'sonner'
import { Scissors } from 'lucide-react'
import { ServiceEditDialog } from '@/components/admin/service-edit-dialog'
import { listAdminServices, updateService } from '@/lib/api/admin-schedule'
import type { ServiceOfferingDetail } from '@/lib/api/services'
import { formatPrice } from '@/lib/utils'
import { ApiFetchError } from '@/lib/api/client'

// Bloque F (issue #63) — port do grid de cards de "Gestão de Serviços"
// (tab "Serviços"). Contadores tipo "32 esta semana" do mock se cortam: são
// métrica analítica, proibida no admin do MVP1 (ADR 003/008, decisão D5 do
// plano de Sprint 6). O toggle ativo/inativo do card chama PUT direto (sem
// abrir o modal) — precisa reenviar o pricing atual porque o DTO é
// substituição completa, não PATCH parcial.
export function ServicesTab() {
  const [items, setItems] = useState<ServiceOfferingDetail[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<ServiceOfferingDetail | undefined>(undefined)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setItems(await listAdminServices())
    } catch {
      toast.error('Não foi possível carregar os serviços.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load()
  }, [load])

  async function toggleActive(service: ServiceOfferingDetail) {
    try {
      await updateService(service.id, {
        nome: service.nome,
        descricao: service.descricao,
        active: !service.active,
        pricing: service.pricing.map((p) => ({
          size: p.size,
          price: p.price,
          durationMinutes: p.durationMinutes,
        })),
      })
      void load()
    } catch (err) {
      toast.error(err instanceof ApiFetchError ? err.message : 'Não foi possível atualizar o serviço.')
    }
  }

  const activeCount = items.filter((s) => s.active).length

  return (
    <div className="flex flex-col gap-4">
      <p className="text-body-sm text-ink-muted">{activeCount} de {items.length} serviços ativos</p>

      {loading ? (
        <p className="text-sm text-ink-muted">Carregando...</p>
      ) : items.length === 0 ? (
        <p className="text-sm text-ink-muted">Nenhum serviço cadastrado.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {items.map((service) => {
            const fromPrice = Math.min(...service.pricing.map((p) => p.price))
            return (
              <div
                key={service.id}
                className="flex flex-col overflow-hidden rounded-lg border border-outline/40 bg-white shadow-card"
              >
                <div className="relative flex h-32 items-center justify-center bg-surface">
                  <Scissors className="size-10 text-outline" aria-hidden />
                  <label className="absolute top-3 right-3 flex items-center gap-2 rounded-full bg-white/90 p-1.5 shadow-card">
                    <input
                      type="checkbox"
                      checked={service.active}
                      onChange={() => toggleActive(service)}
                      className="sr-only peer"
                      aria-label={service.active ? 'Desativar serviço' : 'Ativar serviço'}
                    />
                    <span className="relative h-5 w-9 rounded-full bg-outline transition-colors peer-checked:bg-wa after:absolute after:top-0.5 after:left-0.5 after:size-4 after:rounded-full after:bg-white after:transition-transform peer-checked:after:translate-x-4" />
                  </label>
                </div>

                <div className="flex flex-1 flex-col gap-2 p-4">
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="text-h3 text-navy">{service.nome}</h3>
                    <span className="shrink-0 rounded-full bg-orange/10 px-3 py-1 text-caption text-orange">
                      A partir de {formatPrice(fromPrice)}
                    </span>
                  </div>
                  <p className="text-body-sm text-ink-muted">{service.descricao}</p>

                  <button
                    type="button"
                    onClick={() => setEditing(service)}
                    className="mt-auto w-full rounded-md border border-outline py-2 text-label text-ink hover:bg-surface"
                  >
                    Editar
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {editing && (
        <ServiceEditDialog
          key={editing.id}
          open
          onOpenChange={(open) => !open && setEditing(undefined)}
          service={editing}
          onSaved={load}
        />
      )}
    </div>
  )
}
