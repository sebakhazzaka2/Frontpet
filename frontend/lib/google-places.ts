import { cache } from 'react'

// Único punto de red para datos del Google Business de FrontPet (ADR 025).
// <Reviews>, <TrustBar>, <Hero> y <LocationMap> comparten esta función: se
// pide el superconjunto de campos para que el fetch cache de Next dedupee un
// solo request — la cache key es URL+headers, así que dos `fields` distintos
// serían dos requests. GOOGLE_PLACES_API_KEY no tiene prefijo NEXT_PUBLIC_,
// Next no la expone al bundle del cliente.

const PLACE_FIELDS =
  'id,rating,userRatingCount,shortFormattedAddress,formattedAddress,googleMapsUri,reviews'

// 24h — muy dentro del límite de 30 días del ToS de Places de no cachear
// Place Data indefinidamente, y evita gastar cuota en cada request.
const REVALIDATE_SECONDS = 60 * 60 * 24

export interface GoogleReview {
  id: string
  authorName: string
  authorUri: string | null
  photoUri: string | null
  rating: number
  text: string
  relativeTime: string
  reviewUri: string | null
}

export interface PlaceSummary {
  rating: number | null
  userRatingCount: number | null
  shortAddress: string | null
  fullAddress: string | null
  googleMapsUri: string | null
  reviews: GoogleReview[]
}

const EMPTY: PlaceSummary = {
  rating: null,
  userRatingCount: null,
  shortAddress: null,
  fullAddress: null,
  googleMapsUri: null,
  reviews: [],
}

interface RawReview {
  name: string
  rating: number
  text?: { text: string }
  originalText?: { text: string }
  relativePublishTimeDescription: string
  authorAttribution: {
    displayName: string
    uri?: string
    photoUri?: string
  }
  googleMapsUri?: string
}

interface RawPlace {
  rating?: number
  userRatingCount?: number
  shortFormattedAddress?: string
  formattedAddress?: string
  googleMapsUri?: string
  reviews?: RawReview[]
}

function mapReview(raw: RawReview, index: number): GoogleReview | null {
  // \n embebido (pasa en reviews reales, ej. "Excelente.\nMeu Pet volta...")
  // gastaría una línea entera del line-clamp de la card — se colapsa acá,
  // en el server, en vez de con whitespace-pre-line en el cliente.
  const text = (raw.text?.text ?? raw.originalText?.text ?? '').replace(/\s*\n+\s*/g, ' ').trim()
  if (!text) return null

  return {
    id: raw.name || `review-${index}`,
    authorName: raw.authorAttribution.displayName,
    authorUri: raw.authorAttribution.uri ?? null,
    photoUri: raw.authorAttribution.photoUri ?? null,
    rating: raw.rating,
    text,
    relativeTime: raw.relativePublishTimeDescription,
    reviewUri: raw.googleMapsUri ?? null,
  }
}

function mapPlace(raw: RawPlace): PlaceSummary {
  return {
    rating: raw.rating ?? null,
    userRatingCount: raw.userRatingCount ?? null,
    shortAddress: raw.shortFormattedAddress ?? null,
    fullAddress: raw.formattedAddress ?? null,
    googleMapsUri: raw.googleMapsUri ?? null,
    reviews: (raw.reviews ?? [])
      .map(mapReview)
      .filter((review): review is GoogleReview => review !== null),
  }
}

/**
 * Único punto de red del módulo. Nunca lanza — devuelve EMPTY ante cualquier
 * falla, así ninguna sección puede tumbar la home por un problema de Google.
 * Loguea el motivo del fallo: la integración anterior fallaba en silencio
 * (endpoint legacy dado de baja) y nadie lo notó hasta una revisión manual.
 *
 * `cache()` de React memoiza esta llamada dentro del mismo render — Reviews,
 * TrustBar, Hero y LocationMap la invocan por separado y comparten un único
 * resultado, sin depender únicamente del fetch cache de Next para el dedupe.
 */
export const getPlaceSummary = cache(async (): Promise<PlaceSummary> => {
  const apiKey = process.env.GOOGLE_PLACES_API_KEY
  const placeId = process.env.NEXT_PUBLIC_GOOGLE_PLACE_ID

  if (!apiKey || !placeId) return EMPTY

  try {
    // Places API (New). El endpoint legacy /maps/api/place/details/json
    // devuelve REQUEST_DENIED ("legacy API not enabled") con esta key —
    // verificado 2026-09-01. La key va en header, no en query, para que no
    // forme parte de la URL (cache key, logs, traces).
    const response = await fetch(
      `https://places.googleapis.com/v1/places/${encodeURIComponent(placeId)}?languageCode=pt-BR`,
      {
        headers: {
          'X-Goog-Api-Key': apiKey,
          'X-Goog-FieldMask': PLACE_FIELDS,
        },
        next: { revalidate: REVALIDATE_SECONDS },
      },
    )

    if (!response.ok) {
      console.error(`[google-places] ${response.status} ${response.statusText}`)
      return EMPTY
    }

    return mapPlace(await response.json())
  } catch (error) {
    console.error('[google-places] fetch failed', error)
    return EMPTY
  }
})
