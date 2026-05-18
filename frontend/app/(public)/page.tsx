/**
 * app/(public)/page.tsx — Home / Landing
 *
 * Server Component. Sin 'use client'.
 * Todos los datos vienen de lib/data.ts (estáticos en Sprint 2).
 * Sprint 3: reemplazar FEATURED_PRODUCTS y SERVICES por fetch() al backend.
 *
 * Secciones:
 *   1. Hero
 *   2. Trust bar
 *   3. Category strip
 *   4. Productos destacados
 *   5. Servicios CTA (dark)
 *   6. Por qué elegirnos
 *   7. WhatsApp CTA
 *   8. Botón WA flotante
 */

import type { Metadata } from 'next'
import Link from 'next/link'
import {
  CATEGORIES,
  FEATURED_PRODUCTS,
  SERVICES,
  STATS,
  WHY_US,
  fmt,
  waLink,
  type Product,
  type Service,
} from '@/lib/data'

// ─── Metadata ────────────────────────────────────────────────────────────────
// Sobreescribe el title template de layout.tsx para que el home muestre
// el título completo sin el " | FrontPet" redundante.

export const metadata: Metadata = {
  title: 'FrontPet — Cuidado profesional para tu mascota',
  alternates: { canonical: '/' },
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE
// ─────────────────────────────────────────────────────────────────────────────

export default function HomePage() {
  return (
    <>
      <HeroSection />
      <TrustBar />
      <CategoryStrip />
      <FeaturedProducts />
      <ServicesCta />
      <WhyUsSection />
      <WaCta />
      <FloatingWa />
    </>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. HERO
// ─────────────────────────────────────────────────────────────────────────────

function HeroSection() {
  return (
    <section
      className="relative overflow-hidden bg-brand-50 px-4 py-16 md:py-24"
      aria-label="Presentación de FrontPet"
    >
      {/* Decoración de fondo — círculos difuminados */}
      <div
        className="pointer-events-none absolute -right-20 -top-20 h-80 w-80 rounded-full bg-brand-200 opacity-30 blur-3xl"
        aria-hidden="true"
      />
      <div
        className="pointer-events-none absolute -bottom-16 -left-16 h-60 w-60 rounded-full bg-amber-400 opacity-15 blur-3xl"
        aria-hidden="true"
      />

      <div className="container-main relative grid grid-cols-1 items-center gap-10 lg:grid-cols-2 lg:gap-16">

        {/* Contenido */}
        <div>
          {/* Eyebrow */}
          <p className="eyebrow mb-5">
            🐾 Todo para tu mascota, en un solo lugar
          </p>

          {/* Titular — DM Serif Display via font-display */}
          <h1 className="font-display text-3xl text-stone-900 md:text-[52px] md:leading-[1.1]">
            El mejor cuidado{' '}
            <span className="text-brand-500">merece lo mejor</span>
          </h1>

          <p className="mt-5 max-w-md text-lg text-stone-500">
            Productos premium, baños profesionales y peluquería especializada.
            Pedidos por WhatsApp en segundos.
          </p>

          {/* CTAs */}
          <div className="mt-8 flex flex-wrap gap-3">
            <Link href="/productos" className="btn-primary">
              🛍️ Ver Productos →
            </Link>
            <Link href="/turnos" className="btn-outline">
              📅 Reservar Turno
            </Link>
          </div>

          {/* Trust items inline */}
          <div className="mt-8 flex flex-wrap gap-5">
            {STATS.map((s) => (
              <div key={s.label} className="flex items-center gap-2">
                <span className="text-sm font-bold text-stone-900">{s.value}</span>
                <span className="text-sm text-stone-400">{s.label}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Ilustración — solo desktop */}
        <div className="hidden justify-center lg:flex" aria-hidden="true">
          <div className="relative flex h-80 w-80 animate-pulse items-center justify-center rounded-full bg-gradient-to-br from-brand-200 to-amber-100 text-[130px] shadow-brand-lg">
            🐕
            {/* Floating card 1 */}
            <div className="absolute -right-6 top-8 flex items-center gap-3 rounded-2xl bg-white px-4 py-3 shadow-lg">
              <span className="text-2xl">✨</span>
              <div>
                <p className="text-xs font-bold text-stone-900">Spa Premium</p>
                <p className="text-xs font-semibold text-brand-500">Nuevo servicio</p>
              </div>
            </div>
            {/* Floating card 2 */}
            <div className="absolute -left-8 bottom-12 flex items-center gap-3 rounded-2xl bg-white px-4 py-3 shadow-lg">
              <span className="text-2xl">💬</span>
              <div>
                <p className="text-xs font-bold text-stone-900">¡Pedido enviado!</p>
                <p className="text-xs font-semibold text-wa-dark">Vía WhatsApp</p>
              </div>
            </div>
          </div>
        </div>

      </div>
    </section>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. TRUST BAR
// ─────────────────────────────────────────────────────────────────────────────

function TrustBar() {
  const items = [
    { icon: '⭐', label: '4.9 de calificación' },
    { icon: '🐾', label: '+500 mascotas atendidas' },
    { icon: '⚡', label: 'Entrega el mismo día' },
    { icon: '📍', label: 'Pehuajó, Buenos Aires' },
  ]

  return (
    <div className="border-b border-stone-200 bg-white px-4 py-4">
      <div className="container-main">
        <ul className="flex flex-wrap items-center justify-center gap-x-8 gap-y-2">
          {items.map((item) => (
            <li
              key={item.label}
              className="flex items-center gap-2 text-sm font-medium text-stone-600"
            >
              <span aria-hidden="true">{item.icon}</span>
              {item.label}
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. CATEGORY STRIP
// ─────────────────────────────────────────────────────────────────────────────

function CategoryStrip() {
  return (
    <section
      className="border-b border-stone-200 bg-white px-4 py-5"
      aria-label="Categorías de productos"
    >
      <div className="container-main">
        {/* Scroll horizontal en mobile, wrap en desktop */}
        <div className="flex gap-2 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {CATEGORIES.map((cat) => (
            <Link
              key={cat.slug}
              href={`/productos?categoria=${cat.slug}`}
              className="flex flex-shrink-0 items-center gap-2 rounded-full border border-stone-200 bg-stone-50 px-4 py-2 text-sm font-semibold text-stone-700 transition-all hover:border-brand-300 hover:bg-brand-50 hover:text-brand-600"
            >
              <span aria-hidden="true">{cat.emoji}</span>
              {cat.name}
              <span className="rounded-full bg-brand-500 px-2 py-0.5 text-xs font-bold text-white">
                {cat.count}
              </span>
            </Link>
          ))}
        </div>
      </div>
    </section>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. PRODUCTOS DESTACADOS
// ─────────────────────────────────────────────────────────────────────────────

function FeaturedProducts() {
  return (
    <section className="section bg-stone-50" aria-labelledby="featured-products-title">
      <div className="container-main">

        <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
          <div>
            <span className="eyebrow">Destacados</span>
            <h2
              id="featured-products-title"
              className="font-display text-2xl text-stone-900"
            >
              Productos más elegidos
            </h2>
            <p className="mt-1 text-stone-500">Lo más elegido por nuestros clientes</p>
          </div>
          <Link
            href="/productos"
            className="btn-outline text-sm"
          >
            Ver todos →
          </Link>
        </div>

        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          {FEATURED_PRODUCTS.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>

      </div>
    </section>
  )
}

// ── Product Card ──────────────────────────────────────────────────────────────
// Server-rendered en Sprint 2. Sprint 4: extraer a components/public/product-card.tsx
// como Client Component para agregar el botón de like (useState).

function ProductCard({ product: p }: { product: Product }) {
  const msg =
    `Hola FrontPet! 🐾 Me interesa:\n\n` +
    `*${p.name}*\nPrecio: ${fmt(p.price)}\n\n¿Tienen disponibilidad?`

  return (
    <article className="card group flex flex-col" aria-label={p.name}>

      {/* Badge */}
      {p.badge && (
        <div className="absolute left-3 top-3 z-10">
          <span className={`badge ${p.badgeColor ?? 'bg-stone-500'} text-white`}>
            {p.badge}
          </span>
        </div>
      )}

      {/* Imagen / Emoji placeholder */}
      {/* Sprint 3: reemplazar por <Image> de Next.js con la foto real del producto */}
      <div
        className="relative flex aspect-square items-center justify-center bg-gradient-to-br from-brand-50 to-brand-100 text-6xl transition-transform duration-300 group-hover:scale-110"
        aria-hidden="true"
      >
        {p.emoji}
      </div>

      {/* Info */}
      <div className="flex flex-1 flex-col gap-2 p-3 md:p-4">

        <h3 className="line-clamp-2 text-sm font-semibold leading-snug text-stone-900 md:text-base">
          {p.name}
        </h3>

        <p className="line-clamp-2 text-xs text-stone-500 md:text-sm">
          {p.description}
        </p>

        {/* Rating */}
        <div className="flex items-center gap-1.5 text-sm">
          <span className="text-amber-400" aria-hidden="true">★</span>
          <span className="font-bold text-stone-900">{p.rating}</span>
          <span className="text-stone-400">({p.reviews})</span>
          {p.stock <= 8 && (
            <span className="ml-auto text-xs font-bold text-danger-dark">
              ⚡ Solo {p.stock}
            </span>
          )}
        </div>

        {/* Precio */}
        <div className="mt-auto">
          <div className="flex items-baseline gap-2">
            <span className="text-lg font-extrabold text-brand-500">{fmt(p.price)}</span>
            {p.oldPrice && (
              <span className="text-xs text-stone-400 line-through">{fmt(p.oldPrice)}</span>
            )}
          </div>
        </div>

        {/* CTA WhatsApp */}
        <a
          href={waLink(msg)}
          target="_blank"
          rel="noopener noreferrer"
          className="btn-wa mt-1 text-sm"
        >
          💬 Pedir por WhatsApp
        </a>

      </div>
    </article>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. SERVICIOS CTA — sección oscura
// ─────────────────────────────────────────────────────────────────────────────

function ServicesCta() {
  return (
    <section
      className="section bg-stone-900"
      aria-labelledby="services-title"
    >
      <div className="container-main">

        {/* Header */}
        <div className="mb-10 text-center">
          <span className="eyebrow text-brand-400">Servicios</span>
          <h2
            id="services-title"
            className="font-display text-2xl text-white md:text-3xl"
          >
            ✂️ Peluquería & Baños
          </h2>
          <p className="mt-3 text-stone-400">
            Profesionales especializados en el cuidado de tu mascota
          </p>
        </div>

        {/* Cards */}
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
          {SERVICES.map((service) => (
            <ServiceCard key={service.id} service={service} />
          ))}
        </div>

        {/* Info del local */}
        <div className="mt-10 flex flex-wrap justify-center gap-6 rounded-2xl border border-stone-700 bg-stone-800/50 p-6">
          {[
            ['📍', 'Pehuajó, Buenos Aires'],
            ['🕐', 'Lun a Sáb · 9:00 – 18:00'],
            ['📱', 'Turnos por WhatsApp'],
            ['🏆', '+3 años de experiencia'],
          ].map(([icon, text]) => (
            <div key={text} className="flex items-center gap-2 text-sm font-medium text-stone-300">
              <span aria-hidden="true">{icon}</span>
              {text}
            </div>
          ))}
        </div>

      </div>
    </section>
  )
}

// ── Service Card ──────────────────────────────────────────────────────────────

function ServiceCard({ service: s }: { service: Service }) {
  const msg =
    `Hola FrontPet! 🐾 Quiero consultar sobre el servicio:\n\n` +
    `*${s.name}* (${s.priceLabel})\n\n¿Tienen disponibilidad?`

  const isHighlight = s.highlight

  return (
    <article
      className={[
        'flex flex-col rounded-2xl p-6 transition-all',
        isHighlight
          ? 'bg-brand-500 text-white'
          : 'border border-stone-700 bg-stone-800 text-white',
      ].join(' ')}
    >
      <span className="mb-4 text-4xl" aria-hidden="true">{s.emoji}</span>

      <h3 className="font-display text-xl text-white">{s.name}</h3>
      <p className={`mt-2 text-sm leading-relaxed ${isHighlight ? 'text-orange-100' : 'text-stone-400'}`}>
        {s.description}
      </p>

      <ul className="my-4 space-y-2">
        {s.features.map((f) => (
          <li key={f} className="flex items-center gap-2 text-sm">
            <span
              className={`font-bold ${isHighlight ? 'text-amber-300' : 'text-brand-400'}`}
              aria-hidden="true"
            >
              ✓
            </span>
            <span className={isHighlight ? 'text-orange-50' : 'text-stone-300'}>{f}</span>
          </li>
        ))}
      </ul>

      <div className="mt-auto">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <p className={`text-xs font-semibold uppercase tracking-wide ${isHighlight ? 'text-orange-200' : 'text-stone-500'}`}>
              Desde
            </p>
            <p className={`text-xl font-extrabold ${isHighlight ? 'text-amber-300' : 'text-brand-400'}`}>
              {s.priceLabel}
            </p>
          </div>
          <div className="text-right">
            <p className={`text-xs font-semibold uppercase tracking-wide ${isHighlight ? 'text-orange-200' : 'text-stone-500'}`}>
              Duración
            </p>
            <p className="text-sm font-semibold text-stone-300">🕐 {s.duration}</p>
          </div>
        </div>

        <a
          href={waLink(msg)}
          target="_blank"
          rel="noopener noreferrer"
          className={[
            'flex w-full items-center justify-center gap-2 rounded-full py-3 text-sm font-bold transition-all',
            isHighlight
              ? 'bg-stone-900 text-white hover:bg-stone-800'
              : 'bg-brand-500 text-white shadow-brand hover:bg-brand-600 hover:shadow-brand-lg',
          ].join(' ')}
        >
          📅 Reservar por WhatsApp
        </a>
      </div>
    </article>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. POR QUÉ ELEGIRNOS
// ─────────────────────────────────────────────────────────────────────────────

function WhyUsSection() {
  return (
    <section className="section bg-white" aria-labelledby="why-us-title">
      <div className="container-main text-center">

        <span className="eyebrow">¿Por qué FrontPet?</span>
        <h2
          id="why-us-title"
          className="font-display text-2xl text-stone-900"
        >
          La tienda de confianza para vos y tu mascota
        </h2>
        <p className="mt-2 text-stone-500">Elegida por más de 500 dueños de mascotas en Pehuajó</p>

        <div className="mt-10 grid grid-cols-2 gap-4 md:grid-cols-4">
          {WHY_US.map((item) => (
            <div
              key={item.title}
              className="rounded-xl border border-stone-200 bg-stone-50 p-6 transition-all hover:-translate-y-1 hover:shadow-md"
            >
              <span className="mb-3 block text-4xl" aria-hidden="true">{item.emoji}</span>
              <h3 className="mb-2 text-sm font-bold text-stone-900 md:text-base">
                {item.title}
              </h3>
              <p className="text-xs leading-relaxed text-stone-500 md:text-sm">
                {item.description}
              </p>
            </div>
          ))}
        </div>

      </div>
    </section>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. WHATSAPP CTA BANNER
// ─────────────────────────────────────────────────────────────────────────────

function WaCta() {
  const msg = 'Hola FrontPet! 🐾 Necesito asesoramiento para mi mascota'

  return (
    <section className="bg-wa px-4 py-12 text-center">
      <div className="mx-auto max-w-prose">
        <span className="mb-3 block text-4xl" aria-hidden="true">💬</span>
        <h2 className="font-display text-2xl text-white md:text-3xl">
          ¿Necesitás asesoramiento?
        </h2>
        <p className="mt-3 text-sm leading-relaxed text-white/85 md:text-base">
          Escribinos por WhatsApp y te ayudamos a elegir el mejor producto
          para tu compañero.
        </p>
        <a
          href={waLink(msg)}
          target="_blank"
          rel="noopener noreferrer"
          className="mt-6 inline-flex items-center gap-3 rounded-full bg-white px-8 py-4 text-base font-bold text-wa-dark shadow-wa-lg transition-all hover:-translate-y-0.5 hover:shadow-xl"
        >
          📱 Escribir al WhatsApp
        </a>
      </div>
    </section>
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. BOTÓN WHATSAPP FLOTANTE
// TODO: mover a app/(public)/layout.tsx para que persista entre navegaciones.
// Por ahora en page.tsx para tenerlo funcionando desde el día 1.
//
// El anillo de pulso usa position:absolute dentro del botón (no fixed),
// por lo que no afecta el contenedor del iframe del visualizador.
// ─────────────────────────────────────────────────────────────────────────────

function FloatingWa() {
  const msg = 'Hola FrontPet! 🐾'

  return (
    <a
      href={waLink(msg)}
      target="_blank"
      rel="noopener noreferrer"
      className="float-wa"
      aria-label="Contactar por WhatsApp"
    >
      {/* Anillo de pulso */}
      <span
        className="absolute inset-0 rounded-full bg-wa animate-wa-pulse"
        aria-hidden="true"
      />
      {/* Ícono */}
      <svg
        className="relative z-10 h-7 w-7"
        viewBox="0 0 24 24"
        fill="white"
        aria-hidden="true"
      >
        <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
      </svg>
    </a>
  )
}
