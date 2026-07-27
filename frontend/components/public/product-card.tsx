import Image from 'next/image'
import Link from 'next/link'
import { PawPrint, Star } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { buildWhatsAppLink } from '@/lib/data/site'
import type { Product } from '@/lib/data/products'
import { formatPrice } from '@/lib/utils'

interface ProductCardProps {
  product: Product
}

// Variante de landing (tarefa 2.5): CTA vai direto pro WhatsApp, não pro
// carrinho — "Adicionar à sacola" (Stitch) fica pro Sprint 4, quando existir
// carrinho de verdade (decidido 2026-07-25, ver issue #7).
export function ProductCard({ product }: ProductCardProps) {
  const whatsappHref = buildWhatsAppLink(
    `Olá! Tenho interesse no produto "${product.nome}" (${formatPrice(product.preco)}).`
  )

  return (
    <div className="flex flex-col overflow-hidden rounded-lg bg-surface-card shadow-card transition-shadow hover:shadow-card-hover">
      <Link
        href={`/produtos/${product.slug}`}
        aria-label={product.nome}
        className="relative aspect-square bg-surface"
      >
        {product.imagemUrl ? (
          <Image
            src={product.imagemUrl}
            alt=""
            fill
            className="object-cover"
            sizes="(min-width: 1024px) 25vw, 50vw"
          />
        ) : (
          // TODO: reemplazar por foto real do produto quando exista. Sin
          // stock inventado — mesmo critério do <ServiceCard> (tarefa 2.4).
          <div className="flex h-full w-full items-center justify-center">
            <PawPrint className="size-10 text-outline" aria-hidden />
          </div>
        )}
        {product.maisVendido && (
          <span className="absolute left-3 top-3 rounded-full bg-orange px-3 py-1 text-caption text-white">
            Mais vendido
          </span>
        )}
      </Link>

      <div className="flex flex-1 flex-col gap-2 p-4">
        <Link href={`/produtos/${product.slug}`}>
          <h3 className="line-clamp-2 text-sm font-medium text-ink">{product.nome}</h3>
        </Link>

        <div className="flex items-center gap-1">
          <Star className="size-4 fill-star text-star" />
          <span className="text-caption text-ink-muted">{product.avaliacao.toFixed(1)}</span>
        </div>

        <p className="text-h3 font-semibold text-orange">{formatPrice(product.preco)}</p>

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
