import { Nav } from '@/components/public/nav'

/**
 * Layout de la web pública. Nav acá, no en el root layout (ADR 006 #1) —
 * el admin tiene su propio layout independiente, sin condicionales de ruta.
 *
 * Footer entra en la tarea 2.8 (Sprint 2), todavía no construido.
 */

interface PublicLayoutProps {
  children: React.ReactNode
}

export default function PublicLayout({ children }: PublicLayoutProps) {
  return (
    <>
      <Nav />
      {/* pt-16: el Nav es fixed (h-16); sin esto el contenido queda tapado. */}
      <main className="pt-16">{children}</main>
    </>
  )
}
