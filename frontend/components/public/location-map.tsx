import { MapPin, Clock, Navigation } from 'lucide-react'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { InstagramIcon } from '@/components/shared/instagram-icon'
import { SectionHeading } from '@/components/public/section-heading'
import { getPlaceSummary } from '@/lib/google-places'
import { BUSINESS_HOURS, INSTAGRAM_HANDLE, SITE_CITY, buildWhatsAppLink } from '@/lib/data/site'

// Mapa + datos de contacto debajo de Reviews (ADR 025). Stack hasta `lg`, 12
// columnas a partir de ahí — a 768px un mapa al lado de una card de info no
// entra; el propio grid dominante del repo ya salta `md` (grid-cols-2
// lg:grid-cols-4), el criterio real acá es "lg es donde hay ancho para columnas".
export async function LocationMap() {
  const embedKey = process.env.NEXT_PUBLIC_GOOGLE_MAPS_EMBED_KEY
  const placeId = process.env.NEXT_PUBLIC_GOOGLE_PLACE_ID

  if (!embedKey || !placeId) {
    return null
  }

  const place = await getPlaceSummary()
  const endereco = place.shortAddress ?? SITE_CITY
  const comoChegarHref =
    place.googleMapsUri ?? `https://www.google.com/maps/search/?api=1&query_place_id=${placeId}`
  const mapSrc = `https://www.google.com/maps/embed/v1/place?key=${embedKey}&q=place_id:${placeId}`

  return (
    <section className="px-6 py-16 lg:px-8">
      <div className="mx-auto max-w-content">
        <SectionHeading
          align="center"
          eyebrow="04 — Venha nos visitar"
          titulo="Onde estamos"
          descricao="Estamos em Santana do Livramento. Chegue sem pressa — seu pet é bem-vindo."
        />

        <div className="mt-8 grid gap-6 lg:grid-cols-12 lg:items-stretch">
          <div className="flex flex-col gap-5 rounded-lg border border-outline bg-surface-card p-6 shadow-card lg:col-span-5">
            <div className="flex items-start gap-3">
              <MapPin className="mt-0.5 size-5 shrink-0 text-navy" aria-hidden />
              <div>
                <p className="text-caption uppercase tracking-wide text-ink-muted">Endereço</p>
                <p className="mt-1 text-sm text-ink">{endereco}</p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <Clock className="mt-0.5 size-5 shrink-0 text-navy" aria-hidden />
              <div>
                <p className="text-caption uppercase tracking-wide text-ink-muted">Horários</p>
                <p className="mt-1 text-sm text-ink">
                  {BUSINESS_HOURS.map((h) => `${h.dias} ${h.horas}`).join(' · ')}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <InstagramIcon className="mt-0.5 size-5 shrink-0 text-navy" aria-hidden />
              <div>
                <p className="text-caption uppercase tracking-wide text-ink-muted">Instagram</p>
                <p className="mt-1 text-sm text-ink">{INSTAGRAM_HANDLE}</p>
              </div>
            </div>

            <div className="mt-auto flex flex-col gap-3 pt-2">
              <a
                href={comoChegarHref}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-12 items-center justify-center gap-2 rounded-md bg-navy px-6 text-label font-semibold text-white shadow-system transition-transform active:scale-95"
              >
                <Navigation className="size-5" aria-hidden />
                Como chegar
              </a>
              <a
                href={buildWhatsAppLink('Olá! Queria confirmar o endereço e o horário de vocês.')}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-12 items-center justify-center gap-2 rounded-md bg-wa px-6 text-label font-semibold text-white transition-transform active:scale-95"
              >
                <WhatsAppIcon className="size-5" />
                Falar no WhatsApp
              </a>
            </div>
          </div>

          <div className="overflow-hidden rounded-lg border border-outline lg:col-span-7">
            <iframe
              title={`Localização da FrontPet em ${SITE_CITY}`}
              src={mapSrc}
              loading="lazy"
              referrerPolicy="no-referrer-when-downgrade"
              className="h-[320px] w-full border-0 lg:h-full"
            />
          </div>
        </div>
      </div>
    </section>
  )
}
