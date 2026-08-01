import { formatPrice } from '@/lib/utils'
import type { OrderDetail, OrderStatus } from '@/lib/api/orders'
import type { AdminAppointmentDetail } from '@/lib/api/admin-appointments'
import type { AppointmentStatus } from '@/lib/api/appointments'

// Textos literales de ADR 010 (no inventados): tom cálido, PT-BR, 1-3 emojis,
// primer nome do cliente. Ahora con los 3 templates reales (PENDING desde el
// checkout, CONFIRMED/CANCELLED desde el botón "Enviar confirmação no
// WhatsApp" del admin, issue #34) — recién acá se justifica la tabla de
// dispatch por status (CLAUDE.md §6: no abstracciones sin 3 casos reales).
const MAX_ITEMS_IN_MESSAGE = 10

function firstName(nomeCompleto: string): string {
  return nomeCompleto.trim().split(/\s+/)[0] || nomeCompleto
}

// Código corto y legible para referenciar el pedido en el chat — el UUID v7
// completo (36 caracteres) es ilegible en un mensaje de WhatsApp. No hay un
// número secuencial "amigable" en el modelo (V4__orders.sql solo tiene
// public_id), así que se deriva de ahí: los primeros 8 hex ya son únicos en
// la práctica para el volumen de este negocio (UUID v7 arranca con el
// timestamp, por lo que ni siquiera colisiona con pedidos de otros días).
function orderCode(publicId: string): string {
  return publicId.replace(/-/g, '').slice(0, 8).toUpperCase()
}

function itemsList(order: OrderDetail): string {
  // `nomeSnapshot` ya viene completo (incluye la variante si la hay, ej.
  // "Ração Golden 10,1kg") — lo arma OrderServiceImpl al persistir el item.
  const lines = order.items
    .slice(0, MAX_ITEMS_IN_MESSAGE)
    .map(
      (item) =>
        `• ${item.quantidade}x ${item.nomeSnapshot} — ${formatPrice(item.unitPriceSnapshot * item.quantidade)}`
    )

  if (order.items.length > MAX_ITEMS_IN_MESSAGE) {
    const resto = order.items.length - MAX_ITEMS_IN_MESSAGE
    lines.push(`... e mais ${resto} ${resto === 1 ? 'item' : 'itens'}. Veja o pedido completo no comprovante.`)
  }

  return lines.join('\n')
}

// Exportadas: <OrderDetailPanel> (Bloque F) las reusa para no duplicar la
// misma lógica de "cómo se infiere Entrega/Retirada" en dos lugares.
export function modalidadeLabel(order: OrderDetail): string {
  return order.enderecoEntrega === 'Retirada na loja' ? 'Retirada na loja' : 'Entrega'
}

export function freteLabel(order: OrderDetail): string {
  return order.freteMode === 'GRATIS' ? 'Grátis' : 'A combinar'
}

function pendingTemplate(order: OrderDetail): string {
  const enderecoLine =
    order.enderecoEntrega !== 'Retirada na loja' ? `Endereço: ${order.enderecoEntrega}\n` : ''

  return `Olá ${firstName(order.clienteNome)}! Aqui é da FrontPet 🐾

Recebemos seu pedido #${orderCode(order.publicId)}:
${itemsList(order)}

Subtotal: ${formatPrice(order.subtotal)}
Modalidade: ${modalidadeLabel(order)}
${enderecoLine}
Vamos confirmar com você o valor do frete e o horário ideal. Pode me responder quando puder?`
}

function confirmedTemplate(order: OrderDetail): string {
  const previsao =
    order.enderecoEntrega === 'Retirada na loja'
      ? 'Te aviso assim que estiver pronto para retirada!'
      : order.horarioEntrega
        ? `Horário combinado: ${order.horarioEntrega}`
        : 'Vamos combinar o horário de entrega por aqui.'

  return `Olá ${firstName(order.clienteNome)}! 🐾

Tudo certo com seu pedido #${orderCode(order.publicId)}!

Itens confirmados:
${itemsList(order)}

Total: ${formatPrice(order.subtotal)}
Modalidade: ${modalidadeLabel(order)}
Frete: ${freteLabel(order)}
${previsao}

Qualquer dúvida, é só me avisar. Obrigado pela preferência!`
}

