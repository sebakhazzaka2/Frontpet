import { PawPrint, Star, Truck } from 'lucide-react'
import { getPlaceSummary } from '@/lib/google-places'

// Antes mostraba "4.9 Avaliação Média" y "500+ Pets Atendidos" inventados
// (docs/pending-decisions.md §15). Ahora lee del mismo getPlaceSummary() que
// <Reviews> — el fetch se dedupea, no es una segunda llamada a la API.
export async function TrustBar() {
  const place = await getPlaceSummary()

  const ratingDisplay =
    place.rating !== null
      ? place.rating.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
      : null

  const items = [
    {
      icon: Star,
      label: ratingDisplay ? `${ratingDisplay} de 5 no Google` : 'Avaliado no Google',
      filled: true,
    },
    {
      icon: PawPrint,
      label:
        place.userRatingCount !== null
          ? `${place.userRatingCount} avaliações reais`
          : 'Avaliações reais no Google',
      filled: false,
    },
    { icon: Truck, label: 'Atenção personalizada', filled: false },
  ] as const

  return (
    <div className="w-full bg-navy py-8">
      <div className="mx-auto flex max-w-content flex-wrap items-center justify-center gap-x-12 gap-y-6 px-4 md:justify-around md:px-6 lg:px-8">
        {items.map(({ icon: Icon, label, filled }) => (
          <div key={label} className="flex items-center gap-3">
            <Icon className={`size-7 shrink-0 text-orange ${filled ? 'fill-orange' : ''}`} />
            <span className="text-base font-semibold tracking-tight text-white">{label}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
