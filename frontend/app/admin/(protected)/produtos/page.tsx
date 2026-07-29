import { listCategories, listSpecies } from '@/lib/api/products'
import { ProductList } from '@/components/admin/product-list'

// Tarea 4.12/4.13 (issue #33). Categorías/espécies se piden acá server-side
// (mismos endpoints públicos que ya usa /produtos — son referencia de
// tenant, no datos sensibles) y bajan como prop al client component, que es
// el que hace el fetch autenticado del listado real de productos.
export default async function AdminProdutosPage() {
  const [categories, species] = await Promise.all([listCategories(), listSpecies()])

  return <ProductList categories={categories} species={species} />
}
