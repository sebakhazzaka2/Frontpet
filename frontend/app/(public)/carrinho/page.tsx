import type { Metadata } from 'next'
import { CartPageContent } from '@/components/public/cart-page-content'

// noindex: página transacional (carrinho do sessionStorage de cada
// visitante) sem valor de SEO — o estado "carrinho vazio" não deveria
// rankear no Google.
export const metadata: Metadata = {
  title: 'Sua sacola',
  robots: { index: false, follow: false },
}

// Tarea 4.4/4.6/4.7 (issue #30) — carrito y checkout en una sola vista, no
// dos rutas (decisión del plan de Sprint 4: Stitch dibuja "Sua Sacola" como
// una sola pantalla con lista + resumen + form + CTA). No existe /checkout.
//
// Server Component solo para poder exportar `metadata` (tarea 7.x, SEO) —
// todo el contenido real vive en <CartPageContent> ('use client'), que
// depende de useCart() (sessionStorage), no de datos fetcheados acá.
export default function CarrinhoPage() {
  return <CartPageContent />
}
