# Preguntas y respuestas del cliente — FrontPet

> Respuestas recibidas entre junio y julio 2026. Cada ítem muestra la respuesta
> confirmada y el impacto en el código/DB. Lo que está ❌ sigue pendiente.
>
> **Las marcadas con ⚠️ DB GAP** tienen impacto en el schema que aún no está resuelto.

---

## 1. Catálogo de productos

**1.1 — Variantes** ✅
> Sí. Raciones en 3 kg / 10 kg / 15 kg / 20 kg. Tapetes higiénicos en 7 u / 30 u / 60 u
> (no se trabajan ahora pero el modelo ya tiene que soportarlo). Sobre todo en alimentos e higiene.

_Impacto_: `product_variants` en V2 ✅ cubre esto. El precio vive en la variante cuando existe.

---

**1.2 — Stock** ✅
> Sí, mostrar "agotado" cuando no hay stock.

_Impacto_: columna `stock INT` en `products` y `product_variants` en V2 ✅.

---

**1.3 — Categorías y especies** ✅
> Categorías: **Rações / Acessórios / Higiene / Petiscos / Conforto / Brinquedos**
> (Conforto = casas, camas, colchonetes).
> Un mismo producto puede estar en varias categorías y varias especies. Sí es N:N.

_Impacto_: tablas `categories`, `species`, `product_categories`, `product_species` (N:M) en V2 ✅.
Seed de categorías y especies va en V6 (pendiente).

---

**1.4 — Marca** ✅
> Sí, el cliente quiere poder filtrar por marca (Royal Canin, Pedigree, etc.).

_Impacto_: tabla `brands` + `brand_id FK` en `products` en V2 ✅.
Seed de marcas iniciales va en V6 (pendiente — listar marcas reales que maneja FrontPet).

---

**1.5 — Precios especiales / promos** ✅ ⚠️ DB GAP
> Sí. "Promos de tanto por tanto" (descuento porcentual o absoluto) y "promos por día".

_Impacto_: **V2 solo tiene `price`. Falta `price_original NUMERIC(10,2) NULLABLE`** en `products`
y en `product_variants`. Cuando `price_original IS NOT NULL`, el frontend muestra el precio
original tachado y el `price` como precio de oferta.
→ **Agregar en V6 o en migración separada antes de Sprint 3.**

---

**1.6 — Imágenes** ✅
> Una sola foto por producto.

_Impacto_: `main_image_url TEXT` en `products` en V2 ✅. Galería queda para Fase 2.

---

## 2. Servicios y grooming

**2.1 — Precio y duración por tamaño** ✅
> Sí, precio y duración varían por porte (P incluye filhotes / M / G / GG).
> **2 banhos base**: Essencial y Premium.
> **Add-ons**: tosa higiênica, tosa completa, carding (remoción de pelos muertos),
> hidratação, banho antisséptico, banho antipulga. Próximamente cromoterapia.
>
> Preços confirmados (tabela oficial jul/2026):
>
> | Porte | Essencial | Premium |
> |-------|-----------|---------|
> | P     | R$ 49     | R$ 65   |
> | M     | R$ 59     | R$ 75   |
> | G     | R$ 79     | R$ 95   |
> | GG    | R$ 95     | R$ 115  |
>
> Preços e durações dos add-ons: **pendentes** (❌ ver abajo).

_Impacto_: tablas `services` + `service_pricing` en V3 ✅. Seed V5 tiene los preços base ✅.
Durações estimadas en V5 (confirmar con cliente antes del deploy).
Add-ons van en V6__seed_addons cuando se confirmen precios.

---

**2.2 — Capacidad en paralelo** ✅
> 2 animales simultáneos para cualquier servicio.
> Hay 1 perro extra por horario (solo mañana) pero no es el foco del MVP.

_Impacto_: `tenant.config.capacidade_atendimento = 2` en V5 seed ✅.
El caso especial de la mañana queda fuera del MVP1.

---

**2.3 — Profesionales** ✅
> 2 personas trabajan en simultáneo, pero es una característica interna —
> para el sistema da igual quién atiende.

_Impacto_: capacidad simple (número único), sin agenda por profesional ✅. Fase 2 si acaso.

---

**2.4 — Servicios simultáneos** ✅
> Cada turno es un solo servicio base. Los add-ons se suman al mismo turno.

_Impacto_: `appointments.base_service_id` apunta a un servicio BASE. Los add-ons en
`appointment_addons` (N:M) ✅ — V3 lo modela correctamente.

---

## 3. Booking y agenda

