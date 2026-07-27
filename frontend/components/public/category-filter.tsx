import Link from 'next/link'
import type { TaxonRef } from '@/lib/api/client'

interface CategoryFilterProps {
  categories: TaxonRef[]
  activeSlug?: string
  busca?: string
}

// Server Component a propósito: el filtro vive en la URL (?categoria=slug,
// tarea 3.7), no en estado de cliente — cada chip es un <Link>, sin JS.
// `busca` se re-agrega al href de cada chip para no perder el término de
// búsqueda al cambiar de categoría (tarea 3.8 — combinables entre sí).
export function CategoryFilter({ categories, activeSlug, busca }: CategoryFilterProps) {
  const buscaQuery = busca ? `&busca=${encodeURIComponent(busca)}` : ''

  return (
    <nav className="mb-6 flex gap-2 overflow-x-auto whitespace-nowrap pb-1">
      <Link
        href={busca ? `/produtos?busca=${encodeURIComponent(busca)}` : '/produtos'}
        className={
          activeSlug
            ? 'shrink-0 rounded-full border border-outline px-4 py-1.5 text-label text-ink-muted transition-opacity hover:opacity-80'
            : 'shrink-0 rounded-full bg-orange px-4 py-1.5 text-label text-white'
        }
      >
        Todos
      </Link>
      {categories.map((category) => (
        <Link
          key={category.slug}
          href={`/produtos?categoria=${category.slug}${buscaQuery}`}
          className={
            category.slug === activeSlug
              ? 'shrink-0 rounded-full bg-orange px-4 py-1.5 text-label text-white'
              : 'shrink-0 rounded-full border border-outline px-4 py-1.5 text-label text-ink-muted transition-opacity hover:opacity-80'
          }
        >
          {category.nome}
        </Link>
      ))}
    </nav>
  )
}
