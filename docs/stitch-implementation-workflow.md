# Pipeline Stitch → Implementación

**Fecha**: 2026-07-27 (última actualización: 2026-08-03, Piloto 8)
**Estado**: validado — 8 pilotos portados, ver §4 Historial de pilotos

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

**Pantallas duplicadas por estado**: en el listado de `mcp__stitch__list_screens`, más de
una entrada puede ser la misma vista en un estado de UI distinto — un formulario abierto
sobre la vista base, una lista de productos cargando vs. ya cargada, un estado vacío o de
error. No son vistas separadas a portar una por una: es **un solo componente con variantes
de estado** (condicionales/props), no N componentes. Se detecta comparando el `code.html`
de las candidatas — layout y estructura casi idénticos, difiere solo lo que está visible
(overlay de modal, skeleton, mensaje de vacío). Antes de arrancar la Etapa 1, agrupar estas
variantes, identificar cuál es el estado base, y portar contra esa — las demás quedan como
nota de qué prop/condición dispara ese estado, no como port aparte.

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

## 2. Ejecución: comando `/port-screen`

El prompt de ejecución vive en **`.claude/commands/port-screen.md`** (movido acá el
2026-07-27 — antes era un template copy-paste en esta sección). Se invoca:

```
/port-screen <SCREEN_ID> "<título de la pantalla>"
```

