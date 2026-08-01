import { PawPrint } from 'lucide-react'
import { cn, formatPrice } from '@/lib/utils'
import type { Porte, ServiceOfferingDetail } from '@/lib/api/services'
import { ComboSummary } from './combo-summary'

const PORTES: { value: Porte; iconSize: number }[] = [
  { value: 'P', iconSize: 20 },
  { value: 'M', iconSize: 24 },
  { value: 'G', iconSize: 28 },
  { value: 'GG', iconSize: 32 },
]

interface ServiceStepProps {
  baseServices: ServiceOfferingDetail[]
  addonServices: ServiceOfferingDetail[]
  porte: Porte
  onPorteChange: (porte: Porte) => void
  baseServiceId: number | null
  onBaseServiceChange: (id: number) => void
  addonIds: number[]
  onToggleAddon: (id: number) => void
  totalPrice: number
  totalDurationMinutes: number
  onContinue: () => void
}

function priceForPorte(service: ServiceOfferingDetail, porte: Porte) {
  return service.pricing.find((p) => p.size === porte)
}

// Passo 1 do wizard (Bloque C, issue #60). Server-side não dá: porte, banho
// base e adicionais são escolhas do usuário que recalculam o resumo em vivo —
// precisa de estado de cliente (dono em <BookingWizard>, este componente é
// controlado via props).
export function ServiceStep({
  baseServices,
  addonServices,
  porte,
  onPorteChange,
  baseServiceId,
  onBaseServiceChange,
  addonIds,
  onToggleAddon,
  totalPrice,
  totalDurationMinutes,
  onContinue,
}: ServiceStepProps) {
  return (
    <div>
      <div className="mb-8">
        <h2 className="mb-2 text-h3 font-display text-navy">1. Qual o porte do seu pet?</h2>
        <p className="mb-4 text-caption text-ink-muted">Os valores e tempos de duração variam conforme o tamanho.</p>
        <div className="flex justify-between gap-2">
          {PORTES.map(({ value, iconSize }) => (
            <button
              key={value}
              type="button"
              onClick={() => onPorteChange(value)}
              aria-pressed={porte === value}
              className={cn(
                'flex flex-1 flex-col items-center justify-center gap-1 rounded-lg border p-3 transition-all',
                porte === value ? 'border-navy bg-navy text-white' : 'border-outline hover:bg-hover',
              )}
            >
              <PawPrint style={{ width: iconSize, height: iconSize }} aria-hidden />
              <span className="text-label font-bold">{value}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="mb-8">
        <h2 className="mb-4 text-h3 font-display text-navy">2. Selecione o banho base</h2>
        <div className="grid grid-cols-1 gap-4">
          {baseServices.map((service) => {
            const pricing = priceForPorte(service, porte)
            const isSelected = service.id === baseServiceId
            return (
              <button
                key={service.id}
                type="button"
                onClick={() => onBaseServiceChange(service.id)}
                aria-pressed={isSelected}
                className={cn(
                  'rounded-lg border p-4 text-left transition-all',
                  isSelected ? 'border-orange bg-orange/5' : 'border-outline hover:bg-hover',
                )}
              >
                <div className="mb-1 flex items-start justify-between">
                  <span className="font-bold text-navy">{service.nome}</span>
                  <div
                    className={cn(
                      'flex size-5 shrink-0 items-center justify-center rounded-full border-2',
                      isSelected ? 'border-orange' : 'border-outline',
                    )}
                  >
                    {isSelected && <div className="size-2.5 rounded-full bg-orange" />}
                  </div>
                </div>
                <p className="mb-2 text-caption text-ink-muted">{service.descricao}</p>
                {pricing && (
                  <p className="text-label font-bold text-orange">
                    {formatPrice(pricing.price)} · {pricing.durationMinutes} min
                  </p>
                )}
              </button>
            )
          })}
        </div>
      </div>

      {addonServices.length > 0 && (
        <div className="mb-8">
          <h2 className="mb-4 text-h3 font-display text-navy">3. Adicionais (Opcional)</h2>
          <div className="grid grid-cols-1 gap-3">
            {addonServices.map((service) => {
              const pricing = priceForPorte(service, porte)
              const isChecked = addonIds.includes(service.id)
              return (
                <label
                  key={service.id}
                  className="flex cursor-pointer items-center gap-3 rounded-md border border-outline p-3 transition-colors hover:bg-hover"
                >
                  <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => onToggleAddon(service.id)}
                    className="size-5 rounded border-outline text-orange focus:ring-orange"
                  />
                  <div className="flex-1">
                    <p className="text-sm font-medium text-ink">{service.nome}</p>
                    {pricing && (
                      <p className="text-[10px] text-ink-muted">
                        + {formatPrice(pricing.price)}
                        {pricing.durationMinutes > 0 ? ` · + ${pricing.durationMinutes} min` : ''}
                      </p>
                    )}
                  </div>
                </label>
              )
            })}
          </div>
        </div>
      )}

      <ComboSummary
        totalPrice={totalPrice}
        totalDurationMinutes={totalDurationMinutes}
        ctaLabel="Continuar"
        ctaDisabled={baseServiceId === null}
        onCta={onContinue}
      />
    </div>
  )
}
