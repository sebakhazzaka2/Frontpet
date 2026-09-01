# ADR 025 — Reviews reais do Google Business, mapa de localização e encabezado canónico

**Estado**: Aceptada
**Fecha**: 2026-09-01

---

## Contexto

`docs/pending-decisions.md` §15 (detectado en auditoría de release, 2026-08-03) marcaba
`frontend/lib/data/reviews.ts` como un problema bloqueante para el Sprint Despliegue: los 3
testimonios de `<Reviews>` son de personas que no existen, con textos de elogio inventados. En
un sitio público a nombre de un negocio real, eso es publicidade enganosa (CDC art. 37), no un
detalle de implementación.

La opción registrada en pending-decisions era pedirle testimonios reales al cliente a mano
(`preguntas-cliente.md` §6.5). En su lugar se optó por traer reviews reales directo del
listado de Google Business del negocio vía Places API — misma resolución del problema (deja de
haber contenido inventado), sin depender de que el cliente junte y transcriba testimonios.

Un primer intento de esta integración quedó roto sin que se notara: usaba el endpoint legacy
`maps.googleapis.com/maps/api/place/details/json`, que devuelve `REQUEST_DENIED — "You're
calling a legacy API, which is not enabled for your project"`. El `catch` caía al array
estático (`lib/data/reviews.ts`) y la home publicaba los 3 testimonios inventados sin ningún
error visible — exactamente el riesgo que este ADR existe para eliminar.

De paso se agrega un mapa de ubicación (`<LocationMap>`) debajo de Reviews con la Maps Embed
API, y un componente `<SectionHeading>` compartido — al construir el header de Reviews se hizo
evidente que las 8 secciones de la superficie pública repetían el mismo markup a mano con 7
inconsistencias reales (alineación mixta, 4 espaciados distintos, 3 mecanismos de centrado).

---

## Decisión

### Places API (New), no el endpoint legacy

`GET https://places.googleapis.com/v1/places/{placeId}` con `X-Goog-Api-Key` y
`X-Goog-FieldMask` en headers (no en query, para que la key no quede en la URL — cache key,
logs, traces). El endpoint legacy (`maps.googleapis.com/maps/api/place/details/json`) está dado
de baja para proyectos nuevos y devuelve `REQUEST_DENIED`; verificado 2026-09-01 con la misma
key que la app usa.

### Un solo punto de red (`getPlaceSummary()`) para 4 consumidores

`<Reviews>`, `<TrustBar>`, `<Hero>` y `<LocationMap>` necesitan datos del mismo Place. Se pide
el **superconjunto** de campos (`rating,userRatingCount,shortFormattedAddress,
formattedAddress,googleMapsUri,reviews`) desde una única función en `lib/google-places.ts`, en
vez de que cada componente pida solo lo suyo. La cache key del fetch de Next es
URL+método+headers: si cada consumidor pidiera un `fields` distinto, serían requests (y cuota)
distintos. Con una sola firma, los 4 comparten un fetch.

El dedupe no se deja solo en manos del fetch cache de Next: `getPlaceSummary` está envuelta en
`cache()` de React, que memoiza la llamada dentro del mismo render sin depender de que la
serialización de headers coincida byte a byte entre las 4 invocaciones. **No hay test
automatizado de esto**: `cache()` solo memoiza bajo la condición de módulo "react-server" que
activa el runtime de Server Components de Next — en Vitest (Node plano) es un passthrough sin
memoización, verificado a mano. Sigue siendo un supuesto que depende del runtime real de Next,
mitigado (no eliminado) por `cache()` además del fetch cache.

### Place ID fijo en env, no resuelto por query en runtime

El Place ID de FrontPet se resuelve **una sola vez, a mano**, con la Places API (Find Place
From Text) y queda fijo en `NEXT_PUBLIC_GOOGLE_PLACE_ID`. Se descartó resolverlo en cada
request a partir de un `GOOGLE_PLACE_QUERY` — es una llamada extra a la API por cold start, más
código, y un modo de falla adicional sin ningún beneficio: el Place ID de un negocio no cambia.

