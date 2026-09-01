import { describe, it, expect, afterEach, vi } from 'vitest'

// El riesgo real de este módulo no es el mapeo, es la integración rota que
// pasó desapercibida: el endpoint legacy devolvía REQUEST_DENIED y el catch
// silencioso hacía caer a testimonios inventados sin que nadie lo notara
// (docs/pending-decisions.md §15). Estos tests fijan el endpoint correcto
// como regresión, y que un fallo real nunca lance hacia los consumidores.
//
// vi.resetModules() + import dinámico por test: getPlaceSummary() está
// envuelto en React cache() (mismo motivo que el dedupe entre Reviews/
// TrustBar/Hero/LocationMap) — sin resetear el módulo, el resultado del
// primer test quedaría memoizado para todos los siguientes.

const ENV_KEYS = ['GOOGLE_PLACES_API_KEY', 'NEXT_PUBLIC_GOOGLE_PLACE_ID'] as const

function setEnv() {
  process.env.GOOGLE_PLACES_API_KEY = 'test-key'
  process.env.NEXT_PUBLIC_GOOGLE_PLACE_ID = 'test-place-id'
}

async function importFresh() {
  vi.resetModules()
  return import('./google-places')
}

afterEach(() => {
  vi.unstubAllGlobals()
  for (const key of ENV_KEYS) delete process.env[key]
})

describe('getPlaceSummary — sin configuración', () => {
  it('devuelve EMPTY y no llama a fetch si faltan las env vars', async () => {
    const { getPlaceSummary } = await importFresh()
    const fetchSpy = vi.fn()
    vi.stubGlobal('fetch', fetchSpy)

    const result = await getPlaceSummary()

    expect(fetchSpy).not.toHaveBeenCalled()
    expect(result).toEqual({
      rating: null,
      userRatingCount: null,
      shortAddress: null,
      fullAddress: null,
      googleMapsUri: null,
      reviews: [],
    })
  })
})

describe('getPlaceSummary — endpoint', () => {
  it('pega a la Places API (New), no al endpoint legacy', async () => {
    const { getPlaceSummary } = await importFresh()
    setEnv()
    const fetchSpy = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({}),
    })
    vi.stubGlobal('fetch', fetchSpy)

    await getPlaceSummary()

    const [url, options] = fetchSpy.mock.calls[0]
    expect(String(url)).toContain('https://places.googleapis.com/v1/places/test-place-id')
    expect(String(url)).not.toContain('maps.googleapis.com')
    expect(options.headers['X-Goog-Api-Key']).toBe('test-key')
    expect(options.next).toEqual({ revalidate: 86400 })
  })
})

// No hay test de dedupe acá a propósito: `cache()` de React solo memoiza bajo
// la condición de módulo "react-server" que activa el runtime de Server
// Components de Next — fuera de ese contexto (Node/Vitest plano, sin el
// bundler de Next) es un passthrough sin memoización, verificado a mano:
// `cache(fn)` invocada 3 veces devuelve 3 resultados distintos, no el mismo.
// Un test acá daría una falsa sensación de cobertura sin probar nada real;
// la garantía de dedupe depende del runtime de Next, no de este módulo solo.

describe('getPlaceSummary — mapeo', () => {
  const RAW = {
    rating: 5,
    userRatingCount: 78,
    shortFormattedAddress: "Av. Gen. Daltro Filho, 282 - Centro, Sant'Ana do Livramento",
    formattedAddress: "Av. Gen. Daltro Filho, 282 - Centro, Sant'Ana do Livramento - RS, Brasil",
    googleMapsUri: 'https://maps.google.com/?cid=123',
    reviews: [
      {
        name: 'places/x/reviews/1',
        rating: 5,
        text: { text: 'Excelente.\nMeu Pet volta limpo e bem tratado.' },
        relativePublishTimeDescription: '4 semanas atrás',
        authorAttribution: {
          displayName: 'Elisangela Brito',
          uri: 'https://google.com/maps/contrib/1',
          photoUri: 'https://lh3.googleusercontent.com/a/1',
        },
        googleMapsUri: 'https://maps.google.com/reviews/1',
      },
      {
        name: 'places/x/reviews/2',
        rating: 4,
        // Sin `text`, solo rating — no debe romper el mapeo, se filtra.
        relativePublishTimeDescription: 'um mês atrás',
        authorAttribution: { displayName: 'Sem Texto' },
      },
    ],
  }

  it('normaliza \\n embebidos y filtra reviews sin texto', async () => {
    const { getPlaceSummary } = await importFresh()
    setEnv()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => RAW }))

    const result = await getPlaceSummary()

    expect(result.reviews).toHaveLength(1)
    expect(result.reviews[0]).toEqual({
      id: 'places/x/reviews/1',
      authorName: 'Elisangela Brito',
      authorUri: 'https://google.com/maps/contrib/1',
      photoUri: 'https://lh3.googleusercontent.com/a/1',
      rating: 5,
      text: 'Excelente. Meu Pet volta limpo e bem tratado.',
      relativeTime: '4 semanas atrás',
      reviewUri: 'https://maps.google.com/reviews/1',
    })
  })

  it('mapea rating, address y googleMapsUri del place', async () => {
    const { getPlaceSummary } = await importFresh()
    setEnv()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => RAW }))

    const result = await getPlaceSummary()

    expect(result.rating).toBe(5)
    expect(result.userRatingCount).toBe(78)
    expect(result.shortAddress).toBe(RAW.shortFormattedAddress)
    expect(result.googleMapsUri).toBe(RAW.googleMapsUri)
  })
})

describe('getPlaceSummary — fallas', () => {
  it('devuelve EMPTY si la respuesta no es ok (ej. REQUEST_DENIED)', async () => {
    const { getPlaceSummary } = await importFresh()
    setEnv()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 403, statusText: 'Forbidden' }),
    )
    vi.spyOn(console, 'error').mockImplementation(() => {})

    const result = await getPlaceSummary()

    expect(result.reviews).toEqual([])
    expect(result.rating).toBeNull()
  })

  it('devuelve EMPTY si fetch lanza (red caída, etc.)', async () => {
    const { getPlaceSummary } = await importFresh()
    setEnv()
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network down')))
    vi.spyOn(console, 'error').mockImplementation(() => {})

    const result = await getPlaceSummary()

    expect(result).toEqual({
      rating: null,
      userRatingCount: null,
      shortAddress: null,
      fullAddress: null,
      googleMapsUri: null,
      reviews: [],
    })
  })
})
