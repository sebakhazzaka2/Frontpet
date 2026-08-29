'use client'

import { useEffect } from 'react'
import { trackViewContent } from '@/lib/analytics/pixel'

interface ProductViewTrackerProps {
  publicId: string
  nome: string
  categoria?: string
  price?: number
}

// Componente invisível ('use client' mínimo) só pra disparar ViewContent —
// product-detail.tsx segue Server Component (CLAUDE.md §5: "Server
// Components por padrão"), só esta ilha de interatividade vira cliente.
export function ProductViewTracker({ publicId, nome, categoria, price }: ProductViewTrackerProps) {
  useEffect(() => {
    trackViewContent({ publicId, nome, categoria, price })
    // Dispara uma vez por produto visto — não a cada re-render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [publicId])

  return null
}
