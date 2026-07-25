# Decisiones pendientes

Cosas que aparecieron durante el desarrollo, que **no están decididas** y que no bloquean
el trabajo actual. No son ADRs: un ADR documenta una decisión *tomada*. Esto es la lista de
lo que todavía no lo está.

Cuando una de estas se resuelve → sale de acá y entra como ADR o como tarea del ROADMAP.

---

## 1. Alta de marcas (`brands`)

**Estado**: dirección tomada, sin implementar.

Las marcas no son lista cerrada como las categorías: llega una ração de una marca nueva y
el admin tiene que poder cargarla. Pero un CRUD de marcas con pantalla propia es demasiado
para lo que es.

**Dirección elegida**: alta *inline* en el formulario de producto — el admin escribe la
marca; si no existe, se crea sola. Sin pantalla dedicada.

**Falta definir**: qué pasa con las marcas que quedan sin ningún producto (¿se borran solas?
¿quedan colgando?), y si el campo es un autocompletar sobre las existentes o texto libre
(texto libre garantiza duplicados tipo "Golden" / "golden" / "Golden Foods").

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
