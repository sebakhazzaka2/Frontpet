# ADR 010 — Templates de mensagem pré-formatada para confirmação por WhatsApp

**Estado**: Aceptada
**Fecha**: 2026-05-25
**Sprint**: 2

---

## Contexto

El admin de FrontPet, después de revisar un pedido o agendamento, necesita
comunicarse con el cliente vía WhatsApp para confirmar, cancelar o coordinar
ajustes. El proceso manual sería:

1. Admin lee el pedido en el panel.
2. Abre WhatsApp en otro dispositivo o pestaña.
3. Busca el contacto.
4. Tipea un mensaje desde cero con los datos del pedido (cliente, ID, items,
   modalidade, etc.).
5. Envía.

Eso es lento (~2-3 min por mensaje), inconsistente (cada admin escribe distinto)
y propenso a errores (typeos en el ID, items olvidados).

Las pantallas admin de Pedidos y Agendamentos tienen un CTA principal verde:

- **Pedidos**: "Enviar confirmação no WhatsApp"
- **Agendamentos**: "Confirmar pelo WhatsApp" (al confirmar manualmente)

La pregunta es: ¿cómo se construye el mensaje pre-formateado?

---

## Decisión

**El click en el botão verde abre `wa.me/{telefone}?text={mensagem}` con un
mensaje pre-formateado generado dinámicamente desde los datos del pedido o
agendamento. El texto del mensaje depende del status actual y usa templates
definidos en código (no en DB).**

### Implementación

Los templates viven en `/lib/whatsapp/templates.ts` (o equivalente backend) como
funciones que reciben el objeto del pedido/agendamento y retornan un string ya
formateado.

Flujo:
```ts
function buildPedidoMessage(order: Order): string {
  const templates = {
    PENDING_WHATSAPP: pendingTemplate,
    CONFIRMED: confirmedTemplate,
    CANCELLED: cancelledTemplate,
  }
  return templates[order.status](order)
}

// On click:
const text = encodeURIComponent(buildPedidoMessage(order))
window.open(`https://wa.me/${order.customer.phone}?text=${text}`, '_blank')
```

### Templates de pedidos (3 por status)

**1. Pedido PENDING_WHATSAPP — confirmação inicial:**

```
Olá {nome}! Aqui é da FrontPet 🐾

Recebemos seu pedido #{id}:
{items_lista}

Subtotal: R$ {subtotal}
Modalidade: {modalidade}
{endereco_se_entrega}

Vamos confirmar com você o valor do frete e o horário ideal. Pode me responder
quando puder?
```

**2. Pedido CONFIRMED — confirmação ao cliente:**

```
Olá {nome}! 🐾

Tudo certo com seu pedido #{id}!

Itens confirmados:
{items_lista}

Total: R$ {total}
Modalidade: {modalidade}
{frete_info}
{previsao_entrega_ou_retirada}

Qualquer dúvida, é só me avisar. Obrigado pela preferência!
```

**3. Pedido CANCELLED — aviso de cancelamento:**

```
Olá {nome},

Infelizmente precisamos cancelar seu pedido #{id}.
{motivo_se_houver}

Se quiser, posso te sugerir alternativas ou refazer o pedido em outra data. É só
me avisar!
```

### Templates de agendamentos (3 por status)

**1. Agendamento PENDING — confirmação inicial al cliente:**

```
Olá {nome}! 🐾

Recebemos seu agendamento #{id}:

📅 Data: {data}
⏰ Horário: {hora}
✂️ Serviço: {servico}
🐶 Pet: {pet_nome} ({pet_raca})

Estamos confirmando os detalhes e em breve te aviso se está tudo certo!
```

**2. Agendamento CONFIRMED — confirmação ao cliente:**

```
Olá {nome}! ✨

Está confirmado! Esperamos vocês:

📅 {data} às {hora}
✂️ {servico}
🐶 {pet_nome}

Combinamos: {observacoes_da_loja}

Se precisar reagendar ou cancelar, é só me avisar.
```

**3. Agendamento CANCELLED — aviso de cancelamento:**

```
Olá {nome},

Infelizmente precisamos cancelar o agendamento #{id} do dia {data}.
{motivo_se_houver}

