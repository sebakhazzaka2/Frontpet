# Port de la landing desde Stitch — resultado de la tarea 2.0b

**Fecha**: 2026-07-17
**Pantalla**: `FrontPet - Landing Page com Rodapé Sincronizado`
`projects/3403942466915386698/screens/5ce9a4555404479fb7e807816cda053c` — MOBILE, 780×10100
**Leída por**: MCP de Stitch (no desde `docs/ui/`, que está stale)

> 🧊 **REGISTRO HISTÓRICO (congelado 2026-07-27).** Worksheet de la tarea 2.0b, cerrada.
> Todas sus decisiones están resueltas y viven en sus fuentes canónicas: la excepción de
> radios en el **ADR 014 (Actualización 2026-07-27)**, los tokens nuevos en `globals.css`.
> **Única sección viva: la tabla de iconos de la §6** — se sigue extendiendo con cada
> pantalla portada (ver `docs/stitch-implementation-workflow.md`) hasta que termine la
> migración; ahí se reevalúa extraerla. El resto no se edita más.

---

## 1. Radios — la landing es la excepción del ADR 014

**Movido al ADR 014, "Actualización 2026-07-27".** La landing es la única pantalla con
`borderRadius` propio (escala 4/8/12/20/24); su tabla de traducción específica, la regla
general ("verificar si la pantalla define config propio antes de aplicar la tabla del CDN")
y el cierre del radio de cards (16px) viven ahora en el ADR.

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

## 5. Decisiones — RESUELTAS (2026-07-17)

Las 8 se cerraron con Sebastián. Los cambios de token ya están en `globals.css`.

1. **`honey` `#E0A82E` (estrellas de reviews)** → ✅ **token nuevo `--color-star`**. 5º color,
   rol semántico (rating), no un acento nuevo. Reaparece en el detalle de producto.
2. **`blue` `#2563EB`** → ✅ **eliminado**. Esa instancia se mapea a `navy`. No entra a `@theme`.
3. **Token de hover** (`#F3F4F6`, 8 usos) → ✅ **token nuevo `--color-hover`**.
4. **Navys intermedios** `#01256E` / `#012B7E` → ✅ **un solo token `--color-navy-mid` `#01256e`**.
   Es el card de review apoyado sobre la sección navy: necesita despegarse del fondo, y ni
   `navy-light` (muy claro) ni `navy-dark` (hunde) servían. El segundo hex era el *hover* de
   esos cards; **se descarta**: los reviews no son clickeables, no necesitan hover.
5. **Fondo del body** → ✅ **se mantiene el repo (`#F8F9FF`)**. Al portar, la alternancia de
   secciones de la landing se **invierte** para arrancar desde el ground del repo. No es
   cambio de token, es regla de port.
6. **Cards** → ✅ **16px** (`--radius-lg`). Cierra el pendiente del ADR 014. La landing los
   traía a 20px; se estandariza. `rounded-2xl` de Stitch → `rounded-lg` del repo.
7. **20px de tipografía** → ✅ **NO se agrega a la escala**. El salto 18→24 diferencia bien
   los títulos. Los 16 usos de `text-[20px]` se reparten entre `text-h3` (18) y `text-h2-mobile`
   (24) según jerarquía, al portar.
8. **Bottom Navigation** → ✅ **entra** (tarea 2.0d). El carrito y el WhatsApp flotantes se
   **integran dentro de la barra** en la vista mobile, en vez de flotar encima — resuelve la
   colisión a 320px. Verificar en dispositivo real igual.

> **Nota de paleta**: con `--color-star` la app pasa a **5 colores**, no 4. `CLAUDE.md` §5
> dice "4 colores base" — el star es semántico (rating), no un acento comercial, pero conviene
> anotarlo ahí para que no se lea como violación de la regla.

---

## 6. Decisiones tomadas al codear el Nav + Hero (2026-07-25)

Para que cualquier tab/sesión que siga con las tareas 2.3+ no tenga que re-derivar esto.

### Mapeo de íconos Material Symbols → lucide-react

Verificado contra el paquete instalado (`lucide-react` 1.16), no adivinado. En la landing
aparecen 17 únicos (55 usos) de 53 totales del proyecto — el resto está en otras pantallas
de Stitch y se agrega acá a medida que el pipeline de `docs/stitch-implementation-workflow.md`
las va portando (no se remapean de cero cada vez).

