'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useState } from 'react'
import { Calendar, LayoutDashboard, LogOut, Package, Scissors, ShoppingBag } from 'lucide-react'
import { apiFetch } from '@/lib/api/client'

const NAV_ITEMS = [
  { href: '/admin', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/admin/produtos', label: 'Produtos', icon: Package },
  { href: '/admin/servicos', label: 'Serviços', icon: Scissors },
  { href: '/admin/pedidos', label: 'Pedidos', icon: ShoppingBag },
  { href: '/admin/agendamentos', label: 'Agendamentos', icon: Calendar },
] as const

interface AdminSidebarProps {
  onNavigate?: () => void
}

// Tarea 4.11 (issue #32) — contenido de navegación compartido entre el
// sidebar fijo de desktop y el drawer mobile (<AdminShell>), para que exista
// una sola fuente de verdad de los links. `onNavigate` cierra el drawer al
// clickear un link en mobile — no hace nada en desktop (no se pasa).
//
// Produtos/Pedidos ya son reales dentro de este mismo Sprint 4 (Bloques E/F);
// Serviços/Agendamentos quedan para Sprints 5-7 (ADR 011/booking). Todos
// linkean a su ruta real ya: mismo criterio que <BottomNav> linkeando a
// /agendamento antes de que existiera (Sprint 2) — un 404 transitorio durante
// el sprint es aceptable, no se bloquea el link.
export function AdminSidebar({ onNavigate }: AdminSidebarProps) {
  const pathname = usePathname()
  const [loggingOut, setLoggingOut] = useState(false)

  async function handleLogout() {
    setLoggingOut(true)
    try {
      await apiFetch('/auth/logout', { method: 'POST' })
    } finally {
      // Navegación completa (no router.push): el layout guard lee la cookie
      // en el server en cada request — un push de cliente podría no
      // re-evaluar el guard antes de mostrar contenido protegido.
      window.location.assign('/admin/login')
    }
  }

  return (
    <div className="flex h-full flex-col bg-navy text-white">
      <div className="flex h-16 items-center px-6">
        <span className="font-display text-h3 font-semibold">FrontPet Admin</span>
      </div>

      <nav className="flex flex-1 flex-col gap-1 px-3">
        {NAV_ITEMS.map(({ href, label, icon: Icon }) => {
          const isActive = pathname === href
          return (
            <Link
              key={href}
              href={href}
              onClick={onNavigate}
              className={
                isActive
                  ? 'flex items-center gap-3 rounded-md bg-white/10 px-3 py-2.5 text-sm font-medium text-white'
                  : 'flex items-center gap-3 rounded-md px-3 py-2.5 text-sm text-white/70 transition-colors hover:bg-white/5 hover:text-white'
              }
            >
              <Icon className="size-5 shrink-0" />
              {label}
            </Link>
          )
        })}
      </nav>

      <div className="border-t border-white/10 p-3">
        <button
          type="button"
          onClick={handleLogout}
          disabled={loggingOut}
          className="flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-sm text-white/70 transition-colors hover:bg-white/5 hover:text-white disabled:opacity-50"
        >
          <LogOut className="size-5 shrink-0" />
          {loggingOut ? 'Saindo...' : 'Sair'}
        </button>
      </div>
    </div>
  )
}
