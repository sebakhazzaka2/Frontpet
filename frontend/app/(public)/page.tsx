import { Hero } from '@/components/public/hero'
import { ServiceCard } from '@/components/public/service-card'
import { TrustBar } from '@/components/public/trust-bar'
import { FeaturedProducts } from '@/components/public/featured-products'
import { Reviews } from '@/components/public/reviews'
import { SERVICES_PREVIEW } from '@/lib/data/services'

/**
 * Landing (home). Se arma sección por sección a lo largo del Sprint 2
 * (tareas 2.1 a 2.8) — hoy: Nav (layout) + Hero + TrustBar + Serviços +
 * Produtos em destaque + Reviews + Footer (layout). Falta FinalCTA (2.7).
 */
export default function HomePage() {
  return (
    <>
      <Hero />
      <TrustBar />

      <section className="mx-auto max-w-content px-4 py-16 md:px-6 lg:px-8">
        <span className="text-eyebrow uppercase tracking-wider text-orange">
          01 — Nossos Serviços
        </span>
        <h2 className="mt-2 text-h2-mobile text-ink md:text-h2">Mais que serviços. Carinho.</h2>
        <p className="mt-2 max-w-[600px] text-ink-muted">
          Equipe que trata cada pet como se fosse da família. Sem pressa, com técnica.
        </p>

        <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-3">
          {SERVICES_PREVIEW.map((service) => (
            <ServiceCard key={service.numero} data={service} />
          ))}
        </div>
      </section>

      <FeaturedProducts />
      <Reviews />
    </>
  )
}
