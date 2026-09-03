import type { Metadata } from 'next'
import { notFound } from 'next/navigation'
import { ProductCard } from '@/components/public/product-card'
import { ProductDetailView } from '@/components/public/product-detail'
import { getRelatedProducts } from '@/lib/data/product-detail'
import { getProductBySlug } from '@/lib/api/products'

// generateMetadata llama a la misma getProductBySlug() que el componente de
// abajo — dedupeada por cache() de React (lib/api/products.ts), no es un
// segundo fetch real. Si el produto não existe, devolvemos metadata genérica
// em vez de chamar notFound() acá: notFound() dentro de generateMetadata não
// é o padrão documentado pelo Next.js, e a própria página já chama notFound()
// e renderiza a rota 404 — o <title> genérico nesse meio tempo não importa.
export async function generateMetadata(
  props: PageProps<'/produtos/[slug]'>
): Promise<Metadata> {
  const { slug } = await props.params
  const product = await getProductBySlug(slug)

  if (!product) {
    return { title: 'Produto não encontrado' }
  }

  return {
    title: product.nome,
    description: product.descricao,
    openGraph: product.mainImageUrl
      ? { images: [{ url: product.mainImageUrl }] }
      : undefined,
  }
}

// Next 16: params es una Promise (ver ROADMAP.md tarea 3.9).
export default async function ProductDetailPage(props: PageProps<'/produtos/[slug]'>) {
  const { slug } = await props.params
  const product = await getProductBySlug(slug)

  if (!product) {
    notFound()
  }

  const related = getRelatedProducts(slug)

  return (
    <>
      <ProductDetailView product={product} />

      {related.length > 0 && (
        <section className="mx-auto max-w-content px-6 py-16 lg:px-8">
          <h2 className="mb-8 text-h2-mobile font-display text-ink md:text-h2">
            Produtos relacionados
          </h2>
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4 lg:gap-6">
            {related.map((relatedProduct) => (
              <ProductCard key={relatedProduct.slug} product={relatedProduct} />
            ))}
          </div>
        </section>
      )}
    </>
  )
}
