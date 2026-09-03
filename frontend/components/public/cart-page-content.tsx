'use client'

import { useEffect } from 'react'
import { useCart } from '@/hooks/use-cart'
import { CartLineItem } from '@/components/public/cart-line-item'
import { CartSummary } from '@/components/public/cart-summary'
import { CheckoutForm } from '@/components/public/checkout-form'
import { EmptyCart } from '@/components/public/empty-cart'
import { cartItemKey } from '@/lib/cart/types'
import { trackInitiateCheckout } from '@/lib/analytics/pixel'

// Extraído de app/(public)/carrinho/page.tsx (tarea 7.x, SEO) — un Server
// Component no puede exportar `metadata` desde un archivo 'use client', y
// todo este contenido depende de useCart() (sessionStorage), no de datos
// server-side. page.tsx queda como Server Component solo para el export de
// metadata; este componente conserva el resto tal cual estaba.
export function CartPageContent() {
  const { items, itemCount, subtotal } = useCart()

  useEffect(() => {
    if (items.length > 0) {
      trackInitiateCheckout({
        productPublicIds: items.map((item) => item.productPublicId),
        numItems: itemCount,
        value: subtotal,
      })
    }
    // Só quando a página monta com itens — não a cada mudança de quantidade.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (items.length === 0) {
    return <EmptyCart />
  }

  return (
    <div className="mx-auto flex max-w-content flex-col gap-6 px-6 py-8 lg:px-8 lg:py-12">
      <h1 className="text-h2-mobile font-display text-ink md:text-h2">Sua sacola</h1>

      <div className="flex flex-col gap-3">
        {items.map((item) => (
          <CartLineItem key={cartItemKey(item)} item={item} />
        ))}
      </div>

      <CartSummary />
      <CheckoutForm />
    </div>
  )
}
