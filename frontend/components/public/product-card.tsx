import Image from 'next/image'
import Link from 'next/link'
import { PawPrint } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { AddToCartButton } from '@/components/public/add-to-cart-button'
import { buildWhatsAppLink } from '@/lib/data/site'
import type { Product } from '@/lib/data/products'
import { formatPrice } from '@/lib/utils'

interface ProductCardProps {
  product: Product
}

// Tarea 4.5 (issue #29): "Adicionar à sacola" se agrega junto al CTA de
// WhatsApp que ya existía (tarea 2.5) — la pantalla real de Stitch
// ("Catálogo com CTAs Unificados") muestra los dos botones lado a lado, no
// uno reemplazando al otro (verificado contra el code.html, no inventado).
//
// Solo se ofrece agregar al carrito cuando `product.publicId` existe (los
// datos reales de /produtos lo tienen desde el Bloque A; FEATURED_PRODUCTS
// de la landing es estático y no, ver lib/data/products.ts) y cuando el
// producto NO tiene variantes: con variantes hace falta elegir una primero,
// eso vive en /produtos/{slug} (product-variant-picker.tsx) — agregar "la
// primera" o "la más barata" en silencio sería inventar la elección del
// cliente.
export function ProductCard({ product }: ProductCardProps) {
  const canAddToCart = product.publicId != null && !product.hasVariants
  const whatsappHref = buildWhatsAppLink(
    `Olá! Tenho interesse no produto "${product.nome}" (${formatPrice(product.price)}).`
  )

  return (
    <div className="flex flex-col overflow-hidden rounded-lg bg-surface-card shadow-card transition-shadow hover:shadow-card-hover">
      <Link
        href={`/produtos/${product.slug}`}
        aria-label={product.nome}
        className="relative aspect-square bg-surface"
      >
        {product.mainImageUrl ? (
          <Image
            src={product.mainImageUrl}
            alt=""
            fill
            className="object-cover"
            sizes="(min-width: 1024px) 25vw, 50vw"
          />
        ) : (
          // TODO: reemplazar por foto real del producto cuando exista. Sin
          // stock inventado — mismo criterio que <ServiceCard> (tarea 2.4).
          <div className="flex h-full w-full items-center justify-center">
            <PawPrint className="size-10 text-outline" aria-hidden />
          </div>
        )}
      </Link>

      <div className="flex flex-1 flex-col gap-2 p-4">
        <Link href={`/produtos/${product.slug}`}>
          <h3 className="line-clamp-2 text-sm font-medium text-ink">{product.nome}</h3>
        </Link>

        <p className="text-h3 font-semibold text-orange">
          {product.hasVariants && <span className="text-sm font-normal">A partir de </span>}
          {formatPrice(product.price)}
        </p>

        <div className="mt-auto flex flex-col gap-2">
          {canAddToCart && (
            <AddToCartButton
              item={{
                productPublicId: product.publicId!,
                productSlug: product.slug,
                productNome: product.nome,
                productImageUrl: product.mainImageUrl,
                unitPrice: product.price,
              }}
            />
          )}

          <a
            href={whatsappHref}
            target="_blank"
            rel="noopener noreferrer"
            className="flex h-10 items-center justify-center gap-2 rounded-md bg-wa text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-95"
          >
            <WhatsAppIcon className="size-4" />
            Pedir pelo WhatsApp
          </a>
        </div>
      </div>
    </div>
  )
}
