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

## 9. Tercera copia del rate limit (login/orders/appointments) sin unificar

**Estado**: deuda aceptada a propósito, diferida a Sprint 7 (decisión tomada al planificar el
Sprint 5, ver `docs/decisions/020-algoritmo-slots.md`).

`AppointmentRateLimitService`/`Filter`/`Properties` (Sprint 5, Bloque E) son la **tercera**
copia casi literal del mismo mecanismo (`identity.LoginAttemptService`, `orders.OrderRateLimitService`).
Se aceptó duplicar en vez de extraer porque unificar 3 módulos dentro del sprint marcado como
"el riesgo #1 de todo el plan" no es el momento — y porque las semánticas difieren un poco
(login cuenta solo fallos, orders/appointments cuentan toda request).

**Dónde impacta**: nada roto hoy, es puro mantenimiento futuro. Al tocar cualquiera de los 3
para un cuarto caso de uso, es el momento de extraer un limitador genérico por clave opaca a
`common/`.

---

## 10. `schedule_blocks` bloquea el día completo, no por rango horario

**Estado**: limitación de schema sigue vigente (no se replanteó — ver ADR 021, alternativa
descartada "extender `schedule_blocks` a rango horario"). La mitigación (turno manual del
admin) tiene su **backend ya implementado** (2026-08-01, `POST /api/v1/admin/appointments`,
ADR 021, commit `74abdd9`) pero **todavía no es usable**: no existe UI de admin para cargarlo
(tareas 6.6/6.7 del ROADMAP, Sprint 6, pendientes).

El ADR 012 pedía poder bloquear *horarios* puntuales ocupados por otros canales (teléfono,
local, ERP) mientras el negocio ya está operando en paralelo a la web. El esquema real
(`V3__booking.sql`) solo permite bloquear el **día entero** (`data_desde`/`data_hasta`).

**Dónde impacta**: hasta que la UI de Sprint 6 (6.6/6.7) cierre, si el cliente empieza a
operar con reservas por teléfono la disponibilidad web puede **sobre-ofertar** (mostrar libre
un horario que en la realidad ya está ocupado por un turno tomado por otro canal). Avisar a
Sebastián si eso pasa — el bloqueo de día completo sigue siendo el único paliativo accesible
desde el admin hoy.

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
