import Link from 'next/link'
import { Calendar, Clock, MapPin } from 'lucide-react'
import { listServices } from '@/lib/api/services'
import { ServiceOfferingCard } from '@/components/public/service-offering-card'
import { FaqAccordion, type FaqItem } from '@/components/public/faq-accordion'
import { BUSINESS_HOURS_DISPLAY, SITE_CITY } from '@/lib/data/site'

// force-dynamic: diferente de produtos/page.tsx (que já é dinâmico por ler
// searchParams, então nunca precisou disto). /servicos não tem nenhum sinal
// dinâmico próprio — sem isso, Next tenta pré-renderizar a página no build
// e faz fetch real a GET /services nesse momento. Funciona localmente (com o
// backend rodando), mas quebra o job de frontend do CI (.github/workflows/ci.yml),
// que builda sem backend ao lado — ECONNREFUSED. Custo aceito: perde-se o
// cache ISR de 60s do fetch em listServices() (revalidate vira inerte com
// force-dynamic — Next.js docs), cada visita bate no backend direto. Tráfego
// de um petshop MVP1 não justifica levantar Postgres+Spring Boot só para
// este build no CI.
export const dynamic = 'force-dynamic'

// Porteo de "Serviços (Imagens Sincronizadas)" (Stitch, issue #59, Bloque B
// do Sprint 6). Server Component: catálogo real via GET /api/v1/services
// (lib/api/services.ts, tipado no Bloque 0).
//
// Desvios do mock, todos resolvidos (não ficam como pergunta aberta):
// - Telefone "(55) 9999-9999" e "10+ anos exp." da info strip: cortados —
//   dado de negócio não confirmado (mesmo critério que a landing/footer,
//   ver docs/port-landing-stitch.md). Ficam só os 2 dados reais (cidade,
//   horário — docs/preguntas-cliente.md §3.1).
// - CTA final "Agendar via WhatsApp": trocado por "Agendar horário" →
//   /agendamento. O mock é anterior à ADR 008 (agendamento não redireciona
//   pra WhatsApp) — manter o CTA original teria contradito uma decisão já
//   fechada, não uma omissão.
// - "Como funciona" passo 1: "via site ou WhatsApp" → só "pelo site", mesmo
//   motivo do CTA acima.
// - FAQ: só 3 das 5 perguntas do mock têm conteúdo confirmável (antecedência
//   e janela de agendamento vêm de docs/preguntas-cliente.md §3.3; a de
//   disponibilidade reflete o comportamento real do ADR 020). As outras 2
//   (produtos/segurança, área de espera com "Wi-Fi e café") ficam de fora —
//   anotadas em docs/preguntas-cliente.md como pendentes, não inventadas.
export default async function ServicosPage() {
  const services = await listServices()
  const banhosBase = services.filter((service) => service.type === 'BASE')
  const adicionais = services.filter((service) => service.type === 'ADDON')

  return (
    <div>
      <section className="bg-surface px-6 pt-8 pb-16 lg:px-8 lg:pt-12">
        <div className="mx-auto max-w-content">
          <div className="mb-6 inline-flex items-center rounded-full bg-navy px-4 py-1.5">
            <span className="text-eyebrow uppercase tracking-wider text-white">Serviços</span>
          </div>

          <div className="rounded-lg bg-navy p-6 md:p-8">
            <h1 className="text-display-mobile font-display text-white md:text-display">
              Cuidado profissional para o seu <span className="text-orange">pet</span>
            </h1>
            <p className="mt-4 max-w-[600px] text-base text-white/80">
              Equipe especializada em estética animal para garantir saúde, higiene e bem-estar
              em um ambiente acolhedor e seguro.
            </p>
            <Link
              href="/agendamento"
              className="mt-6 inline-flex items-center gap-2 rounded-md bg-orange px-6 py-3 text-label font-semibold text-white transition-transform active:scale-95"
            >
              <Calendar className="size-5" aria-hidden />
              Agendar horário
            </Link>
          </div>
        </div>
      </section>

      <section className="relative z-10 -mt-10 px-6 lg:px-8">
        <div className="mx-auto grid max-w-content grid-cols-2 gap-6 rounded-lg border border-outline bg-surface-card p-6 shadow-card">
          <div className="flex flex-col items-center gap-2 text-center">
            <MapPin className="size-5 text-navy" aria-hidden />
            <p className="text-label text-ink">{SITE_CITY}</p>
          </div>
          <div className="flex flex-col items-center gap-2 text-center">
            <Clock className="size-5 text-navy" aria-hidden />
            <p className="text-label text-ink">{BUSINESS_HOURS_DISPLAY}</p>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-content px-6 py-12 lg:px-8">
        <div className="flex flex-col gap-6">
          {banhosBase.map((service) => (
            <ServiceOfferingCard key={service.id} service={service} />
          ))}
        </div>

        {adicionais.length > 0 && (
          <div className="mt-10">
            <h2 className="text-h2-mobile font-display text-ink md:text-h2">Adicionais</h2>
            <p className="mt-1 text-sm text-ink-muted">
              Somados a um banho na hora de agendar — não se reservam sozinhos.
            </p>
            <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {adicionais.map((service) => (
                <ServiceOfferingCard key={service.id} service={service} variant="compacto" />
              ))}
            </div>
          </div>
        )}
      </section>

      <section className="bg-surface px-6 py-12 lg:px-8">
        <div className="mx-auto max-w-content text-center">
          <h2 className="text-h2-mobile font-display text-ink md:text-h2">Como funciona</h2>
          <p className="mt-2 text-sm text-ink-muted">
            4 passos simples para o dia de beleza do seu pet
          </p>
        </div>
        <div className="mx-auto mt-8 grid max-w-content grid-cols-1 gap-8 md:grid-cols-4">
          {COMO_FUNCIONA.map((passo) => (
            <div key={passo.numero} className="flex flex-col items-center gap-3 text-center">
              <div className="flex size-12 items-center justify-center rounded-full bg-navy text-h3 text-white">
                {passo.numero}
              </div>
              <h3 className="text-h3 text-ink">{passo.titulo}</h3>
              <p className="text-sm text-ink-muted">{passo.descricao}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-[800px] px-6 py-12 lg:px-8">
        <h2 className="mb-8 text-center text-h2-mobile font-display text-ink md:text-h2">
          Dúvidas frequentes
        </h2>
        <FaqAccordion items={FAQ_ITEMS} />
      </section>

      <section className="bg-surface px-6 py-12 text-center lg:px-8">
        <div className="mx-auto max-w-content">
          <h2 className="text-h2-mobile font-display text-ink md:text-h2">
            Pronto para transformar o dia do seu pet?
          </h2>
          <Link
            href="/agendamento"
            className="mt-6 inline-flex items-center justify-center gap-2 rounded-md bg-orange px-8 py-4 text-h3 font-semibold text-white shadow-card transition-transform active:scale-95"
          >
            <Calendar className="size-5" aria-hidden />
            Agendar horário
          </Link>
          <Link
            href="/produtos"
            className="mt-4 block text-label text-ink-muted transition-opacity hover:text-ink hover:underline"
          >
            Ou continue navegando os produtos →
          </Link>
        </div>
      </section>
    </div>
  )
}

const COMO_FUNCIONA = [
  { numero: '1', titulo: 'Agendamento', descricao: 'Escolha o melhor horário pelo site.' },
  {
    numero: '2',
    titulo: 'Avaliação',
    descricao: 'Analisamos a pelagem e a pele do seu pet antes de começar.',
  },
  {
    numero: '3',
    titulo: 'Cuidado',
    descricao: 'Seu pet recebe todo carinho e atenção profissional.',
  },
  { numero: '4', titulo: 'Pronto!', descricao: 'Pet cheiroso e feliz, pronto para buscar.' },
] as const

const FAQ_ITEMS: FaqItem[] = [
  {
    pergunta: 'Preciso agendar com antecedência?',
    resposta:
      'Recomendamos agendar com pelo menos 1 hora de antecedência. Aceitamos agendamentos com até 60 dias de antecedência.',
  },
  {
    pergunta: 'Quanto tempo demora o serviço?',
    resposta:
      'Varia conforme o porte do pet e os adicionais escolhidos — o tempo estimado aparece no resumo antes de confirmar o agendamento.',
  },
  {
    pergunta: 'E se o dia que eu quero não tiver horário disponível?',
    resposta:
      'Alguns dias podem estar bloqueados por feriados ou imprevistos — o calendário de agendamento só mostra os horários realmente disponíveis.',
  },
]
