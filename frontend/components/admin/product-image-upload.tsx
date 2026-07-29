'use client'

import { useState } from 'react'
import Image from 'next/image'
import { ImagePlus, Loader2, X } from 'lucide-react'
import { compressProductImage } from '@/lib/images/compress'
import { presignProductImageUpload } from '@/lib/api/admin-products'

interface ProductImageUploadProps {
  value?: string
  onChange: (url: string | undefined) => void
}

// Tarea 4.12 (issue #33): comprime client-side (pending-decisions §7) antes
// de pedir la URL firmada — la URL se pide JUSTO antes de subir, nunca
// cacheada de una carga anterior de la página (expira en 5 min, ADR 018).
// El backend nunca ve los bytes: el PUT va directo del browser a R2.
export function ProductImageUpload({ value, onChange }: ProductImageUploadProps) {
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleFile(file: File) {
    setUploading(true)
    setError(null)
    try {
      const compressed = await compressProductImage(file)
      const presigned = await presignProductImageUpload({
        fileName: compressed.name,
        contentType: compressed.type,
        contentLength: compressed.size,
      })

      const putResponse = await fetch(presigned.uploadUrl, {
        method: 'PUT',
        headers: { 'Content-Type': compressed.type },
        body: compressed,
      })
      if (!putResponse.ok) {
        throw new Error('Falha ao enviar a imagem. Tente novamente.')
      }

      onChange(presigned.publicUrl)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Não foi possível enviar a imagem.')
    } finally {
      setUploading(false)
    }
  }

  function handleInputChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (file) {
      void handleFile(file)
    }
    // Sin esto, elegir el mismo archivo dos veces seguidas no dispara onChange.
    event.target.value = ''
  }

  return (
    <div className="flex flex-col gap-2">
      {value ? (
        <div className="relative size-32 overflow-hidden rounded-lg border border-outline/30 bg-surface">
          <Image src={value} alt="" fill className="object-cover" sizes="128px" />
          <button
            type="button"
            onClick={() => onChange(undefined)}
            aria-label="Remover imagem"
            className="absolute right-1 top-1 flex size-6 items-center justify-center rounded-full bg-navy/80 text-white"
          >
            <X className="size-4" />
          </button>
        </div>
      ) : (
        <label className="flex size-32 cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-outline bg-surface text-center transition-colors hover:bg-surface-card">
          {uploading ? (
            <Loader2 className="size-6 animate-spin text-ink-muted" />
          ) : (
            <>
              <ImagePlus className="size-6 text-ink-muted" />
              <span className="px-2 text-caption text-ink-muted">Enviar imagem</span>
            </>
          )}
          <input
            type="file"
            accept="image/png,image/jpeg,image/webp"
            className="sr-only"
            disabled={uploading}
            onChange={handleInputChange}
          />
        </label>
      )}
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  )
}
