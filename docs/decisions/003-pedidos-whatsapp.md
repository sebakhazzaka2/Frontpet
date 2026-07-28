# ADR 003 — Pedidos vía WhatsApp click-to-chat

**Fecha**: 2026-05-17
**Estado**: Aceptada
**Autor**: [tu nombre]

---

## Contexto

El flujo principal de venta del MVP1 es: el cliente arma su carrito en la web y
"envía el pedido" a FrontPet. FrontPet luego confirma manualmente por WhatsApp.

Hay tres formas técnicas de hacer esto:

1. **Click-to-chat** (`https://wa.me/...?text=...`): el navegador abre WhatsApp con
   un mensaje pre-cargado que el cliente solo tiene que enviar.
2. **WhatsApp Business API**: integración oficial con Meta para enviar mensajes
   programáticamente.
3. **Email/SMS al negocio**: notificar por canal alternativo.

## Decisión

**WhatsApp click-to-chat oficial** para MVP1, sin automatización de respuestas.

Flujo concreto:

1. Cliente arma carrito y completa checkout (nombre, WA, modalidad, dirección).
2. Frontend hace `POST /api/v1/orders` con los items y datos del cliente.
3. Backend persiste el pedido con estado `PENDING_WHATSAPP` y retorna el ID.
4. Backend genera el mensaje pre-formateado en el response.
5. Frontend redirige a `https://wa.me/<numero>?text=<mensaje>`.
6. WhatsApp se abre con el mensaje listo para enviar.
7. Cliente envía (o no, pero el pedido ya quedó registrado).
8. FrontPet ve el pedido en el admin con estado `PENDING_WHATSAPP`.
9. FrontPet confirma manualmente al cliente y cambia el estado a `CONFIRMED`.

**Estados del pedido en MVP1**:
- `PENDING_WHATSAPP` — creado en la DB, sin confirmar
- `CONFIRMED` — FrontPet lo aceptó
- `CANCELLED` — no se concretó

## Alternativas consideradas

### ❌ WhatsApp Business API

**A favor**:
- Permite tracking real de envío/entrega del mensaje
- Habilita automatización de respuestas
- Métricas reales del embudo

**En contra**:
- Requiere aprobación de Meta (~2 semanas de proceso)
- Costos por mensaje según el caso
- Configuración compleja para MVP1
- No aporta valor incremental en la primera etapa

### ❌ Email/SMS

**A favor**: Independiente de WhatsApp.

**En contra**:
- FrontPet ya opera por WhatsApp, sería duplicar canales
- Peor experiencia para el cliente
- Costo de SMS

### ❌ Persistir solo si se confirma el envío

**Considerado pero rechazado**: si solo guardamos pedidos cuando el cliente realmente
envía el WhatsApp, perdemos los pedidos abandonados a mitad del flujo. La trazabilidad
mejora si persistimos al hacer click en "enviar pedido", aunque después no se concrete.

## Consecuencias

### Positivas
- Cero costo por mensaje
- Cero fricción para el cliente
- Implementación simple y rápida
- FrontPet opera con su flujo actual de WhatsApp manual
- Pedidos registrados en DB aunque el cliente abandone

### Negativas
- No hay forma de saber programáticamente si el mensaje fue efectivamente enviado
- Sin métricas finas del embudo de WhatsApp (Meta Pixel captura el click, no el envío)
- Cliente puede editar el mensaje pre-cargado antes de enviarlo

### Mitigaciones
- Meta Pixel dispara evento `Contact` al hacer click → métricas básicas
- Confirmación humana por parte de FrontPet asegura calidad del pedido
- Si el cliente no envía el WA, FrontPet puede contactarlo proactivamente con los
  datos del pedido (nombre + WA + items) que sí quedaron en la DB

## Actualización 2026-07-09 — Campos de checkout, forma de pagamento y frete

Al relevar la operación real con el cliente, el checkout y las reglas de pedido se precisaron.
Dos puntos **contradicen reglas escritas en CLAUDE.md sección 6** y se resuelven acá:

### Datos que se capturan en el checkout
Nombre, teléfono, **endereço (obrigatório)**, **forma de pagamento**, **horário de entrega**,
e os itens do pedido.