function cancelledTemplate(order: OrderDetail): string {
  return `Olá ${firstName(order.clienteNome)},

Infelizmente precisamos cancelar seu pedido #${orderCode(order.publicId)}.

Se quiser, posso te sugerir alternativas ou refazer o pedido em outra data. É só me avisar!`
}

const templates: Record<OrderStatus, (order: OrderDetail) => string> = {
  PENDING: pendingTemplate,
  CONFIRMED: confirmedTemplate,
  CANCELLED: cancelledTemplate,
}

// Construye el mensaje pre-formateado a partir del pedido YA persistido
// (`OrderDetail`), no de los CartItem previos al submit — usa el snapshot que
// confirmó el backend (nome/preço congelados, código de pedido real). El
// template usado depende de `order.status` — el admin cambia el status ANTES
// de mandar el WhatsApp (ver <OrderDetailPanel>), así que acá ya está
// actualizado al momento de armar el mensaje.
export function buildOrderMessage(order: OrderDetail): string {
  return templates[order.status](order)
}

// Código curto para referenciar o turno no chat — mesmo critério de
// `orderCode`: os últimos 6 caracteres do `publicId` (UUID v7), não um
// contador inventado (não existe no modelo real, AC do Bloque E/issue #62).
export function appointmentCode(publicId: string): string {
  return publicId.replace(/-/g, '').slice(-6).toUpperCase()
}

// "quarta-feira, 25 de maio às 10:00" — America/Sao_Paulo explícito, mesmo
// fuso que o backend usa pra calcular os slots (SlotGrid.ZONE_ID, ADR 020).
function formatDateTime(iso: string): string {
  const formatted = new Intl.DateTimeFormat('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(iso))
  return formatted.replace(' às ', ', às ').replace(', às', ' às')
}

function servicoLabel(appointment: AdminAppointmentDetail): string {
  return appointment.addonsNomes.length > 0
    ? `${appointment.baseServiceNome} + ${appointment.addonsNomes.join(', ')}`
    : appointment.baseServiceNome
}

function appointmentPendingTemplate(appointment: AdminAppointmentDetail): string {
  return `Olá ${firstName(appointment.clienteNome)}! Aqui é da FrontPet 🐾

Recebemos o agendamento #${appointmentCode(appointment.publicId)} do ${appointment.petNome}:
${servicoLabel(appointment)}
${formatDateTime(appointment.startAt)}

Vamos confirmar direitinho e te avisamos por aqui. Qualquer dúvida, é só chamar!`
}

function appointmentConfirmedTemplate(appointment: AdminAppointmentDetail): string {
  return `Olá ${firstName(appointment.clienteNome)}! 🐾

Tudo certo com o agendamento #${appointmentCode(appointment.publicId)} do ${appointment.petNome}!

${servicoLabel(appointment)}
${formatDateTime(appointment.startAt)}
Valor: ${formatPrice(appointment.totalPriceSnapshot)}

Te esperamos! Qualquer imprevisto, nos avise com antecedência.`
}

function appointmentCancelledTemplate(appointment: AdminAppointmentDetail): string {
  return `Olá ${firstName(appointment.clienteNome)},

Infelizmente precisamos cancelar o agendamento #${appointmentCode(appointment.publicId)} do ${appointment.petNome}, marcado para ${formatDateTime(appointment.startAt)}.

Se quiser, posso te ajudar a remarcar em outro horário. É só me avisar!`
}

const appointmentTemplates: Record<AppointmentStatus, (appointment: AdminAppointmentDetail) => string> = {
  PENDING: appointmentPendingTemplate,
  CONFIRMED: appointmentConfirmedTemplate,
  CANCELLED: appointmentCancelledTemplate,
}

// Mesmo critério de `buildOrderMessage`: o template depende do
// `appointment.status` já atualizado no momento do envio (o admin muda o
// status antes de clicar "Enviar confirmação no WhatsApp").
export function buildAppointmentMessage(appointment: AdminAppointmentDetail): string {
  return appointmentTemplates[appointment.status](appointment)
}
