# Port de la landing desde Stitch — resultado de la tarea 2.0b

**Fecha**: 2026-07-17
**Pantalla**: `FrontPet - Landing Page com Rodapé Sincronizado`
`projects/3403942466915386698/screens/5ce9a4555404479fb7e807816cda053c` — MOBILE, 780×10100
**Leída por**: MCP de Stitch (no desde `docs/ui/`, que está stale)

Este documento es el **worksheet de port**, no una decisión de arquitectura. Las decisiones
que surgieron y necesitan cierre están en la última sección.

---

## 1. Radios — la landing es la excepción del ADR 014

**El ADR 014 tiene un punto ciego justo en la pantalla que portamos primero.**

El ADR dice: *"solo la landing define un `borderRadius` propio; las otras 17 no definen
ninguno y heredan los defaults del CDN de Tailwind v3"*. Correcto. Pero después construye
la **tabla de traducción sobre los defaults del CDN** — y esa tabla **no aplica a la
landing**, que es la única con config propia.

| Clase | Default CDN v3 (tabla del ADR) | **Config propia de la landing** | ¿Coinciden? |
|---|---|---|---|
| `rounded` | 4px | 4px | ✅ |
| `rounded-lg` | 8px | 8px | ✅ |
| `rounded-xl` | 12px | 12px | ✅ |
| `rounded-2xl` | **16px** | **20px** | ❌ |
| `rounded-3xl` | 24px | 24px | ✅ |

La landing usa `rounded-2xl` **10 veces, y son todos los cards** (serviços, produtos,
reviews). O sea: **los cards de la landing están a 20px, no a 12px** como concluyó el ADR
mirando las otras 17 pantallas.

### Esto reabre la pregunta de los cards, y la mejora

El ADR la planteó como *12px (Stitch) vs 16px (docs)*. Con la landing a la vista, es otra cosa:

| | Botones | Cards |
|---|---|---|
| Repo (`@theme`) | 8px | 16px |
| **Landing de Stitch** (config deliberado) | 8px *y* 12px ⚠️ | **20px** |
| Otras 17 pantallas (default heredado) | 8px | 12px |

**La landing está escrita sobre una escala 4/8/12/20/24** — uniformemente ~4px más redonda
que la del repo. No es un error de Stitch: es la única pantalla donde alguien eligió los
valores a mano.

### Botones: la landing se contradice a sí misma

El ADR afirma *"los valores de botón nunca estuvieron en conflicto: 8px en las cuatro
fuentes"*. **En la landing no es así**:

- Los **CTAs del hero** usan `rounded-lg` = **8px** ✅ (2 instancias)
- Los **demás botones** usan `rounded-xl` = **12px** ❌ (7 instancias: "Agendar" de los
  service cards, "Adicionar" de los product cards)

Como los botones más prominentes —los del hero— ya están a 8px, **la resolución del ADR
(botones = 8px) se sostiene**. Pero al portar hay **7 botones que bajan de 12 a 8**, no cero.

### Tabla de traducción para ESTA pantalla

| En el HTML de Stitch | Valor real acá | Clase del repo | Instancias |
|---|---|---|---|
| `rounded-lg` (CTAs del hero) | 8px | `rounded-md` | 2 |
| `rounded-xl` (botones) | 12px | `rounded-md` (8px) | 7 |
| `rounded-xl` (thumbs, chips, cards flotantes) | 12px | `rounded-lg` (16px) | 8 |
| `rounded-2xl` (**cards**) | **20px** | `rounded-lg` (16px) | 10 |
| `rounded-full` | pill | `rounded-full` | 15 |

⚠️ **La instrucción del ADR "comparar contra el `screen.png`" no se puede ejecutar.**
Los dos screenshots disponibles son miniaturas: el del MCP viene a 196×512 y el local
(`docs/ui/frontpet_Publico/frontpet_landing_page/screen.png`) a **128×1600**, para una
pantalla de 780×10100. A esa resolución un radio de 12, 16 o 20px ocupa menos de un píxel.
**Para decidir con el ojo hay que renderizar el `code.html` local en un browser.**

---

## 2. Colores — la landing usa 6, no 4

`CLAUDE.md` §5 fija **4 colores base**. La landing introduce dos más:

| Color de Stitch | Hex | Usos | Estado |
|---|---|---|---|
| `brand` | `#F4640D` | 56 | ✅ = `--color-orange` del repo, solo cambia el nombre |
| `navy` | `#011E5A` | — | ✅ = `--color-navy` |
| `wa` | `#25D366` | 2 | ✅ = `--color-wa` |
| **`honey`** | **`#E0A82E`** | **4** | ❌ **fuera de paleta** — son las estrellas de los reviews |
| **`blue`** | **`#2563EB`** | **1** | ❌ **fuera de paleta** — 1 sola instancia, huele a resto de un tema anterior |
| `brand-soft` | `#FFEDD5` | 1 | ⚠️ tinte de naranja sin token en el repo |

**Hex hardcodeados**: 26 instancias, entre ellas `#012B7E` y `#01256E` — dos navys
intermedios que **no existen en el repo** (tenemos `navy-dark #000A2C` y
`navy-light #7388C9`). Son el hover y el fondo de los review cards. Y `#F3F4F6` ×8,
que es el token `hover` de Stitch: **el repo no tiene token de hover**.

**Divergencias de valor con el mismo rol semántico** (silenciosas y peligrosas — se ven
"casi bien"):

