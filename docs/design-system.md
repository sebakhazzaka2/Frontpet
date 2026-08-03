# FrontPet — Design System: guía de uso

**Versión**: 3.0 — 2026-07-27
**Rol de este documento**: guía de **uso y composición**. Acá viven las reglas, los
patrones y los ejemplos — **no las tablas de valores**.

> **Fuentes canónicas** (este doc las referencia, nunca las duplica):
> - **Valores de todos los tokens** (colores, tipografía, radius, sombras, container):
>   `frontend/app/globals.css`, bloque `@theme`. Si un valor de acá contradice al CSS,
>   manda el CSS.
> - **Decisiones y porqués**: ADR 014 (config CSS-first, escala de radius, traducción
>   desde Stitch, colisión con shadcn).
> - **Intención de diseño**: `DESIGN.md` del proyecto de Stitch (vía MCP) — referencia
>   de intención, no autoridad sobre clases (ver CLAUDE.md §5).
>
> Historia: la v2.0 de este doc duplicaba las tablas de tokens y drifteó dos veces
> (paleta v1.0, radius). La v3.0 elimina la causa: una sola fuente de valores.

---

## 1. Principios

- **Calidez local** — FrontPet es un negocio local de Santana do Livramento (RS, Brasil).
  Cercanía, no frialdad corporativa: naranja comercial sobre bases claras, navy estructural.
- **WhatsApp-first** — el CTA principal de toda la plataforma es WhatsApp; sus botones
  verdes tienen jerarquía igual o mayor que cualquier CTA secundario.
- **Mobile-first, siempre** — 80%+ del tráfico es mobile. Diseñar a 375px y expandir.
- **Consistencia sobre creatividad puntual** — usar los tokens vale más que "se ve mejor
  con 13px". La consistencia acumulada es lo que hace ver profesional al producto.
- **Menos es más en animaciones** — una animación buena en el hero vale más que diez
  micro-animaciones. Reservarlas para momentos de impacto.
- **Sin dark mode en MVP1** — interfaz siempre clara (lo fija `DESIGN.md`).

---

## 2. Tipografía

Dos familias (cargadas por `next/font` en `app/layout.tsx`):

- **`font-display` (Fredoka)** — SOLO headlines h1-h3, nombres de servicios, valores
  destacados. Ya es el default de `h1`-`h3` vía `@layer base`.
- **`font-sans` (Plus Jakarta Sans)** — todo lo demás (default del body).

**Regla dura: pesos 400/500/600 únicamente. Nunca 700+** — `next/font` ni siquiera los
carga; un `font-bold` se renderiza sintetizado y se ve mal. Jerarquía = tamaño + color,
no más peso.

La escala es **custom con nombres semánticos** (no la default de Tailwind — `text-xl`,
`text-2xl` etc. no son del sistema). Pasos y valores: bloque `--text-*` de `@theme`.
De menor a mayor: `eyebrow` · `caption` · `label` · `sm` · `base` · `h3` · `h2-mobile` ·
`h2` · `display-mobile` · `display` · `hero-mobile` · `hero-tablet` · `hero` (los `hero-*`
son exclusivos del headline del Hero).

```tsx
// ✅ Titular de sección (Fredoka la pone @layer base)
<h2 className="text-h2-mobile md:text-h2">Tudo que seu pet precisa</h2>

// ✅ Eyebrow sobre el titular
<span className="text-eyebrow uppercase text-orange">Serviços</span>

// ✅ Título de card — sans, no display
<h3 className="font-sans text-h3 font-semibold text-ink">Royal Canin Medium 15kg</h3>

// ✅ Body y meta
<p className="text-base text-ink-muted">Alimento completo…</p>
<span className="text-caption text-slate">Entrega em Livramento</span>

// ❌ Display en texto corrido · ❌ tamaño fuera de escala · ❌ peso 700
<p className="font-display">…</p>   <p className="text-[17px]">…</p>   <b className="font-bold">…</b>
```

Si una pantalla de Stitch trae un tamaño que no está en la escala: **no se agrega un paso
nuevo** — se mapea al más cercano y se reporta (ver `/port-screen`).

---

## 3. Color

Valores y lista completa de tokens: bloque `--color-*` de `@theme`. Roles:

