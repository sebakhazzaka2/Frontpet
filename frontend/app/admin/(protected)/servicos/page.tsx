import { ServicesManager } from '@/components/admin/services-manager'

// Tarea 6.F (issue #63). Toda a lógica de dados vive em <ServicesManager> e
// seus componentes filhos — esta página é um shell mínimo, mesmo critério de
// /admin/pedidos, /admin/produtos e /admin/agendamentos.
export default function AdminServicosPage() {
  return <ServicesManager />
}
