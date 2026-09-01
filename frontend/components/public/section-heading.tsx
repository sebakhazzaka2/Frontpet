import { cn } from '@/lib/utils'

interface SectionHeadingProps {
  eyebrow?: string
  titulo: string
  descricao?: string
  align?: 'left' | 'center'
  tone?: 'light' | 'dark'
}

// Encabezado canónico de sección (ADR 025). Formaliza el vocabulario que ya
// existía disperso en 8 sitios del repo con 7 inconsistencias reales (3
// mecanismos de centrado, 4 espaciados eyebrow→h2→p distintos): un solo
// mecanismo de centrado, un solo ritmo, un solo tono por variante.
export function SectionHeading({
  eyebrow,
  titulo,
  descricao,
  align = 'left',
  tone = 'light',
}: SectionHeadingProps) {
  const centered = align === 'center'
  const dark = tone === 'dark'

  return (
    <div className={cn('flex flex-col', centered && 'items-center text-center')}>
      {eyebrow && (
        <span className="text-eyebrow uppercase tracking-wider text-orange">{eyebrow}</span>
      )}
      {/* font-display no se repite: @layer base (globals.css) ya lo aplica a h1/h2/h3 */}
      <h2
        className={cn(
          'text-h2-mobile md:text-h2',
          eyebrow && 'mt-2',
          dark ? 'text-white' : 'text-ink',
        )}
      >
        {titulo}
      </h2>
      {descricao && (
        <p
          className={cn(
            'mt-3 max-w-[560px] text-sm md:text-base',
            dark ? 'text-white/70' : 'text-ink-muted',
          )}
        >
          {descricao}
        </p>
      )}
    </div>
  )
}
