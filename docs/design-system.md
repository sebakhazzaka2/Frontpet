# FrontPet — Design System

> Documento de referencia para todo el sistema de diseño del proyecto.
> Antes de crear un componente nuevo, consultá esta guía.
> Si necesitás agregar un token que no está acá, discutirlo primero.

**Versión**: 2.0
**Última actualización**: 2026-05-25
**Archivos fuente**: `frontend/app/globals.css` (bloque `@theme` — **fuente canónica de tokens**) · DESIGN.md (Stitch, intención de diseño). **No existe `tailwind.config.ts`** (ver ADR 014).

> ⚠️ **v2.0 (mayo 2026)**: paleta y tipografía actualizadas al sistema canónico
> definido en Stitch. La sección **Colores** y **Tipografía** ya reflejan los 4
> colores base (Primary Navy / Secondary Orange / Tertiary Green / Neutral Slate)
> y la familia tipográfica final (Fredoka headlines + Plus Jakarta Sans body).
> Las secciones de spacing, radius y componentes pueden tener ajustes pendientes —
> el DESIGN.md de Stitch tiene la verdad final.

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
| `font-display` | **Fredoka** | Headlines (h1, h2, h3), nombres de servicios, valores destacados en KPIs |
| `font-sans` | **Plus Jakarta Sans** | Body, labels, botones, nav, todo lo demás |
| `font-mono` | JetBrains Mono | Código, SKUs, IDs técnicos |

**Por qué Fredoka**: serif redondeado y friendly, perfecto para el vibe "petshop premium accessible". Reemplaza el DM Serif Display anterior que era demasiado high-contrast.

**Por qué Plus Jakarta Sans**: única familia para todo el body. Pesos limitados a 400/500/600 (nunca 700+) para mantener consistencia visual.

**Instalación en `app/layout.tsx`:**

```tsx
import { Fredoka, Plus_Jakarta_Sans } from 'next/font/google'

const plusJakarta = Plus_Jakarta_Sans({
  subsets: ['latin'],
  variable: '--font-sans',
  weight: ['400', '500', '600'],
})

const fredoka = Fredoka({
  subsets: ['latin'],
  variable: '--font-display',
  weight: ['400', '500', '600'],
})

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body className={`${plusJakarta.variable} ${fredoka.variable}`}>
        {children}
      </body>
    </html>
  )
}
```

> **Regla estricta**: nunca usar pesos 700+ en ninguna fuente. Mantener el tono
> friendly y airy de la paleta. Si necesitás más jerarquía, usar tamaño + color,
> no más peso.

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

> **Actualizado mayo 2026**: paleta alineada con el DESIGN.md canónico generado en
> Stitch. Son **4 colores base** + sus escalas tonales + semánticos. Cualquier color
> fuera de esta paleta está prohibido.
>
> ⚠️ **Ampliación 2.0b (jul 2026)**: se sumaron tokens semánticos en `globals.css` que aún
> no están detallados abajo: `--color-star #E0A82E` (rating), `--color-hover #F3F4F6` y
> `--color-navy-mid #01256E`. No son acentos comerciales nuevos. Ver `docs/port-landing-stitch.md`.

### primary — Deep Navy (identidad)

Anchor de marca. Estructura UI, navegação, branding de alto nivel. Transmite
autoridade e confiabilidade.

| Token | Hex | Uso |
|---|---|---|
| `primary` | `#011E5A` | **Color principal de identidad** — wordmark, sidebars admin, footer, headers oscuros, botões "main system actions" (login, salvar) |
| `primary-dark` | `#000A2C` | Hover state de botões primary |
| `primary-light` | `#2A3F73` | Borders activos, accents sutiles |

### secondary — Vibrant Orange (acción comercial)

Color de **alta visibilidad** para elementos de conversión (Add to Cart, Reservar,
ofertas, badges promocionais).

| Token | Hex | Uso |
|---|---|---|
| `secondary` | `#F4640D` | **Color de ação principal** — botões "Adicionar à sacola", "Agendar", "Novo produto", pills "MAIS VENDIDO", precios destacados, accent del logo "Pet" |
| `secondary-dark` | `#C2400A` | Hover de botões secondary |
| `secondary-soft` | `#FFEDD5` | Background de pills/badges naranjas (status Pendente, eyebrows) |

### tertiary — WhatsApp Green (comunicación)

