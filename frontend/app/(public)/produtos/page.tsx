import { listProducts, listCategories } from '@/lib/api/products'
import { LoadMoreProducts } from '@/components/public/load-more-products'
import { CategoryFilter } from '@/components/public/category-filter'

// Tarea 3.6 (issue #16). Server Component: la primera página se fetchea acá
// (SEO, sin loading state inicial); <LoadMoreProducts> toma la posta para
// "Carregar mais" del lado del cliente.
//
// force-dynamic: sin esto, Next 16 prerenderiza esta página como estática en
// build time y el catálogo queda congelado con lo que había en ese momento —
// un producto que el admin agregue/edite (tarea 3.4, ya existe) no aparecería
// hasta el próximo deploy. La estrategia de cache real es la tarea 3.11
// (revalidateTag con cacheLife) — hasta que esté, dynamic es lo correcto, no
// un atajo.
export const dynamic = 'force-dynamic'

export default async function ProductsPage(props: PageProps<'/produtos'>) {
  const { categoria } = await props.searchParams
  const categoriaSlug = typeof categoria === 'string' ? categoria : undefined

  // En paralelo: categorías no dependen del filtro elegido.
  const [data, categories] = await Promise.all([
    listProducts({ categoria: categoriaSlug }),
    listCategories(),
  ])

  return (
    <div className="mx-auto max-w-content px-6 py-8 lg:px-8 lg:py-12">
      <h1 className="mb-8 text-h2-mobile font-display text-ink md:text-h2">Produtos</h1>
      <CategoryFilter categories={categories} activeSlug={categoriaSlug} />
      {/* key: fuerza el remount al cambiar de categoría — ver comentario en
          <LoadMoreProducts>, si no el estado de cliente queda con la lista vieja. */}
      <LoadMoreProducts key={categoriaSlug ?? 'todos'} initialData={data} categoria={categoriaSlug} />
    </div>
  )
}
