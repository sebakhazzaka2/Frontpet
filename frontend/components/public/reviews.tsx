import { SectionHeading } from '@/components/public/section-heading'
import { GoogleRatingBadge } from '@/components/public/google-rating-badge'
import { ReviewsCarousel } from '@/components/public/reviews-carousel'
import { ReviewCard } from '@/components/public/review-card'
import { getPlaceSummary } from '@/lib/google-places'

// Reviews reales del Google Business de FrontPet (ADR 025). Sin fallback a
// testimonios inventados: si la API falla o no hay reviews, se muestra el
// header + el badge de rating (si hay datos) sin cards, nunca contenido
// falso — ver docs/pending-decisions.md §15.
export async function Reviews() {
  const place = await getPlaceSummary()

  return (
    <section className="bg-navy px-6 py-16 lg:px-8">
      <div className="mx-auto max-w-content">
        <div className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
          <SectionHeading
            tone="dark"
            eyebrow="03 — Quem já confiou"
            titulo="Histórias de Tutores"
            descricao="Avaliações reais de tutores no Google."
          />

          {place.rating !== null && place.userRatingCount !== null && (
            <GoogleRatingBadge
              rating={place.rating}
              userRatingCount={place.userRatingCount}
              googleMapsUri={place.googleMapsUri}
            />
          )}
        </div>

        {place.reviews.length > 0 ? (
          <ReviewsCarousel showControls={place.reviews.length > 3}>
            {place.reviews.map((review) => (
              <ReviewCard key={review.id} review={review} />
            ))}
          </ReviewsCarousel>
        ) : (
          place.googleMapsUri && (
            <a
              href={place.googleMapsUri}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-8 inline-block text-sm text-white/70 underline hover:text-white"
            >
              Ver todas as avaliações no Google →
            </a>
          )
        )}
      </div>
    </section>
  )
}
