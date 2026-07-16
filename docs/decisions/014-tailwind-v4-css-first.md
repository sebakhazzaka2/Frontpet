# ADR 014 — Config de Tailwind CSS-first (`@theme` en `globals.css`)

**Estado**: Aceptada
**Fecha**: 2026-07-16
**Sprint**: 2

---

## Contexto

El repo tiene **Tailwind v4** instalado (`tailwindcss@4.3.0`, `@tailwindcss/postcss`),
pero los archivos de estilo estaban escritos con la forma de la v3. Concretamente:

- `frontend/tailwind.config.ts` (280 líneas) definía la paleta, la escala tipográfica,
  los radios y las sombras.
- `frontend/app/globals.css` abría con `@tailwind base / components / utilities` y
  redefinía los mismos colores como custom properties en `:root`.

**Ninguno de los dos llegaba al browser.** Tailwind v4 no autocarga `tailwind.config.ts`:
requiere un `@config` explícito en el CSS, y `globals.css` no lo tenía. Las directivas
`@tailwind` son sintaxis v3, reemplazadas en v4 por `@import "tailwindcss"`.

Verificación (CSS compilado en `.next/dev`, 2026-07-16):

| Token buscado | Ocurrencias |
|---|---|
| `--color-brand` | 0 |
| `.btn-primary` | 0 |
| `.float-wa` | 0 |
| `--tw-*` (defaults de Tailwind) | 141 |

Es decir: Tailwind corría con su tema por defecto y todo el design system del repo
quedaba huérfano. `CLAUDE.md` describía el config como "desactualizado (v1.0)"; en
realidad estaba muerto — la discusión v1.0 vs v2.0 era sobre un archivo sin efecto.

---

## Decisión

### 1. La config vive en `globals.css` vía `@theme`. No hay `tailwind.config.ts`.

`frontend/tailwind.config.ts` se **elimina**. `globals.css` abre con
`@import "tailwindcss"` y declara el design system en un bloque `@theme`.

**Por qué:** es el idiom nativo de Tailwind v4 y deja **una sola fuente de verdad**.
La alternativa (mantener el `.ts` + agregar `@config`) conserva un patrón legacy que
Tailwind desaconseja y que ya causó esta confusión una vez.

**Consecuencia práctica:** cuando el cliente confirme la paleta definitiva, se cambian
los `--color-*` en `@theme` y se actualiza toda la app. No hay hex hardcodeados en
componentes.

### 2. Las clases de componente se descartan; los variants van a shadcn/ui + CVA.

`.btn-primary`, `.btn-wa`, `.btn-outline`, `.card`, `.badge`, `.eyebrow`,
`.container-main`, `.input-search`, `.float-wa` se eliminan del `@layer components`.

**Por qué:** (a) nunca llegaron al browser, así que no hay regresión posible;
(b) el repo ya tiene `shadcn/ui` + `class-variance-authority`, que es donde deben
vivir los variants; (c) las que estaban escritas **contradecían el propio design
system** — usaban `rounded-full` y `font-bold` (700), cuando `DESIGN.md` fija botones
en 8px y prohíbe pesos 700+.

`globals.css` queda con `@theme` + un `@layer base` mínimo (body, h1-h3, focus-visible).

### 3. `@theme` incluye un puente semántico para shadcn/ui.

`components/ui/button.tsx` consume `bg-primary`, `border-border`, `text-destructive`,
`ring-ring`, `--radius-md`. Ninguno existía. `@theme` mapea esos nombres sobre la
paleta FrontPet: `--color-primary: var(--color-navy)`,
`--color-secondary: var(--color-orange)`, etc.

**Por qué:** sin ese mapeo los componentes de shadcn quedan sin estilo. El puente
permite usar shadcn tal como viene, sin forkear cada componente.

### 4. Fuentes: Fredoka + Plus Jakarta Sans, vía `next/font`, pesos 400/500/600.

`app/layout.tsx` cargaba `DM_Sans` + `DM_Serif_Display` (paleta v1.0) con peso 700
incluido. Se reemplazan por las de `DESIGN.md`. Las variables `--font-fredoka` /
`--font-jakarta` se inyectan en `<html>` y `@theme` las mapea a `--font-display` /
`--font-sans`.

### 5. Los colores por defecto de Tailwind NO se limpian (por ahora).

`@theme` **agrega** a la paleta default; no la reemplaza. `bg-stone-50`, `bg-amber-400`
y compañía siguen resolviendo.

**Por qué:** limpiar el namespace (`--color-*: initial`) es lo que haría cumplir la
regla de `CLAUDE.md` §5 ("solo los colores definidos acá") y convertiría un color
fuera de paleta en **error de build** en vez de un bug visual silencioso — valioso
para trabajo en bucle. Pero se pospone hasta que `(public)/` esté portado: hoy esas
páginas usan `stone-*` y `amber-*` por todos lados y limpiar rompería el build.

**Pendiente:** activar `--color-*: initial` una vez portado `(public)/`.

### 6. Escala de radius canónica: 4 / 8 / 16 / 24. Las clases de Stitch no se copian.

Al portar la primera pantalla apareció que **había cuatro escalas de radius en juego**,
y no coincidían:

| Fuente | Botones | Cards | Modales |
|---|---|---|---|
| Prosa de `DESIGN.md` | 8px (`rounded-md`) | **16px** (`rounded-lg`) | 24px (`rounded-xl`) |
| Frontmatter de `DESIGN.md` | 8px (`DEFAULT`) | **16px** (`lg`) | 24px (`xl`) |
| Las 18 pantallas de Stitch | 8px (`rounded-lg`) | **12px** (`rounded-xl`) | 24px (`rounded-3xl`) |
| Repo (`@theme`) | 8px (`rounded-md`) | **16px** (`rounded-lg`) | 24px (`rounded-xl`) |

