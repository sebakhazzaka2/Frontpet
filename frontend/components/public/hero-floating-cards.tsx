'use client'

// 'use client': useReducedMotion necesita el cliente. Separado de <Hero> a
// propósito (ver ADR 006 #2) — el h1/p del Hero es el elemento LCP de la
// página; que todo el árbol dependa de 'use client' agregaba un delay de
// hidratación medible en Lighthouse incluso después de sacarles la animación
// a esos elementos. Estas cards son decorativas, desktop-only, no son el LCP.

import { domAnimation, LazyMotion, m, useReducedMotion } from 'framer-motion'
import { PawPrint, Star } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'

interface HeroFloatingCardsProps {
  rating: number | null
  userRatingCount: number | null
}

// LazyMotion + `m` en vez de `motion`: solo cargamos las features de
// animación que usamos acá (opacity/scale + transition), no drag ni layout
// animations. Es el único archivo de todo el proyecto que usa Framer Motion
// — encontrado con Lighthouse ("Forced reflow" + 813ms de Style & Layout),
// esto reduce el JS que el motor tiene que evaluar antes del primer paint.
export function HeroFloatingCards({ rating, userRatingCount }: HeroFloatingCardsProps) {
  const reduceMotion = useReducedMotion()
  const ratingDisplay = rating !== null ? rating.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) : null

  return (
    <LazyMotion features={domAnimation} strict>
      <div className="pointer-events-none absolute inset-0 hidden lg:block">
        {ratingDisplay && (
          <m.div
            initial={reduceMotion ? { opacity: 1 } : { opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4, delay: reduceMotion ? 0 : 1.6 }}
            className="absolute top-[15%] right-[10%] flex -rotate-2 items-center gap-3 rounded-lg bg-white p-4 shadow-card-hover"
          >
            <Star className="size-6 fill-star text-star" />
            <div>
              <p className="font-display leading-none text-ink">{ratingDisplay}</p>
              <p className="mt-1 text-caption leading-none text-ink-muted">
                {userRatingCount !== null ? `(${userRatingCount} avaliações)` : 'no Google'}
              </p>
            </div>
          </m.div>
        )}

        <m.div
          initial={reduceMotion ? { opacity: 1 } : { opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4, delay: reduceMotion ? 0 : 1.75 }}
          className="absolute top-[45%] right-[5%] flex rotate-1 items-center gap-3 rounded-lg bg-white p-4 shadow-card"
        >
          <div className="flex size-10 items-center justify-center rounded-full bg-orange/10">
            <PawPrint className="size-5 text-orange" />
          </div>
          <div>
            <p className="text-sm font-semibold leading-none text-ink">Banho & Tosa</p>
            <p className="mt-1 text-caption leading-none text-ink-muted">com hora marcada</p>
          </div>
        </m.div>

        <m.div
          initial={reduceMotion ? { opacity: 1 } : { opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4, delay: reduceMotion ? 0 : 1.9 }}
          className="absolute bottom-[20%] right-[12%] flex -rotate-1 items-center gap-3 rounded-lg bg-wa p-4 shadow-card-hover"
        >
          <WhatsAppIcon className="size-6 text-white" />
        </m.div>
      </div>
    </LazyMotion>
  )
}