- **`navy` (+ `navy-dark/light/mid`)** — identidad y estructura: sidebar admin, footer,
  nav; botón de "main system action" (login, salvar). **Nunca** como fill de acción comercial.
- **`orange` (+ `orange-dark/light`)** — acción comercial: "Adicionar à sacola",
  "Agendar", pills, precios destacados. **Nunca** para acciones de sistema.
- **`wa` (+ `wa-dark`)** — **exclusivo WhatsApp**. Si la acción no envía a WhatsApp, no es verde.
- **`slate`** — texto secundario, captions.
- **Superficies y texto**: `surface` (fondo de página) · `surface-card` (cards) ·
  `ink` / `ink-muted` (texto) · `outline` (borders 1px) · `hover` (fondo hover neutro).
- **`star`** — 5º color, semántico: estrellas de rating exclusivamente. No es un acento
  comercial nuevo.

**Puente shadcn**: los componentes de `components/ui/*` consumen `bg-primary`,
`border-border`, etc. — `@theme` los mapea sobre la paleta FrontPet (ADR 014 §3).
En componentes propios usar los nombres FrontPet (`bg-navy`, no `bg-primary`).

```tsx
// ✅ Acción de sistema (admin)          // ✅ Acción comercial
<Button>Entrar</Button>                  <button className="bg-orange hover:bg-orange-dark text-white">Adicionar à sacola</button>

// ✅ WhatsApp
<a className="bg-wa hover:bg-wa-dark text-white">Pedir pelo WhatsApp</a>

// ❌ Fuera de paleta · ❌ hex hardcodeado · ❌ paleta v1.0 muerta
<p className="text-purple-500">  <div className="bg-[#ff6b35]">  <div className="bg-stone-50">
```

⚠️ Los hex de `@theme` son **placeholders** pendientes de confirmación del cliente —
por eso mismo, jamás hardcodear un hex en un componente: el día que se confirme la
paleta, el cambio es solo en `@theme`.

**La única excepción del repo** (verificada 2026-08-03): `themeColor: '#F8F9FF'` en
`app/layout.tsx`. La API `Viewport` de Next serializa a un `<meta>` antes de que exista
CSS, así que no puede leer una var de `@theme` — el hex tiene que estar literal. Es
entonces el único lugar donde la paleta está **duplicada**, y va a driftear en silencio
el día que el cliente confirme los colores. Está en el checklist D.10 del ROADMAP para
sincronizarlo a mano en ese momento. Si aparece un segundo caso así, listarlo acá: la
regla es "cero hex hardcodeados salvo los que estén en esta lista".

---

## 4. Espaciado

Escala: **múltiplos de 4 solamente** — `p-1 p-2 p-3 p-4 p-6 p-8 p-12 p-16` y equivalentes
en `gap`/`m`. No usar `p-5, p-7, p-9, p-10, p-11, p-14` ni valores arbitrarios (`p-[18px]`).

---

## 5. Radius

Escala canónica de 4 pasos + pill (valores en `--radius-*` de `@theme`; decisión y
tabla de traducción desde Stitch: **ADR 014**):

- `rounded-sm` — chips chicos, badges
- `rounded-md` — **botones, inputs, chips, icon buttons** (los botones NO son pill)
- `rounded-lg` — **cards** (cerrado en 16px, decisión 2026-07-17)
- `rounded-xl` — modales, bottom sheets
- `rounded-full` — status pills, avatars

⚠️ Dos trampas permanentes (detalle en ADR 014): las clases `rounded-*` de **Stitch**
significan otra cosa (Tailwind v3) — traducir siempre; y los componentes **shadcn**
recién instalados asumen `lg`=8px — bajar sus radios un paso al instalarlos.

---

## 6. Sombras

Tres tokens, suaves y ambientales (valores en `--shadow-*` de `@theme`):

```tsx
<div className="shadow-[var(--shadow-card)] hover:shadow-[var(--shadow-card-hover)] transition-shadow">
<span className="shadow-[var(--shadow-system)]">   {/* elementos flotantes del sistema */}
```

Nada de sombras arbitrarias propias ni `drop-shadow-[...]`. (Las clases `shadow-brand` /
`shadow-wa` de la v1.0 de este doc **no existen**.)

---

## 7. Patrones de componente

