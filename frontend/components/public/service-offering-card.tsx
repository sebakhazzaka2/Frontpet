import Link from 'next/link'
import { PawPrint, Timer } from 'lucide-react'
import type { ServiceOfferingDetail } from '@/lib/api/services'
import { formatPrice } from '@/lib/utils'

/**
 * Card de serviço para `/servicos` (tarea 6.B, issue #59). Distinta de
 * `<ServiceCard>` (preview estática de la landing, ADR 017, datos de
 * `lib/data/services.ts`) a propósito: acá el dato es real
 * (`ServiceOfferingDetail`), donde precio/duração varían por porte — no hay
 * un valor fijo único como pide `<ServiceCard>` — y no hay `features`
 * inventadas ni badge "mais procurado" sin dato que lo sustente (mismo
 * criterio que descartar `avaliacao`/`maisVendido` en el catálogo real,
 * CLAUDE.md §6).
 *
 * `variant="compacto"` (adicionais): sin foto, sin CTA propio — un adicional
 * nunca se agenda solo, se suma a un banho base al elegir el combo en el
 * wizard (ADR 011), así que un botón "Agendar" acá sería engañoso.
 */
export function ServiceOfferingCard({
  service,
  variant = 'destaque',
}: {
  service: ServiceOfferingDetail
  variant?: 'destaque' | 'compacto'
}) {
  const porteP = service.pricing.find((p) => p.size === 'P')

  if (variant === 'compacto') {
    return (
      <article className="flex flex-col justify-between rounded-lg border border-outline bg-surface-card p-5 shadow-card">
        <div>
          <div className="mb-1 flex items-start justify-between gap-3">
            <h3 className="text-label font-semibold text-ink">{service.nome}</h3>
            {porteP && (
              <p className="shrink-0 text-label font-semibold text-orange">
                + {formatPrice(porteP.price)}
              </p>
            )}
          </div>
          <p className="text-caption text-ink-muted">{service.descricao}</p>
        </div>
        {porteP && (
          <p className="mt-3 flex items-center gap-1 text-caption text-ink-muted">
            <Timer className="size-[14px]" aria-hidden />A partir de {porteP.durationMinutes}min
          </p>
        )}
      </article>
    )
  }

  return (
    <article className="flex flex-col overflow-hidden rounded-lg border border-outline bg-surface-card shadow-card md:flex-row">
      <div className="relative h-48 shrink-0 bg-surface md:h-auto md:w-1/3">
        {/* Sem foto real do serviço ainda — mesmo placeholder de <ServiceCard>/<ProductCard>. */}
        <div className="flex h-full w-full items-center justify-center">
          <PawPrint className="size-10 text-outline" aria-hidden />
        </div>
      </div>

      <div className="flex flex-1 flex-col justify-between p-6">
        <div>
          <div className="mb-2 flex items-start justify-between gap-4">
            <h3 className="text-h3 text-ink">{service.nome}</h3>
            {porteP && (
              <div className="shrink-0 text-right">
                <p className="text-h3 text-orange">A partir de {formatPrice(porteP.price)}</p>
                <p className="text-caption text-ink-muted">Porte P</p>
              </div>
            )}
          </div>
          <p className="text-sm text-ink-muted">{service.descricao}</p>
        </div>

        <div className="mt-4 flex items-center justify-between border-t border-outline pt-4">
          {porteP ? (
            <span className="flex items-center gap-1 text-caption text-ink-muted">
              <Timer className="size-[18px]" aria-hidden />A partir de {porteP.durationMinutes}min
            </span>
          ) : (
            <span />
          )}
          <Link
            href="/agendamento"
            className="rounded-md bg-orange px-6 py-2 text-label font-semibold text-white transition-transform active:scale-95"
          >
            Agendar
          </Link>
        </div>
      </div>
    </article>
  )
}
