import { OrderList } from '@/components/admin/order-list'

// Tarea 4.14 (issue #34). Toda la lógica de datos vive en <OrderList>
// (TanStack Query) — esta página es un shell mínimo, igual criterio que
// /admin/produtos.
export default function AdminPedidosPage() {
  return <OrderList />
}
