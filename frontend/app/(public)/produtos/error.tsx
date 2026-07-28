'use client'

import { useEffect } from 'react'

interface ProductsErrorProps {
  error: Error & { digest?: string }
  reset: () => void
}

// Tarea 3.10 (issue #20). error.tsx tiene que ser Client Component (regla de
// Next) — captura fallos del fetch en page.tsx (ej. backend caído) en vez de
// la pantalla en blanco default. Sin Sentry todavía (Sprint Despliegue),
// console.error es lo mínimo para no perder el error en dev.
export default function ProductsError({ error, reset }: ProductsErrorProps) {
  useEffect(() => {
    console.error(error)
  }, [error])

  return (
    <div className="mx-auto flex max-w-content flex-col items-center gap-4 px-6 py-24 text-center">
      <h1 className="text-h2-mobile font-display text-ink md:text-h2">
        Não foi possível carregar os produtos
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
