'use client'

import Link from 'next/link'
import { Button } from '@/components/ui/button'

interface ConsentBannerProps {
  onAccept: () => void
  onReject: () => void
}

// Sem tela no Stitch para isto (CLAUDE.md §5 — tareas 7.12/7.13, e
// docs/stitch-implementation-workflow.md só cobre PORTAR telas existentes,
// não gerá-las). Um banner de consentimento é uma barra com texto e dois
// botões: derivado direto do design system, sem decisão de layout que Stitch
// precisasse resolver.
//
// z-[60] — primeiro uso no repo. Nav/BottomNav/FloatingWA/Dialog/Sheet já
// ocupam z-50; o banner precisa ficar acima de todos eles.
//
// Card flutuante, não barra full-bleed: uma barra colada em bottom-0 ficaria
// no MESMO espaço físico do BottomNav (também fixed bottom-0) e, com z-index
// maior, o cobriria por completo. bottom-24 (96px) desde a base é suficiente
// para limpar o BottomNav (nav + safe-area-inset-bottom real) na imensa
// maioria dos aparelhos; md:right-28 desde a direita limpa o FloatingWA
// (56px + 24px de offset). Só tokens da escala padrão do Tailwind — nenhum
// valor arbitrário.
//
// Sem role="dialog"/focus trap/bloqueio de scroll: não é um modal — isso
// seria exatamente o cookie wall que a LGPD não admite. Sem botão de fechar:
// sai-se decidindo, e as duas saídas (Aceitar/Recusar) têm o mesmo peso
// visual — um "Recusar" escondido também seria um cookie wall.
export function ConsentBanner({ onAccept, onReject }: ConsentBannerProps) {
  return (
    <div
      role="region"
      aria-label="Aviso de privacidade"
      className="fixed inset-x-4 bottom-24 z-[60] rounded-lg border border-outline bg-surface-card p-4 shadow-card-hover md:inset-x-auto md:bottom-6 md:left-6 md:right-28 md:max-w-md"
    >
      <div className="flex flex-col gap-3">
        <p className="text-sm text-ink">
          <span className="font-medium text-ink">Usamos cookies para melhorar sua experiência.</span>{' '}
          A gente usa cookies de análise e de publicidade para entender como você navega e mostrar
          nossas ofertas nas redes sociais. Você pode recusar — o site funciona igual. Saiba mais na
          nossa{' '}
          <Link href="/privacidade" className="underline hover:text-navy">
            Política de Privacidade
          </Link>
          .
        </p>

        <div className="flex gap-2">
          <Button variant="outline" size="lg" onClick={onReject} className="flex-1">
            Recusar
          </Button>
          <Button variant="default" size="lg" onClick={onAccept} className="flex-1">
            Aceitar
          </Button>
        </div>
      </div>
    </div>
  )
}
