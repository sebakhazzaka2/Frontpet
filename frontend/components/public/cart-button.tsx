'use client'

import Link from 'next/link'
import { ShoppingCart } from 'lucide-react'
import { useCart } from '@/hooks/use-cart'

interface CartButtonProps {
  /** header: ícono suelto en el Nav desktop/mobile. bottom-nav: columna ícone+label. */
  variant?: 'header' | 'bottom-nav'
}

// Tarea 4.3 (issue #29): reemplaza los botones estáticos de carrinho en
// nav.tsx y bottom-nav.tsx — antes no había estado de carrito real
// (sessionStorage recién existe desde el Bloque 0, `useCart()`).
//
// `key={itemCount}` en el badge: al cambiar la cantidad, React desmonta y
// remonta el <span>, así la animación `zoom-in` (tw-animate-css) corre de
// nuevo en cada cambio — sin useState/useEffect extra solo para disparar un
// "pop". Ambos variants apuntan a /carrinho: el carrito es una página
// completa, no un drawer (decisión del plan de Sprint 4 — sigue el mock de
// Stitch "Sua Sacola", que integra lista + resumen + checkout en una vista).
export function CartButton({ variant = 'header' }: CartButtonProps) {
  const { itemCount } = useCart()

  const badge = itemCount > 0 && (
    <span
      key={itemCount}
      className="absolute -right-1 -top-1 flex size-4 animate-in zoom-in-50 items-center justify-center rounded-full bg-orange text-[10px] font-semibold text-white"
    >
      {itemCount > 9 ? '9+' : itemCount}
    </span>
  )

  if (variant === 'bottom-nav') {
    return (
      <Link
        href="/carrinho"
        className="relative flex w-14 flex-col items-center justify-center gap-0.5 text-ink-muted"
      >
        <ShoppingCart className="size-5" />
        {badge}
        <span className="text-caption">Carrinho</span>
      </Link>
    )
  }

  return (
    <Link href="/carrinho" className="relative p-2 text-white" aria-label="Carrinho">
      <ShoppingCart className="size-5" />
      {badge}
    </Link>
  )
}
