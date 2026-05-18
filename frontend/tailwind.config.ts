import type { Config } from 'tailwindcss'

/**
 * FrontPet — Tailwind Design System Config
 * ─────────────────────────────────────────
 * Reglas de disciplina:
 *  - Colores   → solo los definidos acá. Sin purple-500 ni sky-300.
 *  - Tipografía → 7 tamaños. Nada de text-[17px] o valores arbitrarios.
 *  - Radios    → 5 valores. Sin rounded-[10px].
 *  - Sombras   → 5 nombradas. Sin drop-shadow-[...].
 *  - Espaciado → Tailwind default, pero solo p-1,2,3,4,6,8,12,16.
 *
 * Para reforzar la disciplina, agregar en el linter:
 *   "no-restricted-syntax" para detectar valores arbitrarios en clases Tailwind.
 */

const config: Config = {
  content: [
    './app/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './lib/**/*.{js,ts,jsx,tsx}',
  ],

  theme: {
    // ─────────────────────────────────────────────────────────────────
    // COLORES — override completo. Solo la paleta FrontPet existe.
    // ─────────────────────────────────────────────────────────────────
    colors: {
      transparent: 'transparent',
      current:     'currentColor',
      white:       '#ffffff',
      black:       '#000000',

      /**
       * brand — naranja principal
       * Uso: bg-brand-500 (botones), text-brand-600 (hover),
       *      bg-brand-50 (fondos cálidos), border-brand-200
       */
      brand: {
        50:  '#FFF7ED',
        100: '#FFEDD5',
        200: '#FED7AA',
        300: '#FDBA74',
        400: '#FB923C',
        500: '#F97316', // ← color primario de la marca
        600: '#EA580C', // ← hover de botones
        700: '#C2410C',
        800: '#9A3412',
        900: '#7C2D12',
      },

      /**
       * stone — gris cálido (fondo, texto, bordes)
       * Uso: bg-stone-50 (body), bg-stone-100 (cards), text-stone-900 (body text),
       *      text-stone-500 (muted), border-stone-200 (separadores)
       */
      stone: {
        50:  '#FAFAF9',
        100: '#F5F5F4',
        200: '#E7E5E4',
        300: '#D6D3D1',
        400: '#A8A29E',
        500: '#78716C', // ← texto secundario / muted
        600: '#57534E',
        700: '#44403C',
        800: '#292524',
        900: '#1C1917', // ← texto principal
      },

      /**
       * wa — WhatsApp / verde principal
       * Uso: bg-wa (botones WA), bg-wa-light (banners), text-wa-dark (texto sobre claro)
       */
      wa: {
        light:   '#DCF8C6',
        DEFAULT: '#25D366',
        dark:    '#128C7E',
      },

      /**
       * amber — acento / badges / avisos
       * Uso: text-amber-600 (precios en oferta), bg-amber-50 (info suave),
       *      border-amber-400 (highlight)
       */
      amber: {
        50:  '#FFFBEB',
        100: '#FEF3C7',
        400: '#FBBF24',
        500: '#F59E0B',
        600: '#D97706',
        700: '#B45309',
      },

      // ── Semánticos — no usar arbitrariamente ────────────────────────
      success: {
        light:   '#F0FFF4',
        DEFAULT: '#22C55E',
        dark:    '#15803D',
      },
      danger: {
        light:   '#FEF2F2',
        DEFAULT: '#EF4444',
        dark:    '#DC2626',
      },
      warning: {
        light:   '#FFFBEB',
        DEFAULT: '#F59E0B',
        dark:    '#D97706',
      },
    },

    // ─────────────────────────────────────────────────────────────────
    // TIPOGRAFÍA
    // ─────────────────────────────────────────────────────────────────

    fontFamily: {
      /**
       * font-display → DM Serif Display
       * Para: h1, h2, hero titles. Nunca para UI o texto corrido.
       * Instalación: @import en globals.css o next/font/google
       */
      display: ['DM Serif Display', 'Georgia', 'serif'],

      /**
       * font-sans → DM Sans
       * Para: todo lo demás — body, labels, botones, nav.
       */
      sans: ['DM Sans', 'system-ui', 'sans-serif'],

      /**
       * font-mono → JetBrains Mono
       * Para: código, SKUs, IDs, valores técnicos.
       */
      mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
    },

    /**
     * ESCALA TIPOGRÁFICA — 7 tamaños, nada más.
     *
     * text-xs   → 12px  badges, chips, eyebrow text
     * text-sm   → 14px  labels, inputs, meta, timestamps
     * text-base → 16px  body default (la mayoría del contenido)
     * text-lg   → 18px  body grande, subtítulos de sección
     * text-xl   → 24px  título de card, h3
     * text-2xl  → 32px  sección, h2
     * text-3xl  → 48px  hero, h1 — siempre con font-display
     */
    fontSize: {
      xs:   ['12px', { lineHeight: '16px', letterSpacing: '0.01em' }],
      sm:   ['14px', { lineHeight: '20px', letterSpacing: '0em' }],
      base: ['16px', { lineHeight: '24px', letterSpacing: '0em' }],
      lg:   ['18px', { lineHeight: '28px', letterSpacing: '-0.01em' }],
      xl:   ['24px', { lineHeight: '32px', letterSpacing: '-0.01em' }],
      '2xl':['32px', { lineHeight: '40px', letterSpacing: '-0.02em' }],
      '3xl':['48px', { lineHeight: '56px', letterSpacing: '-0.03em' }],
    },

    // ─────────────────────────────────────────────────────────────────
    // BORDER RADIUS — 5 valores + pill
    //
    // rounded-sm  → 4px   chips pequeños, badges
    // rounded     → 8px   inputs, botones pequeños
    // rounded-lg  → 12px  cards, selects
    // rounded-xl  → 16px  panels, modals, drawer
    // rounded-2xl → 24px  secciones grandes
    // rounded-full→ pill  botones principales, avatars, tags
    // ─────────────────────────────────────────────────────────────────
    borderRadius: {
      none:    '0px',
      sm:      '4px',
      DEFAULT: '8px',
      md:      '8px',
      lg:      '12px',
      xl:      '16px',
      '2xl':   '24px',
      full:    '9999px',
    },

    // ─────────────────────────────────────────────────────────────────
    // SHADOWS — 5 nombradas + 2 brand
    //
    // shadow-sm    → hover state en inputs, bordes elevados sutiles
    // shadow-md    → cards por defecto
    // shadow-lg    → drawers, dropdowns, menús flotantes
    // shadow-xl    → modales, toasts
    // shadow-brand → botones CTA naranja (hover)
    // shadow-wa    → botón WhatsApp (hover)
    // ─────────────────────────────────────────────────────────────────
    boxShadow: {
      none:        'none',
      sm:          '0 1px 4px rgba(28, 25, 23, 0.06)',
      DEFAULT:     '0 4px 16px rgba(28, 25, 23, 0.08)',
      md:          '0 4px 16px rgba(28, 25, 23, 0.08)',
      lg:          '0 8px 32px rgba(28, 25, 23, 0.13)',
      xl:          '0 20px 60px rgba(28, 25, 23, 0.18)',
      brand:       '0 4px 20px rgba(249, 115, 22, 0.25)',
      'brand-lg':  '0 8px 32px rgba(249, 115, 22, 0.35)',
      wa:          '0 4px 18px rgba(37, 211, 102, 0.38)',
      'wa-lg':     '0 6px 26px rgba(37, 211, 102, 0.48)',
    },

    extend: {
      /**
       * ESPACIADO — No override. Los valores de Tailwind por defecto coinciden
       * exactamente con la escala elegida:
       *   p-1  → 4px   p-2  → 8px   p-3  → 12px  p-4  → 16px
       *   p-6  → 24px  p-8  → 32px  p-12 → 48px  p-16 → 64px
       *
       * REGLA: usar solo p-1, p-2, p-3, p-4, p-6, p-8, p-12, p-16.
       * No usar p-5, p-7, p-9, p-10, p-11, p-14, ni valores arbitrarios.
       */

      screens: {
        xs: '375px',  // iPhone SE
        // sm: 640px, md: 768px, lg: 1024px, xl: 1280px — Tailwind defaults
      },

      fontWeight: {
        normal:    '400',
        medium:    '500',
        semibold:  '600',
        bold:      '700',
        extrabold: '800',
      },

      transitionDuration: {
        fast:    '150ms',
        DEFAULT: '200ms',
        slow:    '350ms',
      },

      // ── Animaciones reutilizables ────────────────────────────────────
      animation: {
        'fade-in':    'fadeIn 0.4s ease forwards',
        'slide-up':   'slideUp 0.4s ease forwards',
        'slide-up-sm':'slideUpSm 0.3s ease forwards',
        pulse:        'pulseBrand 3s ease-in-out infinite',
        'wa-pulse':   'waPulse 2.2s infinite',
      },

      keyframes: {
        fadeIn: {
          from: { opacity: '0' },
          to:   { opacity: '1' },
        },
        slideUp: {
          from: { opacity: '0', transform: 'translateY(18px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
        slideUpSm: {
          from: { opacity: '0', transform: 'translateY(8px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
        pulseBrand: {
          '0%, 100%': { transform: 'scale(1)' },
          '50%':      { transform: 'scale(1.04)' },
        },
        waPulse: {
          '0%':   { transform: 'scale(1)',   opacity: '0.7' },
          '100%': { transform: 'scale(1.7)', opacity: '0' },
        },
      },

      // ── Max widths para contenedores ─────────────────────────────────
      maxWidth: {
        content: '1200px',  // layout principal
        prose:   '680px',   // texto corrido, formularios
        narrow:  '480px',   // wizards, modals
      },
    },
  },

  plugins: [
    // Descomentar cuando los instales:
    // require('@tailwindcss/typography'),   // para contenido markdown
    // require('@tailwindcss/forms'),        // reset base de form elements
    // require('@tailwindcss/aspect-ratio'), // para imágenes de producto
  ],
}

export default config
