import Link from 'next/link'
import Image from 'next/image'
import { CheckCircle2, PawPrint, Timer } from 'lucide-react'

/**
 * Tipo local a esta card mientras Sprint 2 usa datos estáticos (ADR 017).
 * Cuando el catálogo de serviços tenga backend, esto se reemplaza por el DTO
 * real — la forma ya sigue el mismo shape que va a tener ese DTO.
 */
export interface ServiceCardData {
  numero: string
  nome: string
  /** Sin foto real del cliente todavía para servicios — placeholder si falta. */
  imagemUrl?: string
  imagemAlt: string
  preco: string
  precoLabel: string
  descricao: string
  features: string[]
  duracao?: string
  destaque?: string
}

export function ServiceCard({ data }: { data: ServiceCardData }) {
  const { numero, nome, imagemUrl, imagemAlt, preco, precoLabel, descricao, features, duracao, destaque } = data

  return (
    <article className="flex flex-col overflow-hidden rounded-lg border border-outline bg-surface-card shadow-card transition-all duration-300 hover:-translate-y-1 hover:shadow-card-hover">
      <div className="relative h-64 overflow-hidden bg-surface">
        {imagemUrl ? (
          <Image src={imagemUrl} alt={imagemAlt} fill className="object-cover" />
        ) : (
          // TODO: reemplazar por foto real del cliente. Sin stock inventado.
          <div className="flex h-full w-full items-center justify-center">
            <PawPrint className="size-12 text-outline" aria-hidden />
          </div>
        )}
        {destaque && (
          <span className="absolute top-4 left-4 rounded-full bg-orange px-3 py-1 text-caption font-semibold uppercase tracking-wider text-white">
            {destaque}
          </span>
        )}
      </div>

      <div className="relative flex flex-1 flex-col p-6">
        {/* Número decorativo grande, igual que en Stitch: 88px, 18% de opacidad,
            detrás del contenido (z-index natural, sin pointer-events). */}
        <span
          aria-hidden
          className="pointer-events-none absolute top-2 right-4 font-display text-[88px] leading-none text-orange opacity-[0.18]"
        >
          {numero}
        </span>

        <div className="relative z-10 mb-4 flex items-start justify-between">
          <h3 className="text-h3 text-ink">{nome}</h3>
          <div className="text-right">
            <p className="text-h3 text-orange">{preco}</p>
            <p className="text-caption text-ink-muted">{precoLabel}</p>
          </div>
        </div>

        <p className="relative z-10 mb-6 text-sm text-ink-muted">{descricao}</p>

        <div className="relative z-10 mb-auto grid grid-cols-1 gap-2">
          {features.map((feature) => (
            <div key={feature} className="flex items-center gap-2 text-label text-ink-muted">
              <CheckCircle2 className="size-4 shrink-0 text-orange" />
              {feature}
            </div>
          ))}
        </div>

        <div className="relative z-10 mt-6 flex items-center justify-between border-t border-outline pt-4">
          {duracao ? (
            <span className="flex items-center gap-1 text-caption text-ink-muted">
              <Timer className="size-[18px]" />
              {duracao}
            </span>
          ) : (
            <span />
          )}
          {/* Preview sin booking: el AC de la tarea 2.4 dice que el wizard real
              es Sprint 6. El destino /turnos ya existe como constante en Nav/Hero. */}
          <Link
            href="/turnos"
            className="rounded-md bg-orange px-6 py-2 text-label font-semibold text-white transition-transform active:scale-95"
          >
            Agendar
          </Link>
        </div>
      </div>
    </article>
  )
}
