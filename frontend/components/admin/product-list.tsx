'use client'

import { useCallback, useEffect, useState } from 'react'
import Image from 'next/image'
import { toast } from 'sonner'
import { PawPrint, Plus, Search } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'
import { ProductFormDialog } from '@/components/admin/product-form-dialog'
import {
  listAdminProducts,
  getProductForEdit,
  deactivateProduct,
  type AdminProductSummary,
  type AdminProductDetail,
} from '@/lib/api/admin-products'
import type { TaxonRef } from '@/lib/api/client'
import { formatPrice } from '@/lib/utils'

interface ProductListProps {
  categories: TaxonRef[]
  species: TaxonRef[]
}

// Tarea 4.12/4.13 (issue #33) — port de "Gestão de Produtos". La barra
// lateral y o header já existem (Bloque D, <AdminShell>) — acá se porta
// solo o conteúdo: toolbar de busca/filtro, grid de cards e o modal de alta
// (<ProductFormDialog>). Categorias en sí no tienen CRUD (lista cerrada,
// CLAUDE.md §6) — "edición inline" pasa acá, dentro del form de producto
// (asignar categorías/espécies vía checkbox), no en una pantalla propia.
export function ProductList({ categories, species }: ProductListProps) {
  const [items, setItems] = useState<AdminProductSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [busca, setBusca] = useState('')
  const [incluirInativos, setIncluirInativos] = useState(false)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<AdminProductDetail | undefined>(undefined)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const response = await listAdminProducts({
        busca: busca || undefined,
        incluirInativos,
        size: 48,
      })
      setItems(response.items)
    } catch {
      toast.error('Não foi possível carregar os produtos.')
    } finally {
      setLoading(false)
    }
  }, [busca, incluirInativos])

  // Fetch-on-mount/filter-change estándar: no hay alternativa "más correcta"
  // acá (a diferencia de RHF watch() → useWatch() en checkout-form.tsx) — es
  // el patrón que React documenta para sincronizar con un sistema externo (la API).
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load()
  }, [load])

  function openCreate() {
    setEditing(undefined)
    setDialogOpen(true)
  }

  async function openEdit(publicId: string) {
    try {
      const detail = await getProductForEdit(publicId)
      setEditing(detail)
      setDialogOpen(true)
    } catch {
      toast.error('Não foi possível carregar o produto.')
    }
  }

  async function handleDeactivate(publicId: string) {
    try {
      await deactivateProduct(publicId)
      toast.success('Produto ocultado do catálogo.')
      void load()
    } catch {
      toast.error('Não foi possível ocultar o produto.')
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-h2-mobile font-display text-ink md:text-h2">Produtos</h1>
          <p className="text-body-sm text-ink-muted">{items.length} produto(s) na lista atual</p>
        </div>
        <Button onClick={openCreate} className="w-fit gap-2">
          <Plus className="size-4" /> Novo produto
        </Button>
      </div>

      <div className="flex flex-col gap-3 rounded-lg border border-outline/30 bg-surface-card p-4 shadow-card md:flex-row md:items-center">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-outline" />
          <Input
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            placeholder="Buscar por nome..."
            className="pl-9"
          />
        </div>
        <label className="flex items-center gap-2 text-sm text-ink-muted">
          <Checkbox
            checked={incluirInativos}
            onCheckedChange={(checked) => setIncluirInativos(checked === true)}
          />
          Mostrar inativos
        </label>
      </div>

      {loading ? (
        <p className="text-sm text-ink-muted">Carregando...</p>
      ) : items.length === 0 ? (
        <p className="text-sm text-ink-muted">Nenhum produto encontrado.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {items.map((product) => (
            <div
              key={product.publicId}
              className="flex gap-4 rounded-lg border border-outline/40 bg-white p-4 shadow-card transition-shadow hover:shadow-card-hover"
            >
              <div className="relative size-20 shrink-0 overflow-hidden rounded-md bg-surface">
                {product.mainImageUrl ? (
                  <Image src={product.mainImageUrl} alt="" fill className="object-cover" sizes="80px" />
                ) : (
                  <div className="flex h-full w-full items-center justify-center">
                    <PawPrint className="size-8 text-outline" aria-hidden />
                  </div>
                )}
              </div>

              <div className="flex min-w-0 flex-1 flex-col gap-1">
                {product.brandNome && (
                  <span className="text-[10px] uppercase tracking-wider text-outline">
                    {product.brandNome}
                  </span>
                )}
                <h3 className="truncate text-h3 text-navy">{product.nome}</h3>

                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-semibold text-navy">
                    {product.hasVariants && 'A partir de '}
                    {product.price != null ? formatPrice(product.price) : '—'}
                  </span>
                  <span className="rounded-full bg-surface px-2 py-0.5 text-[10px] text-ink-muted">
                    {product.hasVariants ? 'Com variantes' : `${product.stock ?? 0} em estoque`}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <span
                    className={
                      product.active ? 'size-2 rounded-full bg-wa' : 'size-2 rounded-full bg-outline'
                    }
                  />
                  <span className="text-caption text-ink-muted">
                    {product.active ? 'Ativo' : 'Inativo'}
                  </span>
                </div>

                <div className="mt-2 flex gap-2">
                  <button
                    type="button"
                    onClick={() => openEdit(product.publicId)}
                    className="rounded-md border border-outline px-3 py-1 text-caption text-ink-muted hover:bg-surface"
                  >
                    Editar
                  </button>
                  {product.active && (
                    <button
                      type="button"
                      onClick={() => handleDeactivate(product.publicId)}
                      className="rounded-md border border-outline px-3 py-1 text-caption text-destructive hover:bg-surface"
                    >
                      Ocultar
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <ProductFormDialog
        // key fuerza el remount al abrir un producto distinto (o al pasar de
        // editar a crear): el diálogo inicializa su estado con useState(prop),
        // que solo corre una vez por instancia — sin key, abrir "Editar" en
        // otro produto reusaría el estado (y el modo simple/variantes) del
        // último producto editado. Encontrado con Playwright.
        key={editing?.publicId ?? 'create'}
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        product={editing}
        categories={categories}
        species={species}
        onSaved={load}
      />
    </div>
  )
}
