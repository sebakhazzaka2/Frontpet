'use client'

import { useCart } from '@/hooks/use-cart'
import { formatPrice } from '@/lib/utils'

// Port de "Sua Sacola", sección "Resumo do Pedido" (linhas 218-231 do
// code.html). Frete fixo em "Grátis" — não é um toggle ao vivo como o mock de
// Stitch (que alterna Grátis/R$0 conforme Entrega/Retirada): o backend
// sempre cria o pedido com freteMode=GRATIS (ADR 003, act. 2026-07-28 —
// "o sistema não calcula distância"); se o endereço real passar de 5km, a
// FrontPet ajusta manualmente por WhatsApp depois. Mostrar um toggle que
// muda de valor aqui seria fingir uma lógica que o sistema não tem.
export function CartSummary() {
  const { subtotal } = useCart()

  return (
    <div className="flex flex-col gap-2 rounded-lg border border-outline/30 bg-surface-card p-4 shadow-card">
      <div className="flex items-center justify-between text-sm text-ink-muted">
        <span>Subtotal</span>
        <span>{formatPrice(subtotal)}</span>
      </div>
      <div className="flex items-center justify-between text-sm text-ink-muted">
        <span>Frete</span>
        <span className="font-semibold text-wa">Grátis</span>
      </div>
      <div className="mt-1 flex items-center justify-between border-t border-outline/30 pt-2 text-navy">
        <span className="text-h3 font-semibold">Total</span>
        <span className="text-h3 text-[24px] font-semibold">{formatPrice(subtotal)}</span>
      </div>
    </div>
  )
}
