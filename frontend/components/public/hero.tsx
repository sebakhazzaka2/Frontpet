import Link from 'next/link'
import { ArrowRight, Calendar } from 'lucide-react'
import { HeroFloatingCardsLoader } from '@/components/public/hero-floating-cards-loader'
import { getPlaceSummary } from '@/lib/google-places'

// TODO: reemplazar por la foto real del cliente cuando la provea. Stitch usa una
// URL temporal de Google (aida-public) que no es nuestra y puede dejar de existir
// — no se hotlinkea. Gradiente navy como placeholder mientras tanto.

// Server Component a propósito: el h1/p de acá es el elemento LCP de la
// página (confirmado con Lighthouse). El `await` de acá se resuelve en
// build/revalidate (ISR) — no mete 'use client' en el árbol del LCP, que es
// lo que esa decisión protege. Ver <HeroFloatingCards> para la parte que sí
// anima (decorativa, desktop-only, no es el LCP).
export async function Hero() {
  const place = await getPlaceSummary()
  return (
    <section className="relative h-[560px] w-full overflow-hidden bg-gradient-to-br from-navy to-navy-dark md:h-[600px] lg:h-[720px]">
      <div className="absolute inset-0 bg-gradient-to-r from-ink/78 via-ink/30 to-transparent" />

      <div className="relative flex h-full max-w-[560px] flex-col justify-center px-6 lg:pl-20">
        <div className="mb-5 inline-flex w-fit items-center rounded-full border border-white/20 bg-white/12 px-3 py-1.5 backdrop-blur-sm">
          <span className="text-eyebrow uppercase tracking-wider text-white">
            🐾 Petshop · Santana do Livramento
          </span>
        </div>

        <h1 className="text-hero-mobile font-display text-white md:text-hero-tablet lg:text-hero">
          {/* nbsp entre "o" y "nosso": sin esto, "o" queda huérfano al final de
              línea en desktop — con Fredoka a 64px una "o" redonda sola casi
              se lee como un ícono en vez de una palabra (visto en captura real). */}
          Seu pet merece o{' '}
          <span className="text-orange">nosso melhor.</span>
        </h1>

        <p className="mt-5 max-w-[480px] text-base text-white/85 lg:text-lg">
          Banho, tosa e produtos premium para o seu xodó. Agende ou peça pelo WhatsApp em
          segundos — sem fila.
        </p>

        <div className="mt-9 flex flex-wrap gap-3">
          <Link
            href="/agendamento"
            className="flex h-[52px] items-center gap-2 rounded-md bg-orange px-6 text-[15px] font-medium text-white shadow-[0_8px_20px_rgba(244,100,13,0.3)] transition-all hover:brightness-90 active:scale-95"
          >
            <Calendar className="size-5" />
            Agendar horário
          </Link>
          <Link
            href="/produtos"
            className="flex h-[52px] items-center gap-2 rounded-md border border-white/30 bg-white/12 px-6 text-[15px] font-medium text-white backdrop-blur-sm transition-all hover:bg-white/20 active:scale-95"
          >
            Ver produtos
            <ArrowRight className="size-5" />
          </Link>
        </div>
      </div>

      {/* Social proof flotante: solo desktop (lg:+). A AC de la tarea 2.2:
          oculto en mobile. Rating real del Google Business (ADR 025), no
          inventado. Cargado con next/dynamic (ssr:false, ver el loader) para
          que su JS no compita con el LCP en el primer paint — no aporta SEO
          y en mobile ni siquiera se muestra. */}
      <HeroFloatingCardsLoader rating={place.rating} userRatingCount={place.userRatingCount} />
    </section>
  )
}
