/**
 * app/(public)/layout.tsx
 *
 * Layout para todas las rutas públicas: home, catálogo, servicios, turnos.
 * Agrega la Nav y el Footer que no deben aparecer en el panel admin.
 *
 * Estructura final de rutas:
 *   app/
 *     (public)/
 *       layout.tsx     ← este archivo
 *       page.tsx       ← home
 *       productos/
 *       servicios/
 *       turnos/
 *     (admin)/
 *       layout.tsx     ← layout admin (diferente nav)
 *       dashboard/
 */

import Link from 'next/link'
import { waLink } from '@/lib/data'

// ─────────────────────────────────────────────────────────────────────────────
// NAV LINKS
// ─────────────────────────────────────────────────────────────────────────────

const NAV_LINKS = [
  { label: 'Inicio',    href: '/'          },
  { label: 'Productos', href: '/productos'  },
  { label: 'Servicios', href: '/servicios'  },
  { label: 'Turnos',    href: '/turnos'     },
] as const

const FOOTER_LINKS = [
  { label: 'Inicio',    href: '/'          },
  { label: 'Productos', href: '/productos'  },
  { label: 'Servicios', href: '/servicios'  },
  { label: 'Turnos',    href: '/turnos'     },
]

// ─────────────────────────────────────────────────────────────────────────────
// LAYOUT
// ─────────────────────────────────────────────────────────────────────────────

export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <Nav />
      <main className="flex-1">{children}</main>
      <Footer />
    </div>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// NAV
// Sticky + backdrop blur al hacer scroll (el estado de scroll requiere
// 'use client'. Solución: extraer a components/public/nav.tsx en Sprint 3
// cuando necesites agregar el estado activo del link actual con usePathname.)
//
// Por ahora: Server Component estático, funcional y responsive.
// ─────────────────────────────────────────────────────────────────────────────

function Nav() {
  const waHello = waLink('Hola FrontPet! 🐾')

  return (
    <header className="sticky top-0 z-50 border-b border-stone-200 bg-white/95 backdrop-blur-sm">
      <div className="container-main flex h-16 items-center justify-between gap-4">

        {/* Logo */}
        <Link
          href="/"
          className="flex items-center gap-2.5 font-semibold text-stone-900"
          aria-label="FrontPet — ir al inicio"
        >
          <span
            className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-500 text-lg shadow-brand"
            aria-hidden="true"
          >
            🐾
          </span>
          <span className="text-lg">
            Front<span className="text-brand-500">Pet</span>
          </span>
        </Link>

        {/* Links — ocultos en mobile */}
        <nav className="hidden items-center gap-1 md:flex" aria-label="Navegación principal">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className="rounded-full px-4 py-2 text-sm font-medium text-stone-600 transition-colors hover:bg-brand-50 hover:text-brand-600"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        {/* Actions */}
        <div className="flex items-center gap-2">
          <a
            href={waHello}
            target="_blank"
            rel="noopener noreferrer"
            className="hidden items-center gap-2 rounded-full bg-wa px-4 py-2 text-sm font-semibold text-white shadow-wa transition-all hover:bg-wa-dark hover:shadow-wa-lg sm:flex"
          >
            💬 WhatsApp
          </a>

          {/* Admin — solo visible en dev o con query param ?admin=1 en producción */}
          {/* TODO Sprint 4: reemplazar por link al login real */}
          <Link
            href="/admin"
            className="flex h-9 w-9 items-center justify-center rounded-lg bg-stone-100 text-base text-stone-500 transition-colors hover:bg-stone-200"
            aria-label="Panel administrativo"
            title="Admin"
          >
            ⚙️
          </Link>

          {/* Hamburger mobile — TODO Sprint 3: extraer a MobileNav client component */}
          {/* Por ahora el menú mobile se omite; se agrega con shadcn/ui Sheet en Sprint 3 */}
        </div>
      </div>
    </header>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// FOOTER
// ─────────────────────────────────────────────────────────────────────────────

function Footer() {
  const waHello = waLink('Hola FrontPet! 🐾')
  const year = new Date().getFullYear()

  return (
    <footer className="bg-stone-900 text-white">
      <div className="container-main py-12">

        <div className="grid grid-cols-1 gap-10 sm:grid-cols-2 lg:grid-cols-3">

          {/* Brand */}
          <div>
            <div className="mb-4 flex items-center gap-2.5">
              <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-500 text-lg">
                🐾
              </span>
              <span className="text-lg font-semibold">
                Front<span className="text-brand-500">Pet</span>
              </span>
            </div>
            <p className="mb-5 max-w-xs text-sm leading-relaxed text-stone-400">
              Tu tienda de mascotas de confianza en Pehuajó. Productos premium,
              grooming profesional y la mejor atención.
            </p>
            {/* Social */}
            <div className="flex gap-2">
              {/* TODO: reemplazar href con links reales a RRSS */}
              <a
                href="#"
                aria-label="Facebook"
                className="flex h-9 w-9 items-center justify-center rounded-lg bg-stone-800 text-base transition-colors hover:bg-stone-700"
              >
                📘
              </a>
              <a
                href="#"
                aria-label="Instagram"
                className="flex h-9 w-9 items-center justify-center rounded-lg bg-stone-800 text-base transition-colors hover:bg-stone-700"
              >
                📸
              </a>
              <a
                href={waHello}
                target="_blank"
                rel="noopener noreferrer"
                aria-label="WhatsApp"
                className="flex h-9 w-9 items-center justify-center rounded-lg bg-wa text-base transition-colors hover:bg-wa-dark"
              >
                💬
              </a>
            </div>
          </div>

          {/* Nav links */}
          <div>
            <h3 className="mb-4 text-sm font-semibold text-stone-200">Navegación</h3>
            <ul className="space-y-2">
              {FOOTER_LINKS.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-stone-400 transition-colors hover:text-white"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          {/* Contacto */}
          <div>
            <h3 className="mb-4 text-sm font-semibold text-stone-200">Contacto</h3>
            <ul className="space-y-3 text-sm text-stone-400">
              <li className="flex items-start gap-2">
                <span aria-hidden="true">📍</span>
                <span>Pehuajó, Buenos Aires</span>
              </li>
              <li className="flex items-start gap-2">
                <span aria-hidden="true">🕐</span>
                <span>Lun–Sáb · 9:00 a 18:00</span>
              </li>
              <li className="flex items-start gap-2">
                <span aria-hidden="true">💬</span>
                <a
                  href={waHello}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="transition-colors hover:text-wa"
                >
                  WhatsApp directo
                </a>
              </li>
            </ul>
          </div>

        </div>

        {/* Bottom bar */}
        <div className="mt-10 flex flex-col items-center justify-between gap-2 border-t border-stone-800 pt-6 text-xs text-stone-500 sm:flex-row">
          <span>© {year} FrontPet. Todos los derechos reservados.</span>
          <span>Desarrollado para las mascotas de Pehuajó 🐾</span>
        </div>

      </div>
    </footer>
  )
}
