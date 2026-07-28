'use client'

import { useState } from 'react'
import { Menu } from 'lucide-react'
import { AdminSidebar } from '@/components/admin/admin-sidebar'
import { Sheet, SheetContent, SheetTitle, SheetTrigger } from '@/components/ui/sheet'

interface AdminShellProps {
  children: React.ReactNode
}

// Tarea 4.11 (issue #32) — CLAUDE.md §6: admin desktop-first, sin bottom tab
// bar/FAB/swipe. Desktop: sidebar fijo (md:block). Mobile: header + hamburger
// que abre un drawer (Sheet lateral, ya instalado en el Bloque 0) — nunca la
// bottom nav del sitio público. Sheet sin radio (flush al borde izquierdo,
// igual criterio que un CartDrawer: un panel de borde a borde no tiene
// esquina que redondear, a diferencia de un bottom sheet).
export function AdminShell({ children }: AdminShellProps) {
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  return (
    <div className="min-h-screen bg-surface">
      <aside className="fixed inset-y-0 left-0 hidden w-64 md:block">
        <AdminSidebar />
      </aside>

      <header className="sticky top-0 z-40 flex h-16 items-center justify-between bg-navy px-4 text-white md:hidden">
        <span className="font-display text-h3 font-semibold">FrontPet Admin</span>
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

      <main className="p-6 md:ml-64">{children}</main>
    </div>
  )
}
