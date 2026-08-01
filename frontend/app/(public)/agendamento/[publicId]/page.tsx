import { notFound } from 'next/navigation'
import Link from 'next/link'
import { CheckCircle2, Clock, Home, PawPrint, Scissors, User } from 'lucide-react'
import { getAppointment, type AppointmentStatus } from '@/lib/api/appointments'
import { appointmentDate, appointmentTime } from '@/lib/booking-dates'
import { buildWhatsAppLink } from '@/lib/data/site'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'

const STATUS_LABEL: Record<AppointmentStatus, string> = {
  PENDING: 'Aguardando confirmação',
  CONFIRMED: 'Confirmado',
  CANCELLED: 'Cancelado',
}

// Código curto e legível para referenciar o agendamento no chat — mesmo
// critério de orderCode() (lib/whatsapp/templates.ts), mas com os últimos 6
// caracteres do publicId (AC do issue #61), não os primeiros 8: não há
// contador sequencial novo para inventar, e o UUID v7 já é único o
// suficiente nesses 6.
function bookingCode(publicId: string): string {
  return publicId.replace(/-/g, '').slice(-6).toUpperCase()
}

// Pantalla de confirmação (Bloque D, issue #61). Server Component: GET
// /appointments/{publicId} é público e pode devolver 404 real (publicId
// inexistente) — diferente de createAppointment, que nunca devolve 404.
export default async function AgendamentoConfirmacaoPage(props: PageProps<'/agendamento/[publicId]'>) {
  const { publicId } = await props.params
  const appointment = await getAppointment(publicId)

  if (!appointment) {
    notFound()
  }

  const code = bookingCode(appointment.publicId)
  const servicoLabel =
    appointment.addons.length > 0
      ? `${appointment.baseServiceNome} + ${appointment.addons.map((a) => a.nome).join(', ')}`
      : appointment.baseServiceNome
  const whatsappMessage = `Olá! Gostaria de falar sobre meu agendamento #${code}.`

  return (
    <div className="mx-auto max-w-[600px] px-6 py-8 lg:px-8 lg:py-12">
      <section className="mb-8 flex flex-col items-center text-center">
        <div className="mb-4 flex size-24 items-center justify-center rounded-full bg-orange/10">
          <CheckCircle2 className="size-14 text-orange" />
        </div>
        <h1 className="mb-2 text-h2-mobile font-display text-navy">Recebemos seu pedido de agendamento!</h1>
        <p className="mb-4 text-sm text-ink-muted">
          Estamos analisando sua solicitação. Você receberá uma confirmação em breve.
        </p>
        <div className="inline-flex items-center gap-2 rounded-full bg-muted px-4 py-2">
          <span className="text-label text-ink-muted">Código:</span>
          <span className="text-label font-bold text-orange">#{code}</span>
        </div>
      </section>

      <section className="mb-8 rounded-lg border border-outline bg-surface-card p-6 shadow-card">
        <div className="mb-4 flex items-start justify-between">
          <span className="text-eyebrow tracking-widest text-ink-muted uppercase">Detalhes do Agendamento</span>
          <span className="rounded-full bg-orange/10 px-3 py-1 text-caption font-medium text-orange">
            {STATUS_LABEL[appointment.status]}
          </span>
        </div>

        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between border-b border-outline/20 pb-3">
            <span className="flex items-center gap-2 text-label text-ink">
              <Scissors className="size-4 text-orange" />
              Serviço
            </span>
            <span className="text-right text-label font-semibold">{servicoLabel}</span>
          </div>

          <div className="flex items-center justify-between border-b border-outline/20 pb-3">
            <span className="flex items-center gap-2 text-label text-ink">
              <Clock className="size-4 text-orange" />
              Data e horário
            </span>
            <span className="text-label font-semibold">
              {appointmentDate(appointment.startAt)}, {appointmentTime(appointment.startAt)}
            </span>
          </div>

          <div className="flex items-center justify-between border-b border-outline/20 pb-3">
            <span className="flex items-center gap-2 text-label text-ink">
              <PawPrint className="size-4 text-orange" />
              Pet
            </span>
            <span className="text-label font-semibold">{appointment.petNome}</span>
          </div>

          <div className="flex items-center justify-between">
            <span className="flex items-center gap-2 text-label text-ink">
              <User className="size-4 text-orange" />
              Responsável
            </span>
            <span className="text-label font-semibold">{appointment.clienteNome}</span>
          </div>
        </div>
      </section>

      <section className="mb-8">
        <h2 className="mb-4 text-h3 font-display text-navy">Próximos Passos</h2>
        <div className="flex flex-col gap-2">
          {[
            {
              titulo: 'Análise de horário',
              descricao: 'Nossa equipe verifica a disponibilidade para o horário escolhido.',
            },
            {
              titulo: 'Confirmação por WhatsApp',
              descricao: 'Nosso time vai entrar em contato pelo WhatsApp para confirmar o horário em breve.',
            },
            {
              titulo: 'Pronto para o banho!',
              descricao: `Basta trazer o ${appointment.petNome} no horário agendado.`,
            },
          ].map((passo, i) => (
            <div key={passo.titulo} className="flex gap-3 rounded-lg border border-outline bg-muted p-4">
              <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-navy font-bold text-white">
                {i + 1}
              </div>
              <div>
                <p className="text-label font-semibold text-navy">{passo.titulo}</p>
                <p className="text-sm text-ink-muted">{passo.descricao}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="flex flex-col gap-3">
        <Link
          href="/"
          className="flex h-12 items-center justify-center gap-2 rounded-md bg-orange font-label text-white shadow-md transition-transform active:scale-95"
        >
          <Home className="size-5" />
          Voltar para o início
        </Link>
        <Link
          href="/servicos"
          className="flex h-11 items-center justify-center rounded-md text-navy transition-colors hover:bg-hover"
        >
          Ver outros serviços
        </Link>
        {/* Botão de suporte WhatsApp: canal de ajuda/cancelamento, confirmado
            no cierre do Bloque 0 (comentário do issue #57) — não é o CTA
            principal do submit (ADR 008: sem redirect a wa.me no submit). */}
        <a
          href={buildWhatsAppLink(whatsappMessage)}
          target="_blank"
          rel="noopener noreferrer"
          className="flex h-11 items-center justify-center gap-2 rounded-md border border-wa text-wa transition-colors hover:bg-wa/10"
        >
          <WhatsAppIcon className="size-4" />
          Falar com o suporte
        </a>
      </section>
    </div>
  )
}
