# Pipeline Stitch → Implementación

**Fecha**: 2026-07-27
**Estado**: en validación — piloto: Detalhe do Produto (tarea 3.9)

Este documento es el pipeline reusable para portar una pantalla de Stitch a un componente
real del repo. Nace después de portar la Landing (Sprint 2: Hero, TrustBar, ServiceCard,
ProductCard, Reviews, Footer, BottomNav, FloatingWA) a mano — con el sistema de diseño ya
resuelto (tokens, radios, íconos, reglas de "no inventar datos"), portar la próxima vista
ya no es un trabajo de diseño, es mecánico. Este pipeline lo explota.

**No reemplaza** `docs/port-landing-stitch.md` (el worksheet específico de la Landing, que
sigue siendo la referencia de las decisiones ya cerradas) ni los ADRs. Los referencia.

---

## 1. Etapas

**Etapa 0 — Chequeo de scope** (antes de tocar Stitch)
Confirmar que la vista es pública, no depende de booking backend ni admin, y no requiere
lógica que todavía no existe (carrito real, disponibilidad real). Si la vista necesita
datos que hoy no tienen backend, se construye con datos estáticos explícitamente marcados
como provisorios (mismo criterio que `lib/data/products.ts`). Esto existe para no violar
el orden del ROADMAP (CLAUDE.md §9: "un sprint a la vez") ni el ADR 017 (admin = vertical,
no frontend-first).

**Etapa 1 — Auditoría liviana** (ya no hay que re-derivar el sistema completo, solo
diffear contra lo ya resuelto)
- Leer la pantalla por MCP (`mcp__stitch__get_screen`) + su `code.html`.
- Extraer: copy PT-BR, colores hex usados, clases `rounded-*`, tamaños `text-[Npx]`,
  íconos Material Symbols, cualquier dato que huela a placeholder inventado (direcciones,
  precios, nombres de servicio que no estén en el catálogo real).
- Diffear contra lo ya cerrado:
  - Colores → deben resolver a los 5 tokens (navy/orange/wa/slate/star) + superficies.
    Hex nuevo = se marca, no se inventa token.
  - Radios → tabla del ADR 014 (sección 2).
  - Tipografía → escala existente; tamaño fuera de escala = se marca, no se agrega paso
    nuevo sin decisión.
  - Íconos → tabla de `docs/port-landing-stitch.md` §6 (17 mapeados hoy). Ícono nuevo →
    buscarlo en `lucide-react` y **extender esa tabla**, no duplicar el criterio en otro lado.
  - Contenido inventado → mismo criterio que Footer/Hero/ServiceCard: sin foto real,
    placeholder de ícono (`PawPrint`), no stock hotlinkeado; sin dato de negocio no
    confirmado (direcciones, teléfonos de mock).

**Etapa 2 — Forma de los datos**
Si la vista tiene DTO real en `backend/.../dto/` (ya pasó con `ProductSummary` /
`ProductDetail`), el tipo estático en `lib/data/` copia esos nombres de campo. Si no hay
backend todavía, tipo provisorio con comentario explícito de qué DTO real lo va a
reemplazar y en qué sprint.

**Etapa 3 — Implementación**
Componente(s) en `components/public/`, Server Component salvo interactividad real (con
comentario del motivo), datos en `lib/data/`, reuso de lo que ya existe (`WhatsAppIcon`,
`InstagramIcon`, `buildWhatsAppLink`, `formatPrice`, tokens). Si la ruta/page todavía no
existe, se construye el componente standalone (como `<ProductCard>` antes de
`<FeaturedProducts>`) y se deja documentado dónde se enchufa, o se crea la page si es
parte natural de portar esa vista.

**Etapa 4 — Verificación** (checklist en sección 3).

**Etapa 5 — Reporte estructurado**: qué se construyó, qué se desvió de Stitch y por qué,
qué tokens/íconos nuevos aparecieron (se revisan antes de darlos por definitivos — no se
agregan solos al sistema), screenshots, estado de build/lint/test, preguntas abiertas.

> **Nota de escala**: con un piloto no hace falta, pero cuando corran varias vistas en
> paralelo, las que puedan tocar archivos compartidos (la tabla de íconos, `globals.css`
> si aparece un token nuevo) conviene correrlas en `isolation: "worktree"` o en serie — si
> no, dos agentes pisándose el mismo archivo es un lío de merge innecesario.

---

## 2. Prompt-template del agente frontend