| Rol | Stitch | Repo | Δ |
|---|---|---|---|
| Texto principal | `ink` `#1D1D1D` | `--color-ink` `#0B1C30` | negro neutro vs azulado |
| Texto secundario | `muted` `#6B7280` | `--color-ink-muted` `#444650` | Stitch más claro |
| Bordes | `line` `#E5E7EB` | `--color-outline` `#C5C6D1` | Stitch más claro |
| **Fondo del body** | `bg` = **`#FFFFFF`** | `--color-background` = **`#F8F9FF`** | ⚠️ **invertidos** |
| Fondo alterno | `bg-alt` `#F8F9FF` | — | |

⚠️ **La inversión del fondo importa**: la landing alterna secciones blancas con secciones
`#F8F9FF`. El repo arranca el body en `#F8F9FF`. Portar tal cual **invierte el ritmo visual
de la página entera**.

---

## 3. Tipografía — 65 valores arbitrarios

`CLAUDE.md` §5: *"Prohibido usar valores arbitrarios fuera de la escala"*. La landing tiene
**65 clases `text-[Npx]`**, y varias caen **fuera de la escala del repo**:

| Tamaño usado | ¿En la escala del repo? |
|---|---|
| 12, 13, 14, 16, 18, 24 px | ✅ `caption` / `label` / `sm` / `base` / `h3` / `h2-mobile` |
| **10, 15, 20, 28, 36, 52, 64, 88 px** | ❌ **no existen** — 32 instancias |

Los `text-[88px]` / `[64px]` / `[52px]` son el número grande de las estadísticas del hero.
Los `text-[20px]` (16 usos) y `text-[18px]` (14 usos) son los más frecuentes: **20px no está
en la escala** y es el que más aparece.

También: **`h1` de Stitch = 48px** vs `--text-display` = **44px** del repo, y **`h2` = 32px**
vs `--text-h2` = **30px**.

⚠️ **13 usos de `font-bold` (700)**, que `DESIGN.md` prohíbe explícitamente (máx. 600).
Notar que el `<link>` de Google Fonts carga `wght@400;500;600` — o sea, **el `font-bold` de
la landing ni siquiera tiene el peso cargado**: el browser lo está sintetizando. Al portar
con `next/font`, esos 13 pasan a `font-semibold` (600).

---

## 4. Componentes — lo que la landing tiene y el ROADMAP no

Estructura real (en orden): Header → Hero (imagen de fondo + gradiente + eyebrow chip +
headline + subhead + CTAs + 3 social proof cards flotantes desktop) → Trust Indicators →
Serviços (3 cards) → Produtos em destaque (4 cards) → Reviews (3, sobre navy) → Footer.
Más, fijos: Floating Buttons y Bottom Navigation.

| Componente de Stitch | Tarea del ROADMAP | Nota |
|---|---|---|
| Header, Hero, Trust, Services, Products, Reviews, Footer | 2.1-2.8 | ✅ mapean |
| **Bottom Navigation** (`md:hidden fixed bottom-0`) | **ninguna** | ❌ no está en el Sprint 2 |
| **Social proof cards flotantes** (hero, desktop) | **ninguna** | ❌ no estimadas |
| **Cart FAB con badge "3"** | 4.3 `<CartButton>` | ⚠️ es del **Sprint 4**, aparece acá |
| `<AnnouncementBar>` | **2.3** | ❌ **no existe en Stitch** |

⚠️ **Colisión física**: el Bottom Navigation es `fixed bottom-0` y los Floating Buttons
están a `bottom-[180px]`, con el `z-[60]`. En mobile hay tres capas peleando por la esquina
inferior derecha (bottom nav + FAB del carrito + FloatingWA). **Verificar a 320px** antes de
darlo por bueno.

**Assets**: 10 imágenes y **53 iconos de Material Symbols** cargados por CDN. El repo usa
`lucide-react` (`CLAUDE.md` §2). **Los 53 iconos hay que mapearlos a Lucide** — no está
estimado en ninguna tarea del Sprint 2.

---

## 5. Decisiones que necesitan cierre antes de codear

1. **`honey` `#E0A82E` (estrellas de reviews)**: ¿se suma como 5º token o las estrellas van
   en `orange`? Recomiendo **sumar el token**: naranja sobre naranja de precio confunde, y
   las estrellas van a reaparecer en el detalle de producto.
2. **`blue` `#2563EB`**: 1 sola instancia. Recomiendo **eliminarlo** y mapear a `navy`.
3. **Token de hover** (`#F3F4F6`, 8 usos): el repo no tiene ninguno. Hay que agregarlo.
4. **Navys intermedios** `#01256E` / `#012B7E`: agregar tokens o mapear a `navy` +
   `navy-light`.
5. **Fondo del body**: ¿el repo se alinea a Stitch (body blanco, alterno `#F8F9FF`) o la
   landing se adapta al repo? Recomiendo **alinear el repo a Stitch** — la alternancia es
   una decisión de diseño de la pantalla, no un accidente.
6. **Cards a 16 o 20px**: solo se resuelve renderizando el `code.html`. Ver §1.
7. **20px de tipografía**: 16 usos. O se suma a la escala o se reparten entre 18 y 24.
8. **Bottom Navigation**: no está en el ROADMAP. Si entra, es una tarea nueva en el Sprint 2.

---

## 6. Conclusión operativa

**El copy-paste no sirve para nada acá**, y no solo por los radios: entre 65 valores
arbitrarios, 26 hex hardcodeados, 13 pesos prohibidos, 53 iconos de otra librería y 2
colores fuera de paleta, **la landing de Stitch es una referencia visual, no un origen de
código**. Lo que se porta es el layout y la jerarquía; las clases se reescriben todas.

Esto **no cambia la estimación del Sprint 2** (21 hs) — es lo que ya asumía la tarea 2.0b.
Pero sí agrega dos ítems no estimados: **el mapeo de 53 iconos a Lucide** y el
**Bottom Navigation**.