Color funcional **exclusivo** para acciones de WhatsApp y status de éxito.

| Token | Hex | Uso |
|---|---|---|
| `tertiary` | `#25D366` | **Botões WhatsApp** — "Pedir pelo WhatsApp", "Enviar confirmação", "Falar", floating WA button |
| `tertiary-dark` | `#1FB256` | Hover state |
| `success` | `#00A048` | Status "Confirmado" (pills, dots, borders) |
| `success-soft` | `rgba(0,160,72,0.10)` | Background de pills "Confirmado", "Em estoque" |

### neutral — Slate Grey (texto y estructura)

Balanced slate para texto secundario, borders, y elementos estructurais.

| Token | Hex | Uso |
|---|---|---|
| `ink` | `#0B1C30` | **Texto principal** — headlines, body text con énfasis |
| `muted-strong` | `#444650` | Texto secundario "premium" — labels, meta, breadcrumbs activos |
| `neutral` | `#64748B` | Sub-texto, captions, helper text |
| `muted` | `#6B7280` | Texto muted/desabilitado, placeholders |
| `line` | `#C5C6D1` | **Borders 1px default** |
| `surface` | `#F8F9FF` | Background main da página |
| `surface-alt` | `#EFF4FF` | Background alternativo de secciones, info banners |
| `hover` | `#F5F5F4` | Hover state em rows e items |
| `card` | `#FFFFFF` | Background de cards e superfícies |

### semánticos (estados)

| Token | Uso |
|---|---|
| `error` / `error-soft` | `#BA1A1A` / `rgba(186,26,26,0.06)` — Erros de form, alertas críticas, status "Cancelado" |
| `warning` / `warning-soft` | `#A33E00` / `#FFEDD5` — Status "Pendente", avisos não críticos (usa los mismos tonos del orange secondary-soft) |
| `info` / `info-soft` | `#011E5A` / `#EFF4FF` — Banners informativos, helper text destacado |

### Reglas de uso de color

```tsx
// ✅ Texto principal
<p className="text-ink">...</p>

// ✅ Texto secundario
<p className="text-muted-strong">Detalhes adicionais...</p>

// ✅ Botão de "main system action" (admin)
<button className="bg-primary hover:bg-primary-dark text-white">Entrar</button>

// ✅ Botão de "commercial action"
<button className="bg-secondary hover:bg-secondary-dark text-white">Adicionar à sacola</button>

// ✅ Botão de WhatsApp
<a className="bg-tertiary hover:bg-tertiary-dark text-white">Pedir pelo WhatsApp</a>

// ✅ Pill de status pendente
<span className="bg-secondary-soft text-warning">Pendente</span>

// ✅ Pill de status confirmado
<span className="bg-success-soft text-success">Confirmado</span>

// ❌ Nunca usar colores fuera de la paleta
<p className="text-purple-500">...</p>
<div className="bg-stone-50">...</div>  {/* paleta vieja, eliminada */}

// ❌ Nunca usar valores arbitrarios
<div className="bg-[#ff6b35]">...</div>

// ❌ Nunca usar Navy como background grande
<header className="bg-primary">...</header>  {/* OK SOLO en sidebar admin e footer público */}
```

### Decisiones de uso

- **Navy `#011E5A` solo como superficie estructural** (sidebar admin, footer público,
  nav público) — nunca como botão fill en contexto comercial. Ver ADR 002.
- **Orange `#F4640D` para ações comerciais** — NUNCA para "main system actions" tipo
  login/salvar (eso es navy).
- **Green `#25D366` reservado para WhatsApp** — nunca para botões genéricos de "salvar"
  ou "confirmar". Si la acción es "send to WhatsApp", es verde; si no, no.
- **Sem dark mode** em MVP1. Todo light theme. Ver ADR 007 + sección 6 do CLAUDE.md.

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

⚠️ **Esta sección se reescribió el 2026-07-25.** La versión anterior documentaba clases
`@layer components` (`.btn-primary`, `.btn-wa`, `.card`, `.badge`, `.eyebrow`,
`.container-main`, `.input-search`, `.float-wa`) que **el ADR 014 descartó explícitamente**
el 2026-07-17 — nunca llegaron al browser y contradecían el propio design system (usaban
`rounded-full`/peso 700 donde la escala pide 8px/máx 600). Esta sección quedó sin
actualizar hasta ahora, a pesar de que el ADR ya estaba aceptado. Si ves `container-main`
o `btn-wa` en un componente, es código viejo — no existen en `globals.css`.