```
Estás portando una pantalla de Stitch a un componente de Next.js en el repo FrontPet
(monorepo, frontend/ es un Next 16 + Tailwind v4). Es trabajo de ejecución: escribí
código, no me devuelvas solo un análisis.

CONTEXTO QUE YA ESTÁ RESUELTO — no lo rederives:
- Tokens de diseño: frontend/app/globals.css, bloque @theme. Colores: navy/navy-dark/
  navy-light/navy-mid, orange/orange-dark/orange-light, wa/wa-dark, slate, surface/
  surface-card, ink/ink-muted, outline, hover, star (5º color, semántico, rating). No hay
  tailwind.config.ts (ADR 014) — no lo recrees.
- Radios (ADR 014) — traducí SIEMPRE así, las clases de Stitch no significan lo mismo acá:
  rounded-lg(Stitch)→rounded-md · rounded-xl→rounded-lg · rounded-2xl→rounded-lg ·
  rounded-3xl→rounded-xl · rounded(default)→rounded-sm · rounded-full→rounded-full.
- Escala tipográfica: eyebrow/caption/label/sm/base/h3/h2-mobile/h2/display-mobile/
  display/hero-mobile/hero-tablet/hero. Tamaño Stitch fuera de esta escala: NO agregues
  un paso nuevo, mapealo al más cercano y avisá en el reporte.
- Íconos: lucide-react. Tabla de mapeo ya resuelta en docs/port-landing-stitch.md §6
  (17 íconos). Si aparece uno nuevo: buscá el equivalente 1:1 en lucide-react y
  **agregalo a esa tabla** (no lo resuelvas solo en el componente).
- Sin foto real todavía para [PRODUCTO/SERVICIO/LO QUE APLIQUE]: usá el mismo patrón que
  components/public/product-card.tsx y service-card.tsx — ícono PawPrint centrado sobre
  bg-surface, nunca un stock hotlinkeado. Mismo criterio para cualquier dato de negocio
  que Stitch muestre como placeholder (direcciones tipo "Rua Exemplo", teléfonos de
  mockup, servicios inventados): no se porta, se omite o se pide dato real.
- Copy siempre en PT-BR. Server Component por defecto — 'use client' solo con
  comentario de una línea explicando la razón puntual.
- WhatsApp: buildWhatsAppLink de @/lib/data/site (no hardcodees el número).
  Precios: formatPrice de @/lib/utils.

TU TAREA:
1. Pantalla de Stitch: projects/3403942466915386698/screens/{{SCREEN_ID}} ("{{SCREEN_TITLE}}").
   Leela con mcp__stitch__get_screen y su code.html/screenshot.
2. Auditá contra lo de arriba: colores fuera de los 5 tokens, radios, tamaños de texto
   fuera de escala, íconos sin mapear, contenido placeholder/inventado. Estos son los
   ÚNICOS hallazgos que reportás como "decisión abierta" — el resto (tokens, radios,
   íconos ya mapeados) lo aplicás directo, sin preguntar.
3. Si la vista necesita datos: revisá si existe DTO real en
   backend/src/main/java/com/frontpet/**/dto/. Si existe, el tipo en lib/data/ usa esos
   mismos nombres de campo. Si no existe todavía, tipo provisorio con comentario
   explícito de qué lo va a reemplazar y en qué sprint (ver ROADMAP.md).
4. Implementá: componente(s) en frontend/components/public/, datos (si aplica) en
   frontend/lib/data/. Reusá WhatsAppIcon, InstagramIcon, buildWhatsAppLink, formatPrice
   — no dupliques. Si la ruta/página todavía no existe y no es tu tarea crearla, dejá el
   componente standalone y documentá dónde se enchufa.
5. Verificá (checklist completo en docs/stitch-implementation-workflow.md §3) antes de
   reportar.
6. NO commitees — dejá los cambios en el working tree, el commit lo hace el desarrollador.
   NO toques Nav, Footer, ni ningún componente ya existente salvo que la tarea lo pida
   explícitamente. NO instales dependencias nuevas sin marcarlo como pregunta abierta.

REPORTÁ AL FINAL:
- Archivos tocados/creados
- Desvíos de Stitch y por qué (tokens/radios/tipografía/íconos aplicados según las
  tablas — no hace falta detallar esos; sí los que NO tenían resolución previa)
- Íconos o tokens nuevos que encontraste (para que se agreguen a las tablas canónicas
  antes de darlos por definitivos)
- Resultado de build/lint/test
- Screenshots tomados (rutas) a 320/768/1024px, y 1440 si la vista tiene layout desktop
  propio
- Preguntas abiertas / decisiones que no pudiste tomar solo
```

---

## 3. Checklist de validación

- [ ] `pnpm build`, `pnpm lint`, `pnpm test` en verde
- [ ] Screenshot real (dev server + navegador headless) a 320 / 768 / 1024px — 1440 si la
      vista tiene tratamiento desktop propio
