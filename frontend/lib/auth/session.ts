// Espejo de SessionCookie.NAME (backend/src/main/java/com/frontpet/identity/SessionCookie.java)
// — el guard del admin (app/admin/(protected)/layout.tsx) solo chequea que la
// cookie EXISTA, no valida el JWT en Next.js: verificar la firma acá
// requeriría importar una librería de JWT y compartir el secret entre dos
// runtimes (CLAUDE.md §6 — no libs sin preguntarse si hace falta). La
// verificación real ya la hace el backend en cada request (JwtAuthFilter);
// si la cookie está vencida o es inválida, cualquier fetch admin devuelve 401
// (ApiFetchError) y de ahí se puede redirigir a /admin/login del lado
// cliente. Este guard solo evita el flash de UI protegida sin cookie.
export const SESSION_COOKIE_NAME = 'frontpet_session'
