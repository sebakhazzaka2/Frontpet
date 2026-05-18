# FrontPet — Design System

> Documento de referencia para todo el sistema de diseño del proyecto.
> Antes de crear un componente nuevo, consultá esta guía.
> Si necesitás agregar un token que no está acá, discutirlo primero.

**Versión**: 1.0  
**Última actualización**: mayo 2026  
**Archivos fuente**: `tailwind.config.ts` · `app/globals.css`

---

## Índice

1. [Principios de diseño](#1-principios-de-diseño)
2. [Tipografía](#2-tipografía)
3. [Colores](#3-colores)
4. [Espaciado](#4-espaciado)
5. [Border radius](#5-border-radius)
6. [Sombras](#6-sombras)
7. [Componentes base](#7-componentes-base)
8. [Animaciones](#8-animaciones)
9. [Responsive breakpoints](#9-responsive-breakpoints)
10. [Reglas de disciplina](#10-reglas-de-disciplina)

---

## 1. Principios de diseño

### Calidez local
FrontPet es un negocio local de Pehuajó. El diseño debe transmitir cercanía, no frialdad corporativa. Los colores cálidos (naranja, ámbar, stone) refuerzan esto.

### WhatsApp-first
El CTA principal en toda la plataforma es WhatsApp. Los botones verdes deben tener igual o mayor jerarquía visual que otros CTAs secundarios.

### Mobile-first, siempre
El 80%+ del tráfico es mobile. Diseñar primero en 375px y expandir hacia arriba, nunca al revés.

### Consistencia sobre creatividad puntual
Es más valioso usar los tokens del sistema que "esto se ve mejor con padding 13px". La consistencia acumulada es lo que hace que el producto parezca profesional.

### Menos es más en animaciones
Una animación bien hecha en el hero vale más que diez micro-animaciones en todas las cards. Reservar las animaciones para momentos de impacto.

---

## 2. Tipografía

### Familias

| Variable Tailwind | Familia | Uso |
|---|---|---|
| `font-display` | DM Serif Display | h1, h2, hero titles |
| `font-sans` | DM Sans | Todo lo demás: body, labels, botones, nav |
| `font-mono` | JetBrains Mono | Código, SKUs, IDs técnicos |

**Instalación en `app/layout.tsx`:**

```tsx
import { DM_Sans, DM_Serif_Display } from 'next/font/google'

const dmSans = DM_Sans({
  subsets: ['latin'],
  variable: '--font-sans',
  weight: ['400', '500', '600', '700'],
})

const dmSerif = DM_Serif_Display({
  subsets: ['latin'],
  weight: '400',
  variable: '--font-display',
})

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body className={`${dmSans.variable} ${dmSerif.variable}`}>
        {children}
      </body>
    </html>
  )
}
```

### Escala tipográfica

| Clase | Tamaño | Line-height | Letter-spacing | Uso |
|---|---|---|---|---|
| `text-xs` | 12px | 16px | +0.01em | Badges, chips, eyebrow text |
| `text-sm` | 14px | 20px | 0 | Labels, inputs, meta, timestamps |
| `text-base` | 16px | 24px | 0 | Body default — la mayoría del contenido |
| `text-lg` | 18px | 28px | -0.01em | Body grande, subtítulos de sección |
| `text-xl` | 24px | 32px | -0.01em | Título de card, h3 |
| `text-2xl` | 32px | 40px | -0.02em | Sección, h2 |
| `text-3xl` | 48px | 56px | -0.03em | Hero, h1 — siempre con `font-display` |

### Pesos permitidos

| Clase | Peso | Uso |
|---|---|---|
| `font-normal` | 400 | Body, descripción, texto corrido |
| `font-medium` | 500 | Labels, items de nav |
| `font-semibold` | 600 | Subtítulos, nombres de producto |
| `font-bold` | 700 | Titulares con `font-sans`, precios |
| `font-extrabold` | 800 | Destacados, datos clave en admin |

### Combinaciones correctas

```tsx
// ✅ Titular de sección
<h2 className="font-display text-2xl text-stone-900">
  Nuestros Productos
</h2>

// ✅ Hero
<h1 className="font-display text-3xl text-stone-900">
  El mejor cuidado <span className="text-brand-500">merece lo mejor</span>
</h1>

// ✅ Título de card (sin serif)
<h3 className="font-sans text-xl font-semibold text-stone-900">
  Royal Canin Medium Adult 15kg
</h3>

// ✅ Body
<p className="text-base text-stone-600 leading-relaxed">
  Alimento completo para perros medianos adultos.
</p>

// ✅ Label de campo
<label className="text-sm font-medium text-stone-700">
  Nombre de la mascota
</label>

// ✅ Badge / chip
<span className="text-xs font-bold uppercase tracking-wide">
  Más vendido
</span>

// ❌ Mal: serif en texto corrido
<p className="font-display text-base">Descripción del producto...</p>

// ❌ Mal: tamaño fuera de escala
<p className="text-[17px]">...</p>
```

---

## 3. Colores

### brand — naranja principal

| Token | Hex | Uso |
|---|---|---|
| `brand-50` | `#FFF7ED` | Fondos cálidos, secciones hero |
| `brand-100` | `#FFEDD5` | Backgrounds de badges, chips |
| `brand-200` | `#FED7AA` | Bordes de elementos seleccionados |
| `brand-500` | `#F97316` | **Color primario — botones, íconos, accents** |
| `brand-600` | `#EA580C` | Hover state de botones |
| `brand-700` | `#C2410C` | Active state, textos sobre fondo claro |
| `brand-900` | `#7C2D12` | Textos sobre fondos muy claros brand |

### stone — gris cálido (neutros)

| Token | Hex | Uso |
|---|---|---|
| `stone-50` | `#FAFAF9` | Background del body |
| `stone-100` | `#F5F5F4` | Superficies secundarias, inputs deshabilitados |
| `stone-200` | `#E7E5E4` | Bordes por defecto |
| `stone-300` | `#D6D3D1` | Bordes de hover, separadores |
| `stone-400` | `#A8A29E` | Placeholders |
| `stone-500` | `#78716C` | Texto muted / secundario |
| `stone-700` | `#44403C` | Texto de párrafos |
| `stone-900` | `#1C1917` | Texto principal / headings |

### wa — WhatsApp / verde

| Token | Hex | Uso |
|---|---|---|
| `wa-light` | `#DCF8C6` | Background de banners WA |
| `wa` | `#25D366` | Botones principales de WhatsApp |
| `wa-dark` | `#128C7E` | Hover de botones WA, texto sobre fondo claro |

### amber — acento

| Token | Hex | Uso |
|---|---|---|
| `amber-50` | `#FFFBEB` | Background suave de info |
| `amber-400` | `#FBBF24` | Estrellas de rating, highlights |
| `amber-600` | `#D97706` | Textos de alerta suave |

### Colores semánticos

| Token | Uso |
|---|---|
| `success-light` / `success` / `success-dark` | Stock disponible, confirmaciones, badges "Disponible" |
| `danger-light` / `danger` / `danger-dark` | Errores de form, stock bajo, eliminaciones |
| `warning-light` / `warning` / `warning-dark` | Alertas de stock, avisos no críticos |

### Reglas de uso de color

```tsx
// ✅ Texto principal siempre stone-900
<p className="text-stone-900">...</p>

// ✅ Texto secundario / muted
<p className="text-stone-500">Descripción breve...</p>

// ✅ Botón con naranja
<button className="bg-brand-500 hover:bg-brand-600 text-white">...</button>

// ✅ Badge de stock bajo
<span className="bg-danger-light text-danger-dark">⚡ Solo 3 disponibles</span>

// ✅ Badge de stock ok
<span className="bg-success-light text-success-dark">✓ En stock</span>

// ❌ Nunca usar colores que no están en la paleta
<p className="text-purple-500">...</p>
<div className="bg-sky-100">...</div>

// ❌ Nunca usar valores arbitrarios
<div className="bg-[#ff6b35]">...</div>
```

---

## 4. Espaciado

La escala de Tailwind default coincide exactamente con la escala elegida.

| Clase | Valor | Uso típico |
|---|---|---|
| `*-1` | 4px | Gap entre ícono y texto, padding de chips |
| `*-2` | 8px | Padding interno de badges, gap en filas compactas |
| `*-3` | 12px | Padding de botones pequeños, gap entre items |
| `*-4` | 16px | Padding estándar de cards, gap de grids |
| `*-6` | 24px | Padding de secciones chicas, gap entre cards |
| `*-8` | 32px | Margin entre bloques, padding de containers |
| `*-12` | 48px | Padding de secciones grandes |
| `*-16` | 64px | Padding de hero, separación entre secciones |

**Regla estricta**: usar solo `p-1, p-2, p-3, p-4, p-6, p-8, p-12, p-16`.  
No usar `p-5, p-7, p-9, p-10, p-11, p-14` ni valores arbitrarios.

```tsx
// ✅ Correcto
<div className="p-4 gap-6">

// ❌ Incorrecto — fuera de la escala
<div className="p-5 gap-7">
<div className="p-[18px]">
```

---

## 5. Border Radius

| Clase | Valor | Uso |
|---|---|---|
| `rounded-sm` | 4px | Chips pequeños, badges inline, indicadores |
| `rounded` o `rounded-md` | 8px | Inputs, select, botones chicos, tooltips |
| `rounded-lg` | 12px | Cards de producto, dropdowns, formularios |
| `rounded-xl` | 16px | Panels, drawers laterales, modales |
| `rounded-2xl` | 24px | Secciones con fondo diferente, hero cards |
| `rounded-full` | 9999px | Botones principales (pill), avatars, tags de categoría |

```tsx
// ✅ Card de producto
<div className="rounded-lg">

// ✅ Botón principal — siempre pill
<button className="rounded-full">Ver Productos →</button>

// ✅ Input
<input className="rounded" />

// ✅ Badge
<span className="rounded-sm">Oferta</span>

// ❌ Nunca valores arbitrarios
<div className="rounded-[10px]">
```

---

## 6. Sombras

| Clase | Uso |
|---|---|
| `shadow-sm` | Hover de inputs, elevación mínima |
| `shadow-md` | Cards en estado default |
| `shadow-lg` | Dropdowns, menús flotantes, cards en hover |
| `shadow-xl` | Modales, toasts, drawers |
| `shadow-brand` | Botón primario naranja en estado default |
| `shadow-brand-lg` | Botón primario naranja en hover |
| `shadow-wa` | Botón WhatsApp en estado default |
| `shadow-wa-lg` | Botón WhatsApp en hover |

```tsx
// ✅ Card con hover
<div className="shadow-md hover:shadow-lg transition-shadow">

// ✅ Botón principal
<button className="bg-brand-500 shadow-brand hover:shadow-brand-lg">

// ✅ Botón WhatsApp
<a className="bg-wa shadow-wa hover:shadow-wa-lg">

// ❌ Nunca drop-shadow o valores arbitrarios
<div className="drop-shadow-[0_4px_12px_rgba(0,0,0,0.1)]">
```

---

## 7. Componentes base

Definidos en `globals.css` bajo `@layer components`. Usarlos en lugar de repetir clases.

### Botones

```tsx
// Primario — naranja
<button className="btn-primary">
  🛍️ Ver Productos
</button>

// WhatsApp — ancho completo
<a href={waLink} className="btn-wa">
  💬 Pedir por WhatsApp
</a>

// Outline — borde naranja
<button className="btn-outline">
  📅 Reservar Turno
</button>
```

### Card

```tsx
<div className="card">
  {/* contenido */}
</div>
```

### Badge

```tsx
// El badge solo define tipografía y padding. El color va inline o con utilidades.
<span className="badge bg-brand-500 text-white">Más vendido</span>
<span className="badge bg-danger text-white">Oferta -20%</span>
<span className="badge bg-success text-white">Nuevo</span>
```

### Eyebrow (texto sobre titular)

```tsx
<span className="eyebrow">Servicios</span>
<h2 className="font-display text-2xl text-stone-900">
  Todo lo que tu mascota necesita
</h2>
```

### Container principal

```tsx
<section className="section">
  <div className="container-main">
    {/* máx 1200px, centrado, px-4 */}
  </div>
</section>
```

### Input de búsqueda

```tsx
<div className="relative">
  <span className="absolute left-4 top-1/2 -translate-y-1/2 text-stone-400">🔍</span>
  <input className="input-search pl-10" placeholder="Buscar productos..." />
</div>
```

### Botón WhatsApp flotante

```tsx
<a href={waLink} className="float-wa" aria-label="Contactar por WhatsApp">
  <div className="absolute inset-0 rounded-full bg-wa animate-wa-pulse" />
  {/* ícono WA */}
</a>
```

---

## 8. Animaciones

### Clases disponibles

| Clase | Descripción | Uso |
|---|---|---|
| `animate-fade-in` | Fade de opacidad | Modales, toasts |
| `animate-slide-up` | Slide desde abajo + fade | Secciones al hacer scroll |
| `animate-slide-up-sm` | Slide sutil + fade | Cards, items de lista |
| `animate-pulse` | Escala suave loop | Elemento decorativo del hero |
| `animate-wa-pulse` | Ring que se expande y desaparece | Botón WA flotante |

### Stagger en listas

```tsx
// Los hijos del contenedor entran con delay incremental (80ms cada uno)
<div className="animate-stagger grid grid-cols-2 gap-4">
  <ProductCard />
  <ProductCard />
  <ProductCard />
  <ProductCard />
</div>
```

### Principios de animación

- **Duración estándar**: 200ms. Rápido para respuestas de UI.
- **Duración de entrada**: 400ms. Slide-up en secciones.
- **Easing**: siempre `ease` (equivale a `ease-in-out` suavizado). Nunca `linear`.
- **Transform + opacity**: las dos propiedades más baratas de animar (no fuerzan layout).
- **No animar**: `width`, `height`, `padding`, `margin` — son caras.

```tsx
// ✅ Correcto — transform y opacity
<div className="transition-transform duration-200 hover:-translate-y-1">

// ✅ Sección con entrada
<section className="animate-slide-up">

// ❌ No animar layout properties
<div className="transition-all hover:p-6">
```

---

## 9. Responsive breakpoints

| Nombre | Ancho | Breakpoint Tailwind | Dispositivo |
|---|---|---|---|
| `xs` | 375px | `xs:` | iPhone SE / pequeños |
| `sm` | 640px | `sm:` | Móviles grandes |
| `md` | 768px | `md:` | Tablets portrait |
| `lg` | 1024px | `lg:` | Tablets landscape / desktop pequeño |
| `xl` | 1280px | `xl:` | Desktop |

### Estrategia mobile-first

Escribir los estilos base para móvil, luego expandir:

```tsx
// ✅ Mobile-first
<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">

// ✅ Texto que crece
<h1 className="text-2xl md:text-3xl font-display">

// ❌ Mal: pensado en desktop y luego reducido
<div className="grid grid-cols-4 sm:grid-cols-2 grid-cols-1">
```

### Patrones comunes

```tsx
// Grid de productos
<div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">

// Grid de servicios
<div className="grid grid-cols-1 sm:grid-cols-3 gap-6">

// Layout two-column (hero)
<div className="flex flex-col lg:flex-row gap-12 items-center">

// Ocultar en mobile / mostrar en desktop
<div className="hidden md:block">...</div>
<div className="md:hidden">...</div>

// Container con padding responsive
<div className="container-main px-4 md:px-6 lg:px-8">
```

---

## 10. Reglas de disciplina

Estas reglas no son sugerencias. Son las condiciones para que el sistema de diseño funcione.

### ❌ Prohibido

```tsx
// Valores arbitrarios — siempre
<div className="text-[15px] p-[18px] rounded-[10px] bg-[#F5A623]">

// Colores fuera de la paleta
<div className="bg-purple-500 text-sky-300 border-pink-200">

// Tamaños de fuente fuera de la escala
<p className="text-xl text-[17px]">

// Spacing fuera de la escala permitida
<div className="p-5 gap-7 m-11">

// Font-display en texto corrido
<p className="font-display">Descripción del producto...</p>

// Sombras arbitrarias
<div className="shadow-[0_4px_12px_rgba(0,0,0,0.1)]">
```

### ✅ Excepciones permitidas

```tsx
// Porcentajes en width/height cuando aplica
<div className="w-full h-screen">

// Translates y transforms para animaciones
<div className="-translate-y-1 translate-x-0">

// aspect-ratio para imágenes de producto
<div className="aspect-square">

// z-index para capas (solo: z-0, z-10, z-20, z-50, z-100)
<nav className="z-50">
```

### Checklist antes de hacer PR

- [ ] ¿Usé solo colores de la paleta definida?
- [ ] ¿El espaciado es uno de los 8 valores permitidos?
- [ ] ¿La tipografía es uno de los 7 tamaños de la escala?
- [ ] ¿Los border-radius son de los 6 valores definidos?
- [ ] ¿Las sombras tienen nombre (shadow-md, shadow-brand, etc.)?
- [ ] ¿El diseño funciona en 375px sin scroll horizontal?
- [ ] ¿Usé `font-display` solo en h1 y h2?
- [ ] ¿Los botones principales son `rounded-full`?
- [ ] ¿Los botones de WhatsApp tienen `shadow-wa`?

---

## Referencia rápida de clases más usadas

```
Texto:        text-stone-900 · text-stone-500 · text-brand-500 · text-wa-dark
Fondo:        bg-stone-50 · bg-white · bg-brand-50 · bg-brand-500 · bg-wa
Borde:        border-stone-200 · border-brand-500 · border-brand-200
Radius:       rounded-sm · rounded · rounded-lg · rounded-xl · rounded-full
Sombra:       shadow-md · shadow-lg · shadow-brand · shadow-wa
Tipografía:   font-display · font-sans · text-xs → text-3xl
Espaciado:    p-1 p-2 p-3 p-4 p-6 p-8 p-12 p-16
Animación:    animate-fade-in · animate-slide-up · animate-pulse
Componentes:  btn-primary · btn-wa · btn-outline · card · badge · eyebrow · container-main
```

---

*Este documento debe actualizarse cada vez que se agregue un token nuevo al sistema.*  
*Ante cualquier duda: consultar antes de inventar.*
