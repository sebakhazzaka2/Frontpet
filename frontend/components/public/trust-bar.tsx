import { PawPrint, Star, Truck } from 'lucide-react'

// Datos estáticos — Sprint 2 no tiene backend de reviews/pedidos (ver ADR 017).
// Solo `star` va relleno en el HTML original de Stitch (FILL 1); pets/local_shipping
// van outline — por eso Star es el único con fill-orange acá.
const TRUST_ITEMS = [
  { icon: Star, label: '4.9 Avaliação Média', filled: true },
  { icon: PawPrint, label: '500+ Pets Atendidos', filled: false },
  { icon: Truck, label: 'Atenção personalizada', filled: false },
] as const

export function TrustBar() {
  return (
    <div className="w-full bg-navy py-8">
      <div className="mx-auto flex max-w-content flex-wrap items-center justify-center gap-x-12 gap-y-6 px-4 md:justify-around md:px-6 lg:px-8">
        {TRUST_ITEMS.map(({ icon: Icon, label, filled }) => (
          <div key={label} className="flex items-center gap-3">
            <Icon className={`size-7 shrink-0 text-orange ${filled ? 'fill-orange' : ''}`} />
            <span className="text-base font-semibold tracking-tight text-white">{label}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
