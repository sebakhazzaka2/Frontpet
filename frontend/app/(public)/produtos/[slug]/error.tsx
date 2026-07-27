'use client'

import { useEffect } from 'react'

interface ProductDetailErrorProps {
  error: Error & { digest?: string }
  reset: () => void
}

// Tarea 3.10 (issue #20). Ver comentario de app/(public)/produtos/error.tsx
// — mismo criterio, copy distinto porque acá es un producto puntual, no el
// catálogo entero.
export default function ProductDetailError({ error, reset }: ProductDetailErrorProps) {
  useEffect(() => {
    console.error(error)
  }, [error])

  return (
    <div className="mx-auto flex max-w-content flex-col items-center gap-4 px-6 py-24 text-center">
      <h1 className="text-h2-mobile font-display text-ink md:text-h2">
        Não foi possível carregar este produto
      </h1>
      <p className="text-sm text-ink-muted">Tente novamente em alguns instantes.</p>
      <button
        type="button"
        onClick={reset}
        className="rounded-md bg-orange px-6 py-2 text-label font-semibold text-white transition-opacity hover:opacity-90"
      >
        Tentar novamente
      </button>
    </div>
  )
}
