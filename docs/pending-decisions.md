# Decisiones pendientes

Cosas que aparecieron durante el desarrollo, que **no están decididas** y que no bloquean
el trabajo actual. No son ADRs: un ADR documenta una decisión *tomada*. Esto es la lista de
lo que todavía no lo está.

Cuando una de estas se resuelve → sale de acá y entra como ADR o como tarea del ROADMAP.

---

## 1. Alta de marcas (`brands`) — RESUELTO (Sprint 4)

**Estado**: implementado, backend y UI. Ya no bloquea nada — queda documentado como
historial de la decisión.

Las marcas no son lista cerrada como las categorías: llega una ração de una marca nueva y
el admin tiene que poder cargarla. Pero un CRUD de marcas con pantalla propia es demasiado
para lo que es.

**Dirección elegida**: alta *inline* en el formulario de producto — el admin escribe la
marca; si no existe, se crea sola. Sin pantalla dedicada. **UI**: campo de texto libre
(`brandNome`) en `product-form-dialog.tsx` — no autocompletar sobre las existentes, se
decidió el camino más simple ya que `findOrCreate` funciona igual con cualquiera de los dos.

**Ya implementado**:
- `BrandService.findOrCreate(tenantId, nome)` — busca sin distinguir mayúsculas y crea si
  no existe. Recorta espacios; nombre vacío es inválido.
- `V9__brands_case_insensitive_unique.sql` — el `UNIQUE(tenant_id, nome)` de V2 era
  case-sensitive (dejaba crear "Golden" y "golden" como marcas distintas). Se reemplazó por
  un índice único funcional sobre `LOWER(nome)`: la garantía vive en Postgres, no en la
  disciplina de la UI.
- `BrandRaceSafeCreator` — el `INSERT` corre en su propia transacción (`REQUIRES_NEW`). Dos
  altas de producto concurrentes con el mismo nombre de marca nueva chocan en el índice
  único; sin esto, esa colisión tumbaba la transacción completa del alta de producto por
  una carrera en un dato secundario.
- 4 tests de integración (`BrandServiceIntegrationTest`): crea si falta, reutiliza
  ignorando mayúsculas, recorta espacios, rechaza nombre en blanco.
- Campo de texto libre `brandNome` en `product-form-dialog.tsx` (Sprint 4).

**Sigue sin resolver, pero no bloquea**: qué pasa con marcas que quedan sin ningún producto
(¿se borran solas? ¿quedan colgando?) — barato de decidir si se vuelve un problema real.

**Dónde impacta**: nada bloqueado hoy.

---

## 2. Borrado masivo de productos

**Estado**: pospuesto a después del MVP1 (decisión de Sebastián, jul/2026).

**El problema real**: con 500 productos de los cuales 200 ya no se venden, el admin se
vuelve inmanejable. Ocultarlos uno por uno no escala.

**Lo que ya está resuelto**: el toggle `active` es el mecanismo de MVP1. La lista del admin
debería mostrar activos por defecto, con los inactivos detrás de un filtro — eso solo ya
saca del medio la mayor parte del ruido sin borrar nada.

**El límite duro que no se puede esquivar**: `order_items.product_id` es `ON DELETE RESTRICT`
(ADR 013 §12). Un producto que **alguna vez fue pedido no se puede borrar** — Postgres lo
rechaza. Cualquier "borrar" que se implemente tiene que contemplar que va a fallar
justamente con los productos que más se vendieron.

**Falta definir**: si el borrado masivo selecciona y desactiva (siempre funciona) o
selecciona y borra (falla parcialmente y hay que reportar qué no se pudo).

**Dónde impacta**: post-MVP1. **No está presupuestado** — Sprint 4 ya son 30 hs y es el más
pesado del plan, contra el hito de cobro del 05/09.

---

## 3. Promociones / precio tachado (`price_original`)

**Estado**: sin sección "Nuestros descuentos" en MVP1 (eso sigue sin decidir — falta ver el
diseño de Stitch), pero el hueco operativo que esto generó **ya se resolvió (2026-07-27)**.

`V6__catalog_promos.sql` agregó `price_original` a `products` y `product_variants`, con un
CHECK que garantiza `price_original > price`. La idea era una sección "Nuestros descuentos"
en la landing — eso sigue sin estar en la lista de MVP1 del `CLAUDE.md` §7.

**El hueco que generó la decisión "provisoria" original**: la premisa de "si nadie la llena,
no existe" se rompió con la tarea 3.12 (seed, issue #22) — su propio AC pedía a propósito un
producto en promoción (`bifinho-de-frango`) para que la búsqueda tuviera algo contra qué
probarse. Pero ni `CreateProductRequest` ni `UpdateProductRequest` tenían el campo
`priceOriginal` — no había NINGUNA forma de sacar ese producto de oferta (ni de poner otro)
vía API, solo tocando la DB a mano. Encontrado en QA manual de Sprint 3.

**Resolución**: se agregó `priceOriginal` (nullable) a ambos DTOs. Mismas reglas que
`price` — solo aplica sin variantes, `ProductServiceImpl` valida `priceOriginal > price`
antes de llegar a Postgres (400 con mensaje claro en vez del 500 genérico que tiraría el
CHECK). En el `PUT`, `null` saca el producto de oferta — mismo criterio de reemplazo
completo que ya tenía el resto del record. Tests en `AdminProductControllerTest`.

**Sigue sin resolver** (esto es lo que queda pendiente de verdad): si el diseño de Stitch
tiene una sección de descuentos dedicada. No se pudo verificar porque `docs/ui/` está
gitignoreado y no está en disco — hay que mirarlo por MCP si esto se retoma.

---

## 4. El seed V5 dice "no ejecutar en producción" pero nada lo impide

**Estado**: detectado, sin resolver.

`V5__seed_dev.sql` arranca con *"NÃO executar em produção"*. Pero `application.yml` tiene
`flyway.locations: classpath:db/migration` a secas, así que **Flyway la va a correr en
producción igual**. El comentario es un deseo, no un mecanismo.

