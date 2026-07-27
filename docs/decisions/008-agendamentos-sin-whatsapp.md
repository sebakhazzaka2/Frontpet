# ADR 008 — Agendamentos persisten en DB sin abrir WhatsApp en el submit

**Estado**: Aceptada
**Fecha**: 2026-05-25
**Sprint**: 2

---

## Contexto

Cuando estábamos diseñando el flujo de agendamento (pantalla `/agendar`), llegamos al
final del Step 3 ("Seus dados") y la pregunta era qué hace el botón "Confirmar".

El instinto inicial fue replicar el patrón del checkout de productos definido en
[ADR 003](./003-pedidos-whatsapp.md): persistir en DB **y** abrir `wa.me` con un mensaje
pre-formateado. Eso daba "doble canal de aviso" (DB + WhatsApp del cliente al admin).

Al pensarlo más en frío, **el caso de agendamento no es análogo al de pedidos**:

- En **pedidos**, el carrito vive solo en `sessionStorage` antes del submit. El admin
  no se entera de nada hasta que el cliente envía el WhatsApp. El mensaje es el único
  canal de detalle de lo que pidió.
- En **agendamentos**, la plataforma **ya tiene un canal de verdad**: la DB + el panel
  admin `/admin/agendamentos`. Los slots se calculan dinámicamente (ver
  [ADR 005](./005-slots-dinamicos.md)) y el booking queda con estado `PENDING` listo
  para que el admin lo revise.

Forzar al cliente a enviar también un WhatsApp **agrega ruido y genera una falsa
sensación de control**: el cliente puede pensar que ya confirmó cuando en realidad la
confirmación la da el admin, y si por algún motivo no envía el WhatsApp (cierra la
pestaña, no tiene la app instalada, cancela el redirect), el sistema queda en un
estado raro donde el booking existe pero parece "incompleto".

---

## Decisión

**Los agendamentos persisten en DB con estado `PENDING` al hacer click en
"Confirmar agendamento". No se abre `wa.me` en ningún paso del flujo del cliente.**

Flujo concreto:

1. Cliente completa los 3 pasos del wizard en `/agendar`.
2. Frontend hace `POST /api/v1/bookings` con: `serviceId`, `date`, `time`, `tutorName`,
   `tutorPhone`, `petName`, `petBreed`, `notes`.
3. Backend:
   - Valida que el slot esté disponible (cálculo dinámico, ver ADR 005).
   - Persiste el booking con estado `PENDING`.
   - Retorna `bookingId` (UUID v7) y resumen.
4. Frontend redirige a `/agendamento/confirmacao` que muestra:
   - Mensaje de éxito
   - Código de reserva (`bookingId` truncado a algo legible, ej. `#FP-1024`)
   - Resumen del agendamento (servicio, fecha, hora, tutor, pet)
   - Status visible: "Aguardando confirmação"
   - Copy claro: "Nosso time vai entrar em contato pelo WhatsApp para confirmar
     o horário em breve."
5. Admin abre `/admin/agendamentos`, ve el nuevo `PENDING`, decide:
   - **Confirmar** → cambia estado a `CONFIRMED`. Opcionalmente envía un WhatsApp al
     cliente desde su propio celular para confirmar.
   - **Cancelar** → cambia estado a `CANCELLED`. Idealmente avisa al cliente por WA.
   - **Reagendar** → modifica fecha/hora (Fase 2 lo cubre con flujo formal; en MVP1
     el admin puede editar el booking directo en el panel).

**Todos los cambios de estado son responsabilidad del admin desde el panel.** El
cliente no puede auto-modificar ni cancelar su agendamento en MVP1.

### Estados del booking en MVP1

- `PENDING` — creado por el cliente, esperando que el admin lo revise
- `CONFIRMED` — admin lo aceptó
- `CANCELLED` — admin lo canceló (por conflicto, por pedido del cliente, etc.)

> Sin estados intermedios (`PENDING_WHATSAPP`, `RESCHEDULED`, etc.) en MVP1.
> Se agregan en Fase 2 si la operación lo justifica.

---

## Alternativas consideradas

### ❌ Replicar ADR 003 (persistir + abrir wa.me)

**A favor**:
- Coherencia visual entre los dos flujos
- "Doble canal" de aviso para el admin

**En contra**:
- Genera **falsa sensación de control** en el cliente — puede creer que ya confirmó
  cuando en realidad no lo hizo
- Si el cliente no envía el WhatsApp, el booking queda en un estado ambiguo
- El admin se confunde si recibe el WA del cliente pero el booking ya estaba en DB
  (lee dos veces lo mismo)
