/**
 * TEMPORARY — verificación end-to-end de Sentry en producción (D.8 del
 * deploy-runbook). Revienta a propósito al renderizarse para confirmar que
 * el error llega al dashboard de Sentry. Borrar esta carpeta entera apenas
 * se verifique; buscar "TEMPORARY" + "D.8" para encontrar todo lo relacionado.
 */
export default function SentryTestPage() {
  throw new Error('TEMPORARY — Sentry D.8 verification, safe to delete this whole route')
}
