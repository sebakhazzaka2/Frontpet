import type { Metadata, Viewport } from 'next'
import { DM_Sans, DM_Serif_Display } from 'next/font/google'
import { Providers } from './providers'
import './globals.css'

// ─────────────────────────────────────────────────────────────────────────────
// FUENTES — next/font/google carga las fuentes en build time, sin round-trip
// al servidor de Google en runtime. Cero layout shift garantizado.
// ─────────────────────────────────────────────────────────────────────────────

const dmSans = DM_Sans({
  subsets: ['latin'],
  // Pesos que realmente usamos (ver design-system.md §2)
  weight: ['400', '500', '600', '700'],
  // La variable CSS se inyecta en <html> y Tailwind la usa via font-sans
  variable: '--font-sans',
  display: 'swap',
})

const dmSerifDisplay = DM_Serif_Display({
  subsets: ['latin'],
  weight: '400',
  // DM Serif Display solo tiene regular — el "bold" es solo tamaño/tracking
  variable: '--font-display',
  display: 'swap',
})

// ─────────────────────────────────────────────────────────────────────────────
// METADATA — SEO base. Cada página puede hacer override con su propio export.
// Docs: https://nextjs.org/docs/app/api-reference/functions/generate-metadata
// ─────────────────────────────────────────────────────────────────────────────

const BASE_URL = process.env.NEXT_PUBLIC_BASE_URL ?? 'https://frontpet.com'

export const metadata: Metadata = {
  // metadataBase le dice a Next.js cómo construir URLs absolutas
  // (OpenGraph images, canonical links, etc.)
  metadataBase: new URL(BASE_URL),

  // ── Básico ──────────────────────────────────────────────────────────────
  title: {
    // La página home hereda solo el template: "FrontPet"
    // Las páginas hijas se muestran como: "Productos | FrontPet"
    default: 'FrontPet — Cuidado profesional para tu mascota',
    template: '%s | FrontPet',
  },
  description:
    'Productos premium, baños y peluquería profesional para mascotas en Pehuajó. ' +
    'Pedidos por WhatsApp, turnos online en segundos.',

  // ── OpenGraph — para compartir en redes y WhatsApp ───────────────────────
  openGraph: {
    type: 'website',
    locale: 'es_AR',
    url: BASE_URL,
    siteName: 'FrontPet',
    title: 'FrontPet — Cuidado profesional para tu mascota',
    description:
      'Productos premium, baños y peluquería profesional para mascotas en Pehuajó.',
    images: [
      {
        // TODO: agregar imagen real en /public/og-image.jpg (1200x630px)
        url: '/og-image.jpg',
        width: 1200,
        height: 630,
        alt: 'FrontPet — Petshop en Pehuajó',
      },
    ],
  },

  // ── Twitter / X card ─────────────────────────────────────────────────────
  twitter: {
    card: 'summary_large_image',
    title: 'FrontPet — Cuidado profesional para tu mascota',
    description: 'Productos premium, baños y peluquería para mascotas en Pehuajó.',
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
  // Color de la barra de dirección en Chrome mobile
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#FFF7ED' },
    { media: '(prefers-color-scheme: dark)',  color: '#1C1917' },
  ],
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
      lang="es"
      // Las variables CSS de las fuentes se inyectan acá.
      // Tailwind las recoge automáticamente via font-sans y font-display.
      className={`${dmSans.variable} ${dmSerifDisplay.variable}`}
      // Previene el flash de estilos desincronizados en hidratación
      suppressHydrationWarning
    >
      <head>
        {/*
          Meta Pixel de Meta Ads
          Descomentarlo y reemplazar TU_PIXEL_ID cuando FrontPet lo provea.
          Se configura en Sprint 7 (tarea 7.4).

          <script
            dangerouslySetInnerHTML={{
              __html: `
                !function(f,b,e,v,n,t,s){if(f.fbq)return;n=f.fbq=function(){
                n.callMethod?n.callMethod.apply(n,arguments):n.queue.push(arguments)};
                if(!f._fbq)f._fbq=n;n.push=n;n.loaded=!0;n.version='2.0';
                n.queue=[];t=b.createElement(e);t.async=!0;t.src=v;
                s=b.getElementsByTagName(e)[0];s.parentNode.insertBefore(t,s)}
                (window,document,'script','https://connect.facebook.net/en_US/fbevents.js');
                fbq('init', 'TU_PIXEL_ID');
                fbq('track', 'PageView');
              `,
            }}
          />
        */}

        {/*
          Plausible Analytics — lightweight, sin cookies, GDPR compliant.
          Descomentarlo cuando el dominio esté configurado (Sprint 7, tarea 7.6).

          <script
            defer
            data-domain="frontpet.com"
            src="https://plausible.io/js/script.js"
          />
        */}
      </head>

      <body
        className={[
          // Fondo y texto base del design system
          'bg-stone-50 text-stone-900',
          // Fuente por defecto: DM Sans
          'font-sans',
          // Antialiasing para texto más limpio en Mac/iOS
          'antialiased',
          // Previene el scroll horizontal accidental
          'overflow-x-hidden',
        ].join(' ')}
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
          Portal de toasts — Sonner (Sprint 4+).
          Descomentar cuando se instale: pnpm add sonner

          <Toaster
            position="bottom-right"
            toastOptions={{
              style: { fontFamily: 'var(--font-sans)' },
            }}
          />
        */}
      </body>
    </html>
  )
}
