'use client'

import { useEffect } from 'react'
import { useCart } from '@/hooks/use-cart'
import { CartLineItem } from '@/components/public/cart-line-item'
import { CartSummary } from '@/components/public/cart-summary'
import { CheckoutForm } from '@/components/public/checkout-form'
import { EmptyCart } from '@/components/public/empty-cart'
import { cartItemKey } from '@/lib/cart/types'
import { trackInitiateCheckout } from '@/lib/analytics/pixel'

// Tarea 4.4/4.6/4.7 (issue #30) — carrito y checkout en una sola vista, no
// dos rutas (decisión del plan de Sprint 4: Stitch dibuja "Sua Sacola" como
// una sola pantalla con lista + resumen + form + CTA). No existe /checkout.
//
// 'use client' en la página entera: todo el contenido depende de
// useCart() (sessionStorage), no hay datos que fetchear server-side acá —
// a diferencia de /produtos, que sí Server-rendea su primera página.
export default function CarrinhoPage() {
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
