'use client'

// 'use client': `dynamic(..., { ssr: false })` solo se puede llamar desde un
// límite de cliente — Next lo rechaza si <Hero> (Server Component) lo llama
// directo. Este wrapper existe únicamente para eso.

import dynamic from 'next/dynamic'

const HeroFloatingCards = dynamic(
  () => import('@/components/public/hero-floating-cards').then((mod) => mod.HeroFloatingCards),
  { ssr: false }
)

interface HeroFloatingCardsLoaderProps {
  rating: number | null
  userRatingCount: number | null
}

export function HeroFloatingCardsLoader({ rating, userRatingCount }: HeroFloatingCardsLoaderProps) {
  return <HeroFloatingCards rating={rating} userRatingCount={userRatingCount} />
}
