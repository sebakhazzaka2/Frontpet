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

// Espejo de ApiError (backend/src/main/java/com/frontpet/common/ApiError.java) —
// cuerpo de error uniforme de toda la API. `fieldErrors` solo viene poblado en
// errores de validación (@Valid fallido).
export interface ApiErrorBody {
  status: number
  error: string
  message: string
  path: string
  timestamp: string
  fieldErrors?: Record<string, string>
}

// Error tipado que preserva el `message` en PT-BR del backend (ADR 007) y los
// `fieldErrors` para que un formulario (react-hook-form) pueda mapearlos a
// campo por campo sin reparsear el body.
export class ApiFetchError extends Error {
  status: number
  fieldErrors?: Record<string, string>

  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'ApiFetchError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

interface ApiFetchOptions {
  revalidate?: number
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE'
  body?: unknown
  headers?: Record<string, string>
}

// Wrapper de fetch para Server y Client Components (tarea 3.6a, extendido en
// el Bloque 0 del Sprint 4 para soportar escrituras). Sin librería nueva —
// cuando el catálogo necesite cache/refetch en cliente (3.7 filtro, 3.8
// búsqueda), ahí entra @tanstack/react-query (ya instalado, sin uso todavía).
//
// `revalidate` (tarea 3.11): ISR simple vía next.revalidate, confirmado
// contra la doc de Next 16 (no cacheComponents/PPR — el ROADMAP original
// estaba escrito para Next 14). Cuando esto corre en el browser (llamado
// desde <LoadMoreProducts>/<ProductSearch>, Client Components), `next.*` es
// una extensión server-side de Next — el fetch nativo del browser la ignora
// sin error, no hace falta condicionarlo.
//
// `credentials: 'include'`: necesario para que el navegador mande la cookie
// HttpOnly del login (ADR 004) en los endpoints admin — back y front corren
// en orígenes distintos (CorsConfig.java ya habilita allowCredentials). No
// afecta a los endpoints públicos, que no dependen de cookie.
export async function apiFetch<T>(
  path: string,
  { revalidate, method = 'GET', body, headers }: ApiFetchOptions = {}
): Promise<T | undefined> {
  const res = await fetch(`${API_URL}/api/v1${path}`, {
    method,
    credentials: 'include',
    headers: body !== undefined ? { 'Content-Type': 'application/json', ...headers } : headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    next: revalidate !== undefined ? { revalidate } : undefined,
  })

  if (res.status === 404) {
    return undefined
  }

  if (res.status === 204) {
    return undefined
  }

  if (!res.ok) {
    const apiError = (await res.json().catch(() => null)) as ApiErrorBody | null
    throw new ApiFetchError(
      apiError?.message ?? `API ${path} respondió ${res.status}`,
      res.status,
      apiError?.fieldErrors
    )
  }

  // Algunos endpoints (POST /auth/login, POST /auth/logout) devuelven 200
  // con body vacío en vez de 204 — ResponseEntity<Void> de Spring no fuerza
  // 204. res.json() sobre un body vacío tira SyntaxError ("Unexpected end of
  // JSON input"), que el caller terminaba tratando como fallo de login
  // aunque el login hubiera sido exitoso (bug real, encontrado con Playwright
  // al verificar el Bloque D). Leer como texto primero evita asumir status↔body.
  const raw = await res.text()
  if (raw.length === 0) {
    return undefined
  }
  return JSON.parse(raw) as T
}
