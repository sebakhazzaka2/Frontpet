import { AppointmentList } from '@/components/admin/appointment-list'

// Tarea 6.E (issue #62). Toda la lógica de datos vive en <AppointmentList>
// (TanStack Query) — esta página es un shell mínimo, mismo criterio que
// /admin/pedidos y /admin/produtos.
export default function AdminAgendamentosPage() {
  return <AppointmentList />
}
