import Image from 'next/image'
import Link from 'next/link'
import { PawPrint } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { buildWhatsAppLink } from '@/lib/data/site'
import type { Product } from '@/lib/data/products'
import { formatPrice } from '@/lib/utils'

interface ProductCardProps {
  product: Product
}

// Variante de landing (tarea 2.5): el CTA va directo a WhatsApp, no al
// carrito — "Adicionar à sacola" (Stitch) queda para el Sprint 4, cuando
// exista carrito de verdad (decidido 2026-07-25, ver issue #7).
export function ProductCard({ product }: ProductCardProps) {
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

        <a
          href={whatsappHref}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-auto flex h-10 items-center justify-center gap-2 rounded-md bg-wa text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-95"
        >
          <WhatsAppIcon className="size-4" />
          Pedir pelo WhatsApp
        </a>
      </div>
    </div>
  )
}