| Material Symbols | Lucide | Material Symbols | Lucide |
|---|---|---|---|
| `star` | `Star` | `menu` | `Menu` |
| `pets` | `PawPrint` | `calendar_today` | `Calendar` |
| `shopping_bag` | `ShoppingBag` | `local_shipping` | `Truck` |
| `check_circle` | `CheckCircle2` | `public` | `Globe` |
| `timer` | `Timer` | `share` | `Share2` |
| `shopping_cart` | `ShoppingCart` | `location_on` | `MapPin` |
| `arrow_forward` | `ArrowRight` | `call` | `Phone` |
| `home` | `Home` | `help` | `HelpCircle` |
| `content_cut` | `Scissors` | `chevron_right` | `ChevronRight` |
| `schedule` | `Clock` | `expand_more` | `ChevronDown` |
| `check` | `Check` | `person` | `User` |

Agregados al portar `/servicos` (issue #59, Bloque B Sprint 6): `schedule`/`Clock` (info
strip de horário) y `expand_more`/`ChevronDown` (chevron del FAQ accordion, nativo con
`<details>`, sin librería nueva). `check`/`Check` e `person`/`User` agregados al portar
o wizard de agendamento (issue #60/#61, Bloque C/D Sprint 6) — stepper (passo completado)
e linha "Responsável" do resumo de confirmação, respectivamente. `spa` (ícone de serviço no
mock da Confirmação) **não se mapeou** — reusa-se `Scissors` (já mapeado de `content_cut`),
que já representa "serviço" no resto do repo; não faz sentido introduzir um segundo ícone
para o mesmo conceito.

Ninguno necesitó reemplazo aproximado — todos 1:1. WhatsApp es aparte
(`components/shared/whatsapp-icon.tsx`): ícono de marca, no está en ninguna librería de
pictogramas genéricos.

### `--text-hero` — nuevo paso en la escala tipográfica

El headline del Hero necesita 36/52/64px (mobile/tablet/desktop); el tope anterior de la
escala era `--text-display` (44px). Se agregaron `--text-hero-mobile` (36px),
`--text-hero-tablet` (52px) y `--text-hero` (64px) a `globals.css` — 3 pasos, no 2 como el
resto de la escala, porque el AC de la tarea 2.2 pide validar a 320/768/1024/1440 y con
2 tiers el salto a 64px ya en 768px corta o solapa.

### Logo real del cliente

`frontend/public/brand/`: `frontpet-logo.pdf` (fuente) y `frontpet-logo.png` (export del
cliente, cuadrado 3375×3375, mucho padding navy) → `frontpet-logo-horizontal.png`
(recortado con `sharp().trim()`, 2665×541, para uso en Nav/Hero/Footer). El navy de fondo
del PNG es `#011e5a` — coincide exacto con `--color-navy`, por eso el recorte funde sin
bordes visibles contra cualquier superficie navy del repo. **Decisión 2026-07-25**: el
logo va completo con "PETSHOP" incluido, aunque a 64px de header quede chico — no se separa
en una variante solo-wordmark.

### Sin foto real del Hero todavía

Stitch hotlinkea una URL temporal de `googleusercontent.com` para la imagen de fondo del
Hero — no es nuestra, puede expirar, no se hotlinkea en el repo. Placeholder actual:
gradiente navy (`from-navy to-navy-dark`). Falta pedirle al cliente una foto real.

---

## 7. Conclusión operativa

**El copy-paste no sirve para nada acá**, y no solo por los radios: entre 65 valores
arbitrarios, 26 hex hardcodeados, 13 pesos prohibidos, 53 iconos de otra librería y 2
colores fuera de paleta, **la landing de Stitch es una referencia visual, no un origen de
código**. Lo que se porta es el layout y la jerarquía; las clases se reescriben todas.

Esto **no cambia la estimación del Sprint 2** (21 hs) — es lo que ya asumía la tarea 2.0b.
Pero sí agrega dos ítems no estimados: **el mapeo de 53 iconos a Lucide** y el
**Bottom Navigation**.