- [ ] Sin scroll horizontal a ninguno de esos anchos (`scrollWidth === clientWidth`)
- [ ] Sin errores de consola (`console --errors` o equivalente)
- [ ] Cero hex hardcodeado — grep `#[0-9a-fA-F]{3,8}` en los archivos tocados, solo debe
      aparecer en comentarios si acaso
- [ ] Cero clase `rounded-*` copiada literal de Stitch sin pasar por la tabla del ADR 014
- [ ] Cero `material-symbols` — todo ícono es import nombrado de `lucide-react`
- [ ] Copy en PT-BR
- [ ] Sin foto/dato inventado — placeholder de ícono donde no hay asset real, sin
      direcciones/teléfonos/servicios de mockup
- [ ] Si hay CTA de WhatsApp: usa `buildWhatsAppLink`, no un número hardcodeado
- [ ] Si colisiona con `BottomNav`/`FloatingWA` (fixed) a 320px: verificado sin overlap
- [ ] a11y básica: `alt` en imágenes reales, `aria-label` en links/botones solo-ícono,
      contraste AA en texto sobre navy si aplica

---

## 4. Historial de pilotos

| Vista | Rama | Resultado |
|---|---|---|
| Detalhe do Produto (tarea 3.9) | `feat/product-catalog` | en curso |

### Piloto 1 — Detalhe do Produto (2026-07-27)

Auditoría real (HTML crudo descargado y leído directo, no vía WebFetch — el paso por
markdown de WebFetch descarta atributos `class` y `<script>`, no sirve para auditar
literal). La pantalla de Stitch resultó más rica que lo que pide la tarea 3.9 ("una
imagen y descripción"). Decisiones tomadas con Sebastián:

- **Galería multi-imagen + tabs (Tabela Nutricional/Instruções)**: se cortan. CLAUDE.md
  §7 ya excluye galería; el DTO real (`ProductDetail`) solo tiene `descricao`, no hay
  campos para tabla nutricional ni instrucciones.
- **Rating/"128 avaliações"**: se omite, mismo criterio que `<ProductCard>` (no hay
  tabla de reviews en MVP1).
- **Precio con descuento** (`priceOriginal`, tachado, badge "Oferta"): se implementa de
  verdad — el DTO real lo tiene (`isOnSale()`).
- **"Frete grátis para Livramento e Rivera"**: se mantiene (decisión explícita de
  Sebastián, 2026-07-27). Distinto de "Rua Exemplo, 123" — Livramento/Rivera es el área
  real de operación del cliente (CLAUDE.md), no un placeholder inventado por Stitch.
- **"Entrega em até 24h"**: se saca — promesa operativa sin backing real, distinto del
  caso anterior.
- **Stock**: genérico ("Em estoque" / "Fora de estoque" según `stock > 0`), no un
  contador exacto — no hay control de stock real en MVP1, es una señal simple.
- **Selector de cantidad**: se saca de este piloto. Queda para el Sprint 4, donde sirve
  de verdad (carrinho real).
- **CTA**: solo WhatsApp directo, mismo criterio que `<ProductCard>` (issue #7) —
  "Adicionar ao carrinho" espera al Sprint 4.
- **Colores fuera de paleta** (`#F97316`, `#EA580C`, bg `#FFEDD5` del badge de oferta):
  mismo caso que el "brand-soft" que quedó pendiente en `docs/port-landing-stitch.md`.
  Se mapean a `--color-orange`/`--color-orange-dark`, no se inventa token nuevo.
- **"Produtos Relacionados"**: no hay endpoint de relacionados. Se aproxima reusando
  `<ProductCard>` con productos *reales* del seed-dev (`bifinho-de-frango` → coleira +
  mordedor, misma espécie "cães") — no es la lógica final, pero tampoco es dato
  inventado.
- **Dato de ejemplo**: se usó `bifinho-de-frango` del seed real
  (`backend/.../db/seed-dev/products.sql`, tarea 3.12), no un producto inventado —
  permite probar el camino de descuento con números reales.

**Hallazgo colateral**: los 4 productos estáticos de `lib/data/products.ts` (Landing,
Sprint 2) no coinciden con ninguno del seed-dev real — nombres y slugs distintos. No se
corrige en este piloto (es la Landing ya shippeada), pero el swap a API real en Sprint 3
va a cambiar qué productos aparecen destacados en la home, no es un problema del pipeline
en sí.

**Ícono nuevo agregado a la tabla canónica**: `chevron_right` → `ChevronRight` (breadcrumb).
`star_half`/`remove`/`add`/`chat` de esta pantalla no se mapearon: no aplican (rating,
cantidad y CTA de WhatsApp ya resueltos distinto).
