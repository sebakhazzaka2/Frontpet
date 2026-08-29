'use client'

import { useEffect, useRef } from 'react'
import { usePathname } from 'next/navigation'
import { useConsent } from '@/hooks/use-consent'
import { trackContact, trackPageView } from '@/lib/analytics/pixel'
import { ConsentBanner } from './consent-banner'
import { MetaPixel } from './meta-pixel'

// Isla de cliente única, montada no root layout (não em (public)/layout.tsx
// — ADR 006 reserva o root para fuentes/providers/metadata, e uma isla de
// consentimento é funcionalmente um provider; montá-la só em (public)
// deixaria /admin/** fora do banner). Concentra tudo que precisa ser client:
// o banner, o Pixel condicional, o tracker de PageView e o listener
// delegado de Contact.
export function ConsentGate() {
  const { status, grant, deny } = useConsent()
  const pathname = usePathname()
  const isFirstPathname = useRef(true)

  // PageView em navegação client-side do App Router: fbq não dispara isso
  // sozinho. Pula o primeiro pathname visto por este componente — ou é o
  // PageView que <MetaPixel/> já disparou no seu próprio fbq('init'), ou o
  // Pixel ainda não existe (sem consentimento) e não há nada pra disparar.
  // Deliberadamente NÃO usa useSearchParams(): em Next 16 isso exige um
  // boundary de Suspense e tira a rota do prerender estático — o único
  // searchParam com valor de marketing (?busca=) já tem seu próprio evento
  // Search (ver product-search.tsx).
  useEffect(() => {
    if (isFirstPathname.current) {
      isFirstPathname.current = false
      return
    }
    trackPageView()
  }, [pathname])

  // Fbq oficial de revogação (developers.facebook.com/docs/meta-pixel/implementation/gdpr):
  // o script já pode estar carregado (usuário aceitou e depois revogou) —
  // não dá pra descarregar o <script>, mas dá pra pausar o envio de eventos.
  // A purga completa do lado do navegador só acontece no próximo reload.
  useEffect(() => {
    if (status === 'denied' && typeof window !== 'undefined' && typeof window.fbq === 'function') {
      window.fbq('consent', 'revoke')
    }
  }, [status])

  // Contact: 11 call sites de wa.me espalhados pelo site, vários em Server
  // Components (nav.tsx, footer.tsx, bottom-nav.tsx, floating-wa.tsx,
  // product-card.tsx...). Um único listener delegado no document evita
  // converter 11 arquivos pra 'use client' só para um onClick — vanilla,
  // um listener, zero mudanças nesses arquivos.
  useEffect(() => {
    function onClick(event: MouseEvent) {
      const target = event.target as HTMLElement | null
      const link = target?.closest('a[href^="https://wa.me/"]')
      if (link) {
        trackContact(window.location.pathname)
      }
    }
    document.addEventListener('click', onClick)
    return () => document.removeEventListener('click', onClick)
  }, [])

  return (
    <>
      <MetaPixel granted={status === 'granted'} />
      {status === 'unknown' && <ConsentBanner onAccept={grant} onReject={deny} />}
    </>
  )
}
