import Image from 'next/image'
import Link from 'next/link'
import { CheckCircle2, ChevronRight, PawPrint, Truck } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { buildWhatsAppLink } from '@/lib/data/site'
import { isOnSale, type ProductDetail } from '@/lib/data/product-detail'
import { formatPrice } from '@/lib/utils'

interface ProductDetailViewProps {
  product: ProductDetail
}

// Tarefa 3.9 (issue a criar) — versão adaptada da pantalla de Stitch
// "Detalhe do Produto (Versão Final com Navegação)". Decisões 2026-07-27,
// ver docs/stitch-implementation-workflow.md §4:
// - Uma imagem só, sem galeria de thumbnails (CLAUDE.md §7: galeria é Fase 2).
// - Sem tabs de tabela nutricional/instruções: o DTO real só tem `descricao`.
// - Sem rating/avaliações: sem tabela de reviews em MVP1 (mesmo critério do
//   <ProductCard>, ADR 013).
// - Sem seletor de quantidade: fica para o Sprint 4, quando existir carrinho.
// - CTA direto pro WhatsApp, "Adicionar ao carrinho" fica pro Sprint 4.
export function ProductDetailView({ product }: ProductDetailViewProps) {
  const categoria = product.categories[0]
  const onSale = isOnSale(product)
  const whatsappHref = buildWhatsAppLink(
    `Olá! Tenho interesse no produto "${product.nome}" (${formatPrice(product.price)}).`
  )

  return (
    <div className="mx-auto max-w-content px-6 py-8 lg:px-8 lg:py-12">
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
            />
          ) : (
            // TODO: substituir por foto real do produto quando existir. Sem
            // estoque de imagem inventado — mesmo critério do <ProductCard>.
            <div className="flex h-full w-full items-center justify-center">
              <PawPrint className="size-16 text-outline" aria-hidden />
            </div>
          )}
          {onSale && (
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

          <div className="flex flex-col gap-1">
            {onSale && (
              <span className="text-sm text-ink-muted line-through">
                {formatPrice(product.priceOriginal!)}
              </span>
            )}
            <span className="text-h2 font-semibold text-orange">{formatPrice(product.price)}</span>
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

          <a
            href={whatsappHref}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-2 flex h-12 items-center justify-center gap-2 rounded-md bg-wa text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-95"
          >
            <WhatsAppIcon className="size-5" />
            Pedir pelo WhatsApp
          </a>
        </div>
      </div>
    </div>
  )
}
