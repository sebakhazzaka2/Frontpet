import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { buildWhatsAppLink } from '@/lib/data/site'

const WHATSAPP_LINK = buildWhatsAppLink('Olá! Gostaria de mais informações.')

/**
 * Solo desktop (md:+): en mobile, WhatsApp vive integrado en BottomNav
 * (tarea 2.0d) — dos botones flotantes de WhatsApp en la misma pantalla
 * sería redundante, y ahí es exactamente donde Stitch tenía la colisión que
 * la decisión 2026-07-17 resolvió (ver docs/port-landing-stitch.md §5.8).
 *
 * Server Component: el pulse es CSS puro (`animate-ping` de Tailwind), y
 * `motion-reduce:` es una variant nativa — no hace falta useReducedMotion ni
 * 'use client' para respetar el AC de prefers-reduced-motion.
 */
export function FloatingWA() {
  return (
    <a
      href={WHATSAPP_LINK}
      target="_blank"
      rel="noopener noreferrer"
      aria-label="Falar pelo WhatsApp"
      className="fixed bottom-6 right-6 z-50 hidden md:block"
    >
      <span className="absolute inset-0 rounded-full bg-wa opacity-75 motion-safe:animate-ping motion-reduce:hidden" />
      <span className="relative flex size-14 items-center justify-center rounded-full bg-wa text-white shadow-card-hover transition-transform active:scale-95">
        <WhatsAppIcon className="size-7" />
      </span>
    </a>
  )
}
