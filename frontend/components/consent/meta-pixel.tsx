'use client'

import Script from 'next/script'

// NEXT_PUBLIC_META_PIXEL_ID chega no Sprint 8 (docs/preguntas-cliente.md §7.5)
// — até lá esta var fica vazia e o componente não renderiza nada.
const PIXEL_ID = process.env.NEXT_PUBLIC_META_PIXEL_ID

interface MetaPixelProps {
  granted: boolean
}

// Gate estrito (ADR 024): fbevents.js só é carregado com `granted === true`.
// A alternativa — carregar sempre e chamar fbq('consent','revoke') — deixaria
// um script de terceiro rodando no navegador de quem ainda não disse "sim",
// e a tarea 7.13 do ROADMAP pede que o Pixel não dispare antes do
// consentimento. `next/script` (primeiro uso no repo) resolve o problema de
// montar/desmontar um <script> cru conforme o estado de React muda.
//
// Eventos disparados antes do consentimento são DESCARTADOS, não
// bufferizados — ver lib/analytics/pixel.ts (fire() é no-op sem fbq). Se o
// usuário aceita depois de já ter navegado, as páginas anteriores nunca são
// reportadas — não dá pra reportar retroativamente uma navegação que
// aconteceu sem consentimento.
export function MetaPixel({ granted }: MetaPixelProps) {
  if (!granted || !PIXEL_ID) return null

  return (
    <Script id="meta-pixel-base" strategy="afterInteractive">
      {`
        !function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){
        n.callMethod?n.callMethod.apply(n,arguments):n.queue.push(arguments)};
        if(!f._fbq)f._fbq=n;n.push=n;n.loaded=!0;n.version='2.0';
        n.queue=[];t=b.createElement(e);t.async=!0;t.src=v;
        s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}
        (window,document,'script','https://connect.facebook.net/en_US/fbevents.js');
        fbq('init', '${PIXEL_ID}');
        fbq('track', 'PageView');
      `}
    </Script>
  )
}