### `getPlaceSummary()` vive en `lib/`, no en `app/api/`

Los 4 consumidores ya son Server Components, así que llaman `lib/google-places.ts` directo — no
hace falta un Route Handler intermedio ni un fetch desde el cliente.

### Cache de 24h vía `fetch(..., { next: { revalidate } })`

Google Places ToS exige no cachear Place Data por tiempo indefinido y refrescarlo con
regularidad. `revalidate: 86400` cumple ambos lados: refresca dentro de un día y evita gastar
cuota de la API en cada request. Sigue siendo la API válida en Next 16 (`docs/next16-notes.md`
solo marca cambios en `revalidateTag`/`cacheComponents`, no en `fetch`).

### Sin fallback a contenido inventado — nunca más

`lib/data/reviews.ts` se **borra**, no se mantiene como red de seguridad. Si `getPlaceSummary()`
falla, no está configurada, o el negocio todavía no tiene reviews, `<Reviews>` muestra el
header + el badge de rating (si hay datos) **sin cards**, nunca testimonios de ejemplo. La
integración anterior probó en carne propia que un fallback "temporal" con datos falsos termina
en producción sin que nadie lo note — la corrección correcta es que el fallo se vea, no que se
tape con contenido creíble.

### `<SectionHeading>` compartido, migrado a las 8 secciones

Nuevo `components/public/section-heading.tsx` (`eyebrow`, `titulo`, `descricao`, `align`,
`tone`) reemplaza el markup repetido a mano en `page.tsx` (Serviços, Produtos em destaque),
`reviews.tsx`, `location-map.tsx` y las 4 secciones de `servicos/page.tsx`. No es abstracción
prematura (CLAUDE.md §6 pide 3 casos): había 8 casos reales ya escritos, con 7 inconsistencias
concretas (3 mecanismos de centrado, 4 espaciados eyebrow→h2→p distintos, `font-display`
explícito en unos sí y otros no). El componente las mata por construcción.

Se completa la numeración de eyebrows en la home (`01 — Nossos Serviços` ya existía) hasta
`04 — Venha nos visitar` en LocationMap — da recorrido a las 4 secciones de la home. En
`/servicos` no se numeran: la serie 01-04 es de la home, no un contador global.

**Sin palabra acento en naranja en los títulos de Reviews/LocationMap** ("Tutores", "estamos"):
se probó como parte del diseño (mismo mecanismo que usa el Hero) y se descartó a pedido
explícito — quedan en el color normal del heading (blanco sobre navy, ink sobre superficie
clara). El componente no expone una prop `acento` porque ningún h2 de sección la termina usando
— sería una prop sin caso de uso real.

### Carrusel de Reviews: scroll nativo con snap, cero dependencias

Extiende el patrón que ya existía en `booking/date-strip.tsx` (`overflow-x-auto` + `shrink-0` +
scrollbar oculta) agregando `snap-x snap-mandatory`. Sin embla/swiper (CLAUDE.md §6). Anchos de
card `w-[78%] md:w-[44%] lg:w-[31%]` dejan "peek" de la siguiente card en los tres breakpoints,
señal de que hay más contenido además de las flechas (`hidden md:flex`, ocultas por CSS y no
por JS — el repo no tiene hooks de media query). El shell (`<ReviewsCarousel>`) es
`'use client'` mínimo: recibe las `<ReviewCard>` (Server) como `children`, ningún dato de review
se serializa al cliente.

### Atribución del ToS de Google Places

Nombre y foto del autor linkean a su perfil (`authorAttribution.uri`), cada card linkea a la
review completa en Google (`googleMapsUri` de la review), y `<GoogleRatingBadge>` linkea al
listado del negocio con el texto "no Google" explícito. Fotos de autor vía `next/image` con
`remotePatterns: **.googleusercontent.com` en `next.config.ts` (wildcard, no host exacto — el
mismo criterio que ya usaba `**.r2.dev`, porque `lh3`/`lh4`/`lh5` son subdominios válidos de
Google). Si falta `photoUri`, cae al avatar de iniciales que ya existía.