### Forma de pagamento — se captura, NO es pago online
CLAUDE.md dice "❌ No mostrar métodos de pago". Esa regla apuntaba a **no crear expectativa
de pago online** (checkout con PIX/cartão que cobra en el momento). Lo que el cliente pide es
distinto: que el comprador **indique cómo va a pagar al recibir** (dato operativo para
logística). **Decisión**: se captura `forma_pagamento` como texto/enum en el pedido. **No hay
cobro online, no hay pasarela.** El pago sigue siendo offline al entregar/retirar.
→ *CLAUDE.md §6 debe actualizarse para reflejar este matiz.*

### Frete — regra concreta, no "sempre a combinar"
CLAUDE.md dice "frete siempre 'A combinar', nunca valor numérico". La regla real del cliente:
- **Grátis até 5km.**
- **Mais de 5km: a combinar.**
- Llegan a toda Rivera y Livramento.

**Decisión**: el pedido guarda la modalidad de frete resultante (`GRATIS` / `A_COMBINAR`), no
un valor calculado (el sistema no calcula distancia en MVP1; la determina el admin/logística).
En el total sigue valiendo `Total = Subtotal` (el frete no suma un número al total).
→ *CLAUDE.md §6 debe actualizarse.*

### Estado del pedido
Se alinea con CLAUDE.md §6: `PENDING / CONFIRMED / CANCELLED` (sin `PENDING_WHATSAPP`, que
este ADR usaba antes). El estado inicial es `PENDING`.

## Actualización 2026-07-28 — Modalidade Entrega/Retirada (Bloque 0, Sprint 4)

Al fijar el contrato de `POST /api/v1/orders` aparecieron dos drifts entre Stitch
("Sua Sacola") y lo confirmado acá + `V4__orders.sql`, resueltos en el Bloque 0:

### Modalidade Retirada — no tiene columna propia
Stitch ofrece **Entrega/Retirada** como radio button en el checkout. `V4__orders.sql` solo
tiene `endereco_entrega` (obligatorio) y `frete_mode` (`GRATIS`/`A_COMBINAR`), sin lugar para
"retirar en el local" — no se agrega columna nueva por esto. **Decisión**: `modalidade` vive
en el DTO (`ModalidadeEntrega`), no en la tabla. `OrderServiceImpl` la mapea al persistir:
- `RETIRADA` → `enderecoEntrega = "Retirada na loja"`, `freteMode = GRATIS`.
- `ENTREGA` → `enderecoEntrega` = el que mandó el cliente (obligatorio), `freteMode = GRATIS`
  por defecto (ver punto siguiente).

### `frete_mode` no lo elige el cliente
Reafirma lo ya dicho arriba ("el sistema no calcula distancia en MVP1; la determina el
admin/logística"): el checkout **no** le pregunta al cliente si es `GRATIS` o `A_COMBINAR`.
Todo pedido con `ENTREGA` nace en `GRATIS`; si la dirección real supera los 5km, FrontPet lo
ajusta manualmente por WhatsApp. **No hay UI de admin para editar `frete_mode` en Sprint 4**
— deuda conocida, documentada en `docs/pending-decisions.md`.

### Form real: 6 campos, no 3
Stitch dibuja el form de "Sua Sacola" con solo 3 campos (nome, telefone, modalidade). El form
real (`CreateOrderRequest`) tiene 6: se agregan `enderecoEntrega`, `formaPagamento` y
`horarioEntrega` porque `preguntas-cliente.md` §4.2 y esta misma actualización del ADR los
piden explícitamente, y `V4__orders.sql` los tiene `NOT NULL`. Al portar la pantalla
(Bloque B), el form extiende el layout de Stitch con esos 3 campos adicionales — no es una
pantalla nueva, es más campos sobre la misma.

---

## Notas para el futuro

Cuando se justifique (volumen alto de pedidos + necesidad de métricas reales):

- Migración a WhatsApp Business API (Fase 4)
- Recordatorios automáticos a clientes que no confirmaron
- Respuestas automáticas a preguntas frecuentes
- Confirmaciones automáticas con bot
- Métricas reales del funnel completo

**Estimación de Fase 4 con WhatsApp API**: 3-4 semanas + costo recurrente Meta.
