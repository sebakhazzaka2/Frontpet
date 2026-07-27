import { Star } from 'lucide-react'
import { REVIEWS } from '@/lib/data/reviews'

// Fundo `navy` + cards `navy-mid` (AC da tarefa 2.6, issue #8) — o card
// precisa se despegar do fundo, ver a decisão do token em globals.css.
export function Reviews() {
  return (
    <section className="bg-navy px-6 py-16 lg:px-8">
      <div className="mx-auto max-w-content">
        <h2 className="text-center text-h2-mobile font-display text-white md:text-h2">
          Histórias de Tutores
        </h2>

        <div className="mt-10 grid gap-4 md:grid-cols-3 md:gap-6">
          {REVIEWS.map((review) => (
            <article key={review.id} className="flex flex-col gap-4 rounded-lg bg-navy-mid p-5">
              <div className="flex gap-1">
                <span className="sr-only">Avaliação: {review.avaliacao} de 5 estrelas</span>
                {Array.from({ length: review.avaliacao }).map((_, i) => (
                  <Star key={i} aria-hidden="true" className="size-4 fill-star text-star" />
                ))}
              </div>

              <p className="text-sm leading-relaxed text-white">&ldquo;{review.texto}&rdquo;</p>

              <div className="mt-auto flex items-center gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-white/15 text-sm font-semibold text-white">
                  {review.iniciais}
                </div>
                <div>
                  <p className="text-sm font-medium text-white">{review.nome}</p>
                  <p className="text-caption text-white/80">{review.relacao}</p>
                </div>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