**3.1 — Horarios de atención** ✅
> Lunes a viernes: 09:00–17:00. Sábado: 09:00–19:00. Domingo: cerrado.
> No cierra al mediodía.

_Impacto_: `business_hours` seeded en V5 ✅ (activo=FALSE para domingo).

---

**3.2 — Feriados y excepciones** ✅
> Sí, necesita poder bloquear días puntuales.

_Impacto_: tabla `schedule_blocks` en V3 ✅.

---

**3.3 — Anticipación mínima y máxima** ✅
> Cliente: "no hay drama" — sin restricción especial. Se usan defaults razonables.
> Decisión técnica: **mínimo 1 h, máximo 60 días**.
> - 1 h: permite cargar turnos de último momento sin ser restrictivo.
> - 60 días: estándar para servicios locales.

_Impacto_: valores en `tenant.config.anticipacao_min_horas = 1` y
`anticipacao_max_dias = 60`. Hardcodeados como fallback en el servicio Java;
el admin los puede sobrescribir desde el panel sin tocar código.

---

**3.4 — Cómo se confirma un turno hoy** ✅
> El cliente escribe por WhatsApp → FrontPet ofrece horarios disponibles → cliente elige →
> FrontPet lo ingresa al ERP. La app reemplaza al ERP.

_Impacto_: turno nace como `PENDING` (el cliente reserva online) y el admin lo pasa a
`CONFIRMED` desde el panel. Mismo flujo, diferente canal ✅.

---

**3.5 — Cancelaciones** ✅
> Solo por WhatsApp, manejado por el admin. El cliente no cancela solo.

_Impacto_: solo el admin puede cancelar desde el panel (no hay flujo de cancelación pública).
Estados `CANCELLED` + `cancelled_at` en `appointments` ✅.

---

**3.6 — Flujo completo del servicio** ✅
> Cliente → WhatsApp → FrontPet ofrece horarios → cliente elige → admin lo ingresa al sistema.
> Día del turno: recepción del animal, registro, observaciones, posible ajuste del servicio
> (puede agregar add-ons en el momento), baño, fin del servicio.

_Impacto_: el admin puede editar add-ons de un turno existente (cambiar de PENDING a CONFIRMED
puede coincidir con agregar add-ons al momento de confirmar). El campo `tempo_extra` también
se setea en ese momento (primer baño o perro difícil → +20% duración).

---

## 4. Pedidos y WhatsApp

**4.1 — Flujo completo del pedido** ✅
> Cliente hace pedido → FrontPet recibe → ingresa al ERP → pasa a logística → logística
> prepara y entrega.

_Impacto_: `orders` con estados PENDING / CONFIRMED / CANCELLED ✅.

---

**4.2 — Datos del checkout** ✅
> Campos requeridos:
> - Nombre *
> - Número de teléfono *
> - Dirección *
> - Forma de pago *
> - Horario de entrega *
> - Cuál es el pedido (generado por la app)

_Impacto_: `orders` necesita `cliente_nome`, `cliente_telefone`, `endereco_entrega`,
`forma_pagamento`, `horario_entrega_preferido` (texto libre o enum). Revisar V4.

---

**4.3 — Entrega y zonas** ✅
> Gratis hasta 5 km. Cubren Rivera y Livramento completos.
> Más de 5 km: "a combinar".

_Impacto_: modalidad de frete `GRATIS / A_COMBINAR`, sin cálculo automático de distancia.
El total del pedido = subtotal (el frete no suma un número). V4 ✅.

---

**4.4 — Qué necesita el admin para procesar** ✅
> Todo lo de 4.2 — los mismos datos que el cliente ingresa al checkout.

---

## 5. Panel admin

**5.1 — Quién usa el panel y desde dónde** ✅
> Múltiples funcionarios. PC hoy, posible tablet en el futuro.

_Impacto_: desktop-first ✅. Layout responsive pero con prioridad de diseño en pantalla grande.
En MVP1 todos los usuarios tienen rol ADMIN (sin diferenciación).

---

**5.2 — Qué ve primero al abrir el panel** ✅
> Ventas y agendamientos.

_Impacto_: el mini-dashboard prioriza turnos del día en la parte superior, pedidos debajo.

---

**5.3 — Expectativa del panel** ⚠️ CLARIFICAR
> "Pedidos del mes no es interesante al menos para que lo vean todos."

