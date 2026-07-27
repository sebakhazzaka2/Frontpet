import { Nav } from '@/components/public/nav'
import { Footer } from '@/components/public/footer'
import { BottomNav } from '@/components/public/bottom-nav'
import { FloatingWA } from '@/components/public/floating-wa'

/**
 * Layout de la web pública. Nav y Footer acá, no en el root layout (ADR 006 #1) —
 * el admin tiene su propio layout independiente, sin condicionales de ruta.
 *
 * FloatingWA acá y no en page.tsx: el ADR 006 #3 lo había dejado en page.tsx
 * con un TODO para moverlo cuando existiera un MobileNav client component —
 * BottomNav (tarea 2.0d) es exactamente eso, así que el TODO se resuelve
 * directo acá en vez de en Sprint 3.
 */

interface PublicLayoutProps {
  children: React.ReactNode
}

export default function PublicLayout({ children }: PublicLayoutProps) {
  return (
    <>
      <Nav />
      {/* pt-16: el Nav es fixed (h-16). pb-20 mobile: BottomNav es fixed
          también — sin esto el final de cada página queda tapado. md:pb-0
          porque BottomNav se oculta desde ese breakpoint. */}
      <main className="pt-16 pb-20 md:pb-0">{children}</main>
      <Footer />
      <BottomNav />
      <FloatingWA />
    </>
  )
}
