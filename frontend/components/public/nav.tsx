'use client'

// 'use client': el toggle del menú mobile necesita useState. Mismo criterio que
// ADR 006 #2 — Server Component hasta que haya interactividad real; acá ya la hay.

import Image from 'next/image'
import Link from 'next/link'
import { useState } from 'react'
import { Menu, ShoppingCart, X } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import logoHorizontal from '@/public/brand/frontpet-logo-horizontal.png'

// TODO: reemplazar por el WhatsApp real del tenant cuando exista fuente de datos
// (V5__seed_dev.sql tiene whatsapp_destino, pero el frontend no consume el
// backend todavía — Sprint 2 usa datos estáticos por diseño, ver ADR 017).
const WHATSAPP_LINK = 'https://wa.me/555596724124'

const NAV_LINKS = [
  { href: '/produtos', label: 'Catálogo' },
  { href: '/turnos', label: 'Agendar horário' },
  { href: '/admin/login', label: 'Login' },
] as const

export function Nav() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  return (
    <header className="fixed top-0 z-50 h-16 w-full border-b border-white/10 bg-navy">
      <div className="mx-auto flex h-full max-w-7xl items-center justify-between px-4 md:px-6 lg:px-8">
        {/* Logo real del cliente (jul/2026), recortado con sharp desde el PNG
            original: fondo navy #011e5a — coincide exacto con --color-navy, por
            eso el recorte funde sin bordes visibles contra el header. */}
        <Link href="/" className="flex h-full items-center py-3">
          <Image
            src={logoHorizontal}
            alt="FrontPet Petshop"
            className="h-full w-auto object-contain"
            priority
          />
        </Link>

        <div className="flex items-center gap-3">
          <nav className="hidden items-center gap-6 text-label text-white md:flex">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="transition-opacity hover:opacity-80"
              >
                {link.label}
              </Link>
            ))}
          </nav>

          <a
            href={WHATSAPP_LINK}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center justify-center rounded-full bg-wa p-2 text-white transition-opacity hover:opacity-90"
            aria-label="Falar pelo WhatsApp"
          >
            <WhatsAppIcon className="size-5" />
          </a>

          {/* Carrinho estático: sin badge de cantidad. No hay estado de carrito
              real hasta el Sprint 4 (sessionStorage) — un punto de notificación
              acá sería un dato inventado. */}
          <button type="button" className="p-2 text-white" aria-label="Carrinho">
            <ShoppingCart className="size-5" />
          </button>

          <button
            type="button"
            className="text-white md:hidden"
            aria-label={mobileMenuOpen ? 'Fechar menu' : 'Abrir menu'}
            aria-expanded={mobileMenuOpen}
            onClick={() => setMobileMenuOpen((open) => !open)}
          >
            {mobileMenuOpen ? <X className="size-6" /> : <Menu className="size-6" />}
          </button>
        </div>
      </div>

      {mobileMenuOpen && (
        <nav className="flex flex-col gap-1 border-t border-white/10 bg-navy px-4 py-3 md:hidden">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="rounded-md px-3 py-2 text-label text-white hover:bg-white/10"
              onClick={() => setMobileMenuOpen(false)}
            >
              {link.label}
            </Link>
          ))}
        </nav>
      )}
    </header>
  )
}
