// Testimonios de la landing. No modelan una entidad real del backend — ADR 013
// no define tabla de reviews y CLAUDE.md §7 deja "reviews dinámicas
// (usuarios cargan)" fuera del MVP1. Quedan estáticos acá indefinidamente,
// no son un placeholder a la espera de API (a diferencia de `products.ts`).
export interface Review {
  id: string
  nome: string
  relacao: string
  iniciais: string
  texto: string
  avaliacao: number
}

export const REVIEWS: Review[] = [
  {
    id: 'mariana-l',
    nome: 'Mariana L.',
    relacao: 'Tutora do Thor',
    iniciais: 'ML',
    texto:
      'Melhor pet shop da região! O cuidado com o meu Thor é excepcional, ele volta sempre cheiroso e muito calmo.',
    avaliacao: 5,
  },
  {
    id: 'ricardo-s',
    nome: 'Ricardo S.',
    relacao: 'Tutor do Duke',
    iniciais: 'RS',
    texto: 'Serviço de banho impecável. Chegou em casa super cheiroso e calmo. Recomendo muito!',
    avaliacao: 5,
  },
  {
    id: 'fabiana-m',
    nome: 'Fabiana M.',
    relacao: 'Tutora da Mel',
    iniciais: 'FM',
    texto: 'Amo a variedade de produtos e o atendimento pelo WhatsApp é muito rápido e eficiente.',
    avaliacao: 5,
  },
]