**El patrón vigente** (ADR 014): variants como componentes React con shadcn/ui + CVA
(`components/ui/button.tsx` ya tiene `default`, `outline`, `secondary`, `ghost`,
`destructive`, `link`), o composición directa de utilidades para lo que no tiene
variantes reales. **No existen clases `@layer components`** (`.btn-primary`, `.card`,
`.container-main` — si aparecen en un componente, es código muerto v1.0).

```tsx
// Botones
<Button>Ver Produtos</Button>
<Button variant="outline">Agendar Horário</Button>

// WhatsApp: NO es un variant de Button — es acción de marca ajena, se compone in situ
<a href={waLink}
   className="inline-flex items-center gap-2 rounded-md bg-wa px-4 py-2 text-white hover:opacity-90">
  <WhatsAppIcon className="size-4" /> Pedir pelo WhatsApp
</a>

// Card — composición directa
<div className="rounded-lg bg-surface-card p-4 shadow-[var(--shadow-card)]">…</div>

// Badge
<span className="rounded-sm bg-orange px-2 py-1 text-caption text-white">Mais vendido</span>

// Container principal — 1280px = max-w-7xl exacto, sin token propio
<div className="mx-auto max-w-7xl px-4 md:px-6 lg:px-8">…</div>

// WhatsApp flotante
<a href={waLink} className="fixed bottom-6 right-4 z-50" aria-label="Contatar pelo WhatsApp">
  <span className="absolute inset-0 rounded-full bg-wa animate-ping opacity-75" />
  <span className="relative flex size-14 items-center justify-center rounded-full bg-wa text-white shadow-[var(--shadow-system)]">
    <WhatsAppIcon className="size-6" />
  </span>
</a>
```

Reusar siempre lo existente antes de crear: `WhatsAppIcon`, `InstagramIcon`,
`buildWhatsAppLink` (`@/lib/data/site`), `formatPrice` (`@/lib/utils`). Iconos:
`lucide-react` (mapeo desde Material Symbols: `docs/port-landing-stitch.md` §6).

---

## 8. Animación

No hay utilidades de animación custom en `@theme` — las entradas y transiciones de
impacto se hacen con **Framer Motion** (instalado; el Hero es el caso de referencia,
tarea 2.2), y los micro-estados con las utilidades `transition-*` de Tailwind.
(`animate-fade-in`, `animate-slide-up`, `animate-stagger` de la v1.0 **no existen**;
`animate-ping`/`animate-pulse` son de Tailwind core y sí.)

Principios:
- Micro-interacciones ~200ms, entradas ~400ms; easing `ease`, nunca `linear`
- Animar solo `transform` + `opacity` — nunca `width`/`height`/`padding`/`margin`

```tsx
// ✅ hover barato
<div className="transition-transform duration-200 hover:-translate-y-1">
// ❌ anima layout
<div className="transition-all hover:p-6">
```

---

## 9. Responsive

Breakpoints: **los default de Tailwind** (`sm` 640 · `md` 768 · `lg` 1024 · `xl` 1280).
No existe breakpoint custom `xs`. Estrategia mobile-first: estilos base para móvil,
expandir hacia arriba.

```tsx
// Grid de productos                          // Two-column hero
<div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
<div className="flex flex-col lg:flex-row gap-12 items-center">

// Mostrar/ocultar por dispositivo (ej.: BottomNav es md:hidden)
<div className="hidden md:block">…</div>  <div className="md:hidden">…</div>
```

Validación: 320 / 768 / 1024px (+1440 si hay layout desktop propio) — es parte de la
DoD (CLAUDE.md §10).

---

## 10. Checklist de disciplina (antes de PR)

- [ ] Solo colores de la paleta (`bg-navy`, `bg-orange`, `bg-wa`, …) — cero hex, cero
      `stone-*`/`amber-*`/paletas default
- [ ] Spacing en múltiplos de 4 · tipografía en la escala custom · radius de la escala de 4 pasos
- [ ] Botones `rounded-md`, cards `rounded-lg` — nada copiado literal de Stitch
- [ ] Sombras solo con los 3 tokens `--shadow-*`
- [ ] `font-display` solo en h1-h3 · ningún peso 700+
- [ ] WhatsApp: `bg-wa` + `buildWhatsAppLink`, nunca número hardcodeado
- [ ] Funciona a 320px sin scroll horizontal

---

*Los valores viven en `globals.css`. Ante la duda: consultar antes de inventar un token.*
