'use client'

import { useEffect, useState } from 'react'
import { usePathname, useRouter, useSearchParams } from 'next/navigation'
import { Search } from 'lucide-react'
import { trackSearch } from '@/lib/analytics/pixel'

// 'use client': input controlado + debounce de 300ms antes de navegar (tarea
// 3.8) — la URL (?busca=) es la fuente de verdad, no un estado que se pierda
// al recargar.
//
// El timeout lee window.location.search recién cuando dispara, no en el
// render en que se programó — si en el medio cambia el filtro de categoría
// (otro <Link>), el debounce no lo pisa con un searchParams viejo.
export function ProductSearch() {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()
  const [value, setValue] = useState(searchParams.get('busca') ?? '')

  useEffect(() => {
    const timeout = setTimeout(() => {
      const params = new URLSearchParams(window.location.search)
      if (value) {
        params.set('busca', value)
        // Só dispara com termo não vazio — evita disparar no mount inicial
        // (quando `value` já vem de ?busca=) e ao limpar o campo.
        trackSearch(value)
      } else {
        params.delete('busca')
      }
      router.push(`${pathname}?${params}`)
    }, 300)

    return () => clearTimeout(timeout)
  }, [value, pathname, router])

  return (
    <div className="relative mb-4">
      <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-muted" />
      <input
        type="search"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Buscar produtos..."
        aria-label="Buscar produtos"
        className="w-full rounded-md border border-outline bg-surface-card py-2 pl-9 pr-3 text-sm text-ink placeholder:text-ink-muted focus:outline-none focus:ring-2 focus:ring-navy"
      />
    </div>
  )
}
