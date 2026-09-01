import Image from 'next/image'
import { Star } from 'lucide-react'
import type { GoogleReview } from '@/lib/google-places'

function iniciaisFrom(nome: string): string {
  return nome
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase())
    .join('')
}

interface ReviewCardProps {
  review: GoogleReview
}

// Card de review real de Google (ADR 025). min-h + mt-auto en vez de medir
// nada: las 5 reviews reales miden entre 18 y 161 caracteres, así que el
// problema no es truncar textos largos sino que los cortos no se vean vacíos.
export function ReviewCard({ review }: ReviewCardProps) {
  const iniciais = iniciaisFrom(review.authorName)

  return (
    <li className="w-[78%] shrink-0 snap-start md:w-[44%] lg:w-[31%]">
      <article className="flex h-full flex-col rounded-lg bg-navy-mid p-5">
        <div className="flex gap-1">
          <span className="sr-only">Avaliação: {review.rating} de 5 estrelas</span>
          {Array.from({ length: 5 }).map((_, i) => (
            <Star
              key={i}
              aria-hidden="true"
              className={i < review.rating ? 'size-4 fill-star text-star' : 'size-4 fill-white/20 text-white/20'}
            />
          ))}
        </div>

        <blockquote className="mt-4 min-h-[96px] text-sm leading-relaxed text-white line-clamp-6">
          {review.text}
        </blockquote>

        <footer className="mt-auto flex items-center gap-3 pt-5">
          {review.photoUri ? (
            <Image
              src={review.photoUri}
              alt=""
              width={40}
              height={40}
              className="size-10 shrink-0 rounded-full object-cover"
            />
          ) : (
            <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-white/15 text-sm font-semibold text-white">
              {iniciais}
            </div>
          )}

          <div className="min-w-0">
            {review.authorUri ? (
              <a href={review.authorUri} target="_blank" rel="noopener noreferrer" className="text-sm font-medium text-white hover:underline">
                {review.authorName}
                <span className="sr-only"> — avaliação no Google</span>
              </a>
            ) : (
              <p className="text-sm font-medium text-white">{review.authorName}</p>
            )}
            <p className="text-caption text-white/60">{review.relativeTime}</p>
          </div>

          {review.reviewUri && (
            <a
              href={review.reviewUri}
              target="_blank"
              rel="noopener noreferrer"
              className="ml-auto shrink-0 self-start whitespace-nowrap text-caption text-white/55 hover:text-white"
            >
              Ler no Google →
            </a>
          )}
        </footer>
      </article>
    </li>
  )
}
