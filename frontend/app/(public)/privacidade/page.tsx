import type { Metadata } from 'next'
import { SITE_CITY, WHATSAPP_DISPLAY } from '@/lib/data/site'

// Tarea 4.16 (issue #30) — LGPD: aviso de tratamento de dados no checkout.
// Sin pantalla en Stitch (docs/stitch-implementation-workflow.md no la
// lista) — página de texto simples, com a tipografia do sistema, sem
// inventar layout que não existe em nenhuma referência visual.
export const metadata: Metadata = {
  title: 'Política de Privacidade',
  robots: { index: false, follow: false },
}

export default function PrivacidadePage() {
  return (
    <div className="mx-auto max-w-content px-6 py-8 lg:px-8 lg:py-12">
      <h1 className="mb-2 text-h2-mobile font-display text-ink md:text-h2">
        Política de Privacidade
      </h1>
      <p className="mb-8 text-sm text-ink-muted">Última atualização: julho de 2026</p>

      <div className="flex flex-col gap-6 text-sm leading-relaxed text-ink">
        <section>
          <h2 className="mb-2 text-h3 font-display text-ink">Quais dados coletamos</h2>
          <p>
            Ao fazer um pedido pelo site, coletamos seu nome, telefone, endereço de entrega (quando
            aplicável), forma de pagamento escolhida e os itens do pedido. Esses dados são
            necessários para que a FrontPet possa confirmar, preparar e entregar o seu pedido.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-h3 font-display text-ink">Como usamos seus dados</h2>
          <p>
            Usamos essas informações exclusivamente para processar seu pedido e entrar em contato
            pelo WhatsApp para confirmar detalhes de entrega, pagamento e horário. Não vendemos nem
            compartilhamos seus dados com terceiros para fins de marketing.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-h3 font-display text-ink">Pagamento</h2>
          <p>
            A forma de pagamento é apenas uma informação para a nossa logística — o pagamento em si
            acontece de forma offline, no momento da entrega ou retirada. Não processamos
            pagamentos online nem armazenamos dados de cartão.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-h3 font-display text-ink">Por quanto tempo guardamos seus dados</h2>
          <p>
            Mantemos os dados do seu pedido para fins de histórico e atendimento. Você pode
            solicitar a exclusão dos seus dados a qualquer momento, entrando em contato pelo
            WhatsApp {WHATSAPP_DISPLAY}.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-h3 font-display text-ink">Seus direitos</h2>
          <p>
            Conforme a Lei Geral de Proteção de Dados (LGPD), você pode solicitar acesso,
            correção ou exclusão dos seus dados pessoais a qualquer momento, entrando em contato
            conosco pelo WhatsApp.
          </p>
        </section>

        <section>
          <h2 className="mb-2 text-h3 font-display text-ink">Contato</h2>
          <p>
            FrontPet Petshop — {SITE_CITY}. Dúvidas sobre esta política podem ser enviadas pelo
            WhatsApp {WHATSAPP_DISPLAY}.
          </p>
        </section>
      </div>
    </div>
  )
}
