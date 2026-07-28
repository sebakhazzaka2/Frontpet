'use client'

import { ShoppingBag } from 'lucide-react'
import { toast } from 'sonner'
import { useCart } from '@/hooks/use-cart'
import type { CartItem } from '@/lib/cart/types'
import { cn } from '@/lib/utils'

interface AddToCartButtonProps {
  item: Omit<CartItem, 'quantidade'>
  quantidade?: number
  label?: string
  className?: string
}

// Tarea 4.5 (issue #29): único punto de llamada a useCart().addItem — tanto
// desde la grilla (<ProductCard>, quantidade fija en 1) como desde el detalle
// y el variant-picker (quantidade controlada por <QuantityStepper>). El
// feedback visual es un toast de Sonner (Bloque 0), no un estado local del
// botón — funciona igual sin importar cuántas instancias haya en pantalla.
//
// Color orange siempre: CLAUDE.md §5 — "orange = ação comercial, nunca navy
// (sistema) ni wa (exclusivo WhatsApp)".
export function AddToCartButton({
  item,
  quantidade = 1,
  label = 'Adicionar à sacola',
  className,
}: AddToCartButtonProps) {
  const { addItem } = useCart()

  function handleClick() {
    addItem({ ...item, quantidade })
    toast.success(`${item.productNome} adicionado à sacola`)
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      className={cn(
        'flex h-10 items-center justify-center gap-2 rounded-md bg-orange text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-95',
        className
      )}
    >
      <ShoppingBag className="size-4" />
      {label}
    </button>
  )
}
