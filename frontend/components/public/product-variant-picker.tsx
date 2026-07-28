'use client'

import { useState } from 'react'
import { CheckCircle2, Truck } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { AddToCartButton } from '@/components/public/add-to-cart-button'
import { QuantityStepper } from '@/components/public/quantity-stepper'
import { buildWhatsAppLink } from '@/lib/data/site'
import type { ProductVariant } from '@/lib/data/product-detail'
import { formatPrice } from '@/lib/utils'

interface ProductVariantPickerProps {
  variants: ProductVariant[]
  productPublicId: string
  productSlug: string
  productNome: string
  productImageUrl?: string
  descricao: string
}

// Tarea 3.9 (issue #19): selector de variante. Client Component porque
// precio/estoque/CTA dependen de una elección que vive en memoria, no en la
// URL (a diferencia del filtro de categoría de la 3.7, que sí es
// bookmarkeable a propósito) — no tiene sentido indexar /produtos/[slug] por
// tamaño elegido.
//
// El mock de Stitch ("Detalhe do Produto") no tiene selector de variante —
// muestra un SKU fijo. En vez de generar una pantalla nueva (riesgo de
// romper el patrón de radius/color ya resuelto, ADR 014), reutiliza el
// mismo par de estados activo/inactivo que ya validaron los chips de
// <CategoryFilter> (tarea 3.7) — decisión 2026-07-27.
//
// `descricao` entra como prop (no queda en el Server Component padre) porque
// el orden visual real es precio → estoque → descrição → CTA, y las tres
// primeras dependen de la misma selección — separarlas partiría el estado.
//
// Add-to-cart (issue #29, Bloque A): `quantidade` se resetea a 1 cada vez que
// cambia `selectedId` — cambiar de variante y arrastrar la cantidad de la
// anterior sería confuso (¿"3" es de la variante vieja o la nueva?).
export function ProductVariantPicker({
  variants,
  productPublicId,
  productSlug,
  productNome,
  productImageUrl,
  descricao,
}: ProductVariantPickerProps) {
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [quantidade, setQuantidade] = useState(1)
  const selected = variants.find((variant) => variant.id === selectedId)

  function selectVariant(id: number) {
    setSelectedId(id)
    setQuantidade(1)
  }

  const whatsappHref = selected
    ? buildWhatsAppLink(
        `Olá! Tenho interesse no produto "${productNome}" (${selected.nomeVariante}, ${formatPrice(selected.price)}).`
      )
    : undefined

  return (
    <>
      <div className="flex flex-col gap-2">
        <span className="text-label text-ink-muted">Tamanho</span>
        <div className="flex flex-wrap gap-2">
          {variants.map((variant) => (
            <button
              key={variant.id}
              type="button"
              onClick={() => selectVariant(variant.id)}
              className={
                variant.id === selectedId
                  ? 'shrink-0 rounded-full bg-orange px-4 py-1.5 text-label text-white'
                  : 'shrink-0 rounded-full border border-outline px-4 py-1.5 text-label text-ink-muted transition-opacity hover:opacity-80'
              }
            >
              {variant.nomeVariante}
            </button>
          ))}
        </div>
      </div>

      <div className="flex flex-col gap-1">
        {selected ? (
          <>
            {selected.priceOriginal != null && (
              <span className="text-sm text-ink-muted line-through">
                {formatPrice(selected.priceOriginal)}
              </span>
            )}
            <span className="text-h2 font-semibold text-orange">
              {formatPrice(selected.price)}
            </span>
          </>
        ) : (
          <span className="text-sm text-ink-muted">Selecione uma variante para ver o preço</span>
        )}
      </div>

      <div className="flex flex-col gap-2 rounded-lg border border-outline/30 bg-surface p-4">
        <div className="flex items-center gap-2">
          <CheckCircle2 className="size-5 shrink-0 text-ink" />
          <span className="text-label text-ink">
            {selected
              ? selected.stock > 0
                ? 'Em estoque'
                : 'Fora de estoque'
              : 'Selecione uma variante para ver o estoque'}
          </span>
        </div>
        <div className="flex items-center gap-2">
          <Truck className="size-5 shrink-0 text-ink" />
          <span className="text-label text-ink">
            Frete grátis para Santana do Livramento e Rivera
          </span>
        </div>
      </div>

      <p className="text-sm leading-relaxed text-ink-muted">{descricao}</p>

      {selected && selected.stock > 0 && (
        <QuantityStepper quantidade={quantidade} onChange={setQuantidade} max={selected.stock} />
      )}

      {selected && selected.stock > 0 && (
        <AddToCartButton
          item={{
            productPublicId,
            productSlug,
            productNome,
            productImageUrl,
            variantId: selected.id,
            variantNome: selected.nomeVariante,
            unitPrice: selected.price,
          }}
          quantidade={quantidade}
        />
      )}

      {whatsappHref ? (
        <a
          href={whatsappHref}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-2 flex h-12 items-center justify-center gap-2 rounded-md bg-wa text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-95"
        >
          <WhatsAppIcon className="size-5" />
          Pedir pelo WhatsApp
        </a>
      ) : (
        <button
          type="button"
          disabled
          className="mt-2 flex h-12 cursor-not-allowed items-center justify-center gap-2 rounded-md bg-outline/40 text-sm font-medium text-ink-muted"
        >
          Selecione uma variante
        </button>
      )}
    </>
  )
}
