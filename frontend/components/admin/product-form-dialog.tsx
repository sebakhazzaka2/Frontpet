'use client'

import { useState } from 'react'
import { toast } from 'sonner'
import { Plus, Trash2 } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'
import { ProductImageUpload } from '@/components/admin/product-image-upload'
import {
  createProduct,
  updateProduct,
  replaceProductVariants,
  setProductActive,
  type AdminProductDetail,
  type ProductVariantUpsertRequest,
} from '@/lib/api/admin-products'
import { ApiFetchError, type TaxonRef } from '@/lib/api/client'

interface ProductFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  product?: AdminProductDetail
  categories: TaxonRef[]
  species: TaxonRef[]
  onSaved: () => void
}

interface VariantRow {
  id?: number
  nomeVariante: string
  price: string
  stock: string
}

// Tarea 4.12 (issue #33) — port de "Cadastrar Novo Produto" (modal do mock de
// Stitch "Gestão de Produtos"), pero SIN os campos que o mock desenha e o
// modelo real não tem: SKU, Tags e "Destaque na home" não existem em nenhum
// DTO do backend — inventar esses campos violaria CLAUDE.md §5 (tipos
// copiam os DTOs reais). Categoria/Espécie viram checkboxes (N:M no schema
// real), não o <select> de opção única que o mock desenha.
//
// Validação simples com useState + checagem manual antes do submit, não
// react-hook-form: a estrutura dinâmica (linhas de variante, arrays de
// categoria/espécie, toggle de modo) tornaria o schema de zod bem mais
// complexo que o ganho de consistência com checkout-form/login-form — troca
// pragmática dado o tamanho já grande deste bloco (o próprio issue #33 o
// chama de "o PR mais grande do sprint").
export function ProductFormDialog({
  open,
  onOpenChange,
  product,
  categories,
  species,
  onSaved,
}: ProductFormDialogProps) {
  const isEdit = product != null
  const wasVariants = product ? product.variants.length > 0 : false

  const [mode, setMode] = useState<'simple' | 'variants'>(wasVariants ? 'variants' : 'simple')
  const [nome, setNome] = useState(product?.nome ?? '')
  const [descricao, setDescricao] = useState(product?.descricao ?? '')
  const [mainImageUrl, setMainImageUrl] = useState<string | undefined>(product?.mainImageUrl)
  const [brandNome, setBrandNome] = useState(product?.brandNome ?? '')
  const [categorySlugs, setCategorySlugs] = useState<string[]>(
    product?.categories.map((c) => c.slug) ?? []
  )
  const [speciesSlugs, setSpeciesSlugs] = useState<string[]>(
    product?.species.map((s) => s.slug) ?? []
  )
  const [active, setActive] = useState(product?.active ?? true)
  const [price, setPrice] = useState(product?.price != null ? String(product.price) : '')
  const [priceOriginal, setPriceOriginal] = useState(
    product?.priceOriginal != null ? String(product.priceOriginal) : ''
  )
  const [stock, setStock] = useState(product?.stock != null ? String(product.stock) : '0')
  const [variants, setVariants] = useState<VariantRow[]>(
    product?.variants.map((v) => ({
      id: v.id,
      nomeVariante: v.nomeVariante,
      price: String(v.price),
      stock: String(v.stock),
    })) ?? []
  )
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function toggleSlug(list: string[], slug: string, setList: (value: string[]) => void) {
    setList(list.includes(slug) ? list.filter((s) => s !== slug) : [...list, slug])
  }

  function addVariantRow() {
    setVariants((rows) => [...rows, { nomeVariante: '', price: '', stock: '0' }])
  }

  function removeVariantRow(index: number) {
    setVariants((rows) => rows.filter((_, i) => i !== index))
  }

  function updateVariantRow(index: number, patch: Partial<VariantRow>) {
    setVariants((rows) => rows.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  function validate(): string | null {
    if (nome.trim().length < 2) return 'Informe o nome do produto.'

    if (mode === 'simple') {
      const priceNum = Number(price)
      if (!price || Number.isNaN(priceNum) || priceNum <= 0) return 'Informe um preço válido.'
      if (priceOriginal) {
        const promoNum = Number(priceOriginal)
        if (Number.isNaN(promoNum) || promoNum <= priceNum) {
          return 'O preço promocional deve ser maior que o preço.'
        }
      }
    } else {
      if (variants.length === 0) return 'Adicione ao menos uma variante.'
      for (const variant of variants) {
        if (!variant.nomeVariante.trim()) return 'Toda variante precisa de um nome.'
        const variantPrice = Number(variant.price)
        if (!variant.price || Number.isNaN(variantPrice) || variantPrice <= 0) {
          return 'Toda variante precisa de um preço válido.'
        }
      }
    }
    return null
  }

  async function handleSubmit() {
    const validationError = validate()
    if (validationError) {
      setError(validationError)
      return
    }
    setError(null)
    setSaving(true)

    try {
      if (!isEdit) {
        await createProduct({
          nome,
          descricao: descricao || undefined,
          mainImageUrl,
          brandNome: brandNome || undefined,
          categorySlugs,
          speciesSlugs,
          price: mode === 'simple' ? Number(price) : undefined,
          priceOriginal: mode === 'simple' && priceOriginal ? Number(priceOriginal) : undefined,
          stock: mode === 'simple' ? Number(stock || 0) : undefined,
          variants:
            mode === 'variants'
              ? variants.map((v) => ({
                  nomeVariante: v.nomeVariante,
                  price: Number(v.price),
                  stock: Number(v.stock || 0),
                }))
              : undefined,
        })
      } else {
        const convertingToVariants = !wasVariants && mode === 'variants'

        // Mode-switching (docs/pending-decisions.md §6): convertir a
        // variantes exige DOS llamadas — el PUT general todavía valida el
        // invariante "simple" (manda el price actual sin cambiar), y recién
        // el PUT /variants de después es el que realmente hace la
        // conversión (pone price=null, stock=0 en el producto). Convertir a
        // simple es UNA sola llamada: mandar price ya dispara todo en el
        // PUT general (soft-delete de variantes incluido).
        await updateProduct(product.publicId, {
          nome,
          descricao: descricao || undefined,
          mainImageUrl,
          brandNome: brandNome || undefined,
          categorySlugs,
          speciesSlugs,
          price:
            mode === 'simple' ? Number(price) : convertingToVariants ? (product.price ?? 0) : undefined,
          priceOriginal: mode === 'simple' && priceOriginal ? Number(priceOriginal) : undefined,
          stock: mode === 'simple' ? Number(stock || 0) : undefined,
        })

        if (mode === 'variants') {
          const upsert: ProductVariantUpsertRequest[] = variants.map((v) => ({
            id: v.id,
            nomeVariante: v.nomeVariante,
            price: Number(v.price),
            stock: Number(v.stock || 0),
          }))
          await replaceProductVariants(product.publicId, upsert)
        }

        if (active !== product.active) {
          await setProductActive(product.publicId, active)
        }
      }

      toast.success(isEdit ? 'Produto atualizado.' : 'Produto criado.')
      onOpenChange(false)
      onSaved()
    } catch (err) {
      setError(err instanceof ApiFetchError ? err.message : 'Não foi possível salvar o produto.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] overflow-y-auto rounded-xl sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Editar produto' : 'Cadastrar novo produto'}</DialogTitle>
        </DialogHeader>

        <div className="flex flex-col gap-6">
          <ProductImageUpload value={mainImageUrl} onChange={setMainImageUrl} />

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="flex flex-col gap-1.5 md:col-span-2">
              <label className="text-label text-ink-muted" htmlFor="nome">
                Nome do produto
              </label>
              <Input
                id="nome"
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                placeholder="Ex: Ração Golden Special Adulto"
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-label text-ink-muted" htmlFor="marca">
                Marca
              </label>
              <Input
                id="marca"
                value={brandNome}
                onChange={(e) => setBrandNome(e.target.value)}
                placeholder="Se não existir, é criada"
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <span className="text-label text-ink-muted">Modalidade de preço</span>
              <div className="flex rounded-md border border-outline bg-white p-1">
                <button
                  type="button"
                  onClick={() => setMode('simple')}
                  className={
                    mode === 'simple'
                      ? 'flex-1 rounded-md bg-navy py-1.5 text-sm text-white'
                      : 'flex-1 rounded-md py-1.5 text-sm text-ink-muted'
                  }
                >
                  Preço único
                </button>
                <button
                  type="button"
                  onClick={() => setMode('variants')}
                  className={
                    mode === 'variants'
                      ? 'flex-1 rounded-md bg-navy py-1.5 text-sm text-white'
                      : 'flex-1 rounded-md py-1.5 text-sm text-ink-muted'
                  }
                >
                  Com variantes
                </button>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="text-label text-ink-muted">Categorias</span>
            <div className="flex flex-wrap gap-3">
              {categories.map((c) => (
                <label key={c.slug} className="flex items-center gap-1.5 text-sm text-ink">
                  <Checkbox
                    checked={categorySlugs.includes(c.slug)}
                    onCheckedChange={() => toggleSlug(categorySlugs, c.slug, setCategorySlugs)}
                  />
                  {c.nome}
                </label>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="text-label text-ink-muted">Espécies</span>
            <div className="flex flex-wrap gap-3">
              {species.map((s) => (
                <label key={s.slug} className="flex items-center gap-1.5 text-sm text-ink">
                  <Checkbox
                    checked={speciesSlugs.includes(s.slug)}
                    onCheckedChange={() => toggleSlug(speciesSlugs, s.slug, setSpeciesSlugs)}
                  />
                  {s.nome}
                </label>
              ))}
            </div>
          </div>

          {mode === 'simple' ? (
            <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <div className="flex flex-col gap-1.5">
                <label className="text-label text-ink-muted" htmlFor="price">
                  Preço (R$)
                </label>
                <Input
                  id="price"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  placeholder="0,00"
                  inputMode="decimal"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-label text-ink-muted" htmlFor="priceOriginal">
                  Preço original (oferta)
                </label>
                <Input
                  id="priceOriginal"
                  value={priceOriginal}
                  onChange={(e) => setPriceOriginal(e.target.value)}
                  placeholder="Sem oferta"
                  inputMode="decimal"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-label text-ink-muted" htmlFor="stock">
                  Estoque
                </label>
                <Input
                  id="stock"
                  value={stock}
                  onChange={(e) => setStock(e.target.value)}
                  inputMode="numeric"
                />
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              <span className="text-label text-ink-muted">Variantes</span>
              {variants.map((row, index) => (
                <div key={row.id ?? `new-${index}`} className="flex items-end gap-2">
                  <div className="flex flex-1 flex-col gap-1">
                    <label className="text-caption text-ink-muted">Nome</label>
                    <Input
                      value={row.nomeVariante}
                      onChange={(e) => updateVariantRow(index, { nomeVariante: e.target.value })}
                      placeholder="Ex: 10kg"
                    />
                  </div>
                  <div className="flex w-28 flex-col gap-1">
                    <label className="text-caption text-ink-muted">Preço</label>
                    <Input
                      value={row.price}
                      onChange={(e) => updateVariantRow(index, { price: e.target.value })}
                      inputMode="decimal"
                    />
                  </div>
                  <div className="flex w-24 flex-col gap-1">
                    <label className="text-caption text-ink-muted">Estoque</label>
                    <Input
                      value={row.stock}
                      onChange={(e) => updateVariantRow(index, { stock: e.target.value })}
                      inputMode="numeric"
                    />
                  </div>
                  <button
                    type="button"
                    onClick={() => removeVariantRow(index)}
                    aria-label="Remover variante"
                    className="mb-1 flex size-9 items-center justify-center text-destructive"
                  >
                    <Trash2 className="size-4" />
                  </button>
                </div>
              ))}
              <button
                type="button"
                onClick={addVariantRow}
                className="flex w-fit items-center gap-1.5 rounded-md border border-outline px-3 py-1.5 text-sm text-ink-muted hover:bg-surface"
              >
                <Plus className="size-4" /> Adicionar variante
              </button>
            </div>
          )}

          <div className="flex flex-col gap-1.5">
            <label className="text-label text-ink-muted" htmlFor="descricao">
              Descrição
            </label>
            <Textarea
              id="descricao"
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              rows={4}
              placeholder="Detalhes, benefícios e modo de uso..."
            />
          </div>

          <label className="flex items-center gap-3 rounded-lg bg-surface p-3">
            <Checkbox checked={active} onCheckedChange={(checked) => setActive(checked === true)} />
            <span className="text-sm text-ink">Produto ativo (visível no catálogo)</span>
          </label>

          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button onClick={handleSubmit} disabled={saving}>
            {saving ? 'Salvando...' : 'Salvar produto'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