Dos cosas distintas se estaban mezclando:

- **Los valores de botón nunca estuvieron en conflicto**: 8px en las cuatro fuentes.
  Lo que cambia es el *nombre* de la clase. La prosa lo llama `rounded-md`, el
  frontmatter `DEFAULT`, las pantallas `rounded-lg`.
- **Los cards sí tenían conflicto de valor**: 12px (pantallas) vs 16px (prosa +
  frontmatter). Verificado: `rounded-xl` aparece en 29 elementos `bg-white` con
  `shadow-sm`/`shadow-md` y `p-4` — son cards, y en el CDN v3 `rounded-xl` = 12px.

**Se resuelve a favor de 16px para cards.** Dos de tres fuentes lo dicen, y `CLAUDE.md`
ya lo declaraba. Las pantallas son la fuente más débil: **solo la landing define un
`borderRadius` propio; las otras 17 no definen ninguno y heredan los defaults del CDN de
Tailwind v3**. O sea, sus radios no son una decisión de diseño — son un default que se
coló.

**Se descarta el paso de 12px.** `DESIGN.md` solo lo menciona para "icon buttons:
10-12px", que no justifica un token. Usan `rounded-md` (8px).

Escala canónica final: **`sm` 4px · `md` 8px · `lg` 16px · `xl` 24px · `full` pill.**

#### Tabla de traducción Stitch → repo

Las pantallas de Stitch usan **324 clases `rounded-*`** y **ninguna significa lo mismo
en el repo**. Al portar, traducir siempre:

| En `code.html` de Stitch | Valor real (CDN v3) | Clase equivalente en el repo |
|---|---|---|
| `rounded-lg` | 8px | **`rounded-md`** |
| `rounded-xl` | 12px | **`rounded-lg`** (se sube a 16px, ver arriba) |
| `rounded-2xl` | 16px | **`rounded-lg`** |
| `rounded-3xl` | 24px | **`rounded-xl`** |
| `rounded` (DEFAULT) | 4px | **`rounded-sm`** |
| `rounded-full` | pill | `rounded-full` (única que no cambia) |

#### Colisión con la convención de shadcn/ui

Hay un segundo choque, menos obvio y **permanente**: el tema por defecto de shadcn asume
`--radius-lg` = 0.5rem (**8px**), y sus componentes están escritos con esa premisa. Nuestra
escala redefine `rounded-lg` a **16px**, así que **todo componente de shadcn que se instale
va a renderizar radios al doble de lo que su autor pensó**.

Ya pasó con el único componente del repo: `components/ui/button.tsx` usaba `rounded-lg`
para el botón base (16px con nuestra escala, cuando los botones van a 8px) y en cuatro
variantes de `button-group`. Corregido a `rounded-md`.

**Regla al instalar un componente shadcn nuevo**: revisar sus clases `rounded-*` y bajarlas
un paso (`rounded-lg` → `rounded-md`, `rounded-xl` → `rounded-lg`). No es opcional: el
componente se ve mal, no rompe el build, así que nadie se entera solo.

Se aceptó esta fricción en vez de renombrar los tokens a la convención de shadcn, porque
`DESIGN.md` y `CLAUDE.md` nombran explícitamente `rounded-md` = botones y `rounded-lg` =
cards, y esos nombres ya están en la cabeza y en la doc del proyecto.

**Pendiente de revisión estética.** Los 16px de cards salen de la documentación, no del
ojo. Al portar la primera pantalla, comparar contra el `screen.png` y confirmar si se
ven mejor a 12 o a 16. Es cambiar `--radius-lg` en `globals.css`, una línea.

---

## Consecuencias

- **No recrear `tailwind.config.ts`.** Si algo necesita config, va en `@theme`.
- La paleta en `@theme` son **placeholders de `DESIGN.md`**, pendientes de confirmación
  del cliente. Cambiarlos ahí, nunca en componentes.
- `app/(public)/layout.tsx` y `app/(public)/page.tsx` siguen escritos contra el design
  system muerto (`bg-brand-500`, `container-main`, `btn-primary`, `shadow-brand`,
  `font-display`). **Ya estaban rotos antes de este ADR**; este cambio no los rompe
  más, pero tampoco los arregla. Requieren un port aparte — y como además están en
  español y dicen "Pehuajó" (el cliente es de Santana do Livramento y opera en PT-BR,
  ver ADR 007), conviene regenerarlos desde las pantallas de Stitch en vez de portarlos.
- `CLAUDE.md` §5 debe actualizarse: el bloque que dice "`frontend/tailwind.config.ts`
  está DESACTUALIZADO (v1.0)" ya no aplica — el archivo no existe.
- `CLAUDE.md` §2 declara Next.js 14 + React 18; el repo tiene **Next 16.2.6 + React 19.2.4**.
  Desfasaje detectado acá, se corrige aparte.

---

## Verificación pendiente

Este ADR se escribió sin poder correr `npm run build` (el entorno de desarrollo
resetea el cwd a una ruta UNC que `cmd` rechaza, así que `npx next build` no arranca).
La evidencia es estática: contenido de archivos + CSS compilado en `.next/dev`.

**Antes de dar esto por cerrado**: correr `npm run build` y validar visualmente a
320px / 768px / 1024px (Definition of Done, `CLAUDE.md` §10).