### `TrustBar` y las cards flotantes del Hero pasan a leer datos reales

Con el 5,0 · 78 real en pantalla, dejar "4.9 Avaliação Média" / "500+ Pets Atendidos"
(`trust-bar.tsx`) y "4.9 (327 avaliações)" / "500+ pets atendidos" (`hero-floating-cards.tsx`)
—todos inventados— habría hecho que la misma home se contradijera a sí misma. Ambos componentes
leen ahora de `getPlaceSummary()` (el mismo fetch, dedupeado). "500+ pets atendidos" no tiene
equivalente real en los datos de Google — se reemplazó el copy de esa card por "Banho & Tosa /
com hora marcada", verificable, en vez de inventar un número distinto.

### `GOOGLE_PLACES_API_KEY` server-only; `NEXT_PUBLIC_GOOGLE_MAPS_EMBED_KEY` y
### `NEXT_PUBLIC_GOOGLE_PLACE_ID` públicas por diseño, restringidas por HTTP referrer / IP

La key de Places API nunca sale del server (sin prefijo `NEXT_PUBLIC_`). La key de Maps Embed
viaja en el `src` del iframe — pública sin importar el nombre de la variable. Restringirla por
HTTP referrer en Google Cloud Console es la única protección real. La de Places se restringe por
IP recién en el Sprint Despliegue, cuando exista la IP fija del VPS (hoy sin restricción de
aplicación porque en dev la IP local cambia — anotado en `ROADMAP.md` D.10).

---

## Alternativas consideradas

### ❌ Pedirle testimonios reales al cliente (opción 1 de pending-decisions §15)

Depende de que el cliente junte capturas de su Instagram y las transcriba — más lento, y sigue
siendo contenido que un dev pega a mano sin forma de verificar que no cambió. Las reviews de
Google están verificadas por Google mismo y se actualizan solas.

### ❌ Mantener el array estático como fallback

Es justo lo que causó el problema: un fallo silencioso de la integración volvió a publicar
testimonios falsos en producción sin que nadie lo notara hasta una revisión manual.

### ❌ Route Handler en `app/api/` para exponer las reviews

Innecesario: los 4 consumidores son Server Components, pueden llamar la función directo.

### ❌ Resolver el Place ID con `GOOGLE_PLACE_QUERY` en cada build/request

Cuota y complejidad extra sin beneficio, dado que el ID de un negocio no cambia.

### ❌ Instalar embla/swiper para el carrusel

Se resuelve con `overflow-x-auto` + scroll-snap nativos — el repo ya tenía el patrón base en
`date-strip.tsx`. CLAUDE.md §6: no instalar sin preguntarse si vanilla alcanza.

### ❌ Acento naranja en "Tutores"/"estamos"

Consistente con el resto del sistema (mismo mecanismo que el Hero), pero descartado a pedido
explícito tras ver el preview visual — los títulos quedan en su color normal.

---

## Consecuencias

### Positivas
- Cierra `pending-decisions.md` §15: ya no queda contenido inventado en Reviews ni en TrustBar.
- Reviews, rating y "avaliações" se mantienen al día solos (revalidate 24h).
- Un solo fetch para 4 secciones — agregar un quinto consumidor no suma requests.
- `<SectionHeading>` deja las 8 secciones públicas con un único vocabulario verificable por test.

### Negativas
- Nueva dependencia externa (Google Places + Maps Embed): dos keys más para gestionar/rotar, y
  el sitio queda expuesto a cuota/downtime de Google en 4 secciones en vez de 1.
- Sin fallback de contenido: si Google Business no tiene reviews todavía, `<Reviews>` se ve
  "pelada" (header + badge, sin cards) en vez de mostrar algo de relleno — aceptado a propósito,
  ver "Decisión" arriba.
- `GOOGLE_PLACES_API_KEY` sigue sin restricción de IP hasta que exista el VPS de producción.
