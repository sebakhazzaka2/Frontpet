import Image from 'next/image'
import Link from 'next/link'
import { CheckCircle2, ChevronRight, PawPrint, Truck } from 'lucide-react'
import { ProductVariantPicker } from '@/components/public/product-variant-picker'
import { SimpleProductActions } from '@/components/public/simple-product-actions'
import { ProductViewTracker } from '@/components/public/product-view-tracker'
import type { ProductDetail } from '@/lib/data/product-detail'
import { formatPrice } from '@/lib/utils'

interface ProductDetailViewProps {
  product: ProductDetail
}

// Tarea 3.9 (issue #19) — versión adaptada de la pantalla de Stitch "Detalhe do
// Produto (Versão Final com Navegação)". Decisiones 2026-07-27, ver
// docs/stitch-implementation-workflow.md §4:
// - Una sola imagen, sin galería de thumbnails (CLAUDE.md §7: galería es Fase 2).
// - Sin tabs de tabla nutricional/instrucciones: el DTO real solo tiene `descricao`.
// - Sin rating/avaliações: sin tabla de reviews en MVP1 (mismo criterio del
//   <ProductCard>, ADR 013).
//
// Selector de cantidad + add-to-cart (issue #29, Bloque A — resuelve el TODO
// que dejó la 3.9): el bloque precio/estoque/descrição/CTA para el caso SIN
// variantes se delega a <SimpleProductActions>, misma razón que
// <ProductVariantPicker> para el caso CON variantes — cuando
// `product.variants.length > 0`, `price` viene null del backend.
export function ProductDetailView({ product }: ProductDetailViewProps) {
  const categoria = product.categories[0]

  return (
    <div className="mx-auto max-w-content px-6 py-8 lg:px-8 lg:py-12">
      <ProductViewTracker
        publicId={product.publicId}
        nome={product.nome}
        categoria={categoria?.nome}
        price={product.price}
      />

      <nav className="mb-4 flex items-center gap-1 overflow-x-auto whitespace-nowrap text-caption text-ink-muted">
        <Link href="/" className="hover:text-ink">
          Início
        </Link>
        <ChevronRight className="size-3.5 shrink-0" />
        <Link href="/produtos" className="hover:text-ink">
          Loja
        </Link>
        {categoria && (
          <>
            <ChevronRight className="size-3.5 shrink-0" />
            <Link href={`/produtos?categoria=${categoria.slug}`} className="hover:text-ink">
              {categoria.nome}
            </Link>
          </>
        )}
        <ChevronRight className="size-3.5 shrink-0" />
        <span className="text-ink">{product.nome}</span>
      </nav>

      <div className="lg:grid lg:grid-cols-2 lg:gap-8">
        <div className="relative mb-6 aspect-square overflow-hidden rounded-lg border border-outline/30 bg-surface lg:mb-0">
          {product.mainImageUrl ? (
            <Image
              src={product.mainImageUrl}
              alt={product.nome}
              fill
              className="object-contain"
              sizes="(min-width: 1024px) 50vw, 100vw"
              // Casi seguro el elemento LCP de esta página — sin priority,
              // Next la carga lazy por default y retrasa justo la métrica
              // que más pesa en el Lighthouse de la página de venta.
              priority
            />
          ) : (
            // TODO: reemplazar por foto real del producto cuando exista. Sin
            // stock de imagen inventado — mismo criterio que <ProductCard>.
            <div className="flex h-full w-full items-center justify-center">
              <PawPrint className="size-16 text-outline" aria-hidden />
            </div>
          )}
          {product.onSale && (
            <span className="absolute left-3 top-3 rounded-full bg-orange px-3 py-1 text-caption text-white">
              Oferta
            </span>
          )}
        </div>

        <div className="flex flex-col gap-4">
          <div>
            {product.brandNome && (
              <p className="text-label uppercase text-ink-muted">{product.brandNome}</p>
            )}
            <h1 className="mt-1 text-h2-mobile font-display text-ink md:text-h2">
              {product.nome}
            </h1>
          </div>

          {product.variants.length > 0 ? (
            <ProductVariantPicker
              variants={product.variants}
              productPublicId={product.publicId}
              productSlug={product.slug}
              productNome={product.nome}
              productImageUrl={product.mainImageUrl}
              descricao={product.descricao}
            />
          ) : (
            <>
              <div className="flex flex-col gap-1">
                {product.onSale && (
                  <span className="text-sm text-ink-muted line-through">
                    {formatPrice(product.priceOriginal!)}
                  </span>
                )}
                <span className="text-h2 font-semibold text-orange">
                  {formatPrice(product.price!)}
                </span>
              </div>

              <div className="flex flex-col gap-2 rounded-lg border border-outline/30 bg-surface p-4">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="size-5 shrink-0 text-ink" />
                  <span className="text-label text-ink">
                    {product.stock > 0 ? 'Em estoque' : 'Fora de estoque'}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <Truck className="size-5 shrink-0 text-ink" />
                  <span className="text-label text-ink">
                    Frete grátis para Santana do Livramento e Rivera
                  </span>
                </div>
              </div>

              <p className="text-sm leading-relaxed text-ink-muted">{product.descricao}</p>

              <SimpleProductActions
                productPublicId={product.publicId}
                productSlug={product.slug}
                productNome={product.nome}
                productImageUrl={product.mainImageUrl}
                price={product.price!}
                stock={product.stock}
              />
            </>
          )}
        </div>
      </div>
    </div>
  )
}
