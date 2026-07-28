import { formatPrice } from '@/lib/utils'
import type { OrderDetail } from '@/lib/api/orders'

// Texto literal del template PENDING de ADR 010 (no inventado): tom cálido,
// PT-BR, 1-3 emojis, primer nome do cliente. Los templates de CONFIRMED/
// CANCELLED viven en el admin (Bloque F, Sprint 4/7) — todavía no existen acá
// porque un pedido recién creado siempre nace `PENDING` (OrderStatus.PENDING
// es el único estado posible en este punto del flujo), así que no hay
// necesidad real de una tabla de dispatch por status todavía (CLAUDE.md §6:
// no abstracciones genéricas sin 3 casos de uso reales).
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

function modalidadeLabel(order: OrderDetail): string {
  return order.enderecoEntrega === 'Retirada na loja' ? 'Retirada na loja' : 'Entrega'
}

// Construye el mensaje pre-formateado a partir del pedido YA persistido
// (`OrderDetail`, respuesta de `createOrder()`), no de los CartItem previos
// al submit — usa el snapshot que confirmó el backend (nome/preço congelados,
// código de pedido real), no lo que había en el carrito antes de enviar.
export function buildOrderMessage(order: OrderDetail): string {
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
