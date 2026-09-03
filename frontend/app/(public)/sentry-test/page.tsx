/**
 * TEMPORARY — verificación end-to-end de Sentry en producción (D.8 del
 * deploy-runbook). Revienta a propósito al renderizarse para confirmar que
 * el error llega al dashboard de Sentry. Borrar esta carpeta entera apenas
 * se verifique; buscar "TEMPORARY" + "D.8" para encontrar todo lo relacionado.
 */
// force-dynamic: sin esto, Next intenta pre-renderizar la página en build
// time (es una ruta estática común) y el throw tumba el build entero antes
// de llegar a producción — verificado 2026-09-03 contra un run real de CI.
export const dynamic = 'force-dynamic'

export default function SentryTestPage() {
  throw new Error('TEMPORARY — Sentry D.8 verification, safe to delete this whole route')
}
