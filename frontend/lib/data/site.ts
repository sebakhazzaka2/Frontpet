// Datos estáticos del tenant (FrontPet). Sprint 2 no consume el backend
// todavía (ver ADR 017) — cuando exista `tenant.config`, esto se reemplaza
// por la fuente real, no se hardcodea en cada componente.

// TODO: reemplazar por el WhatsApp real del tenant cuando exista fuente de
// datos (V5__seed_dev.sql tiene whatsapp_destino). Mismo placeholder que ya
// usaba <Nav> — centralizado acá para que Footer y ProductCard no diverjan.
export const WHATSAPP_PHONE = '555596724124'
export const WHATSAPP_DISPLAY = '(55) 9672-4124'

export function buildWhatsAppLink(message: string) {
  return `https://wa.me/${WHATSAPP_PHONE}?text=${encodeURIComponent(message)}`
}

// Normaliza un teléfono ingresado a mano en el checkout (ej. "(55) 99876-5432")
// al formato que wa.me espera: solo dígitos, con código de país. El schema del
// checkout (lib/schemas/checkout.ts) no exige E.164 — solo dígitos/espacios/
// símbolos —, así que puede faltar el "55". Heurística válida porque el
// tenant es single-business en Brasil (ADR 007): si quedan 11 dígitos o menos
// tras limpiar, se asume que falta el código de país local y se antepone.
function normalizeBrazilPhone(phone: string): string {
  const digits = phone.replace(/\D/g, '')
  return digits.length <= 11 ? `55${digits}` : digits
}

// Tarea 4.14 (issue #34) — link de WhatsApp HACIA el cliente (admin
// confirmando/cancelando un pedido), no hacia el número fijo del negocio
// como buildWhatsAppLink(). Mismo encoding, distinto destinatario.
export function buildWhatsAppLinkTo(customerPhone: string, message: string) {
  return `https://wa.me/${normalizeBrazilPhone(customerPhone)}?text=${encodeURIComponent(message)}`
}

export const INSTAGRAM_HANDLE = '@frontpet.br'
export const INSTAGRAM_URL = 'https://instagram.com/frontpet.br'

export const SITE_CITY = 'Santana do Livramento, RS'

// Confirmado con el cliente (docs/preguntas-cliente.md §3.1), sembrado en
// business_hours (V5__seed_dev.sql). No hay endpoint público que exponga
// business_hours (solo /admin/business-hours, con auth) — texto estático
// hasta que exista uno, mismo criterio que SITE_CITY.
export const BUSINESS_HOURS: { dias: string; horas: string }[] = [
  { dias: 'Seg-Sáb', horas: '09h-19h' },
]

export const BUSINESS_HOURS_DISPLAY = BUSINESS_HOURS.map((h) => `${h.dias}: ${h.horas}`).join(
  ' · ',
)
