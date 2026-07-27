// Testemunhos da landing. Não modelam uma entidade real do backend — ADR 013
// não define tabela de reviews e CLAUDE.md §7 deixa "reviews dinâmicas
// (usuários carregam)" fora do MVP1. Ficam estáticos aqui indefinidamente,
// não são um placeholder à espera de API (diferente de `products.ts`).
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
