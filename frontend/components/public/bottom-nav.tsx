'use client'

// 'use client': usePathname para marcar el item activo.

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { Calendar, Home, ShoppingBag } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { CartButton } from '@/components/public/cart-button'
import { buildWhatsAppLink } from '@/lib/data/site'

const WHATSAPP_LINK = buildWhatsAppLink('Olá! Gostaria de mais informações.')

const NAV_ITEMS = [
  { href: '/', label: 'Início', icon: Home },
  { href: '/produtos', label: 'Catálogo', icon: ShoppingBag },
  { href: '/agendamento', label: 'Agendar', icon: Calendar },
] as const

/**
 * Nav inferior mobile-only. Decisión 2026-07-17 (docs/port-landing-stitch.md
 * §5.8): el carrito y el WhatsApp flotantes de Stitch —que en el mockup
 * original flotan como FABs separados encima de esta barra— se integran
 * *dentro* de la barra en vez de flotar. Stitch los superpone (colisión a
 * 320px); acá son dos ítems más de la misma barra.
 *
 * Por eso el carrito no vive en un botón flotante en mobile (sí en el header
 * de escritorio, `Nav`) y por eso `FloatingWA` (tarea 2.7) solo se muestra en
 * desktop — en mobile, el acceso a WhatsApp es este último ítem.
 */
export function BottomNav() {
  const pathname = usePathname()

  return (
    <nav
      className="fixed inset-x-0 bottom-0 z-50 flex items-center justify-around border-t border-outline bg-surface-card py-2 shadow-card-hover md:hidden"
      style={{ paddingBottom: 'env(safe-area-inset-bottom, 8px)' }}
    >
      {NAV_ITEMS.map(({ href, label, icon: Icon }) => {
        const isActive = pathname === href
        return (
          <Link
            key={href}
            href={href}
            className={
              isActive
                ? 'flex flex-col items-center justify-center gap-0.5 rounded-full bg-orange px-4 py-1.5 text-white'
                : 'flex w-14 flex-col items-center justify-center gap-0.5 text-ink-muted'
            }
          >
            <Icon className="size-5" />
            <span className="text-caption">{label}</span>
          </Link>
        )
      })}

      <CartButton variant="bottom-nav" />

      <a
        href={WHATSAPP_LINK}
        target="_blank"
        rel="noopener noreferrer"
        className="flex w-14 flex-col items-center justify-center gap-0.5 text-wa"
      >
        <WhatsAppIcon className="size-5" />
        <span className="text-caption">WhatsApp</span>
      </a>
    </nav>
  )
}
