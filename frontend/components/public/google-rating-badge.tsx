import { Star } from 'lucide-react'
import { GoogleIcon } from '@/components/shared/google-icon'

interface GoogleRatingBadgeProps {
  rating: number
  userRatingCount: number
  googleMapsUri: string | null
}

// Atribución a Google requerida por el ToS de Places (ADR 025): mostrar la
// nota y linkear al listado, no solo las reviews individuales.
export function GoogleRatingBadge({ rating, userRatingCount, googleMapsUri }: GoogleRatingBadgeProps) {
  const ratingDisplay = rating.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })

  const content = (
    <>
      <GoogleIcon className="size-4" aria-hidden />
      <Star className="size-4 fill-star text-star" aria-hidden />
      <span className="font-display font-semibold text-white">{ratingDisplay}</span>
      <span className="text-white/40">·</span>
      <span className="text-white/75">{userRatingCount} avaliações no Google</span>
    </>
  )

  const className =
    'inline-flex items-center gap-2 self-start rounded-full border border-white/16 bg-white/8 px-4 py-2 text-sm'

  if (!googleMapsUri) {
    return <div className={className}>{content}</div>
  }

  return (
    <a href={googleMapsUri} target="_blank" rel="noopener noreferrer" className={`${className} transition-colors hover:bg-white/14`}>
      {content}
    </a>
  )
}
