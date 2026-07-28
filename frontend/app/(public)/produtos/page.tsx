import { listProducts, listCategories } from '@/lib/api/products'
import { LoadMoreProducts } from '@/components/public/load-more-products'
import { CategoryFilter } from '@/components/public/category-filter'
import { ProductSearch } from '@/components/public/product-search'

// Tarea 3.6 (issue #16). Server Component: la primera página se fetchea acá
// (SEO, sin loading state inicial); <LoadMoreProducts> toma la posta para
// "Carregar mais" del lado del cliente.
//
// Sin force-dynamic (tarea 3.11): el ISR de listProducts/listCategories
// (next.revalidate: 60, en lib/api/products.ts) ya resuelve el problema que
// force-dynamic tapaba en la 3.6 — un producto editado por el admin aparece
// en como máximo 60s, no recién en el próximo deploy. force-dynamic e ISR
// no son compatibles: force-dynamic fuerza revalidate:0 en todo fetch de la
// ruta (doc de Next 16), así que dejarlo puesto habría vuelto inerte el
// revalidate de abajo.
export default async function ProductsPage(props: PageProps<'/produtos'>) {
  const { categoria, busca } = await props.searchParams
  const categoriaSlug = typeof categoria === 'string' ? categoria : undefined
  const buscaTermo = typeof busca === 'string' ? busca : undefined

  // En paralelo: categorías no dependen del filtro elegido.
  const [data, categories] = await Promise.all([
    listProducts({ categoria: categoriaSlug, busca: buscaTermo }),
    listCategories(),
  ])

  return (
    <div className="mx-auto max-w-content px-6 py-8 lg:px-8 lg:py-12">
      <h1 className="mb-8 text-h2-mobile font-display text-ink md:text-h2">Produtos</h1>
      <ProductSearch />
      <CategoryFilter categories={categories} activeSlug={categoriaSlug} busca={buscaTermo} />
      {/* key: fuerza el remount al cambiar de filtro — ver comentario en
          <LoadMoreProducts>, si no el estado de cliente queda con la lista vieja. */}
      <LoadMoreProducts
        key={`${categoriaSlug ?? 'todos'}-${buscaTermo ?? ''}`}
        initialData={data}
        categoria={categoriaSlug}
        busca={buscaTermo}
      />
    </div>
  )
}