Hoy no hace daño: los datos de V5 son reales (el tenant FrontPet, los precios de la tabla
oficial del cliente, los horarios confirmados), no inventados. Lo único falso es el número
de WhatsApp, que ya está marcado como pendiente de reemplazo.

**Falta definir**: si se separan las migraciones por perfil (`db/migration/common` +
`db/migration/dev`) o si se acepta que V5 es data real y se le saca el cartel.

**Dónde impacta**: Sprint Despliegue.

---

## 5. Gestión de variantes vía admin — RESUELTO (2026-07-27, tarea 3.4b)

**Estado**: implementado. Ya no bloquea nada — queda documentado como historial de por qué
existía el hueco y qué se decidió, más el límite explícito de lo que 3.4b cubre.

Grep al ROADMAP entero por "variant": aparecía en la 3.2 (modelado) y en la 2.5 (nombre de
una card del frontend) — **ningún endpoint de admin para crear/editar/borrar variantes**,
ni en 3.4 ni en Sprint 4 ("Admin productos"). El alta (`POST /admin/products`) sí acepta
variantes al crear el producto; no había forma de tocarlas después.

**Por qué no se resolvió dentro de 3.4** (que sí edita el resto de los campos del
producto): `order_items` tiene FK real a `product_variants(id)` (ADR 013). Un endpoint que
haga "reemplazar el array completo" de variantes borraría filas viejas vía
`orphanRemoval` — si algún pedido real ya referencia una de esas variantes, el `DELETE`
choca contra la FK y la transacción falla. Funciona perfecto en dev (sin pedidos históricos)
y explota en producción justo cuando hay más que perder.

**Resolución (tarea 3.4b)**: `PUT /api/v1/admin/products/{publicId}/variants` —
`ProductServiceImpl.replaceVariants`. Upsert por id (`id == null` = variante nueva, `id`
presente = actualiza la existente) + soft-delete (`active = false`, nunca
`list.remove()`/hard-delete) de las que no vienen en el request. Tres rechazos con 400:
- el producto todavía no tiene variantes (precio simple) — ver §6, es un caso aparte
- un `id` del request no pertenece a ESE producto (evita adivinar ids ajenos)
- el resultado dejaría el producto sin ninguna variante activa (rompería el cálculo de
  precio del listado público, que hace `COALESCE(price, MIN(variant.price WHERE active))`)

Tests: `AdminProductControllerTest` (upsert + soft-delete, producto sin variantes, id
ajeno, resultado sin variantes activas).

---

## 6. Mode-switching precio simple ↔ variantes — RESUELTO (2026-07-29, Sprint 4 Bloque E)

**Estado**: implementado. Ya no bloquea nada — queda documentado como historial de por qué
existía el hueco y qué se decidió.

`PUT .../variants` (§5) solo funcionaba si el producto **ya** tenía variantes. No había
forma de convertir un producto de precio simple a "viene en 3kg/10kg/15kg" (o al revés)
desde el admin.

**Resolución**: ambas direcciones, en `ProductServiceImpl`:
- **Simple → con variantes**: se relajó el gate de `replaceVariants` para aceptar también un
  producto sin variantes. Si el resultado queda con ≥1 variante activa, el producto pasa a
  `price = null` / `priceOriginal = null` / `stock = 0`.
- **Con variantes → simple**: `PUT /admin/products/{id}` interpreta un `price` no nulo sobre
  un producto que tiene variantes como "volver a precio simple" — soft-deletea todas las
  variantes activas (nunca `list.remove()`, misma razón de siempre: la FK real de
  `order_items`).

Tests: `AdminProductControllerTest` — se reescribieron dos tests que afirmaban el rechazo
viejo (`replaceVariantsRejectsSimpleProduct`, `updateRejectsPriceOnVariantProduct`) para
afirmar la conversión nueva, no una regresión. UI en `<ProductFormDialog>` (toggle
"Preço único"/"Com variantes"), issue #33.

---

## 7. Optimización de imágenes de producto — RESUELTO (2026-07-29, Sprint 4 Bloque E)

**Estado**: implementado. Ya no bloquea nada.

El upload va directo del browser a R2 vía URL firmada — el backend nunca ve los bytes de la
imagen, así que nada la redimensionaba ni la reencodeaba antes de este bloque.

**Resolución**: `frontend/lib/images/compress.ts` — Canvas API, resize a máx 1200x1200 +
reencode a WebP antes de pedir la URL firmada, bajando la calidad de encode hasta acercarse
a <200KB (meta, no garantía dura: no hay forma de asegurar el tope con cualquier foto de
origen). Fallback Safari: se detecta con un encode real de prueba (1x1 a WebP), no con user
agent — funciona solo el día que Safari lo soporte, sin lista de versiones que mantener.
Integrado en `<ProductImageUpload>`, issue #33.

---

## 8. Tope de tamaño en uploads a R2 no lo garantiza el storage

**Estado**: limitación de la plataforma, aceptada para MVP1 (2026-07-27). El flujo de firmado
en sí ya es una decisión tomada — ver [ADR 018](decisions/018-r2-presigned-upload-flow.md).
Esta entrada documenta específicamente la limitación de tamaño y qué haría falta si el modelo
de amenaza cambia; no está "pendiente de decidir", está pendiente de *revisar si sigue
alcanzando*.

3.5b pedía "tamaño máximo... firmado en la política", asumiendo que funcionaría igual que
`Content-Type` (que R2 sí aplica: un PUT real con otro tipo distinto al firmado rompe la
firma, 403). Contra la doc oficial de R2 (`developers.cloudflare.com/r2/api/s3/presigned-urls/`,
verificado 2026-07-27): **R2 no soporta `content-length-range` ni presigned POST** — a
diferencia de S3, no hay forma de que el storage garantice el tamaño máximo de lo que
efectivamente se sube por un PUT firmado.

