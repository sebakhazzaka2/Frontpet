import { FEATURED_PRODUCTS } from '@/lib/data/products'
import { ProductCard } from '@/components/public/product-card'

// Envoltório de seção pra dar contexto ao <ProductCard> (tarefa 2.5) — não é
// uma tarefa própria do ROADMAP, mas o card precisa de algo em volta pra
// validar visualmente (DoD: screenshot + mobile real).
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
