'use client'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'

/**
 * Providers — wrapper de cliente para el App Router.
 *
 * Por qué está separado del layout:
 * layout.tsx es un Server Component. Los providers de React (QueryClientProvider,
 * futuros: ToastProvider, etc.) necesitan 'use client'. Separándolos podemos
 * mantener el layout como Server Component y ganar el beneficio de streaming SSR.
 *
 * Para agregar providers futuros (ej: Sonner toast, ThemeProvider):
 * agregarlos acá, no en layout.tsx.
 */

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        // Con SSR, queremos que los datos no se marquen como stale inmediatamente
        // para evitar un refetch innecesario al montar el componente en el cliente.
        staleTime: 60 * 1000, // 1 minuto
        retry: 1,
        refetchOnWindowFocus: false,
      },
    },
  })
}

// Singleton del QueryClient en el browser (evitar recrear en cada render)
let browserQueryClient: QueryClient | undefined

function getQueryClient() {
  if (typeof window === 'undefined') {
    // Server: siempre crear uno nuevo
    return makeQueryClient()
  }
  // Browser: reusar la instancia
  if (!browserQueryClient) browserQueryClient = makeQueryClient()
  return browserQueryClient
}

interface ProvidersProps {
  children: React.ReactNode
}

export function Providers({ children }: ProvidersProps) {
  // Usar useState para que el QueryClient no se recree en cada render de Suspense
  const [queryClient] = useState(() => getQueryClient())

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {/* Agregar ReactQueryDevtools solo en desarrollo: */}
      {/* {process.env.NODE_ENV === 'development' && <ReactQueryDevtools />} */}
    </QueryClientProvider>
  )
}