`R2StorageServiceImpl` valida el `contentLength` que el cliente **declara** al pedir la URL
(rechaza sobre 5MB) — es un chequeo honesto pero no una garantía: alguien con curl podría
declarar 100KB y mandar un PUT de 500MB. Se acepta este límite porque el endpoint de presign
vive detrás del login de admin (`anyRequest().authenticated()`) — el único que podría
abusarlo ya tiene, con esa misma sesión, acceso a borrar/editar todo el catálogo. El riesgo
real (costo de storage) es bajo: R2 cobra ~$0.015/GB/mes, sin cargo por egreso.

**Si el modelo de amenaza cambia** (multi-tenant real, más admins, señales de abuso): la
garantía dura sería un `HeadObject` post-upload (necesita un `S3Client` además del
`S3Presigner` actual) que borre el objeto si excede el tope — pero eso suma un endpoint de
"confirmar upload" que el frontend tendría que integrar, no está estimado.

**Dónde impacta**: nada bloqueado hoy. Revisar si MVP1 deja de ser single-admin.

---

## 9. Tercera copia del rate limit (login/orders/appointments) sin unificar — RESUELTO (2026-08-29)

**Estado**: resuelto. Se extrajo `common.SlidingWindowLimiter` (ventana deslizante, mapa
`ConcurrentHashMap` con barrido perezoso, `MAX_TRACKED_KEYS`) como núcleo compartido. Los 3
`*RateLimitService` (`identity.LoginAttemptService`, `orders.OrderRateLimitService`,
`booking.AppointmentRateLimitService`) quedaron como wrappers finos que instancian su propio
`SlidingWindowLimiter` desde su propia `@ConfigurationProperties` y le agregan la semántica que
sí difiere entre ellos (login cuenta solo fallos y resetea en éxito; orders/appointments cuentan
toda request y no resetean).

Sin cambios de comportamiento observable: los 3 `*RateLimitFilter`, las 3 `*RateLimitProperties`
(mismos prefijos/env vars en `application.yml`) y `SecurityConfig` quedaron intactos — mismos
límites, mismos mensajes, mismo lugar en la cadena de filtros. La clave (`"ip:" + remoteAddr`)
ya era idéntica en los 3 antes de unificar, no fue algo que hubiera que resolver.

**Dónde impacta**: nada roto. `common.SlidingWindowLimiterTest` cubre la lógica de ventana ahora
centralizada; los 3 integration tests existentes (uno por endpoint protegido) siguen verificando
el comportamiento HTTP end-to-end sin modificaciones.

---

## 10. `schedule_blocks` bloquea el día completo, no por rango horario — RESUELTO parcialmente (2026-08-03, Bloques E/F, Sprint 6)

