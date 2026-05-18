# ADR 006 — Estructura de layouts del frontend

**Estado**: Aceptado  
**Fecha**: 2026-05-18  
**Sprint**: 2

---

## Contexto

Al armar el frontend en Next.js 14 (App Router), había que decidir dónde vivía
el Nav, el Footer, y los providers globales, considerando que el panel admin
tiene una interfaz completamente distinta a la web pública.

---

## Decisiones

### 1. Nav pública en `(public)/layout.tsx`, no en el root layout

El root layout (`app/layout.tsx`) solo contiene fuentes, providers y metadata base.
El Nav y Footer viven en `app/(public)/layout.tsx`.

**Por qué:** Si el Nav estuviera en el root layout, habría que ocultarlo
condicionalmente en las rutas `/admin/*`. Con route groups, `(public)` y `(admin)`
tienen layouts completamente independientes sin condiciones.

### 2. `ProductCard` como Server Component en Sprint 2

`ProductCard` se renderiza en el servidor. No tiene interactividad (no hay like,
no hay estado local).

**Por qué:** Renderizar en el servidor no agrega bundle JS al cliente. Cuando en
Sprint 4 se agregue el botón de like (`useState`), se extrae a
`components/public/product-card.tsx` con `"use client"` — el cambio es mínimo.

### 3. Botón WA flotante en `page.tsx` con TODO en Sprint 2

El botón flotante de WhatsApp está en `app/(public)/page.tsx` con un TODO para
moverlo a `(public)/layout.tsx` en Sprint 3.

**Por qué:** Es funcional desde hoy. Al navegar a `/productos` va a desaparecer
(está solo en home). El TODO recuerda moverlo a layout cuando se extraiga
`MobileNav` como client component en Sprint 3, momento en que el refactor tiene
costo casi nulo.

---

## Estructura resultante

```
app/
  layout.tsx          ← root: fuentes, providers (TanStack Query), metadata base
  providers.tsx       ← TanStack Query provider ("use client")
  globals.css
  (public)/
    layout.tsx        ← Nav + Footer
    page.tsx          ← home (botón WA flotante acá hasta Sprint 3)
  (admin)/
    layout.tsx        ← pendiente Sprint 4
lib/
  data.ts             ← tipos + data estática del catálogo
```

---

## Consecuencias

- No mover el Nav al root layout sin discutir primero.
- En Sprint 4, cuando se agregue auth al admin, `(admin)/layout.tsx` recibe
  su propio layout sin afectar `(public)`.
- `ProductCard` permanece Server Component hasta que haya interactividad real.