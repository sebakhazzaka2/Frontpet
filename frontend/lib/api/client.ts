const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

// Espejo de TaxonRef (backend/src/main/java/com/frontpet/catalog/dto/TaxonRef.java) —
// referencia mínima a una categoría o especie (mismo shape para las dos, ver comentario
// del .java). Vive acá y no en lib/data/product-detail.ts porque ahora lo consumen
// tanto el detalle de producto como el filtro de categorías (tarea 3.7).
export interface TaxonRef {
  nome: string
  slug: string
}

// Espejo de PageResponse (backend/src/main/java/com/frontpet/common/PageResponse.java) —
// envoltorio explícito porque la forma JSON de PageImpl de Spring no es un contrato
// estable (avisado desde Spring 3.3).
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
  hasNext: boolean
}

// Wrapper mínimo de fetch para Server Components (tarea 3.6a). Sin
// librería nueva — cuando el catálogo necesite cache/refetch en cliente
// (3.7 filtro, 3.8 búsqueda), ahí entra @tanstack/react-query (ya instalado,
// sin uso todavía).
export async function apiFetch<T>(path: string): Promise<T | undefined> {
  const res = await fetch(`${API_URL}/api/v1${path}`)

  if (res.status === 404) {
    return undefined
  }

  if (!res.ok) {
    throw new Error(`API ${path} respondió ${res.status}`)
  }

  return res.json() as Promise<T>
}
