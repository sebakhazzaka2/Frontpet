'use client'

// Dono do estado do wizard completo (Passos 1-3, Bloque C+D compartilham a
// mesma rama/estado — ver notas dos issues #60/#61). Estado de cliente: cada
// mudança de porte/serviço/adicional recalcula o resumo em vivo e invalida a
// grade de horários.
import { useMemo, useState } from 'react'
import type { Porte, ServiceOfferingDetail } from '@/lib/api/services'
import { useAvailability } from '@/lib/hooks/use-availability'
import { nextDays } from '@/lib/booking-dates'
import { Stepper } from './stepper'
import { ServiceStep } from './service-step'
import { DateStrip } from './date-strip'
import { SlotGrid } from './slot-grid'

interface BookingWizardProps {
  services: ServiceOfferingDetail[]
}

// 14 días de tira. La ventana real del tenant es `anticipacao_max_dias` (60
// en el seed) y no está expuesta en ningún endpoint público, así que la tira
// no la puede leer; 14 entra holgado dentro de los 60 y no arriesga ofrecer
// días que el backend rechace. Si el cliente pide ver más lejos, esto sale de
// una config real, no de subir el número a ojo.
const DATE_STRIP_DAYS = 14

export function BookingWizard({ services }: BookingWizardProps) {
  const [step, setStep] = useState<1 | 2 | 3>(1)
  const [porte, setPorte] = useState<Porte>('M')
  // Pre-seleccionado como en Stitch (el mock trae `checked` en el primer
  // banho base). Sin esto el resumo arranca en "R$ 0,00 · 0 min" con el CTA
  // apagado, que se lee como pantalla rota en vez de como paso pendiente.
  const [baseServiceId, setBaseServiceId] = useState<number | null>(
    () => services.find((s) => s.type === 'BASE')?.id ?? null,
  )
  const [addonIds, setAddonIds] = useState<number[]>([])
  const [selectedDate, setSelectedDate] = useState<string | null>(null)
  const [selectedSlot, setSelectedSlot] = useState<string | null>(null)

  const baseServices = useMemo(() => services.filter((s) => s.type === 'BASE'), [services])
  const addonServices = useMemo(() => services.filter((s) => s.type === 'ADDON'), [services])
  const dates = useMemo(() => nextDays(DATE_STRIP_DAYS), [])

  const { totalPrice, totalDurationMinutes } = useMemo(() => {
    const pricingFor = (id: number | null) => {
      if (id === null) return undefined
      const service = services.find((s) => s.id === id)
      return service?.pricing.find((p) => p.size === porte)
    }

    const base = pricingFor(baseServiceId)
    let price = base?.price ?? 0
    let duration = base?.durationMinutes ?? 0

    for (const addonId of addonIds) {
      const addon = pricingFor(addonId)
      if (addon) {
        price += addon.price
        duration += addon.durationMinutes
      }
    }

    return { totalPrice: price, totalDurationMinutes: duration }
  }, [services, baseServiceId, addonIds, porte])

  const availability = useAvailability({
    baseServiceId,
    porte,
    addonIds,
    data: selectedDate,
    enabled: step === 2,
  })

  // Cambiar el combo cambia la duración total, y con ella qué horarios entran
  // en el día — un slot elegido antes deja de significar lo mismo. Se limpia
  // en los handlers y no en un efecto: el efecto correría *después* de un
  // render en que el slot viejo todavía se ve seleccionado.
  function resetSlot() {
    setSelectedSlot(null)
  }

  function changePorte(next: Porte) {
    setPorte(next)
    resetSlot()
  }

  function changeBaseService(id: number) {
    setBaseServiceId(id)
    resetSlot()
  }

  function toggleAddon(id: number) {
    setAddonIds((current) => (current.includes(id) ? current.filter((addonId) => addonId !== id) : [...current, id]))
    resetSlot()
  }

  function changeDate(date: string) {
    setSelectedDate(date)
    resetSlot()
  }

  // Derivado, no un segundo estado: si la grilla se refresca (por foco de
  // ventana, por ejemplo) y alguien se quedó con el cupo mientras el usuario
  // pensaba, el slot elegido deja de estar disponible y "Próximo" se apaga
  // solo. Guardar un booleano aparte obligaría a re-sincronizarlo a mano.
  const isSelectedSlotAvailable =
    selectedSlot !== null && (availability.data?.slots.some((s) => s.horario === selectedSlot && s.disponivel) ?? false)

  return (
    <div className="rounded-lg border border-outline bg-surface-card p-6 shadow-card">
      <Stepper currentStep={step} />

      {step === 1 && (
        <ServiceStep
          baseServices={baseServices}
          addonServices={addonServices}
          porte={porte}
          onPorteChange={changePorte}
          baseServiceId={baseServiceId}
          onBaseServiceChange={changeBaseService}
          addonIds={addonIds}
          onToggleAddon={toggleAddon}
          totalPrice={totalPrice}
          totalDurationMinutes={totalDurationMinutes}
          onContinue={() => setStep(2)}
        />
      )}

      {step === 2 && (
        <div>
          <h2 className="mb-6 text-h3 font-display text-navy">Data e Horário</h2>

          <p className="mb-3 text-label text-ink-muted">Selecione o dia:</p>
          <DateStrip dates={dates} selectedDate={selectedDate} onSelectDate={changeDate} />

          <p className="mt-6 mb-3 text-label text-ink-muted">Horários disponíveis:</p>
          {selectedDate === null ? (
            <p className="rounded-lg border border-dashed border-outline p-6 text-center text-sm text-ink-muted">
              Escolha um dia acima para ver os horários.
            </p>
          ) : (
            <SlotGrid
              availability={availability.data}
              isLoading={availability.isLoading}
              isError={availability.isError}
              onRetry={availability.refetch}
              selectedSlot={selectedSlot}
              onSelectSlot={setSelectedSlot}
            />
          )}

          <div className="mt-8 flex gap-4">
            <button
              type="button"
              onClick={() => setStep(1)}
              className="flex-1 rounded-full border border-navy py-4 font-bold text-navy transition-transform active:scale-95"
            >
              Voltar
            </button>
            <button
              type="button"
              disabled={!isSelectedSlotAvailable}
              onClick={() => setStep(3)}
              className="flex-1 rounded-full bg-orange py-4 font-bold text-white shadow-md transition-transform active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
            >
              Próximo
            </button>
          </div>
        </div>
      )}

      {/* Passo 3 (dados do cliente + submit) e confirmação: Bloque D (#61). */}
    </div>
  )
}
