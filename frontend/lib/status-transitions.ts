// Espejo de OrderServiceImpl.TRANSITIONS / AppointmentServiceImpl.TRANSITIONS
// (backend, ambos idênticos — CLAUDE.md §6: só PENDING/CONFIRMED/CANCELLED,
// nunca se volta a PENDING). Fonte única para os dois painéis de admin
// (appointment-detail-panel, order-detail-panel) não duplicarem a regra.
export type OperationalStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

const ALLOWED_TRANSITIONS: Record<OperationalStatus, ReadonlySet<OperationalStatus>> = {
  PENDING: new Set(['CONFIRMED', 'CANCELLED']),
  CONFIRMED: new Set(['CANCELLED']),
  CANCELLED: new Set(),
}

export function canTransitionStatus(
  current: OperationalStatus,
  target: OperationalStatus
): boolean {
  return current === target || ALLOWED_TRANSITIONS[current].has(target)
}