- En productos el WhatsApp **es necesario** porque la DB no sabe qué pidió
  (carrito en sessionStorage). En bookings la DB **ya tiene todo** — el WA es
  redundante.
- Más fricción para el cliente sin valor incremental real.

**Veredicto**: la simetría con ADR 003 no justifica el ruido. Los casos son
estructuralmente distintos.

### ❌ Solo abrir wa.me (sin persistir)

**A favor**: simple.

**En contra**:
- Sin DB, no podés calcular slots disponibles → doble-booking inevitable
- Sin DB, el panel admin `/admin/agendamentos` no existe
- Va explícitamente contra ADR 005 (slots dinámicos requieren DB)
- Va contra CLAUDE.md sección 7 (admin debe ver turnos del día + próximos 7 días)

**Veredicto**: rompe la arquitectura.

### ❌ Notificación automática al admin (Slack/email/WA-API)

**A favor**: el admin se entera en tiempo real sin tener que mirar el panel.

**En contra**:
- WhatsApp Business API: requiere aprobación de Meta + costos por mensaje
  (ver ADR 003)
- Slack/email: agrega infraestructura externa que no existe en MVP1
- El panel admin **ya da visibilidad en tiempo real** si el admin lo tiene abierto
- El admin es una sola persona y opera en horario comercial — abrir el panel
  al empezar el día y revisarlo de tanto en tanto es suficiente

**Veredicto**: over-engineering para MVP1. Se agrega en Fase 2+ si el volumen
de turnos lo justifica.

---

## Consecuencias

### Positivas

- Flujo del cliente más simple y honesto: una sola acción, una sola pantalla
  de confirmación.
- Sin estados intermedios ambiguos (cliente sin enviar WA con booking pendiente).
- El admin tiene **un solo canal de verdad** (el panel). Menos confusión.
- Reduce dependencia de WhatsApp click-to-chat al mínimo (queda solo para productos,
  donde realmente aporta).
- Permite que la página de confirmación sea fully informative — código de reserva,
  status, próximos pasos.

### Negativas

- El admin **debe abrir el panel proactivamente** para enterarse de nuevos turnos.
  Si abre el panel después de varias horas, el cliente puede sentir que "lo
  abandonaron".
- Sin WhatsApp en el submit, el admin no recibe un mensaje del cliente con sus
  datos directamente en la app de WhatsApp. Si necesita los datos rápido, tiene
  que abrir el panel.
- Falta de notificación automática es un riesgo en horario de alta demanda.

### Mitigaciones

- **Recordatorio operativo al cliente piloto**: dejar abierto el panel admin en
  un dispositivo dedicado durante el horario de atención. Es un hábito de 5 min
  por día, no requiere infraestructura.
- **Status visible en la página de confirmação**: el cliente entiende que está
  PENDING y que el admin va a contactarlo — gestión de expectativa.
- **SLA implícito**: la página de confirmación dice "vamos confirmar em breve".
  Cuando haya datos, definir un número (ej. "em até 30 min em horário comercial")
  y ponerlo en el copy.
- **En Fase 2**, evaluar agregar notificación admin (Slack webhook, email
  transaccional, push web) cuando el volumen lo justifique.

---

## Diferencias clave con ADR 003 (pedidos)

| Aspecto | Pedidos (ADR 003) | Agendamentos (ADR 008) |
|---|---|---|
| Carrito antes del submit | `sessionStorage` (no persistido) | — (datos en form state) |
| Submit hace | POST + abre wa.me | POST solamente |
| Necesita WhatsApp para que admin se entere | Sí (solo canal con detalle) | No (DB + panel cubren todo) |
| Estados | `PENDING_WHATSAPP / CONFIRMED / CANCELLED` | `PENDING / CONFIRMED / CANCELLED` |
| Confirmación del cliente | Envía mensaje pre-formateado | Página de confirmação con código de reserva |
| Confirmación final | Admin lee el WhatsApp + cambia estado | Admin abre panel + cambia estado |

---

## Notas para el futuro

Cuando se justifique (volumen alto + necesidad de respuesta más rápida al cliente):

- **Notificación automática al admin** cuando entra un nuevo `PENDING` (Slack webhook
  o email transaccional con Resend/Postmark) — Fase 2.
- **Auto-confirmação** de bookings cuando el admin tiene un horario "libre y
  confiable" configurado — Fase 2.
- **Cancelamento / reagendamiento por el cliente** desde un link mágico enviado por
  email/WhatsApp — Fase 2.
- **Recordatorios automáticos pre-turno** (24h antes / 1h antes) por WhatsApp
  Business API — Fase 3.

**Estimación de Fase 2 con notificaciones automáticas**: 1 sprint adicional + setup
de servicio transaccional.
