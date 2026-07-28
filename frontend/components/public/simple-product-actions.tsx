'use client'

import { useState } from 'react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { AddToCartButton } from '@/components/public/add-to-cart-button'
import { QuantityStepper } from '@/components/public/quantity-stepper'
import { buildWhatsAppLink } from '@/lib/data/site'
import { formatPrice } from '@/lib/utils'

interface SimpleProductActionsProps {
  productPublicId: string
  productSlug: string
  productNome: string
  productImageUrl?: string
  price: number
  stock: number
}

// Tarea 4.5 (issue #29): contraparte de <ProductVariantPicker> para productos
// SIN variantes — extraído a Client Component por el mismo motivo (el estado
// de cantidad vive en memoria, no en la URL). `product-detail.tsx` sigue
// siendo Server Component: solo esta franja de interactividad baja al cliente.
//
// `max={stock}`: el stepper no deja pedir más de lo que hay — pero el chequeo
// real de stock sigue siendo del backend al crear el pedido (Bloque C), esto
// es solo UX, no la fuente de verdad.
export function SimpleProductActions({
  productPublicId,
  productSlug,
  productNome,
  productImageUrl,
  price,
  stock,
}: SimpleProductActionsProps) {
  const [quantidade, setQuantidade] = useState(1)
  const inStock = stock > 0
  const whatsappHref = buildWhatsAppLink(
    `Olá! Tenho interesse no produto "${productNome}" (${formatPrice(price)}).`
  )

  return (
    <div className="flex flex-col gap-3">
      {inStock && (
        <QuantityStepper quantidade={quantidade} onChange={setQuantidade} max={stock} />
      )}

      {inStock ? (
        <AddToCartButton
          item={{ productPublicId, productSlug, productNome, productImageUrl, unitPrice: price }}
          quantidade={quantidade}
        />
      ) : (
        <button
          type="button"
          disabled
          className="flex h-12 cursor-not-allowed items-center justify-center rounded-md bg-outline/40 text-sm font-medium text-ink-muted"
        >
          Fora de estoque
        </button>
      )}

      <a
        href={whatsappHref}
        target="_blank"
        rel="noopener noreferrer"
        className="flex h-12 items-center justify-center gap-2 rounded-md bg-wa text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-95"
      >
        <WhatsAppIcon className="size-5" />
        Pedir pelo WhatsApp
      </a>
    </div>
  )
}
