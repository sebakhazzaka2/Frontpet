import { Hero } from '@/components/public/hero'
import { ServiceCard } from '@/components/public/service-card'
import { TrustBar } from '@/components/public/trust-bar'
import { FeaturedProducts } from '@/components/public/featured-products'
import { Reviews } from '@/components/public/reviews'
import { LocationMap } from '@/components/public/location-map'
import { SectionHeading } from '@/components/public/section-heading'
import { SERVICES_PREVIEW } from '@/lib/data/services'

/**
 * Landing (home). Se arma sección por sección a lo largo del Sprint 2
 * (tareas 2.1 a 2.8) — hoy: Nav (layout) + Hero + TrustBar + Serviços +
 * Produtos em destaque + Reviews + Localização (ADR 025) + Footer (layout).
 * Falta FinalCTA (2.7).
 */
export default function HomePage() {
  return (
    <>
      <Hero />
      <TrustBar />

      <section className="mx-auto max-w-content px-4 py-16 md:px-6 lg:px-8">
        <SectionHeading
          eyebrow="01 — Nossos Serviços"
          titulo="Mais que serviços. Carinho."
          descricao="Equipe que trata cada pet como se fosse da família. Sem pressa, com técnica."
        />

        <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-3">
          {SERVICES_PREVIEW.map((service) => (
            <ServiceCard key={service.numero} data={service} />
          ))}
        </div>
      </section>

      <FeaturedProducts />
      <Reviews />
      <LocationMap />
    </>
  )
}
