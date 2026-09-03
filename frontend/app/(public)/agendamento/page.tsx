import type { Metadata } from 'next'
import { listServices } from '@/lib/api/services'
import { BookingWizard } from '@/components/public/booking/booking-wizard'

export const metadata: Metadata = {
  title: 'Agendamento',
  description:
    'Agende banho e tosa para o seu pet em Santana do Livramento. Escolha serviço, data e horário online.',
}

// force-dynamic: mesmo critério de app/(public)/servicos/page.tsx (issue #59,
// commit e0eaba9). Sem sinal dinâmico próprio (não lê searchParams/params),
// Next tentaria pré-renderizar no build e faria fetch real a GET /services
// nesse momento — quebra o job de frontend do CI, que builda sem backend ao
// lado (ECONNREFUSED). Custo aceito: perde o cache ISR de 60s de
// listServices(), cada visita bate no backend direto.
export const dynamic = 'force-dynamic'

// Passo 1-2 do wizard (Bloque C, issue #60). Server Component: busca o
// catálogo de serviços (banhos base + adicionais) uma vez; o wizard em si é
// client (estado de escolhas do usuário).
export default async function AgendamentoPage() {
  const services = await listServices()

  return (
    <div className="mx-auto max-w-content px-6 py-8 lg:px-8 lg:py-12">
      <div className="mb-8 rounded-lg bg-navy p-6 text-center shadow-system md:p-8">
        <h1 className="mb-2 text-h2-mobile font-display text-white md:text-h2">
          Agende o horário do seu <span className="text-orange">PET</span>
        </h1>
        <p className="mx-auto max-w-[600px] text-white/80">
          Escolha o serviço, a data e o horário ideais para cuidar de quem você mais ama de forma rápida e segura.
        </p>
      </div>

      <BookingWizard services={services} />
    </div>
  )
}
