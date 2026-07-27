import { FEATURED_PRODUCTS } from '@/lib/data/products'
import { ProductCard } from '@/components/public/product-card'

// Envoltorio de sección para darle contexto a <ProductCard> (tarea 2.5) — no
// es una tarea propia del ROADMAP, pero la card necesita algo alrededor para
// validarla visualmente (DoD: screenshot + mobile real).
export function FeaturedProducts() {
  return (
    <section className="mx-auto max-w-content px-6 py-16 lg:px-8">
      <div className="mb-8 text-center">
        <h2 className="text-h2-mobile font-display text-ink md:text-h2">Produtos em destaque</h2>
        <p className="mt-2 text-sm text-ink-muted">Selecionados especialmente para o seu pet</p>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4 lg:gap-6">
        {FEATURED_PRODUCTS.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
    </section>
  )
}
