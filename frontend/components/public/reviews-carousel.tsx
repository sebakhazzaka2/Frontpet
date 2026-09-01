'use client'

// 'use client' mínimo: solo el shell del scroll — las <ReviewCard> se
// renderizan en el servidor y viajan como children (payload RSC), ningún
// dato de review se serializa al cliente. Extiende el patrón de
// booking/date-strip.tsx (overflow-x-auto + shrink-0 + scrollbar oculta)
// agregando scroll-snap. Sin librería (embla/swiper): CLAUDE.md §6.

import { useRef, type ReactNode } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'

interface ReviewsCarouselProps {
  children: ReactNode
  showControls: boolean
}

export function ReviewsCarousel({ children, showControls }: ReviewsCarouselProps) {
  const trackRef = useRef<HTMLUListElement>(null)

  function scrollByCard(direction: 1 | -1) {
    const track = trackRef.current
    if (!track) return

    // Mide el paso del DOM en vez de hardcodear px por breakpoint: los
    // anchos de card son porcentuales y cambian con el viewport.
    const firstCard = track.firstElementChild as HTMLElement | null
    const step = firstCard ? firstCard.offsetWidth + 16 : track.clientWidth * 0.8
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

    track.scrollBy({ left: direction * step, behavior: reduceMotion ? 'auto' : 'smooth' })
  }

  return (
    <div className="mt-8">
      <ul
        ref={trackRef}
        tabIndex={0}
        aria-label="Avaliações de clientes no Google"
        className="-mx-6 flex snap-x snap-mandatory gap-4 overflow-x-auto scroll-px-6 px-6 pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden md:gap-6 lg:-mx-8 lg:scroll-px-8 lg:px-8"
      >
        {children}
      </ul>

      {showControls && (
        <div className="mt-6 hidden justify-end gap-3 md:flex">
          <button
            type="button"
            onClick={() => scrollByCard(-1)}
            aria-label="Ver avaliações anteriores"
            className="flex size-11 items-center justify-center rounded-full border border-white/25 text-white transition-colors hover:bg-white/10"
          >
            <ChevronLeft className="size-5" aria-hidden />
          </button>
          <button
            type="button"
            onClick={() => scrollByCard(1)}
            aria-label="Ver mais avaliações"
            className="flex size-11 items-center justify-center rounded-full border border-white/25 text-white transition-colors hover:bg-white/10"
          >
            <ChevronRight className="size-5" aria-hidden />
          </button>
        </div>
      )}
    </div>
  )
}
