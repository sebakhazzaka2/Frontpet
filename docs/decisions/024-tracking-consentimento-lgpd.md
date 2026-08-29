# ADR 024 — Tracking de terceiros e consentimento LGPD

**Estado**: Aceptada
**Fecha**: 2026-08-29
**Sprint**: 7 (tareas 7.4, 7.6, 7.13)

---

## Contexto

FrontPet llegaba al Sprint 7 sin ninguna integración de terceiros en el cliente: cero `fbq`,
cero `gtag`, cero `next/script`, cero `localStorage`. Los snippets de Meta Pixel y Plausible
existían **comentados** en el `<head>` de `frontend/app/layout.tsx`, con el Pixel ID en
`TU_PIXEL_ID` y el dominio de Plausible hardcodeado.

El ROADMAP pide dos cosas acopladas: instrumentar eventos estándar de Meta Pixel (7.4) y
Plausible (7.6), y un banner de consentimiento LGPD que gatee el Pixel (7.13) — con la
restricción explícita de que 7.13 tiene que implementarse **antes** que 7.4 ("el Meta Pixel
no puede disparar antes del consentimiento").

Es la primera vez que este repo integra scripts de terceros en el cliente — no había
precedente de código para decidir sobre.

---

## Decisión

### Plausible no pasa por el gate de consentimiento

Verificado contra su data policy: Plausible no usa cookies, no genera identificadores de
visitante persistentes y no recolecta datos personales (plausible.io/data-policy — *"By using
Plausible, you do not need cookie banners for analytics or to collect consent for
tracking"*). Gatearlo degradaría la única métrica de tráfico que el cliente va a mirar a
diario, sin ningún beneficio legal a cambio. Se carga incondicionalmente desde
`NEXT_PUBLIC_ANALYTICS_DOMAIN` en el `<head>` del root layout.

Tampoco necesita código adicional para el App Router: `script.js` ya engancha listeners al
History API y dispara pageviews solo en `pushState` (plausible.io/docs/spa-support) — no hace
falta `next-plausible` ni un listener de `usePathname`.

### El Meta Pixel se gatea con un gate estricto: no se carga sin consentimiento

La alternativa — cargar siempre `fbevents.js` y llamar `fbq('consent', 'revoke')` — es más
simple, pero permite que un script de un tercero se ejecute en el navegador de alguien que
todavía no dijo que sí. El ROADMAP pide explícitamente que el Pixel no dispare antes del
consentimiento, así que el componente `<MetaPixel granted={...}>` directamente no renderiza
nada mientras `granted` sea falso.

`fbq('consent', 'revoke')` sí se usa, pero para el caso complementario: el usuario acepta y
**después** revoca en la misma sesión. Ahí el script ya está cargado y no se puede
descargar — se pausa el envío con la API oficial de Meta
(developers.facebook.com/docs/meta-pixel/implementation/gdpr).

### Estado de consentimiento en `localStorage`, con versión y timestamp

`frontend/hooks/use-consent.ts` replica el store vanilla de `hooks/use-cart.ts`
(`useSyncExternalStore` + `Set` de listeners + snapshot cacheado), con tres diferencias:

1. `localStorage`, no `sessionStorage` — el carrito muere con la pestaña a propósito; un
   consentimiento que se re-pregunta en cada pestaña es hostil y ambiguo desde lo legal.
2. `subscribe` también escucha el evento `storage` del `window`, porque `localStorage` es
   compartido entre pestañas (a diferencia del carrito).
3. `getServerSnapshot` devuelve `'unknown'` — nunca `'granted'` — garantizando que el Pixel
   jamás se renderiza en SSR ni en el primer render del cliente.

El valor guardado es `{ v: number, status: 'granted'|'denied', ts: string }`, no un `boolean`
pelado: `v` (versión de la política) permite forzar re-consentimiento cuando el texto del
banner cambie de forma material, sin tocar código de lectura; `ts` es la evidencia de *cuándo*
se consintió, que es lo que la LGPD exige poder demostrar.

### Eventos disparados antes del consentimiento se descartan, no se bufferean

Si el usuario navega 3 páginas y recién ahí acepta, esas 3 páginas nunca se reportan — no se
puede reportar retroactivamente una navegación que ocurrió sin consentimiento. La capa de
tracking (`frontend/lib/analytics/pixel.ts`) es no-op mientras `window.fbq` no exista, que es
exactamente cuándo no hay consentimiento — sin necesidad de que cada punto de instrumentación
(`add-to-cart-button.tsx`, `checkout-form.tsx`, etc.) sepa nada sobre consentimiento.

### `Purchase` es best-effort frente al redirect a WhatsApp

`checkout-form.tsx` hace `window.location.assign(buildWhatsAppLink(...))` inmediatamente
después de crear el pedido (ADR 003) — navegación same-tab que puede matar el beacon del
Pixel. Se dispara `trackPurchase` apenas resuelve `createOrder()`, antes de esa navegación, y
se acepta la pérdida marginal restante. No se cambia la UX del checkout para favorecer la
medición: abrir WhatsApp en pestaña nueva rompería un flujo diseñado a propósito y los popup
blockers lo castigan. El arreglo robusto es Conversions API server-side — ya está en el
ROADMAP como tarea 7.18, marcada opcional.

### `Contact` vía un único listener delegado, no 11 componentes convertidos a cliente

Hay 11 call sites de `wa.me` en el sitio, varios en Server Components (`nav.tsx`,
`footer.tsx`, `bottom-nav.tsx`, `floating-wa.tsx`, `product-card.tsx`...). Convertirlos todos
a `'use client'` para agregar un `onClick` violaría "Server Components por defecto"
(CLAUDE.md §5). `ConsentGate` agrega un único listener a nivel de `document` que matchea
`a[href^="https://wa.me/"]` — vanilla, sin tocar los 11 archivos.

---

## Alternativas consideradas

### ❌ Gatear Plausible también, por consistencia

Simetría cosmética a costa de datos de tráfico reales desde el día uno. Plausible no
recolecta nada que la LGPD regule — no hay razón legal para tratarlo igual que el Pixel.

### ❌ Cargar el Pixel siempre y usar `fbq('consent', 'revoke')` como gate por defecto

Es el patrón "oficial" de Meta para consentimiento, pero dispara la descarga y ejecución de
`fbevents.js` en el navegador de cualquiera, incluso sin consentimiento. El ROADMAP pide
explícitamente que el Pixel "no dispare antes del consentimiento" — cargarlo ya es parte de
disparar.

### ❌ Bufferear eventos pre-consentimiento y flushearlos si el usuario acepta

Retrasar el envío en vez de descartarlo sigue siendo tracking de una navegación no
consentida — solo que diferido. No resuelve el problema, lo esconde.

---

## Consecuencias

### Positivas
- Cero riesgo de que Meta reciba datos de un visitante que no consintió.
- La métrica de tráfico (Plausible) no se degrada por el gate.
- El patrón de estado (`useSyncExternalStore` + storage) es idéntico al del carrito — sin
  librería nueva, sin abstracción de "proveedores de analytics" (CLAUDE.md §6: solo hay un
  caso real, Plausible no necesita ninguna línea de tracking).

### Negativas
- El `Purchase` puede perderse si el navegador corta la petición antes de que el beacon salga
  — aceptado, con la Conversions API (7.18) como arreglo futuro si se vuelve un problema real.
- Si el usuario navega varias páginas antes de aceptar, esas páginas nunca entran en las
  métricas del Pixel — inherente a cualquier gate de consentimiento serio, no un defecto de
  esta implementación.
