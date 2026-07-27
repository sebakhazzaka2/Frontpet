const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

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
