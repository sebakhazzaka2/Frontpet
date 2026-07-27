import Link from 'next/link'
import type { TaxonRef } from '@/lib/api/client'

interface CategoryFilterProps {
  categories: TaxonRef[]
  activeSlug?: string
}

// Server Component a propósito: el filtro vive en la URL (?categoria=slug,
// tarea 3.7), no en estado de cliente — cada chip es un <Link>, sin JS.
export function CategoryFilter({ categories, activeSlug }: CategoryFilterProps) {
  return (
    <nav className="mb-6 flex gap-2 overflow-x-auto whitespace-nowrap pb-1">
      <Link
        href="/produtos"
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
          href={`/produtos?categoria=${category.slug}`}
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
