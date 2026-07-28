// Tarea 3.10 (issue #20). Mismo layout que <ProductDetailView> para que no
// salte al llegar los datos reales.
export default function ProductDetailLoading() {
  return (
    <div className="mx-auto max-w-content px-6 py-8 lg:px-8 lg:py-12">
      <div className="mb-4 h-4 w-64 animate-pulse rounded bg-outline/20" />

      <div className="lg:grid lg:grid-cols-2 lg:gap-8">
        <div className="mb-6 aspect-square animate-pulse rounded-lg bg-outline/20 lg:mb-0" />

        <div className="flex flex-col gap-4">
          <div className="h-8 w-3/4 animate-pulse rounded bg-outline/20" />
          <div className="h-8 w-1/3 animate-pulse rounded bg-outline/20" />
          <div className="h-20 w-full animate-pulse rounded-lg bg-outline/20" />
          <div className="h-24 w-full animate-pulse rounded bg-outline/20" />
          <div className="h-12 w-full animate-pulse rounded-md bg-outline/20" />
        </div>
      </div>
    </div>
  )
}
