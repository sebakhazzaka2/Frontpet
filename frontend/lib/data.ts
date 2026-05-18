/**
 * lib/data.ts — datos estáticos para Sprint 2
 *
 * Sprint 2: datos hardcodeados acá, el componente los importa directamente.
 * Sprint 3: estos arrays se reemplazan por llamadas a la API REST del backend.
 *           Los tipos (Category, Product, Service) se mueven a types/index.ts
 *           y se reutilizan en los hooks de TanStack Query.
 *
 * No agregar lógica de negocio acá — solo data shapes y valores iniciales.
 */

// ─────────────────────────────────────────────────────────────────────────────
// TYPES
// ─────────────────────────────────────────────────────────────────────────────

export type Category = {
  name: string
  slug: string
  emoji: string
  count: number
}

export type Product = {
  id: number
  slug: string
  name: string
  price: number
  oldPrice?: number
  category: string       // coincide con Category.slug
  emoji: string
  badge?: string
  badgeColor?: string    // clase de Tailwind — ej: 'bg-brand-500'
  description: string
  rating: number
  reviews: number
  stock: number
}

export type Service = {
  id: number
  name: string
  duration: string
  priceLabel: string
  priceFrom: number
  emoji: string
  description: string
  features: string[]
  highlight?: boolean    // true → tarjeta oscura (Spa Premium)
}

export type Stat = {
  value: string
  label: string
}

export type WhyUsItem = {
  emoji: string
  title: string
  description: string
}

// ─────────────────────────────────────────────────────────────────────────────
// DATA
// ─────────────────────────────────────────────────────────────────────────────

export const CATEGORIES: Category[] = [
  { name: 'Perros',  slug: 'perros',  emoji: '🐕', count: 48 },
  { name: 'Gatos',   slug: 'gatos',   emoji: '🐱', count: 32 },
  { name: 'Aves',    slug: 'aves',    emoji: '🦜', count: 15 },
  { name: 'Peces',   slug: 'peces',   emoji: '🐟', count: 22 },
  { name: 'Higiene', slug: 'higiene', emoji: '🛁', count: 19 },
  { name: 'Salud',   slug: 'salud',   emoji: '💊', count: 11 },
]

export const PRODUCTS: Product[] = [
  {
    id: 1,
    slug: 'royal-canin-medium-adult-15kg',
    name: 'Royal Canin Medium Adult 15kg',
    price: 18500,
    oldPrice: 21000,
    category: 'perros',
    emoji: '🐕',
    badge: 'Más vendido',
    badgeColor: 'bg-brand-500',
    description: 'Alimento completo para perros medianos adultos de 1 a 7 años.',
    rating: 4.8,
    reviews: 124,
    stock: 8,
  },
  {
    id: 2,
    slug: 'arena-silice-crystals-8l',
    name: 'Arena Sílice Crystals 8L',
    price: 4200,
    category: 'gatos',
    emoji: '🐱',
    badge: 'Oferta -20%',
    badgeColor: 'bg-danger',
    description: 'Arena ultraabsorbente de sílice, sin polvo y sin olor.',
    rating: 4.6,
    reviews: 89,
    stock: 15,
  },
  {
    id: 3,
    slug: 'kong-classic-rojo-talle-m',
    name: 'Kong Classic Rojo Talle M',
    price: 3800,
    category: 'perros',
    emoji: '🦴',
    description: 'Juguete resistente rellenable con premios para estimulación mental.',
    rating: 4.9,
    reviews: 203,
    stock: 5,
  },
  {
    id: 4,
    slug: 'whiskas-pouch-salmon-x12',
    name: 'Whiskas Pouch Salmón x12',
    price: 5600,
    oldPrice: 6200,
    category: 'gatos',
    emoji: '🐟',
    badge: 'Nuevo',
    badgeColor: 'bg-success',
    description: 'Pack de 12 bolsitas húmedas sabor salmón para gatos adultos.',
    rating: 4.5,
    reviews: 67,
    stock: 20,
  },
  {
    id: 5,
    slug: 'shampoo-hipoalergenico-500ml',
    name: 'Shampoo Hipoalergénico 500ml',
    price: 2900,
    category: 'higiene',
    emoji: '🛁',
    description: 'Fórmula suave para pieles sensibles, sin parabenos.',
    rating: 4.7,
    reviews: 45,
    stock: 12,
  },
  {
    id: 6,
    slug: 'collar-antiparasitario-8-meses',
    name: 'Collar Antiparasitario 8 meses',
    price: 8900,
    category: 'salud',
    emoji: '💊',
    badge: 'Recomendado',
    badgeColor: 'bg-amber-600',
    description: 'Protección continua 8 meses contra pulgas y garrapatas.',
    rating: 4.8,
    reviews: 156,
    stock: 7,
  },
  {
    id: 7,
    slug: 'pedigree-adulto-razas-grandes-18kg',
    name: 'Pedigree Adulto Razas Grandes 18kg',
    price: 15800,
    category: 'perros',
    emoji: '🐶',
    description: 'Fórmula con pollo y vegetales para razas grandes activas.',
    rating: 4.4,
    reviews: 88,
    stock: 10,
  },
  {
    id: 8,
    slug: 'repelente-antipulgas-spray-250ml',
    name: 'Repelente Antipulgas Spray 250ml',
    price: 3200,
    category: 'salud',
    emoji: '🌿',
    description: 'Fórmula natural a base de extracto de neem y lavanda.',
    rating: 4.3,
    reviews: 31,
    stock: 18,
  },
]

