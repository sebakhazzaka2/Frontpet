import type { ServiceCardData } from '@/components/public/service-card'

/**
 * Preview de serviços para la landing (Sprint 2 — datos estáticos, ADR 017).
 *
 * Nombres, precios y descripciones son los **reales**, confirmados con el
 * cliente y ya sembrados en `V5__seed_dev.sql` (tabla oficial de preços,
 * jul/2026) — no los 3 servicios inventados que muestra el mockup de Stitch
 * ("Banho & Tosa" R$55, "Tosa Higiênica" R$30, "Spa Premium" R$80). El
 * catálogo real es 2 banhos base por porte + adicionais (ADR 011), no 3
 * servicios a precio fijo.
 *
 * Precio y duração muestran el valor de porte P (el más barato/rápido) con
 * "a partir de" — el precio real depende del porte del pet, recién elegible
 * en el wizard de booking (Sprint 6).
 *
 * El tercer card es un adicional real (ver CLAUDE.md §6) sin precio: las
 * tarifas de adicionais siguen "pendentes" en V5 (nota del propio seed).
 * "Sob consulta" en vez de inventar un número.
 */
export const SERVICES_PREVIEW: ServiceCardData[] = [
  {
    numero: '01',
    nome: 'Banho Essencial',
    imagemAlt: 'Banho Essencial',
    preco: 'A partir de R$ 49,00',
    precoLabel: 'Conforme o porte',
    descricao: 'Limpeza completa, secagem profissional e perfume.',
    features: ['Limpeza completa', 'Secagem profissional'],
    duracao: 'A partir de 45min',
  },
  {
    numero: '02',
    nome: 'Banho Premium',
    imagemAlt: 'Banho Premium',
    preco: 'A partir de R$ 65,00',
    precoLabel: 'Conforme o porte',
    descricao:
      'Banho completo, limpeza dental, spray bucal, limpeza de ouvidos e corte de unhas (se necessário).',
    features: ['Limpeza dental', 'Corte de unhas'],
    duracao: 'A partir de 60min',
    destaque: 'Mais procurado',
  },
  {
    numero: '03',
    nome: 'Tosa Completa',
    imagemAlt: 'Tosa Completa',
    preco: 'Sob consulta',
    precoLabel: 'Adicional',
    descricao: 'Acabamento completo do pelo, feito junto com o banho.',
    features: ['Tesoura ou máquina', 'Acabamento nas patas e rosto'],
  },
]
