import Link from 'next/link'
import { ArrowRight, ShoppingBag } from 'lucide-react'

// Port de "Sua Sacola Vazia" (92f0b842e8c9489e8f928a313e5c8cc1) — mismo
// componente que <CartLineItem>/<CartSummary>, variante de estado (Etapa 0
// del workflow: agrupar estados de una pantalla, no portar aparte). Sin la
// sección "Você também pode gostar" del mock: es contenido decorativo fuera
// del AC de la issue #30 (requeriría un endpoint de recomendados que no
// existe — se armaría con datos inventados).
export function EmptyCart() {
  return (
    <div className="flex flex-col items-center gap-4 px-6 py-20 text-center">
      <div className="rounded-full bg-surface-card p-6 shadow-card">
        <ShoppingBag className="size-16 text-navy" aria-hidden />
      </div>
      <h2 className="text-h2-mobile font-display text-ink">Sua sacola está vazia</h2>
      <p className="max-w-[320px] text-sm text-ink-muted">
        Que tal dar uma olhadinha nos produtos selecionados pelos nossos especialistas?
      </p>
      <Link
        href="/produtos"
        className="mt-2 flex items-center gap-2 rounded-md bg-orange px-6 py-3 text-sm font-semibold text-white shadow-card transition-opacity hover:opacity-90 active:scale-95"
      >
        Ver produtos
        <ArrowRight className="size-4" />
      </Link>
    </div>
  )
}
