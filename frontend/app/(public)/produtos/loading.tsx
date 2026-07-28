// Tarea 3.10 (issue #20). Next 16 envuelve la ruta en <Suspense> con esto
// mientras page.tsx (Server Component) resuelve el fetch — mismas clases de
// grid que el real para que no salte el layout al llegar los datos.
export default function ProductsLoading() {
  return (
    <div className="mx-auto max-w-content px-6 py-8 lg:px-8 lg:py-12">
      <div className="mb-8 h-9 w-40 animate-pulse rounded-md bg-outline/20" />
      <div className="mb-4 h-10 w-full animate-pulse rounded-md bg-outline/20" />
      <div className="mb-6 flex gap-2">
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="h-8 w-20 shrink-0 animate-pulse rounded-full bg-outline/20" />
        ))}
      </div>
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4 lg:gap-6">
        {Array.from({ length: 8 }).map((_, i) => (
          <div
            key={i}
            className="flex flex-col overflow-hidden rounded-lg bg-surface-card shadow-card"
          >
            <div className="aspect-square animate-pulse bg-outline/20" />
            <div className="flex flex-col gap-2 p-4">
              <div className="h-4 w-3/4 animate-pulse rounded bg-outline/20" />
              <div className="h-5 w-1/2 animate-pulse rounded bg-outline/20" />
              <div className="h-10 w-full animate-pulse rounded-md bg-outline/20" />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
