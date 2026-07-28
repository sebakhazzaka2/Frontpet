import { cookies } from 'next/headers'
import { redirect } from 'next/navigation'
import { AdminShell } from '@/components/admin/admin-shell'
import { SESSION_COOKIE_NAME } from '@/lib/auth/session'

interface ProtectedAdminLayoutProps {
  children: React.ReactNode
}

// Tarea 4.11 (issue #32) — guard del admin. `(protected)` es un route group
// SIN segmento propio en la URL: agrupa todo lo que necesita sesión
// (/admin, /admin/produtos, /admin/pedidos, ...) bajo un mismo layout, sin
// envolver /admin/login — que vive afuera, en app/admin/login/page.tsx,
// como hermano de este grupo (si login estuviera DENTRO de este layout, el
// redirect de acá abajo entraría en loop infinito contra sí misma).
//
// `await cookies()`: Next 16 la volvió async (docs/next16-notes.md). Solo se
// chequea presencia de la cookie, no se decodifica el JWT acá — ver el
// comentario largo en lib/auth/session.ts sobre por qué eso alcanza para
// este bloque.
export default async function ProtectedAdminLayout({ children }: ProtectedAdminLayoutProps) {
  const cookieStore = await cookies()

  if (!cookieStore.has(SESSION_COOKIE_NAME)) {
    redirect('/admin/login')
  }

  return <AdminShell>{children}</AdminShell>
}
