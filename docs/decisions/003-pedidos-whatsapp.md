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

## Notas para el futuro

Cuando se justifique (volumen alto de pedidos + necesidad de métricas reales):

- Migración a WhatsApp Business API (Fase 4)
- Recordatorios automáticos a clientes que no confirmaron
- Respuestas automáticas a preguntas frecuentes
- Confirmaciones automáticas con bot
- Métricas reales del funnel completo

**Estimación de Fase 4 con WhatsApp API**: 3-4 semanas + costo recurrente Meta.
