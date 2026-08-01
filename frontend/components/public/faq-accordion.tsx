import { ChevronDown } from 'lucide-react'

export interface FaqItem {
  pergunta: string
  resposta: string
}

/**
 * `<details>`/`<summary>` nativos (tarea 6.B, issue #59): mismo patrón que
 * la pantalla de Stitch, sin JS ni librería nueva — teclado/lector de
 * pantalla ya funcionan de fábrica (CLAUDE.md: "no instalar sin preguntarse
 * si se resuelve con vanilla").
 */
export function FaqAccordion({ items }: { items: FaqItem[] }) {
  return (
    <div className="flex flex-col gap-4">
      {items.map((item, index) => (
        <details
          key={item.pergunta}
          className="group rounded-lg border border-outline bg-surface-card"
          open={index === 0}
        >
          <summary className="flex cursor-pointer list-none items-center justify-between gap-4 p-4 text-label font-semibold text-ink [&::-webkit-details-marker]:hidden">
            {item.pergunta}
            <ChevronDown
              className="size-5 shrink-0 text-ink-muted transition-transform group-open:rotate-180"
              aria-hidden
            />
          </summary>
          <p className="px-4 pb-4 text-sm text-ink-muted">{item.resposta}</p>
        </details>
      ))}
    </div>
  )
}