**El patrón vigente**: variants como componentes React con `shadcn/ui` + `class-variance-
authority` (ya en el repo, ver `components/ui/button.tsx`), o composición directa de
utilidades Tailwind para todo lo que no sea un componente con variantes reales.

### Botones

`components/ui/button.tsx` ya tiene variants (`default`, `outline`, `secondary`, `ghost`,
`destructive`, `link`). WhatsApp no es una variante genérica de `Button` — es una acción de
marca ajena (verde `--color-wa`, no forma parte de los 4 colores de FrontPet) y suele ir con
el ícono de WhatsApp, así que se compone en el propio componente que lo usa:

```tsx
<Button>Ver Produtos</Button>
<Button variant="outline">Agendar Horário</Button>

// WhatsApp: no es un variant de Button, es una acción de marca ajena
<a
  href={waLink}
  className="inline-flex items-center gap-2 rounded-md bg-wa px-4 py-2 text-white hover:opacity-90"
>
  <WhatsAppIcon className="size-4" />
  Pedir pelo WhatsApp
</a>
```

### Card

Sin clase `.card`: composición directa contra los tokens.

```tsx
<div className="rounded-lg bg-card p-4 shadow-[var(--shadow-card)]">
  {/* contenido */}
</div>
```

### Badge

```tsx
<span className="rounded-sm bg-orange px-2 py-1 text-caption text-white">Mais vendido</span>
```

### Eyebrow (texto sobre titular)

```tsx
<span className="text-eyebrow uppercase tracking-[0.08em] text-orange">Serviços</span>
<h2 className="text-h2-mobile md:text-h2">Tudo que seu pet precisa</h2>
```

### Container principal

1280px = `max-w-7xl` de Tailwind (80rem) **exacto** — no hace falta un token propio.

```tsx
<section>
  <div className="mx-auto max-w-7xl px-4 md:px-6 lg:px-8">
    {/* contenido */}
  </div>
</section>
```

### Botón WhatsApp flotante

```tsx
<a href={waLink} className="fixed bottom-6 right-4 z-50" aria-label="Contatar pelo WhatsApp">
  <span className="absolute inset-0 rounded-full bg-wa animate-ping opacity-75" />
  <span className="relative flex size-14 items-center justify-center rounded-full bg-wa text-white shadow-[var(--shadow-system)]">
    <WhatsAppIcon className="size-6" />
  </span>
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

// Container con padding responsive (1280px = max-w-7xl exacto, ver sección 7)
<div className="mx-auto max-w-7xl px-4 md:px-6 lg:px-8">
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
- [ ] ¿Los botones principales son `rounded-md` (8px)? — **no** `rounded-full` (ver sección 5)
- [ ] ¿Los botones de WhatsApp usan `bg-wa`?

---

## Referencia rápida de clases más usadas

⚠️ **El bloque de abajo tiene el mismo drift de v1.0 que la sección 7** (colores
`stone`/`brand` que no existen en `@theme`, `rounded-full` en botones). Pendiente una
pasada completa de este documento — no bloquea hoy, pero no copiar de acá sin verificar
contra `globals.css` primero.

```
Texto:        text-ink · text-ink-muted · text-navy · text-orange
Fondo:        bg-surface · bg-surface-card · bg-navy · bg-orange · bg-wa
Borde:        border-outline
Radius:       rounded-sm (4px) · rounded-md (8px) · rounded-lg (16px) · rounded-xl (24px) · rounded-full (pills)
Sombra:       shadow-[var(--shadow-card)] · shadow-[var(--shadow-system)]
Tipografía:   font-display (solo h1-h3) · font-sans (default) · text-eyebrow → text-display
Espaciado:    p-1 p-2 p-3 p-4 p-6 p-8 p-12 p-16 (múltiplos de 4, ver sección 5 del CLAUDE.md)
Animación:    animate-fade-in · animate-slide-up · animate-pulse
Componentes:  Button (shadcn/ui + CVA) · composición directa para card/badge/eyebrow (sección 7)
```

---

*Este documento debe actualizarse cada vez que se agregue un token nuevo al sistema.*  
*Ante cualquier duda: consultar antes de inventar.*
