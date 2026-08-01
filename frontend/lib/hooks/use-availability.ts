'use client'

import { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getAvailability, type AvailabilityResponse } from '@/lib/api/availability'
import type { Porte } from '@/lib/api/services'

/**
 * Disponibilidad del wizard de agendamento (Bloque C, issue #60). Primer uso
 * real de TanStack Query en el repo — instalado desde la tarea 1.4, sin
 * consumidor hasta acá.
 *
 * Tres cosas que el AC pide explícitamente y que resuelve este hook:
 *
 * 1. **Re-dispara ante cualquier cambio del combo** (base/porte/adicional/
 *    fecha). Sale gratis poniendo el combo entero en la `queryKey`: cambiar
 *    cualquier parte es otra entrada de cache y React Query refetchea sola.
 *    No hace falta un useEffect que dispare a mano.
 *
 * 2. **Debounce**. Se debouncea el *combo* que alimenta la key, no el fetch.
 *    Tocar 3 adicionais seguidos mueve el combo 3 veces pero dispara un solo
 *    request, y lo que se renderiza sigue derivando de la key vigente — no
 *    hay un timer paralelo que pueda quedar desincronizado del render.
 *
 * 3. **Cancelación de requests in-flight**. React Query pasa un AbortSignal
 *    al `queryFn` y lo aborta cuando la key cambia; ese signal llega hasta el
 *    `fetch` (ver `signal` en `lib/api/client.ts`). Sin esto el request viejo
 *    igual llegaría y se descartaría en cliente — funciona, pero deja el
 *    request colgado contra el backend al pedo.
 */

interface UseAvailabilityParams {
  baseServiceId: number | null
  porte: Porte
  addonIds: number[]
  data: string | null
  /** El wizard solo consulta cuando el Passo 2 está a la vista. */
  enabled?: boolean
}

const DEBOUNCE_MS = 250

function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timeout = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timeout)
  }, [value, delayMs])

  return debounced
}

export interface AvailabilityState {
  data: AvailabilityResponse | undefined
  isLoading: boolean
  isError: boolean
  refetch: () => void
}

export function useAvailability({
  baseServiceId,
  porte,
  addonIds,
  data,
  enabled = true,
}: UseAvailabilityParams): AvailabilityState {
  // Ordenado y serializado a string por dos razones distintas:
  // - React Query hashea el array por estructura, así que `[2,1]` y `[1,2]`
  //   serían dos entradas de cache (y dos requests) para un combo idéntico;
  //   al backend no le importa el orden y además dedupe.
  // - `addonIds` es un array nuevo en cada render del wizard. Un debounce
  //   sobre esa identidad reprogramaría el timer para siempre sin llegar a
  //   disparar nunca; sobre el string, solo cambia cuando cambia el contenido.
  const addonKey = [...addonIds].sort((a, b) => a - b).join(',')

  // El array se reconstruye desde `addonKey` en vez de cerrar sobre
  // `addonIds`: así la única dependencia del memo es el contenido, y no hace
  // falta silenciar react-hooks/exhaustive-deps.
  const combo = useMemo(
    () => ({
      baseServiceId,
      porte,
      addonIds: addonKey === '' ? [] : addonKey.split(',').map(Number),
      data,
    }),
    [baseServiceId, porte, addonKey, data]
  )

  const debounced = useDebouncedValue(combo, DEBOUNCE_MS)

  const query = useQuery({
    queryKey: ['availability', debounced.baseServiceId, debounced.porte, debounced.addonIds, debounced.data],
    queryFn: ({ signal }) =>
      getAvailability({
        baseServiceId: debounced.baseServiceId!,
        porte: debounced.porte,
        addonIds: debounced.addonIds,
        data: debounced.data!,
        signal,
      }),
    enabled: enabled && debounced.baseServiceId !== null && debounced.data !== null,
    // La disponibilidad caduca sola: entre que el usuario mira la grilla y
    // elige, otro cliente puede haberse quedado con el cupo. El staleTime
    // global de 60 s (app/providers.tsx) es razonable para el catálogo y
    // demasiado largo acá.
    staleTime: 0,
    refetchOnWindowFocus: true,
  })

  return {
    data: query.data,
    // isFetching y no isLoading: isLoading solo cubre el primer fetch de cada
    // key. Al volver a un combo ya cacheado, isLoading es false mientras
    // revalida y la grilla mostraría los slots viejos como si fueran los del
    // combo nuevo.
    isLoading: query.isFetching,
    isError: query.isError,
    refetch: query.refetch,
  }
}