export const FEATURED_PRODUCTS = PRODUCTS.slice(0, 4)

export const SERVICES: Service[] = [
  {
    id: 1,
    name: 'Baño & Secado',
    duration: '1h 30min',
    priceLabel: 'desde $3.500',
    priceFrom: 3500,
    emoji: '🛁',
    description: 'Baño con productos premium, secado profesional y perfume.',
    features: [
      'Shampoo premium',
      'Secado profesional',
      'Perfume incluido',
      'Limpieza de oídos',
    ],
  },
  {
    id: 2,
    name: 'Peluquería Completa',
    duration: '2h - 3h',
    priceLabel: 'desde $5.000',
    priceFrom: 5000,
    emoji: '✂️',
    description: 'Corte a medida según raza, baño incluido y extras.',
    features: [
      'Baño incluido',
      'Corte según raza',
      'Uñas cortadas',
      'Moño o bandana',
    ],
  },
  {
    id: 3,
    name: 'Spa Premium ✨',
    duration: '2h 30min',
    priceLabel: 'desde $7.500',
    priceFrom: 7500,
    emoji: '⭐',
    description: 'Experiencia completa de bienestar para tu compañero.',
    features: [
      'Todo lo anterior',
      'Masaje relajante',
      'Hidratación de pelaje',
      'Foto de recuerdo',
    ],
    highlight: true,
  },
]

export const STATS: Stat[] = [
  { value: '4.9 ★', label: 'Calificación promedio' },
  { value: '+500',  label: 'Mascotas atendidas'    },
  { value: '+3 años', label: 'De experiencia'      },
]

export const WHY_US: WhyUsItem[] = [
  {
    emoji: '🚀',
    title: 'Entrega el mismo día',
    description: 'Pedidos por WhatsApp y entrega el mismo día en Pehuajó.',
  },
  {
    emoji: '🏆',
    title: 'Productos premium',
    description: 'Solo marcas líderes con calidad y trazabilidad garantizada.',
  },
  {
    emoji: '💬',
    title: 'Asesoramiento real',
    description: 'Hablás con personas reales que conocen las necesidades de tu mascota.',
  },
  {
    emoji: '📅',
    title: 'Turnos online',
    description: 'Reservá tu turno fácil y rápido, disponible las 24 horas.',
  },
]

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Formatea un número como precio en pesos argentinos. Ej: 18500 → "$18.500" */
export function fmt(n: number): string {
  return `$${n.toLocaleString('es-AR')}`
}

/** Genera un link de WhatsApp con mensaje pre-cargado. */
export function waLink(msg: string): string {
  const number = process.env.NEXT_PUBLIC_WA_NUMBER ?? '5492396000000'
  return `https://wa.me/${number}?text=${encodeURIComponent(msg)}`
}
