'use client'

import { useState } from 'react'
import { ProductCard } from './product-card'
import { listProducts } from '@/lib/api/products'
import type { Product } from '@/lib/data/products'
import type { PageResponse } from '@/lib/api/client'

interface LoadMoreProductsProps {
  initialData: PageResponse<Product>
  categoria?: string
}

// 'use client': mantiene el estado de la lista acumulada y dispara el fetch
// de la próxima página al click de "Carregar mais" (issue #16 — sin scroll
// infinito, catálogo chico, menos riesgo de romper el Footer fixed).
//
// El padre (page.tsx) tiene que pasar key={categoria} — sin eso, cambiar de
// chip navega a una nueva URL y refetchea initialData, pero este componente
// no se remonta solo: el useState de abajo se queda con los productos de la
// categoría anterior.
export function LoadMoreProducts({ initialData, categoria }: LoadMoreProductsProps) {
  const [products, setProducts] = useState(initialData.items)
  const [page, setPage] = useState(initialData.page)
  const [hasNext, setHasNext] = useState(initialData.hasNext)
  const [loading, setLoading] = useState(false)

  async function handleLoadMore() {
    setLoading(true)
    try {
      const next = await listProducts({ page: page + 1, size: initialData.size, categoria })
      setProducts((prev) => [...prev, ...next.items])
      setPage(next.page)
      setHasNext(next.hasNext)
    } finally {
      setLoading(false)
    }
  }

  if (products.length === 0) {
    return <p className="text-center text-sm text-ink-muted">Nenhum produto encontrado.</p>
  }

  return (
    <>
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4 lg:gap-6">
        {products.map((product) => (
          <ProductCard key={product.slug} product={product} />
        ))}
      </div>

      {hasNext && (
        <div className="mt-8 flex justify-center">
          <button
            type="button"
            onClick={handleLoadMore}
            disabled={loading}
            className="rounded-md border border-outline px-6 py-2 text-label font-semibold text-ink transition-opacity hover:opacity-80 disabled:opacity-50"
          >
            {loading ? 'Carregando...' : 'Carregar mais'}
          </button>
        </div>
      )}
    </>
  )
}
