'use client'

import Image from 'next/image'
import { PawPrint, X } from 'lucide-react'
import { QuantityStepper } from '@/components/public/quantity-stepper'
import { useCart } from '@/hooks/use-cart'
import { cartItemKey, type CartItem } from '@/lib/cart/types'
import { formatPrice } from '@/lib/utils'

interface CartLineItemProps {
  item: CartItem
}

// Port de "Sua Sacola" (f6c2082b36e84be291bc44714ef6784c) — item card, línea
// 149-171 del code.html. rounded-lg (card, Stitch dibuja rounded-xl=12px →
// nuestra escala lo sube a 16px, ADR 014 §6), thumbnail rounded-md (Stitch
// rounded-lg=8px → rounded-md).
export function CartLineItem({ item }: CartLineItemProps) {
  const { updateQuantity, removeItem } = useCart()
  const key = cartItemKey(item)

  return (
    <div className="flex gap-4 rounded-lg border border-outline/30 bg-surface-card p-4 shadow-card">
      <div className="relative size-24 shrink-0 overflow-hidden rounded-md bg-surface">
        {item.productImageUrl ? (
          <Image src={item.productImageUrl} alt="" fill className="object-cover" sizes="96px" />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <PawPrint className="size-8 text-outline" aria-hidden />
          </div>
        )}
      </div>

      <div className="flex flex-1 flex-col justify-between">
        <div className="flex items-start justify-between gap-2">
          <div>
            <h3 className="text-sm font-medium text-ink">{item.productNome}</h3>
            {item.variantNome && <p className="text-caption text-ink-muted">{item.variantNome}</p>}
          </div>
          <button
            type="button"
            onClick={() => removeItem(key)}
            aria-label="Remover item"
            className="shrink-0 text-ink-muted transition-opacity hover:opacity-70"
          >
            <X className="size-4" />
          </button>
        </div>

        <div className="flex items-end justify-between">
          <span className="text-h3 font-semibold text-orange">{formatPrice(item.unitPrice)}</span>
          <QuantityStepper
            quantidade={item.quantidade}
            onChange={(quantidade) => updateQuantity(key, quantidade)}
          />
        </div>
      </div>
    </div>
  )
}
