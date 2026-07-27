# Decisiones pendientes

Cosas que aparecieron durante el desarrollo, que **no están decididas** y que no bloquean
el trabajo actual. No son ADRs: un ADR documenta una decisión *tomada*. Esto es la lista de
lo que todavía no lo está.

Cuando una de estas se resuelve → sale de acá y entra como ADR o como tarea del ROADMAP.

---

## 1. Alta de marcas (`brands`)

**Estado**: backend resuelto (2026-07-25). Falta la UI del admin (Sprint 4).

Las marcas no son lista cerrada como las categorías: llega una ração de una marca nueva y
el admin tiene que poder cargarla. Pero un CRUD de marcas con pantalla propia es demasiado
para lo que es.

**Dirección elegida**: alta *inline* en el formulario de producto — el admin escribe la
marca; si no existe, se crea sola. Sin pantalla dedicada.

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

**Falta definir**: si el campo del form es autocompletar sobre las existentes o texto
libre (el `findOrCreate` funciona con cualquiera de los dos; es una decisión de UI, no de
backend), y qué pasa con marcas que quedan sin ningún producto (¿se borran solas? ¿quedan
colgando?). Ninguna bloquea: son decisiones baratas de tomar cuando se construya el form.

**Dónde impacta**: Sprint 4, formulario de producto del admin.

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

**Estado**: la columna existe, sin UI y sin decidir.

`V6__catalog_promos.sql` agregó `price_original` a `products` y `product_variants`, con un
CHECK que garantiza `price_original > price`. La idea era una sección "Nuestros descuentos"
en la landing.

**Problema**: no está en la lista de MVP1 del `CLAUDE.md` §7, y ningún componente del
frontend la usa todavía (grep = 0 resultados, jul/2026).

**Decisión provisoria**: **se deja la columna, sin UI en MVP1.** Es NULLABLE — si nadie la
llena, no existe. Costo cero y cero riesgo, y evita dos migraciones de ida y vuelta si
después resulta que el diseño sí la pide.

**Falta definir**: si el diseño de Stitch tiene sección de descuentos. No se pudo verificar
porque `docs/ui/` está gitignoreado y no está en disco — hay que mirarlo por MCP al portar
la landing.

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

## 6. Mode-switching precio simple ↔ variantes — no cubierto por 3.4b, sin tarea asignada

**Estado**: hueco identificado a propósito al acotar el alcance de 3.4b (2026-07-27), no
implementado.

`PUT .../variants` (§5) solo funciona si el producto **ya** tiene variantes. Hoy no hay
forma de convertir un producto de precio simple a "viene en 3kg/10kg/15kg" (o al revés)
desde el admin — meterlo en 3.4b hubiera repetido el mismo error que originó el hueco
original: una transición de modo que toca el invariante precio/variantes (ADR 013 §2) no es
un `if` que se cuela en un endpoint pensado para otra cosa.

**Estimado** (para cuando se agende): ~3-4 hs.
- Simple → con variantes: relajar el gate de `replaceVariants` para que también acepte un
  producto sin variantes, y cuando el resultado tenga ≥1 variante activa, poner
  `price = null` / `stock = 0` en el producto (~1h — la lógica de "crear variante nueva" ya
  existe, es la parte fácil).
- Con variantes → simple: enganchar la transición inversa en `PUT /admin/products/{id}`
  (si el producto tiene variantes y llega un `price` no nulo, interpretarlo como "volver a
  precio simple" y soft-deletear todas las variantes activas) + tests de la combinación
  (~2-2.5h — la parte que realmente pesa).

**Dónde impacta**: candidato a Sprint 4 (admin productos) o Fase 2, a criterio de
Sebastián — no bloquea nada de Sprint 3.

---

## 7. Optimización de imágenes de producto — sin tarea asignada

**Estado**: hueco identificado al implementar 3.5/3.5b (2026-07-27), no implementado.

El upload de 3.5 va directo del browser a R2 vía URL firmada — el backend nunca ve los
bytes de la imagen. Eso significa que **nada la redimensiona ni la reencodea**: si el admin
sube una foto de celular sin comprimir, se guarda tal cual y se sirve tal cual a cualquiera
que entre al catálogo. Contradice el spec recomendado del ROADMAP (WebP, máx 1200x1200,
<200KB) — hoy ese spec depende 100% de la disciplina del admin, no hay nada en el código que
lo garantice.

**Dirección recomendada**: resolverlo **client-side** (Canvas API nativa del browser,
redimensionar + reencodear a WebP antes de subir), no server-side. El backend no participa
del upload (va directo a R2), así que procesar server-side implicaría un viaje extra
(bajar de R2 → procesar → volver a subir) en vez de resolverlo una vez, antes de que salga
del browser. Esto la convierte en tarea de **frontend**, no de backend.

**Estimado**: ~1.5-2 hs (incluye fallback para Safari, que no siempre soporta encode a WebP
vía Canvas).

**Dónde impacta**: candidato a sumarse al trabajo de 3.6+ (frontend) o Fase 2, a criterio de
Sebastián.

---

## 8. Tope de tamaño en uploads a R2 no lo garantiza el storage

**Estado**: limitación de la plataforma, aceptada para MVP1 (2026-07-27).

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
