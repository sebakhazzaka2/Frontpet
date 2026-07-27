'use client'

// 'use client': Framer Motion necesita el cliente para las animaciones de entrada.

import Link from 'next/link'
import { motion, useReducedMotion } from 'framer-motion'
import { ArrowRight, Calendar, PawPrint, Star } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'

// TODO: reemplazar por la foto real del cliente cuando la provea. Stitch usa una
// URL temporal de Google (aida-public) que no es nuestra y puede dejar de existir
// — no se hotlinkea. Gradiente navy como placeholder mientras tanto.

export function Hero() {
  const reduceMotion = useReducedMotion()

  // Con prefers-reduced-motion, todo entra ya visible y sin desplazamiento —
  // la condición del AC de la tarea 2.2.
  const fadeUp = reduceMotion
    ? { initial: { opacity: 1, y: 0 }, animate: { opacity: 1, y: 0 } }
    : { initial: { opacity: 0, y: 16 }, animate: { opacity: 1, y: 0 } }

  return (
    <section className="relative h-[560px] w-full overflow-hidden bg-gradient-to-br from-navy to-navy-dark md:h-[600px] lg:h-[720px]">
      <div className="absolute inset-0 bg-gradient-to-r from-ink/78 via-ink/30 to-transparent" />

      <div className="relative flex h-full max-w-[560px] flex-col justify-center px-6 lg:pl-20">
        <motion.div
          {...fadeUp}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="mb-5 inline-flex w-fit items-center rounded-full border border-white/20 bg-white/12 px-3 py-1.5 backdrop-blur-sm"
        >
          <span className="text-eyebrow uppercase tracking-wider text-white">
            🐾 Petshop · Santana do Livramento
          </span>
        </motion.div>

        <motion.h1
          {...fadeUp}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="text-hero-mobile font-display text-white md:text-hero-tablet lg:text-hero"
        >
          {/* nbsp entre "o" y "nosso": sin esto, "o" queda huérfano al final de
              línea en desktop — con Fredoka a 64px una "o" redonda sola casi
              se lee como un ícono en vez de una palabra (visto en captura real). */}
          Seu pet merece o{' '}
          <span className="text-orange">nosso melhor.</span>
        </motion.h1>

        <motion.p
          {...fadeUp}
          transition={{ duration: 0.5, delay: 0.3 }}
          className="mt-5 max-w-[480px] text-base text-white/85 lg:text-lg"
        >
          Banho, tosa e produtos premium para o seu xodó. Agende ou peça pelo WhatsApp em
          segundos — sem fila.
        </motion.p>

        <motion.div
          {...fadeUp}
          transition={{ duration: 0.5, delay: 0.4 }}
          className="mt-9 flex flex-wrap gap-3"
        >
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
        </motion.div>
      </div>

      {/* Social proof flotante: solo desktop (lg:+). A AC de la tarea 2.2:
          oculto en mobile. Datos estáticos — Sprint 2 no tiene backend de
          reviews todavía (ver ADR 017). */}
      <div className="pointer-events-none absolute inset-0 hidden lg:block">
        <motion.div
          initial={reduceMotion ? { opacity: 1 } : { opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4, delay: reduceMotion ? 0 : 1.6 }}
          className="absolute top-[15%] right-[10%] flex -rotate-2 items-center gap-3 rounded-lg bg-white p-4 shadow-card-hover"
        >
          <Star className="size-6 fill-star text-star" />
          <div>
            <p className="font-display leading-none text-ink">4.9</p>
            <p className="mt-1 text-caption leading-none text-ink-muted">(327 avaliações)</p>
          </div>
        </motion.div>

        <motion.div
          initial={reduceMotion ? { opacity: 1 } : { opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4, delay: reduceMotion ? 0 : 1.75 }}
          className="absolute top-[45%] right-[5%] flex rotate-1 items-center gap-3 rounded-lg bg-white p-4 shadow-card"
        >
          <div className="flex size-10 items-center justify-center rounded-full bg-orange/10">
            <PawPrint className="size-5 text-orange" />
          </div>
          <div>
            <p className="text-sm font-semibold leading-none text-ink">500+ pets atendidos</p>
            <p className="mt-1 text-caption leading-none text-ink-muted">este ano</p>
          </div>
        </motion.div>

        <motion.div
          initial={reduceMotion ? { opacity: 1 } : { opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4, delay: reduceMotion ? 0 : 1.9 }}
          className="absolute bottom-[20%] right-[12%] flex -rotate-1 items-center gap-3 rounded-lg bg-wa p-4 shadow-card-hover"
        >
          <WhatsAppIcon className="size-6 text-white" />
        </motion.div>
      </div>
    </section>
  )
}
