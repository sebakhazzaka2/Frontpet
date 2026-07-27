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

export const INSTAGRAM_HANDLE = '@frontpet.br'
export const INSTAGRAM_URL = 'https://instagram.com/frontpet.br'

export const SITE_CITY = 'Santana do Livramento, RS'