_Impacto_: MVP1 tiene un solo rol (ADMIN), todos los funcionarios ven lo mismo. La frase
sugiere que en el futuro podría querer permisos diferenciados (un empleado no ve facturación).
Por ahora: el panel muestra pedidos del día / semana, sin "pedidos del mes" en el dashboard
principal. Las métricas mensuales van en una pestaña aparte si entran en Sprint 7.
→ **Confirmar**: ¿el panel de Sprint 7 puede mostrar totales mensuales o el cliente prefiere
que ese dato no sea visible para empleados?

---

## 6. Contenido y assets

**6.1 — Logo y paleta oficial** ❌ PENDIENTE
> No respondida todavía.

_Bloquea_: cerrar la paleta definitiva del design system. Los tokens actuales de `globals.css`
son placeholders hasta que el cliente confirme los códigos exactos.

---

**6.2 — Cantidad de productos** ✅
> ~40 productos al arrancar.

_Impacto_: carga manual desde el admin (no hace falta import CSV). 40 productos
es manejable a mano entre el dev y el cliente.

---

**6.3 — Material fotográfico y textos** ❌ PENDIENTE
> No respondida todavía.

_Bloquea_: carga real de contenido. Si no tiene fotos, hay una tarea previa de generación
que no está en el plan.

---

**6.4 — Textos de la marca para la landing** ❌ PENDIENTE
> No respondida todavía.

_Bloquea_: copy de la landing (sección "Sobre nosotros", tagline, etc.).

---

## 7. Técnico y operativo

**7.1 — Exportación de productos** ✅
> Carga manual. Los ~40 productos los cargan entre el dev y el cliente directamente
> desde el panel admin.

_Impacto_: no hace falta import CSV ni herramienta de migración. El admin CRUD
de productos cubre el caso.

---

**7.2 — Quién carga los productos** ✅
> El dev (Sebastián) y el cliente juntos, al momento del deploy.

_Impacto_: el admin tiene que ser usable por alguien sin experiencia técnica.
Priorizar UX simple en el ABM de productos.

---

**7.3 — Número de WhatsApp** ✅
> **+55 55 9672-4124** (E.164: `+555596724124`). FrontPet evalúa comprar un chip dedicado
> para la plataforma — confirmar número definitivo antes del deploy.

_Impacto_: seedeado en V5 ✅. Actualizar si cambia el número.

---

**7.4 — Dominio** ⏳ SPRINT DESPLIEGUE
> Todavía no comprado. Se compra cuando llegue el Sprint Despliegue.

_Impacto_: no bloquea el desarrollo. Necesario antes de configurar Caddy + Cloudflare.

---

**7.5 — Facebook Page y Meta Pixel** ✅ (ID pendiente para Sprint 7)
> Tienen página de Facebook: **FRONT PET**. Ya tienen presencia en Meta.

_Impacto_: el Pixel se conecta al Business Manager existente en Sprint 7.
Pedir el Pixel ID cuando llegue ese sprint — no bloquea ahora.

---

**7.6 — Instagram** ✅
> Confirmado: **@frontpet.br**. Enlazar desde la landing.

---

## Resumen de pendientes

### Bloquean código / DB — resolver antes de Sprint 3
| # | Pendiente | Acción |
|---|-----------|--------|
| 1.5 | **DB GAP: `price_original` en products/variants** | Migración V6 |

### Pendientes de contenido del cliente — no bloquean código
| # | Pendiente | Cuándo |
|---|-----------|--------|
| 6.1 | Logo + paleta oficial (colores exactos) | Antes de cerrar design system |
| 6.3 | Fotos y textos de productos | Antes del Sprint Despliegue |
| 6.4 | Copy de la landing ("sobre nosotros", tagline) | Antes del Sprint Despliegue |
| 2.1 | Duraciones de banhos (estimados en V5) + precios de add-ons | ⚠️ **Venció sin respuesta** — Sprint 5 cerró (2026-07-30) con valores provisorios en `V10__seed_addons_dev.sql` (marcados como tales en su header). No bloqueó el código (ADR 011: el algoritmo es agnóstico a los valores), pero sigue bloqueando que el cliente pueda vender de verdad. Repreguntar ya — idealmente antes de cerrar Sprint 6, y obligatorio antes del hito de cobro del 05/09 |

### A confirmar antes del deploy
| # | Pendiente |
|---|-----------|
| 5.3 | ¿Totales mensuales visibles para todos los empleados? (MVP1 tiene un solo rol) |
| 7.3 | Número WhatsApp definitivo (posible chip nuevo — actualizar V5 seed) |
| 7.5 | Pixel ID de Meta (pedir en Sprint 7) |