El comando referencia las fuentes canónicas (tokens en `globals.css`, radios en ADR 014,
íconos en `port-landing-stitch.md` §6, este workflow para etapas y checklist) en vez de
duplicarlas — si una regla cambia, se cambia en su fuente y el comando la levanta sola.
Las reglas duras del port (no inventar tokens/datos, no commitear, no tocar componentes
existentes, no instalar dependencias) están en el comando mismo.

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
| Sua Sacola (carrinho + checkout, tarea 4.4/4.6) | `feat/cart-checkout` | portado — fusionado en una sola vista `/carrinho`, ver Piloto 2 |
| Login Administrativo (tarea 4.11) | `feat/admin-shell` | portado — única pantalla DESKTOP, ver Piloto 3 |
| Gestão de Produtos (tarea 4.12) | `feat/admin-products` | portado — solo el contenido, sidebar ya existía (Bloque D), ver Piloto 4 |
| Gestão de Pedidos (tarea 4.14) | `feat/admin-orders` | portado — sidebar ya existía, ver Piloto 4 |
| Serviços (Imagens Sincronizadas) (issue #59, Bloque B Sprint 6) | `feat/servicos-page` | portado — datos reales via GET /services, 3 desvios resueltos (WhatsApp CTA→/agendamento por ADR 008, FAQ reducido a 3/5 confirmables, info strip a 2 items), validación visual pendiente (sin extensión de Chrome disponible en la sesión) |
| Agendamento Passos 1-2 (tareas 6.1-6.3) | `feat/agendamento-wizard` | portado — ver Piloto 5 |
| Agendamento Passo 3 + Confirmação (issue #61, Bloque D Sprint 6) | `feat/agendamento-wizard` | portado — ver Piloto 6 |
| Gestão de Agendamentos, Vista Semanal (issue #62, Bloque E Sprint 6) | `feat/admin-agendamentos` | portado — ver Piloto 7 |
| Gestão de Serviços, Admin Interativo (issue #63, Bloque F Sprint 6) | `feat/admin-services` | portado — ver Piloto 8 |

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

### Piloto 2 — Sua Sacola / carrinho + checkout (Sprint 4, Bloque B, 2026-07-28)

HTML crudo descargado y leído directo (misma disciplina del Piloto 1). Decisiones:

- **Carrinho y checkout se portaron como una sola vista `/carrinho`**, no dos rutas — el
  mock ya dibuja lista + resumen + form + CTA en una sola pantalla ("Sua Sacola"), separarla
  hubiera sido inventar una estructura que el diseño no tiene. No existe `/checkout`.
- **"Sua Sacola Vazia" no es un piloto aparte**: es la variante de estado vacío del mismo
  componente (Etapa 0 — agrupar variantes de una pantalla antes de portar).
- **Form real: 6 campos, no 3**. Stitch dibuja nome/telefone/modalidade; se agregan
  endereço, forma de pagamento y horário porque `V4__orders.sql` los exige `NOT NULL` y
  `docs/preguntas-cliente.md` §4.2 los pidió explícitamente — ver ADR 003 (act. 2026-07-28)
  para el detalle completo de esta divergencia y de cómo se resuelve "Retirada" (modalidad
  sin columna propia en la migración).
- **Sección "Você também pode gostar"**: se corta. Requeriría un endpoint de recomendados
  que no existe — mismo criterio que "Produtos Relacionados" del Piloto 1.
- **Toggle Grátis/R$0 de frete del mock**: no se porta tal cual. El sistema no calcula
  distancia (ADR 003); el frete nace siempre `GRATIS` y el admin lo ajusta manualmente si
  hace falta — mostrar un toggle interactivo acá sería fingir una lógica que no existe.

Detalle completo (incluye dos bugs reales encontrados con Playwright): issue #30.

### Piloto 3 — Login Administrativo (Sprint 4, Bloque D, 2026-07-28)

Única pantalla DESKTOP del proyecto (las otras 18 son MOBILE 780px) — validada también a
1440px, no solo a los tres breakpoints habituales. Decisiones:

- **"Manter conectado" y "Esqueceu a senha" se cortan**: ninguno tiene backing real (sesión
  de duración fija; reset de contraseña es la tarea 7.12, no existe todavía). Un control
  inerte es peor que omitirlo.
- Estructura de rutas: el guard vive en `app/admin/(protected)/layout.tsx`, no en un
  `app/(admin)/layout.tsx` a secas — ese layout envolvería también `login/page.tsx` y
  entraría en loop contra su propio redirect. Detalle completo: issue #32.

### Piloto 4 — Gestão de Produtos y Gestão de Pedidos (Sprint 4, Bloques E/F, 2026-07-28/29)

Ambas pantallas traen su propio sidebar/topbar en el mock de Stitch — se ignoró: el shell
real ya existe desde el Bloque D (`<AdminShell>`), solo se portó el contenido de cada una.

- **Gestão de Produtos**: SKU, Tags y "Destaque na home" del modal "Cadastrar Novo Produto"
  se cortan — ningún DTO real los tiene, agregarlos sería inventar campos de backend.
  Categoria/Espécie pasan de `<select>` de opción única a checkboxes (el modelo real es
  N:M). Detalle completo, incluye un bug real de estado stale en el form de edición: issue
  #33.
- **Gestão de Pedidos**: se corta el botón de WhatsApp genérico junto al nombre del
  cliente (el mock tiene dos entradas a WhatsApp con alcance ambiguo; el AC solo pide una,
  la de confirmação). Los thumbnails de producto en los items del pedido tampoco se portan
  — `OrderItemDetail` no tiene campo de imagen. Detalle completo, incluye un bug real de
  overlay en desktop: issue #34.

### Piloto 5 — Agendamento, Passos 1-2 (Sprint 6, Bloque C, 2026-08-01)

Las 4 pantallas del wizard ("Passo 1 Dinâmico", "Passo 2 Sincronizado", "Passo 3", 
"Confirmação") son **un solo componente con variantes de estado**, no 4 ports: el mock las
dibuja como `<div id="step-N">` con `display:none`, dentro de la misma card. Se portó como
`<BookingWizard>` (client, dueño del estado) + un componente por paso.

- **La tabla de precios del mock se descarta entera.** El `<script>` de Stitch hardcodea
  `prices = { p: { essencial: 45, … } }`; el precio y la duración reales salen de
  `ServicePricingDetail` por porte (`GET /services`), que trae los 4 portes de los 8
  serviços del seed. El mock también inventa un "Banho Premium 79/75min" que no coincide con
  el real (75/75min).
- **Trust strip del mock** ("Profissionais Certificados", "Busca & Entrega Grátis"): se
  corta. Son promesas operativas sin backing — mismo criterio que "Entrega em até 24h" del
  Piloto 1. "Busca & Entrega" además no es un servicio que FrontPet ofrezca hoy.
- **Header/footer/bottom-nav propios del mock**: ignorados, ya existen en `(public)/layout.tsx`.
- **Cuarto estado vacío, que ni Stitch ni el AC contemplan**: `AvailabilityServiceImpl`
  devuelve `indisponibilidade: null` con `slots: []` cuando el día está abierto y sin
  bloquear pero ningún inicio entra (el combo no termina antes del cierre, o ya pasaron
  todos los horarios de hoy). No es teórico: **hoy mismo, el primer chip de la tira cae en
  ese caso**. Sin copy propio el usuario ve una grilla vacía muda en la interacción más
  probable. Se le dio mensaje propio (`SEM_ENCAIXE_COPY` en `slot-grid.tsx`).
- **Íconos nuevos que NO salen de Stitch** (`CalendarX2` en el estado vacío, `RefreshCw` en
  el de error): son estados que el mock no dibuja, así que no van a la tabla de mapeo
  Material Symbols → lucide. `check` → `Check` sí se agregó a esa tabla (lo usa el stepper).

### Piloto 6 — Agendamento, Passo 3 + Confirmação (Sprint 6, Bloque D, 2026-08-01)

Continuación del Piloto 5: "Passo 3 (Fluxo Interno)" y "Confirmação (Fluxo Interno)" son
variantes de estado del mismo `<BookingWizard>`, no ports aparte — mismo criterio de Etapa 0.

- **Submit interno directo, sin WhatsApp** (D2 del plan de Sprint 6, ya resuelto en Stitch
  antes de codear): `<BookingForm>` es RHF + `zodResolver`, mismo patrón de
  `checkout-form.tsx` — el botón "Confirmar agendamento" llama `POST /appointments`, ningún
  `wa.me` en el flujo (ADR 008).
- **`409` (cupo agotado) vuelve al Passo 2**, no deja al usuario en un form muerto: otro
  cliente puede haber reservado el mismo horário mientras este completaba el form — el
  wizard recarga la grilla y vuelve un paso (AC de la issue #61).
- **`429`** muestra el tiempo de espera (`Retry-After` del rate limit, 5.8) en vez de un
  error genérico.
- **Código de reserva legible**: `bookingCode()` toma los **últimos 6** caracteres del
  `publicId` (UUID v7) en mayúscula — no un contador secuencial nuevo, no hay campo para eso
  en el DTO real.
- **Sin botón de soporte por WhatsApp en la confirmação**: verificado contra el diseño real
  de Stitch (nota de D2 del plan) — Sebastián confirmó portar tal cual, sin agregar UI que
  el mock no dibuja.
- `GET /appointments/{publicId}` es público y puede devolver `404` real (`publicId`
  inexistente) — a diferencia de `createAppointment`, que nunca da 404. La page es Server
  Component por eso (`notFound()` de Next).

### Piloto 7 — Gestão de Agendamentos, Vista Semanal (Sprint 6, Bloque E, 2026-08-01)

Port de `<AppointmentList>` + `<AppointmentDetailPanel>` + `<ManualAppointmentDialog>`
(issue #62). Mismo esqueleto que `<OrderList>` (Piloto 4): TanStack Query, split 60/40
desktop, `<Sheet>` en mobile — pero agrupado por día en vez de lista plana, siguiendo el mock.

- **Un solo fetch de la ventana de 7 días sin filtro de status**: el filtro de las tabs
  (Pendentes/Confirmados/Cancelados) y el toggle "Hoje/Próximos 7 dias" se aplican
  client-side sobre los mismos datos — evita 4 round-trips por cambio de tab.
- **Sin el estado "FECHADO" que el mock dibuja para domingo**: al portar no existía un
  endpoint que expusiera `business_hours` al admin — gap real, cerrado recién en el Bloque F
  (`GET /admin/business-hours`). Queda como deuda de UI menor, no bloqueante: el admin igual
  ve los agendamentos reales de cada día, solo no distingue visualmente "domingo cerrado".
- **"+ Novo agendamento" (`<ManualAppointmentDialog>`)**: no valida contra la grilla de
  slots ni llama a `/availability` antes del submit — el admin puede tipear cualquier
  horário (ADR 021); el backend resuelve preço/duração server-side y devuelve `avisos` si
  supera capacidad/grilla/día bloqueado, sin bloquear la creación.
- **Inputs nativos `type="date"`/`type="time"`** en el form manual, sin picker propio —
  deuda anotada en `pending-decisions.md` §13, no bloqueante.
- Botão verde de WhatsApp usa los templates de `lib/whatsapp/templates.ts` (ADR 010),
  ya versionados por status — no texto hardcodeado en el componente.

### Piloto 8 — Gestão de Serviços, Admin Interativo (Sprint 6, Bloque F, 2026-08-03)

HTML crudo descargado y leído directo (misma disciplina de los pilotos anteriores). Port de
`<ServicesManager>` con dos tabs, `<ServiceEditDialog>`, `<BusinessHoursForm>` y
`<ScheduleBlocksPanel>` (issue #63).

- **Contadores "32 esta semana" por serviço**: se cortan enteros. Son métrica analítica,
  prohibida en el admin de MVP1 (ADR 003/008, decisión D5 del plan de Sprint 6).
- **Nome SÍ editable**, al revés de lo que dibuja el mock (`disabled`, con nota "fale com o
  suporte técnico" para cambiarlo): el DTO real (`UpdateServiceRequest`) acepta `nome` y el
  AC de la issue #63 pide explícitamente poder editarlo. Se sigue el DTO/AC, no el mock.
- **"Categoria" y "O que inclui" del modal**: se cortan. Ningún DTO real tiene esos campos
  — agregarlos sería inventar estructura de backend (mismo criterio del Piloto 4 con
  SKU/Tags en Gestão de Produtos).
- **Grilla de 4 portes reemplaza los inputs únicos "Duração"/"Preço Base" del mock**: el
  modelo real tiene preço y duração por porte P/M/G/GG (ADR 011), no un valor único por
  serviço — la estructura del form tiene que reflejar eso, no lo que Stitch dibuja para un
  modelo de precio simple.
- **Una sola pausa opcional por día** ("Intervalo de almoço"), no el "+ Adicionar intervalo"
  (N ilimitado) que dibuja Stitch (decisión D6 del plan) — el schema de `business_hours`
  solo soporta `pausaInicio`/`pausaFin`, un cambio a N intervalos exigiría migración +
  rehacer el cálculo de slots del ADR 020, fuera de escopo.
- **Sin foto real en los cards**: `ServiceOfferingDetail` no tiene campo de imagen —
  patrón `Scissors` sobre `bg-surface`, mismo criterio que `product-card`/`service-card`
  sin foto.
- **Toggle activo/inactivo en el card** llama `PUT` directo (reenviando el pricing actual
  sin cambios), sin abrir el modal — atajo que el propio mock dibuja.
- **Hueco de backend encontrado y cerrado en este bloque**: `GET /admin/services`,
  `GET /admin/business-hours` y `GET /admin/schedule-blocks` no existían — el frontend
  (`lib/api/admin-schedule.ts`, escrito en el Bloque 0) ya los llamaba, pero el backend
  solo tenía PUT/POST/DELETE. Agregados repo→service→controller, sin lógica nueva, con
  tests de integración (auth/happy-path/tenant-scoping).
- **Verificación end-to-end vía API** (backend real, sin extensión de Chrome disponible al
  momento del port): cambiar el preço de un porte se reflejó al instante en `GET /services`
  público y en `GET /availability`; crear un bloqueio de día devolvió
  `indisponibilidade: BLOQUEADO` para esa fecha en el wizard. Validación visual a
  320/768/1024px y prueba en celular real diferidas al Bloque G (cierre del sprint).
