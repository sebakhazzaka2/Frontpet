// Capa de tracking do Meta Pixel — ADR 024. Cada função é no-op sempre que
// `window.fbq` não existir, e `window.fbq` só existe depois que
// <MetaPixel/> (components/consent/meta-pixel.tsx) o carrega, o que só
// acontece com consentimento concedido E NEXT_PUBLIC_META_PIXEL_ID setado.
// Por isso os call sites (add-to-cart-button.tsx, checkout-form.tsx, etc.)
// não precisam saber nada sobre consentimento — chamam trackX() sempre, e o
// gate vive inteiramente aqui.
//
// Moeda sempre BRL: todo o site formata preços em pt-BR/BRL (lib/utils.ts).
//
// Sem abstração de "provedores de analytics" — CLAUDE.md §6 proíbe
// abstrações genéricas antes de 3 casos reais, e aqui só existe este (o
// Plausible não precisa de nenhuma linha de código de tracking, ver
// app/layout.tsx).

declare global {
  interface Window {
    fbq?: (...args: unknown[]) => void
  }
}

const CURRENCY = 'BRL'

function fire(event: string, params?: Record<string, unknown>) {
  if (typeof window === 'undefined' || typeof window.fbq !== 'function') return
  window.fbq('track', event, params)
}

export function trackPageView() {
  fire('PageView')
}

export function trackViewContent(product: {
  publicId: string
  nome: string
  categoria?: string
  price?: number
}) {
  fire('ViewContent', {
    content_ids: [product.publicId],
    content_type: 'product',
    content_name: product.nome,
    content_category: product.categoria,
    value: product.price,
    currency: CURRENCY,
  })
}

export function trackSearch(searchString: string) {
  fire('Search', { search_string: searchString })
}

export function trackAddToCart(item: {
  productPublicId: string
  productNome: string
  value: number
}) {
  fire('AddToCart', {
    content_ids: [item.productPublicId],
    content_type: 'product',
    content_name: item.productNome,
    value: item.value,
    currency: CURRENCY,
  })
}

export function trackInitiateCheckout(cart: { productPublicIds: string[]; numItems: number; value: number }) {
  fire('InitiateCheckout', {
    content_ids: cart.productPublicIds,
    num_items: cart.numItems,
    value: cart.value,
    currency: CURRENCY,
  })
}

export function trackPurchase(order: {
  publicId: string
  productPublicIds: string[]
  numItems: number
  value: number
}) {
  // Purchase é o único evento padrão do Meta com value/currency obrigatórios
  // (developers.facebook.com/docs/meta-pixel/reference).
  fire('Purchase', {
    content_ids: order.productPublicIds,
    content_type: 'product',
    num_items: order.numItems,
    value: order.value,
    currency: CURRENCY,
  })
}

export function trackSchedule(appointment: { servicoNome: string; value: number }) {
  fire('Schedule', {
    content_name: appointment.servicoNome,
    value: appointment.value,
    currency: CURRENCY,
  })
}

export function trackContact(origin?: string) {
  fire('Contact', origin ? { content_name: origin } : undefined)
}
