import { FEATURED_PRODUCTS } from '@/lib/data/products'
import { ProductCard } from '@/components/public/product-card'
import { SectionHeading } from '@/components/public/section-heading'

// Envoltorio de sección para darle contexto a <ProductCard> (tarea 2.5) — no
// es una tarea propia del ROADMAP, pero la card necesita algo alrededor para
// validarla visualmente (DoD: screenshot + mobile real).
export function FeaturedProducts() {
  return (
    <section className="mx-auto max-w-content px-6 py-16 lg:px-8">
      <SectionHeading
        align="center"
        eyebrow="02 — Para o dia a dia"
        titulo="Produtos em destaque"
        descricao="Selecionados especialmente para o seu pet"
      />

      <div className="mt-8 grid grid-cols-2 gap-4 lg:grid-cols-4 lg:gap-6">
        {FEATURED_PRODUCTS.map((product) => (
          <ProductCard key={product.slug} product={product} />
        ))}
      </div>
    </section>
  )
}
