// Compresión client-side de fotos de producto (docs/pending-decisions.md §7,
// issue #33). El upload va directo del browser a R2 vía URL firmada — el
// backend nunca ve los bytes (ADR 018) — así que si algo no redimensiona ni
// reencodea la imagen antes de subir, no hay ninguna otra capa que lo haga.
//
// Resize a 1200x1200 máx + reencode a WebP, apuntando a <200KB (spec del
// ROADMAP). "Apuntando a": bajar la calidad de encode hasta cumplir el tope
// o quedarse sin margen razonable (calidad mínima 0.4) — no hay garantía dura
// de quedar bajo 200KB con cualquier foto de origen, es una meta, no un
// hard cap silencioso que arruine fotos ya livianas.
const MAX_DIMENSION = 1200
const TARGET_MAX_BYTES = 200 * 1024
const WEBP_TYPE = 'image/webp'
const JPEG_TYPE = 'image/jpeg'
const MIN_QUALITY = 0.4
const QUALITY_STEP = 0.1

export async function compressProductImage(file: File): Promise<File> {
  const targetType = (await canEncodeWebp()) ? WEBP_TYPE : JPEG_TYPE

  const bitmap = await createImageBitmap(file)
  const { width, height } = fitWithinBounds(bitmap.width, bitmap.height, MAX_DIMENSION)

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')

  if (!ctx) {
    // No se pudo obtener contexto 2D (extremadamente raro) — subir el
    // original sin comprimir es mejor que romper el flujo de alta.
    bitmap.close()
    return file
  }

  ctx.drawImage(bitmap, 0, 0, width, height)
  bitmap.close()

  const blob = await encodeWithSizeTarget(canvas, targetType)
  const extension = targetType === WEBP_TYPE ? 'webp' : 'jpg'
  const baseName = file.name.replace(/\.[^./\\]+$/, '')

  return new File([blob], `${baseName}.${extension}`, { type: targetType })
}

function fitWithinBounds(width: number, height: number, max: number) {
  if (width <= max && height <= max) {
    return { width, height }
  }
  const scale = max / Math.max(width, height)
  return { width: Math.round(width * scale), height: Math.round(height * scale) }
}

// Fallback Safari (pending-decisions §7): históricamente Safari decodifica
// WebP pero no siempre lo codifica vía canvas.toBlob. Se detecta probando un
// encode real de 1x1 en vez de mirar el user agent — así funciona solo el
// día que Safari lo soporte, sin lista de versiones que mantener.
async function canEncodeWebp(): Promise<boolean> {
  const canvas = document.createElement('canvas')
  canvas.width = 1
  canvas.height = 1
  return new Promise((resolve) => {
    canvas.toBlob((blob) => resolve(blob != null && blob.type === WEBP_TYPE), WEBP_TYPE)
  })
}

async function encodeWithSizeTarget(canvas: HTMLCanvasElement, type: string): Promise<Blob> {
  let quality = 0.85
  let blob = await canvasToBlob(canvas, type, quality)

  while (blob.size > TARGET_MAX_BYTES && quality > MIN_QUALITY) {
    quality -= QUALITY_STEP
    blob = await canvasToBlob(canvas, type, quality)
  }

  return blob
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob)
        } else {
          reject(new Error('Não foi possível processar a imagem.'))
        }
      },
      type,
      quality
    )
  })
}