**Estado**: la limitación de schema **sigue vigente** (no se replanteó — ver ADR 021,
alternativa descartada "extender `schedule_blocks` a rango horario"), pero la mitigación **ya
es usable end-to-end**: turno manual (`POST /api/v1/admin/appointments`, backend desde
2026-08-01, ADR 021, commit `74abdd9`; UI desde Bloque E, `<ManualAppointmentDialog>`) y
bloqueo de día completo desde el admin (`<ScheduleBlocksPanel>`, Bloque F, issue #63) ya están
ambos accesibles desde `/admin/agendamentos` y `/admin/servicos` respectivamente. Verificado
end-to-end el 2026-08-03: crear un bloqueo de día en el admin → el wizard público devuelve
`indisponibilidade: BLOQUEADO` para esa fecha.

El ADR 012 pedía poder bloquear *horarios* puntuales ocupados por otros canales (teléfono,
local, ERP) mientras el negocio ya está operando en paralelo a la web. El esquema real
(`V3__booking.sql`) solo permite bloquear el **día entero** (`data_desde`/`data_hasta`).

**Dónde impacta hoy**: si el cliente toma una reserva puntual por teléfono para un horario
suelto (no el día entero), la disponibilidad web puede **sobre-ofertar** ese horario — el
paliativo real para ese caso puntual es el turno manual (ocupa el cupo real), no el bloqueo de
día. El bloqueo de día completo sigue siendo la única herramienta para "no tomamos reservas
este día" (feriado, cierre excepcional). Replantear el schema a rango horario sigue siendo
trabajo de un sprint futuro si el negocio real lo necesita.

---

## 11. `GET /api/v1/appointments/{publicId}` expone nombre y pet, sin teléfono

**Estado**: decisión tomada al implementar el Bloque E (Sprint 5) — riesgo abierto, no bloqueante.

La vista pública reducida (`AppointmentDetail`) no incluye `clienteTelefone` — el UUID v7 del
link no es adivinable, pero por sí solo no es una garantía de que el link no se filtre (queda
en el historial de WhatsApp del cliente, en logs de proxies, etc.). Se decidió qué SÍ exponer
al implementar: `publicId`, `status`, servicio+adicionais, porte, horarios, precios y `petNome`
— sin `clienteNome` completo tampoco sería útil para que el cliente confirme "sí, es mi
reserva", así que quedó incluido.

**Dónde impacta**: nada bloqueado. Si en el futuro se agrega auth de clientes finales (Fase 2,
fuera de scope MVP1), este endpoint debería revalidar contra la sesión en vez de confiar solo
en la opacidad del UUID.

---

## 12. Hero del wizard de agendamento (Stitch) necesita estética propia

**Estado**: pendiente de diseño — anotado 2026-07-30 al revisar el Passo 1 corregido en Stitch,
no bloquea Sprint 6.

El hero actual ("Agende o horário do seu pet") es el mismo header genérico repetido en los 4
pasos del wizard (Passo 1, 2, 3, Confirmação) — texto plano, sin foto ni tratamiento visual
propio. Sebastián quiere pensar una estética más profesional para ese elemento, no solo agregar
una foto de stock. Como es el **mismo elemento repetido en los 4 pasos**, conviene resolverlo
una sola vez y aplicarlo consistente, no pantalla por pantalla.

Se decidió no resolverlo ahora junto con el fix de porte/adicionais (issue distinta, ADR 020)
por dos motivos: las fotos reales del petshop siguen pendientes del cliente (§6.3 de
`docs/preguntas-cliente.md`, "antes del Sprint Despliegue") — diseñar contra una foto de stock
ahora probablemente se rehace cuando lleguen las reales — y es un cambio transversal a las 4
pantallas, mejor decidirlo una vez con foto+copy+tratamiento definitivos que iterar sobre él
varias veces.

**Dónde impacta**: nada bloqueado. Retomar cuando (a) haya fotos reales del cliente, o (b)
se defina la dirección estética antes de eso vía referencia visual (CLAUDE.md §8: UI siempre
con referencia visual antes de codear/diseñar).

---

## 13. `<ManualAppointmentDialog>` usa inputs nativos `type="date"`/`type="time"`

**Estado**: deuda anotada 2026-08-01 (feedback de Sebastián), no bloquea Bloque E.

El form de "+ Novo agendamento" (Bloque E, issue #62) usa `<input type="date">` e
`<input type="time">` nativos del browser para elegir fecha/horário — sin picker propio, sin
integración visual con el resto del design system (el navegador dibuja su propio calendario/
selector de hora, fuera de nuestro control de estilos). Funciona y no bloquea el flujo — la
fecha además ya arranca en el día de hoy por default — pero el estilo queda inconsistente con
el resto de la UI.

**Por qué no se resolvió ahora**: no era parte del AC de la tarea 6.E, y construir un
date/time picker propio (o instalar uno) es una decisión de diseño separada que merece su
propia referencia visual (CLAUDE.md §8) — no algo para resolver de apuro dentro de este bloque.

**Dónde impacta**: nada bloqueado, es solo estético. Retomar cuando haya una referencia de
Stitch o una decisión de qué componente usar (¿extender algo de `components/ui/`? ¿instalar
uno, revisando primero si vainilla alcanza, CLAUDE.md §6?). Candidato natural: reusar/adaptar
el `date-strip` que el wizard público (Bloque C, `feat/agendamento-wizard`) esté construyendo,
si termina siendo genérico.

## 14. `SERVICES_PREVIEW` (home) no se entera si el admin edita serviços

**Estado**: deuda baja anotada 2026-08-03 (Bloque F, issue #63), no bloquea el sprint.

`frontend/lib/data/services.ts` (`SERVICES_PREVIEW`) es dato estático de la landing (Sprint 2,
ADR 017 — home frontend-first) con nome/preço/duração de "Banho Essencial"/"Banho Premium"
copiados a mano del seed de julio. Con el Bloque F, `/admin/servicos` ya permite editar nome
y preço de esos mismos serviços vía `PUT /admin/services/{id}` — pero ese cambio solo se
refleja en `/servicos` (público), el wizard de agendamento y el propio admin, que leen
`GET /api/v1/services` en vivo. La home sigue mostrando el valor hardcodeado hasta que alguien
edite `services.ts` a mano.

**Por qué no se resolvió ahora**: no es parte del AC del Bloque F (que es CRUD de admin, no la
home) y conectar la home a datos reales es una decisión de scope aparte (¿Server Component con
`GET /services` en vivo? ¿revalidate?), no algo para resolver de apuro acá.

**Dónde impacta**: si el admin renombra o cambia el precio base de un banho, la sección
"Serviços" de la home (`/`, 3 cards) puede quedar desactualizada respecto al resto del sitio
hasta que se actualice `services.ts` a mano. Candidato natural para cuando se generalice el
swap estático→API de la home (mismo criterio que ya se usó para `/servicos` y el catálogo de
produtos).

---

## 15. `REVIEWS` y `TrustBar` muestran contenido inventado como si fuera real

**Estado**: detectado 2026-08-03 en auditoría de release (`/release-check`). **Resuelto
2026-09-01** (ADR 025): `<Reviews>`, `<TrustBar>` y las cards flotantes del `<Hero>` leen ahora
de la Places API del Google Business real (5,0 · 78 avaliações verificado). `lib/data/reviews.ts`
se borró — **sin fallback a contenido inventado**: si la API falla o el negocio no tiene reviews
todavía, la sección se muestra sin cards en vez de mostrar testimonios de ejemplo (opción 2 de
abajo, aplicada automáticamente en ese caso puntual, no como decisión manual). Un primer intento
de esta integración había quedado roto en silencio (endpoint legacy dado de baja,
`REQUEST_DENIED` sin que nadie lo notara) — detalle completo en ADR 025.

`frontend/lib/data/reviews.ts` tiene 3 testimonios con **personas que no existen** ("Mariana
L., Tutora do Thor", "Ricardo S., Tutor do Duke", "Fabiana M., Tutora da Mel") y textos de
elogio atribuidos a ellas. `frontend/components/public/trust-bar.tsx` afirma **"4.9 Avaliação
Média"** y **"500+ Pets Atendidos"** — dos métricas que no salen de ninguna fuente: no hay
tabla de reviews (ADR 013 no la define) ni conteo real de atendimentos.

Los comentarios del código documentan bien que son datos **estáticos** y por qué (ADR 017,
home frontend-first; reviews dinámicas son Fase 2). Lo que **ningún doc registraba** es que
además son **falsos**, y que eso deja de ser un detalle de implementación en el momento en
que el sitio es público y comercial.

**Por qué importa acá y no es purismo**: el sitio va a producción a nombre de un negocio real
en Brasil. Testimonios inventados y métricas infladas son publicidade enganosa (CDC art. 37) —
el expuesto es FrontPet, no el dev. Y a diferencia de las fotos placeholder (que se ven como
placeholder), esto es indistinguible de contenido genuino para quien lo lee.

**Opciones**:
1. **Pedirle testimonios reales al cliente** (tiene Instagram con comentarios de clientes —
   fuente natural, con permiso). Ver `preguntas-cliente.md` §6.5. Es la opción que conserva
   la sección.
2. **Sacar la sección** `<Reviews>` y las dos métricas numéricas de `<TrustBar>`, dejando solo
   claims verificables ("Atenção personalizada" ya lo es). Reversible: cuando lleguen
   testimonios reales, la sección vuelve.

**Recomendación**: pedir los reales (opción 1) y tener la 2 como fallback si no llegan antes
del deploy — el deploy no debería esperar por esto, pero tampoco salir con lo inventado.

**Dónde impacta**: bloquea el Sprint Despliegue (checklist D.10 del ROADMAP), no el desarrollo.

---

## 16. La modalidade Entrega/Retirada se reconstruye por string mágico en el frontend

**Estado**: detectado 2026-08-03 en auditoría de release. Fix chico y cerrado, sin decisión
de diseño pendiente — está acá para que no se pierda, no porque haya que debatirlo.

ADR 003 (act. 2026-07-28) decidió que `modalidade` **no tiene columna propia**: vive en el DTO
de entrada y `OrderServiceImpl` la mapea al persistir (`RETIRADA` → `enderecoEntrega =
"Retirada na loja"`). Esa decisión está bien y no se discute acá.

Lo que no se documentó es su **consecuencia del lado de la lectura**: `OrderDetail` tampoco
expone `modalidade`, así que el frontend la **revierte comparando contra el literal**, en 4
lugares:

- `frontend/lib/whatsapp/templates.ts:48` (`modalidadeLabel`), `:57` (línea de endereço del
  template PENDING), `:72` (previsão del template CONFIRMED)
- `frontend/components/admin/order-detail-panel.tsx:63`

El literal vive en `OrderServiceImpl.java:40` (`RETIRADA_ENDERECO`). Dos problemas concretos:

1. **Falso positivo**: un cliente que elige ENTREGA y escribe exactamente "Retirada na loja"
   en el campo de endereço queda etiquetado como retirada en el admin y en el WhatsApp que se
   le manda. Improbable, pero el sistema no tiene forma de distinguirlo.
2. **Acoplamiento silencioso**: cambiar ese literal en Java (o traducirlo, o corregirle un
   acento) rompe los 4 puntos del frontend sin que falle ningún test — `OrderServiceIntegrationTest:80`
   afirma el literal del lado backend, y del lado frontend nadie lo verifica.

**Fix propuesto** (~30 min): agregar `modalidade` (`ModalidadeEntrega`) a `OrderDetail` — el
dato ya existe en `CreateOrderRequest` y solo se pierde en la respuesta — y consumir ese campo
en los 4 lugares en vez del string. No requiere migración: se deriva de `enderecoEntrega` en el
mapeo a DTO, pero queda derivado **una sola vez y del lado que es dueño del literal**.

**Dónde impacta**: nada bloqueado. Candidato a Sprint 7 junto con la unificación del rate
limit (§9), o a cualquier hueco chico antes.

---

## 17. Valores arbitrarios de Tailwind en el código portado, y la tensión con shadcn

**Estado**: detectado 2026-08-03 en auditoría de release. **La parte que necesita decisión es
la excepción para shadcn**; el resto es limpieza mecánica.

`CLAUDE.md` §5 y `design-system.md` §4 prohíben valores arbitrarios y exigen spacing en
múltiplos de 4. `port-landing-stitch.md` §3 ya documentó los 65 arbitrarios **del mock de
Stitch** (pre-porteo). Lo que no estaba medido es el **drift en el código ya portado**:

- **51 ocurrencias** de arbitrarios tipo `h-[...]`, `text-[...]`, `shadow-[...]`, `max-w-[...]`,
  concentradas en `components/public/hero.tsx` (10 — `h-[560px]`, `text-[15px]`, `h-[52px]`,
  `shadow-[0_8px_20px_rgba(...)]`) y `components/public/hero-floating-cards.tsx` (6). El hero
  es el peor caso del repo y el más visible.
- **~20 spacings fuera de la escala de 4**: `py-1.5` (6px), `py-2.5` (10px), `py-0.5` (2px),
  `pl-9` (36px) en `admin-sidebar.tsx:63-79`, `product-form-dialog.tsx`, `category-filter.tsx:23`,
  `appointment-list.tsx:221`, `bottom-nav.tsx:47`.

**La tensión real**: `py-1.5` / `px-2.5` son el idiom de **shadcn** — vienen así en los
componentes generados y en los patrones que la comunidad copia. La regla de "solo múltiplos de
4" choca de frente con eso cada vez que se instala o adapta un componente de `components/ui/`.
Hoy se está resolviendo caso por caso y sin criterio explícito, que es exactamente cómo una
regla se erosiona sin que nadie la derogue.

**Opciones**:
1. **Excepción documentada**: la escala de 4 aplica al código propio; los componentes de
   `components/ui/*` (shadcn) conservan su spacing nativo. Se escribe en `design-system.md` §4
   y deja de ser una violación cada vez.
2. **Normalizar shadcn al instalarlo**: bajar `py-1.5` → `py-1`/`py-2` como ya se hace con los
   radios (`design-system.md` §5 ya tiene ese precedente: "bajar sus radios un paso al
   instalarlos"). Coherente con lo que el repo ya hace, pero es trabajo en cada instalación.

**Recomendación**: opción 1 para spacing (el precedente del radius existe porque el radius es
visible en la identidad de marca; 2px de padding no lo es), y limpiar aparte los arbitrarios de
`hero.tsx`, que no tienen excusa de shadcn — son porteo directo de Stitch sin traducir.

**Dónde impacta**: nada bloqueado. La limpieza del hero es candidata natural al pase de polish
de UI ya diferido en `WORKING-CONTEXT.md`.

---

## 18. DNS/R2/Resend de producción corren en la cuenta personal de Cloudflare de Sebastián, compartida con otro cliente

**Estado**: decisión operativa tomada 2026-08-24, deferida a propósito — no bloquea el deploy.
Ampliada 2026-08-29 (tarea 7.12): Resend suma un tercer servicio a la misma cuenta compartida.

La cuenta de Cloudflare que se usa hoy para `frontpet.com` (DNS proxied), el bucket R2
(`frontpet-dev`, y el que se cree para prod) y, desde la tarea 7.12, los registros DNS de envío
del dominio de email (`resend._domainkey`, `send.frontpet.com.br` — DKIM/SPF/MX de Resend) es la
**cuenta personal de Sebastián**, la misma donde vive `turnosuy.com` (dominio de otro cliente,
consultorio). El dominio de FrontPet en sí **no tiene este problema** — está registrado en
`registro.br` a nombre del cliente, independiente de Cloudflare. La cuenta de Resend en sí
también es personal de Sebastián (mismo criterio, mismo riesgo de facturación cruzada si se
supera el free tier de envíos).

**Por qué no se resuelve ahora**: separar en una cuenta de Cloudflare exclusiva del cliente
de FrontPet requiere su tarjeta de crédito (Billing → Payment methods), y la próxima vez que
se van a juntar en persona es recién en la reunión de entrega, cerca del final del proyecto.
No tiene sentido bloquear el Sprint Despliegue por esto.

**Riesgo aceptado mientras tanto**: la tarjeta de Sebastián queda como método de pago de la
cuenta compartida — cualquier facturación (R2 pasando el free tier, algún plan pago que se
active sin querer) se cobra a él, no al cliente. Mitigación: revisar el uso de R2
periódicamente contra el free tier (10GB / 1M-10M operaciones), no delegarlo a una alerta de
Cloudflare que no está configurada.

**Acción pendiente**: en la reunión de entrega, crear cuenta de Cloudflare nueva y exclusiva
para el cliente de FrontPet (su email + tarjeta), migrar el dominio (cambiar nameservers desde
`registro.br` a los nuevos) y recrear el bucket R2 de producción ahí. Hasta entonces, el
bucket `frontpet-dev` en la cuenta compartida sigue sirviendo tanto para dev como para prod.

**Dónde impacta**: `docs/deploy-runbook.md` D.2, D.3 y D.6.3 — anotado ahí también.

---

## 19. Reset de contraseña (7.12): dominio de Resend sin verificar + sin validación visual real

**Estado**: código mergeado a `main` y con DoD automatizado en verde (`./mvnw test`,
`pnpm build/test/lint`), pero dos pasos del DoD (CLAUDE.md §10) quedaron sin correr en esta
sesión porque dependen de accesos y herramientas que la sesión no tenía.

1. **Paso 0 del plan** (`docs/decisions/022-reset-senha-admin.md`, verificación del dominio
   `frontpet.com.br` en Resend — 3 registros DNS en Cloudflare, ver `docs/deploy-runbook.md`
   D.3) **no se ejecutó**: requiere el dashboard de Resend y de Cloudflare, a los que esta
   sesión no tiene acceso. Sin esto, `RESEND_API_KEY`/`RESEND_FROM` en prod van a fallar en
   el primer envío real aunque `StartupEnvValidator` deje arrancar la app (solo valida que
   las env vars no estén vacías, no que el dominio esté verificado del lado de Resend).
2. **Validación visual no se hizo**: sin Playwright ni herramienta de captura disponible en
   esta sesión, no se verificaron `/admin/esqueci-senha` y `/admin/redefinir-senha` en un
   browser real a 320/768/1024px, y no hay screenshot para la issue (CLAUDE.md §10, "UX
   visible → screenshot en la issue"). El build/lint/test en verde confirma que compila y
   los tests pasan, pero no que se vea bien.

**Dónde impacta**: bloquea marcar 7.12 como completo según el DoD estricto. Antes de darlo
por cerrado: (a) correr el Paso 0 completo y probar un envío real de punta a punta, (b)
abrir las dos pantallas nuevas en Chrome/mobile real a los 3 breakpoints y sacar el
screenshot para la issue.

---

## 20. Mini-dashboard (7.1-7.3): sin comparar contra Stitch en vivo + sin correr ni probar en mobile

**Estado**: código mergeado a `main`, tests de integración en verde, pero el porteo se hizo
sin poder comparar contra la fuente de verdad real (CLAUDE.md §5: "Stitch es la fuente de
verdad del diseño, leído por MCP") y sin las validaciones manuales del DoD.

1. **MCP de Stitch en vivo falló por autenticación** (no autorizado en esta sesión) — el
   porteo del mini-dashboard se hizo mirando únicamente `docs/ui/` (export offline, marcado
   como stale en CLAUDE.md §5) en vez de leer la pantalla real vía MCP. Puede haber desvíos
   no documentados respecto del diseño actual en Stitch.
2. **Los dos commits no se corrieron** (`docker-compose up` + `mvnw spring-boot:run` +
   `pnpm dev`) para verificar el dashboard funcionando de punta a punta contra datos reales,
   más allá de lo que cubren los tests de integración.
3. **Sin prueba en mobile real** (CLAUDE.md §10: "Funciona en mobile real, no solo
   DevTools") — el `<KPICard>` y el layout del mini-dashboard no se probaron en un celular.

**Dónde impacta**: bloquea marcar 7.1-7.3 como completo según el DoD estricto. Antes de
cerrarlo: (a) autorizar el MCP de Stitch (`claude mcp` o `/mcp`) y comparar la pantalla
portada contra la real, corrigiendo desvíos si aparecen; (b) levantar el stack local y
probar el dashboard con datos de verdad; (c) probarlo en un celular real, no solo DevTools.

---

## 21. El wizard de agendamento no protege contra doble submit — un doble clic puede crear 2 turnos

**Estado**: detectado 2026-08-31 probando el flujo de agenda contra el stack local (Postgres +
backend + frontend corriendo de verdad, no solo tests). No es una suposición: se reprodujo a
propósito mandando 2 `POST /api/v1/appointments` casi simultáneos para el mismo horário y
ambos devolvieron `201` (dos turnos `PENDING` distintos, mismo cliente, mismo slot) porque la
`capacidade_atendimento` del tenant es `2` — el segundo request todavía entraba dentro del
cupo.

**Por qué pasa, en las dos capas**:

1. **Frontend** (`frontend/components/public/booking/booking-form.tsx:232`): el botón de
   "Confirmar agendamento" se deshabilita con `disabled={isSubmitting}` de React Hook Form.
   Es una mitigación razonable, pero `isSubmitting` recién pasa a `true` después de un
   re-render — en un doble clic rápido (o un doble tap en mobile con la red lenta, que es
   justo el caso real: "demoró en responder 1 segundo") los dos clics pueden disparar
   `handleSubmit` antes de que el DOM refleje el botón deshabilitado.
2. **Backend** (`AppointmentServiceImpl.create`, ver `lockTenantDay` línea ~393): SÍ hay un
   lock real —`pg_advisory_xact_lock` por `tenant_id + día`— que serializa correctamente los
   `POST` concurrentes y evita pasarse de la capacidad configurada. Pero ese lock protege
   contra **sobreventa** (2 clientes distintos peleando el último cupo), no contra
   **intención duplicada**: no existe ningún chequeo de "¿este mismo cliente/teléfono ya
   tiene un turno pendiente para este mismo horário, creado hace unos segundos?". Si hay cupo
   para 2, el sistema crea 2 turnos legítimos aunque el usuario solo quiso agendar 1.

**Dónde impacta**: el admin ve 2 turnos `PENDING` para el mismo cliente/horário en
`/admin/agendamentos` sin ninguna señal de que es un duplicado accidental — tiene que
detectarlo a ojo y cancelar uno a mano. Con `capacidade_atendimento` baja (el valor de
producción probablemente sea 1 o 2) el impacto real es acotado, pero no nulo.

**Fix propuesto** (no implementado): dos opciones, no excluyentes —
(a) frontend: además de `isSubmitting`, marcar el botón `disabled` de forma síncrona en el
`onClick` (antes de que React re-renderice) o envolver el submit en un `useRef` de guarda;
(b) backend: dentro de `lockTenantDay`, antes de crear, rechazar con `409` si ya existe un
turno `PENDING`/`CONFIRMED` con el mismo `clienteTelefone` que se solape con el horário
solicitado — mismo criterio que ya se usa para el solapamiento general. La opción (b) es la
que realmente cierra el hueco (no depende del timing del cliente); la (a) solo lo hace menos
probable.

---

## 22. `POST /admin/products` responde 500 genérico ante un body JSON con bytes UTF-8 inválidos

**Estado**: detectado 2026-08-31 probando el alta de productos contra el stack local. Con un
body bien formado (UTF-8 válido) el alta funciona correctamente — subtotal, categorías,
espécies y snapshot de precio se verificaron end-to-end sin problemas. El hallazgo es
puntual: si el JSON llega con un byte UTF-8 inválido a mitad de un string (en la práctica,
un cliente HTTP mal configurado que trunca o corrompe un acento/carácter especial), Jackson
tira `HttpMessageNotReadableException` y esa excepción no tiene handler propio en
`RestExceptionHandler` — cae en el `@ExceptionHandler(Exception.class)` genérico y responde
`500 "Ocorreu um erro inesperado"` en vez de un `400` de validación.

**Dónde impacta**: bajo. No es explotable como bug de negocio (el propio `<AdminProductForm>`
del frontend nunca genera JSON inválido) y no se disparó con datos reales — apareció al
probar con un payload armado a mano desde la terminal con encoding roto. Vale documentarlo
porque cualquier 500 en logs de producción (Sentry, D.8) dispara una alerta como si fuera un
bug real, y este caso puntual no lo es — solo un `400` mal clasificado.

**Fix propuesto** (no implementado, ~15 min): agregar
`@ExceptionHandler(HttpMessageNotReadableException.class)` en `RestExceptionHandler` que
devuelva `400` con un mensaje genérico ("Corpo da requisição inválido"), igual que ya existe
para `MethodArgumentNotValidException`. Candidato a Sprint 7 (polish) o a cualquier hueco
chico — no bloquea nada.

---

## 23. Sentry backend corre con `send-default-pii` en `false` (default), pendiente de revisar cuando exista banner LGPD

**Estado**: decisión tomada 2026-09-02 al activar Sentry (D.4/D.8, Sprint Despliegue) —
deliberada, no un olvido, pero atada a que el banner LGPD (CLAUDE.md §5, "no existen...
banner LGPD" — pendiente de generarse en Stitch, tareas 7.12/7.13 mencionadas ahí) todavía no
existe.

`sentry-spring-boot-starter-jakarta` no tiene `sentry.send-default-pii` seteado en
`application.yml` — corre con el default del SDK (`false`). Con eso, Sentry captura stack
traces y contexto de la excepción, pero **no** IP del request ni headers como cookies/
autorización. Con `true`, Sentry adjunta esos datos a cada evento — que es información
personal (IP) mandada a un tercer país (Sentry, EEUU) sin que el sitio tenga todavía ningún
aviso al usuario de que eso pasa.

**Por qué se decidió así y no al revés**: el sitio es público y comercial, a nombre de un
negocio real en Brasil (mismo criterio que el hallazgo #15, testimonios falsos — exposición
legal real, no un detalle técnico). Sin banner LGPD, no hay dónde informarle a un visitante
que su IP puede terminar en un servicio de terceros. Los stack traces siguen siendo
igual de útiles para debuggear sin ese dato — no se pierde capacidad de diagnóstico real,
solo la correlación por IP.

**Qué haría falta para poder pasarlo a `true`**: que exista el banner LGPD (o al menos una
política de privacidad accesible) que mencione el uso de Sentry para monitoreo de errores. Si
en algún momento se decide que hace falta correlacionar errores por IP (ej. para identificar
un atacante specific o abuso repetido), revisar esto junto con esa necesidad concreta, no
activarlo "por si sirve".

**Dónde impacta**: `backend/src/main/resources/application.yml` (bloque `sentry:`, sin
`send-default-pii` explícito hoy). Cuando el banner LGPD exista, volver acá y decidir si vale
la pena agregar `sentry.send-default-pii: true` (o dejarlo como está — no es obligatorio
activarlo solo porque ya se puede).

---

## 24. Coolify no pasa variables marcadas como "secret" al build de Docker, aunque tengan el toggle de "Available at Buildtime" activo

**Estado**: descubierto 2026-09-02 debuggeando en producción por qué "Histórias de Tutores"
seguía vacía después de agregar `GOOGLE_PLACES_API_KEY` como `ARG` en `frontend/Dockerfile`
(D.6/D.8, Sprint Despliegue). No es un bug nuestro — es un comportamiento (¿de seguridad?, no
documentado) de Coolify 4.3.14.

`GOOGLE_PLACES_API_KEY` tenía el toggle de **"Available at Buildtime"** tildado en Production,
pero seguía marcada como **secret** (🔒). El build igual la recibía vacía: confirmado
inspeccionando `/data/coolify/applications/<app>/.env` en el VPS directamente — la variable
aparecía ahí, pero con valor vacío, pese al toggle. Solo cuando se le sacó el flag de secret
(dejando el toggle de Buildtime) el valor real llegó al build. Diagnóstico completo: comparar
runtime (`docker exec <container> env`, sí tenía el valor) vs. el `.env` de build en el
filesystem del VPS (vacío) aisló el problema al paso de build específicamente, no al
container corriendo.

**Por qué importa**: el propio `Dockerfile` ya tenía un comentario (junto a `SENTRY_AUTH_TOKEN`)
que decía *"no confirmado que [Coolify] soporte secrets de build"* — este hallazgo confirma que
efectivamente **no los soporta**, al menos en esta versión. Cualquier variable que:
(a) el código necesite en build time (típicamente porque una página es ISR/SSG y ejecuta el
fetch durante `pnpm build`, ver hallazgo relacionado en el mismo debugging) y (b) sea sensible,
tiene que elegir entre quedar sin protección de "secret" en Coolify, o no funcionar en build.

**Resolución aplicada**: se sacó el flag de secret a `GOOGLE_PLACES_API_KEY` en Coolify. Riesgo
aceptado explícitamente — mismo criterio que `SENTRY_AUTH_TOKEN` (queda en el historial de
capas de la imagen, pero la imagen nunca sale del VPS, y la key está restringida en Google
Cloud Console por HTTP referrer/IP, no es una credencial de admin ni de DB).

**Dónde impacta**: cualquier variable futura que necesite build-time + confidencialidad al
mismo tiempo va a pisar el mismo límite. Si en algún momento se vuelve un problema real
(rotación de claves más sensibles, ej. si `SENTRY_AUTH_TOKEN` se llegara a usar), revisar si
Coolify agregó soporte de `--mount=type=secret` de BuildKit en una versión más nueva, en vez de
repetir el workaround de sacar el flag de secret.

---

## 25. La cookie de sesión del admin no tenía `Domain`, así que el login nunca persistía entre subdominios en producción — bug crítico, no de config

**Estado**: encontrado y arreglado 2026-09-02, probando el login del admin contra el sitio
real recién deployado (Sprint Despliegue). A diferencia de los hallazgos #23/#24 (config de
Coolify), **este es un bug real de código**, presente desde que existe el login (tarea 1.x) —
nunca se manifestó antes porque nunca se había probado el admin contra dos subdominios reales.

**Síntoma**: `/admin/login` con las credenciales correctas devolvía un `POST
/api/v1/auth/login` que en los logs del backend decía `"Login bem-sucedido"` — pero el
navegador quedaba en loop, rebotando de vuelta a `/admin/login` en vez de entrar al panel. Se
probó varias veces pensando que era la contraseña (recién reseteada vía el flujo de 7.12), pero
los logs mostraban el login aceptándose una y otra vez.

**Causa real**: `AuthController.sessionCookie()` (`backend/.../identity/api/AuthController.java`)
seteaba la cookie `frontpet_session` sin `Domain` explícito — queda "host-only" para el host que
la emitió. En producción, el login pega contra `api.frontpet.com.br` (esa es la que recibe la
cookie), pero el guard del admin (`ProtectedAdminLayout`, Server Component de Next.js en
`frontend/app/admin/(protected)/layout.tsx:22-24`) lee las cookies de la request al **frontend**,
`frontpet.com.br` — un host distinto. Una cookie host-only para `api.frontpet.com.br` **nunca**
se manda en una request a `frontpet.com.br`, aunque sea el mismo "site" a efectos de
SameSite/CORS. El check `cookieStore.has(SESSION_COOKIE_NAME)` siempre daba `false`.

**Por qué nunca se detectó antes**: en dev, front (`localhost:3000`) y back (`localhost:8080`)
comparten el mismo **host** (`localhost`, solo cambia el puerto) — las cookies se scopean por
host, no por puerto, así que ahí sí se compartía sin que nadie tuviera que pensarlo. El bug es
invisible en cualquier entorno donde front y back no estén en subdominios realmente distintos —
exactamente el caso de producción, que es la primera vez que esto se probó de punta a punta.

**Resolución**: nuevo `frontpet.cookie.domain` (`COOKIE_DOMAIN` env var), vacío por default
(dev sigue igual). En producción, `COOKIE_DOMAIN=frontpet.com.br` (sin protocolo, sin
subdominio — el dominio raíz, para que lo compartan `frontpet.com.br` y `api.frontpet.com.br`).
`AuthController.sessionCookie()` aplica `.domain(cookieDomain)` solo si no está vacío.

**Dónde impacta**: bloqueaba el panel admin **completo** en producción — ningún login hubiera
funcionado nunca, con cualquier contraseña. Bug crítico para el hito de cobro del 05/09 (el
admin es cómo el cliente gestiona pedidos/turnos/productos). Requiere: mergear esta branch,
redeploy del backend (cambio de código Java, no alcanza con solo la env var — hace falta
recompilar), y agregar `COOKIE_DOMAIN=frontpet.com.br` a las env vars del backend en Coolify.