Posso te ajudar a reagendar para outra data? Tenho alguns horários disponíveis
nesta semana!
```

### Reglas de tono y formato

- **Tom**: cálido, próximo, brasileño coloquial pero respetuoso.
- **Idioma**: PT-BR (per ADR 007).
- **Emojis**: permitidos en pequeña cantidad (1-3 por mensaje) para humanizar.
- **Encoding**: el mensaje se URL-encoda con `encodeURIComponent` para que los
  saltos de línea y caracteres especiales pasen al wa.me sin romperse.
- **Personalización**: siempre usar el primer nome do cliente (no "Sr./Sra.").
- **Sin emojis genéricos de loja chic ou luxo** — manter o tom "petshop local".

### Cómo y dónde editar los templates

Los templates viven en código (TypeScript / Java). Editarlos requiere:

1. Cambiar el string en el archivo correspondiente.
2. PR + review.
3. Deploy.

**No hay UI para editar templates en MVP1.** Si el cliente piloto pide cambios
operacionales urgentes (típico: "queria que o cumprimento fosse 'bom dia' em vez
de 'olá'"), el dev hace el cambio en código y deploy. Es ~30 min total.

---

## Alternativas consideradas

### ❌ Templates editables desde el admin

**A favor**:
- Flexibilidad máxima
- Owner cambia el copy sin depender del dev

**En contra**:
- Requer UI de gestión (modal con preview, variables, validación de
  placeholders, etc.) — 1-2 dias de dev
- Risk de templates rotos (placeholders mal escritos, formato inválido)
- Para MVP1 con un solo tenant, el dev cambia los templates en código y deploy
  en menos tiempo que el owner cambiaría desde la UI

**Veredicto**: over-engineering para MVP1. Se evalúa en Fase 2 SaaS multi-tenant
donde cada negocio querría su propio tono.

### ❌ Mensaje manual sin template

**A favor**:
- Cero código, cero mantenimiento

**En contra**:
- Pierde todo el valor de la pantalla admin
- Admin tipea cada vez = inconsistencia + errores + tiempo
- El botão "Enviar confirmação no WhatsApp" deja de tener sentido

**Veredicto**: descarta la utilidad del feature.

### ❌ Templates en DB con interpolación de variables

**A favor**:
- "Más profesional" architecturalmente

**En contra**:
- Adiciona tabla + endpoint + lógica de fetch
- Marginal benefit vs strings en código para un solo tenant
- En Fase 2 multi-tenant se reevaluará

**Veredicto**: complejidad sin payoff. Defer a Fase 2.

### ❌ WhatsApp Business API com automação

**A favor**:
- Templates oficiales aprovados por Meta
- Mensajes enviados programáticamente sin click

**En contra**:
- Requiere aprobación Meta (~2 semanas)
- Costos por mensaje
- Per ADR 003: WhatsApp Business API está marcado como Fase 4

**Veredicto**: fuera de scope MVP1.

---

## Consecuencias

### Positivas

- **Workflow del admin se reduce de ~2-3 min a ~10 seg por mensaje**.
- **Mensajes consistentes** en formato y tono.
- **Menos errores** (IDs, items, valores tipeados a mano).
- **Onboarding del owner casi nulo** — solo aprende a hacer click en el botão verde.
- **Templates versionados** en git → trazabilidad de cambios.

### Negativas

- Si el owner quiere modificar el tono o agregar algo (ej. nueva línea de
  saudação por temporada), depende del dev.
- Si la lista de items es muy larga, el mensaje queda extenso — WhatsApp tiene
  límite de 4096 caracteres pero la lectura en celular pierde calidad pasando de
  ~600.

### Mitigaciones

- Si los items pasan de 10, truncar la lista y agregar "... e mais X itens. Veja
  o pedido completo no comprovante."
- Documentar en `CLAUDE.md` o README dónde están los templates para que cualquier
  dev pueda editarlos rápido.
- Cuando llegue Fase 2 SaaS, mover templates a DB con UI de edição por tenant.

---

## Notas para o futuro

En Fase 2:

- UI de edição de templates por tenant.
- Variables disponibles documentadas (`{nome}`, `{id}`, `{items_lista}`, etc.).
- Preview do mensaje antes de enviar.
- Suporte a múltiplos canais (WhatsApp, SMS, email) com templates por canal.
- A/B testing de mensagens.

En Fase 3+:

- WhatsApp Business API com templates oficiais Meta.
- Confirmação automática de status (cliente responde "SIM" → admin recebe
  notificação).
- Lembretes automáticos pré-turno via WA API.
