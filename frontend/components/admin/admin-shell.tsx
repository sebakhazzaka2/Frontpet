'use client'

import Image from 'next/image'
import { usePathname } from 'next/navigation'
import { useState } from 'react'
import { Menu } from 'lucide-react'
import { AdminSidebar, NAV_ITEMS } from '@/components/admin/admin-sidebar'
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import logoHorizontal from '@/public/brand/frontpet-logo-horizontal.png'

interface AdminShellProps {
  children: React.ReactNode
}

// Título de la sección activa para el breadcrumb del topbar desktop. '/admin'
// exacto (si no, matchearía todo por startsWith vacío); el resto por prefijo
// para cubrir subrutas (ej. /admin/produtos/[id]).
function useActiveSectionLabel() {
  const pathname = usePathname()
  const match = NAV_ITEMS.find(({ href }) =>
    href === '/admin' ? pathname === '/admin' : pathname.startsWith(href)
  )
  return match?.label ?? 'Dashboard'
}

// Tarea 4.11 (issue #32) — CLAUDE.md §6: admin desktop-first, sin bottom tab
// bar/FAB/swipe. Desktop: sidebar fijo (md:block). Mobile: header + hamburger
// que abre un drawer (Sheet lateral, ya instalado en el Bloque 0) — nunca la
// bottom nav del sitio público. Sheet sin radio (flush al borde izquierdo,
// igual criterio que un CartDrawer: un panel de borde a borde no tiene
// esquina que redondear, a diferencia de un bottom sheet).
export function AdminShell({ children }: AdminShellProps) {
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const activeSection = useActiveSectionLabel()

  return (
    <div className="min-h-screen bg-surface">
      <aside className="fixed inset-y-0 left-0 hidden w-64 md:block">
        <AdminSidebar />
      </aside>

      <header className="sticky top-0 z-40 flex h-16 items-center justify-between bg-navy px-4 text-white md:hidden">
        <Image src={logoHorizontal} alt="FrontPet Admin" className="h-8 w-auto object-contain" sizes="140px" priority />
        <Sheet open={mobileNavOpen} onOpenChange={setMobileNavOpen}>
          <SheetTrigger asChild>
            <button type="button" aria-label="Abrir menu" className="p-2">
              <Menu className="size-6" />
            </button>
          </SheetTrigger>
          <SheetContent side="left" className="w-64 border-none bg-navy">
            {/* sr-only: Radix exige un título accesible en todo Dialog/Sheet
                (encontrado con Playwright — warning real, no cosmético). No
                hay título visible en el mock de Stitch para el drawer mobile. */}
            <SheetTitle className="sr-only">Menu de navegação</SheetTitle>
            <AdminSidebar onNavigate={() => setMobileNavOpen(false)} />
          </SheetContent>
        </Sheet>
      </header>

      {/* Topbar desktop: antes el <h1> de cada página flotaba solo contra
          bg-surface sin ninguna jerarquía arriba (feedback visual
          2026-08-31) — el público siempre tiene esa estructura vía <Nav>.
          Solo breadcrumb por ahora: no hay ninguna acción comercial real sin
          dueño para un CTA acá (los "+ Novo X" ya viven, navy, en cada
          página — ver ADR pendiente sobre semántica de orange). */}
      <div className="hidden h-14 items-center border-b border-outline/30 bg-surface-card px-6 md:ml-64 md:flex">
        <span className="text-caption text-ink-muted">
          Admin / <span className="font-medium text-ink">{activeSection}</span>
        </span>
      </div>

      <main className="p-6 md:ml-64">{children}</main>
    </div>
  )
}
