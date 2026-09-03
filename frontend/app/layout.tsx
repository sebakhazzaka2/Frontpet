import type { Metadata, Viewport } from 'next'
import { Fredoka, Plus_Jakarta_Sans } from 'next/font/google'
import { Toaster } from '@/components/ui/sonner'
import { ConsentGate } from '@/components/consent/consent-gate'
import { Providers } from './providers'
import './globals.css'

// ─────────────────────────────────────────────────────────────────────────────
// FUENTES — next/font/google carga las fuentes en build time, sin round-trip
// al servidor de Google en runtime. Cero layout shift garantizado.
// ─────────────────────────────────────────────────────────────────────────────

// DESIGN.md restringe los pesos a 400/500/600: nada de 700+.
const jakarta = Plus_Jakarta_Sans({
  subsets: ['latin'],
  weight: ['400', '500', '600'],
  // globals.css mapea esta variable a --font-sans dentro de @theme
  variable: '--font-jakarta',
  display: 'swap',
})

const fredoka = Fredoka({
  subsets: ['latin'],
  weight: ['400', '500', '600'],
  // globals.css mapea esta variable a --font-display dentro de @theme
  variable: '--font-fredoka',
  display: 'swap',
})

// ─────────────────────────────────────────────────────────────────────────────
// METADATA — SEO base. Cada página puede hacer override con su propio export.
// Docs: https://nextjs.org/docs/app/api-reference/functions/generate-metadata
// ─────────────────────────────────────────────────────────────────────────────

// `||`, no `??`: un ARG de Docker sin --build-arg se resuelve a '' (no a
// undefined) — con `??` el fallback nunca se dispara y `new URL('')`
// revienta el build entero (verificado 2026-09-02 contra un build real).
const BASE_URL = process.env.NEXT_PUBLIC_BASE_URL || 'https://frontpet.com.br'

// Plausible es cookieless y no recolecta dados pessoais (plausible.io/data-policy)
// — no pasa por el gate de consentimiento de <ConsentGate>. Sin fallback: un
// domínio errado suja a conta do Plausible, então preferimos não renderizar nada.
const ANALYTICS_DOMAIN = process.env.NEXT_PUBLIC_ANALYTICS_DOMAIN

export const metadata: Metadata = {
  // metadataBase le dice a Next.js cómo construir URLs absolutas
  // (OpenGraph images, canonical links, etc.)
  metadataBase: new URL(BASE_URL),

  // ── Básico ──────────────────────────────────────────────────────────────
  title: {
    // La página home hereda solo el template: "FrontPet"
    // Las páginas hijas se muestran como: "Productos | FrontPet"
    default: 'FrontPet — Cuidado profissional para o seu pet',
    template: '%s | FrontPet',
  },
  description:
    'Produtos premium, banho e tosa profissional para o seu pet em Santana do Livramento. ' +
    'Peça pelo WhatsApp, agende horário online em segundos.',

  // ── OpenGraph — para compartir en redes y WhatsApp ───────────────────────
  openGraph: {
    type: 'website',
    locale: 'pt_BR',
    url: BASE_URL,
    siteName: 'FrontPet',
    title: 'FrontPet — Cuidado profissional para o seu pet',
    description:
      'Produtos premium, banho e tosa profissional para o seu pet em Santana do Livramento.',
    images: [
      {
        // TODO: agregar imagen real en /public/og-image.jpg (1200x630px)
        url: '/og-image.jpg',
        width: 1200,
        height: 630,
        alt: 'FrontPet — Petshop em Santana do Livramento',
      },
    ],
  },

  // ── Twitter / X card ─────────────────────────────────────────────────────
  twitter: {
    card: 'summary_large_image',
    title: 'FrontPet — Cuidado profissional para o seu pet',
    description: 'Produtos premium, banho e tosa profissional para o seu pet.',
    images: ['/og-image.jpg'],
  },

  // ── Robots ───────────────────────────────────────────────────────────────
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-image-preview': 'large',
      'max-snippet': -1,
    },
  },

  // ── Otros ────────────────────────────────────────────────────────────────
  // TODO: reemplazar con el dominio real al desplegar
  alternates: {
    canonical: BASE_URL,
  },

  // Verificación de Google Search Console
  // TODO: agregar el código real cuando esté configurado
  // verification: {
  //   google: 'tu-codigo-de-verificacion',
  // },

  // Manifest para PWA (futuro — Sprint 7)
  // manifest: '/manifest.json',
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEWPORT — separado de metadata desde Next.js 14.
// Controla el comportamiento del viewport en móviles.
// ─────────────────────────────────────────────────────────────────────────────

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 5, // Permitir zoom para accesibilidad (no bloquear con maximum-scale=1)
  // Color de la barra de dirección en Chrome mobile.
  // Un solo valor: DESIGN.md prohíbe dark mode, la interfaz es siempre clara.
  themeColor: '#F8F9FF',
}

// ─────────────────────────────────────────────────────────────────────────────
// LAYOUT ROOT
// ─────────────────────────────────────────────────────────────────────────────

interface RootLayoutProps {
  children: React.ReactNode
}

export default function RootLayout({ children }: RootLayoutProps) {
  return (
    <html
      lang="pt-BR"
      // Las variables CSS de las fuentes se inyectan acá.
      // Tailwind las recoge automáticamente via font-sans y font-display.
      className={`${jakarta.variable} ${fredoka.variable}`}
      // Previene el flash de estilos desincronizados en hidratación
      suppressHydrationWarning
    >
      <head>
        {/*
          Plausible Analytics — cookieless, sin dados pessoais, engancha solo
          las navegações client-side do App Router via History API
          (plausible.io/docs/spa-support). Sem gate de consentimento: ver
          ADR 024. O Meta Pixel (tarea 7.4) é carregado por <MetaPixel/> dentro
          de <ConsentGate/>, condicionado ao consentimento — não pode viver
          aqui porque este é um Server Component e o gate precisa de estado.
        */}
        {ANALYTICS_DOMAIN && (
          <script defer data-domain={ANALYTICS_DOMAIN} src="https://plausible.io/js/script.js" />
        )}
      </head>

      <body
        // Fondo, color, fuente y antialiasing salen del @layer base de globals.css
        className="overflow-x-hidden"
      >
        {/*
          Providers envuelve toda la app con los context providers de cliente.
          Estructura actual: QueryClientProvider
          Futuros: ToastProvider (Sonner), AnalyticsProvider, etc.
        */}
        <Providers>
          {children}
        </Providers>

        {/*
          Portal de toasts — Sonner (Bloque 0, Sprint 4). top-center: el borde
          inferior de la pantalla ya está ocupado por BottomNav + FloatingWA
          (y CartButton en 4.3) — bottom-right choca con eso en mobile.
        */}
        <Toaster
          position="top-center"
          toastOptions={{
            style: { fontFamily: 'var(--font-sans)' },
          }}
        />

        {/*
          Banner LGPD + Meta Pixel gateado + trackers de PageView/Contact.
          Vive en el root (no en (public)/layout.tsx) para cubrir también
          /admin/** — ver ADR 024.
        */}
        <ConsentGate />
      </body>
    </html>
  )
}
