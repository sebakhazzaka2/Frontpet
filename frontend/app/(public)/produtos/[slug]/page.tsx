import { notFound } from 'next/navigation'
import { ProductCard } from '@/components/public/product-card'
import { ProductDetailView } from '@/components/public/product-detail'
import { getRelatedProducts } from '@/lib/data/product-detail'
import { getProductBySlug } from '@/lib/api/products'

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
