# Contratos JSON — Booking API (Sprint 5)

Publicado el día 1 del Sprint 5, antes de codear el bloque C, para que Sprint 6 pueda tipar
`frontend/lib/data/` (ADR 013) contra shapes fijas en vez de esperar al backend terminado.
**Sujeto a ajuste si un caso de borde de los tests de integración lo exige** — si cambia algo acá,
avisar antes de que Sprint 6 empiece a consumirlo.

Convención: URLs kebab-case, JSON camelCase, PT-BR en mensajes de error, `/api/v1/...`
(CLAUDE.md §5). IDs públicos son UUID v7 (`publicId`); `serviceId` es el `BIGSERIAL` interno de
`services` — no lleva `public_id` (ADR 020, `ServiceOffering` no tiene UUID público).

---

## Público

### `GET /api/v1/services`

Catálogo de banhos base + adicionais, con tarifa por porte. Sin autenticación.

```json
[
  {
    "id": 1,
    "type": "BASE",
    "nome": "Banho Essencial",
    "descricao": "Limpeza completa, secagem profissional e perfume.",
    "active": true,
    "pricing": [
      { "size": "P", "price": 49.00, "durationMinutes": 45 },
      { "size": "M", "price": 59.00, "durationMinutes": 60 },
      { "size": "G", "price": 79.00, "durationMinutes": 90 },
      { "size": "GG", "price": 95.00, "durationMinutes": 120 }
    ]
  },
  {
    "id": 3,
    "type": "ADDON",
    "nome": "Tosa Higiênica",
    "descricao": "Aparação das áreas íntimas, patas e focinho.",
    "active": true,
    "pricing": [
      { "size": "P", "price": 15.00, "durationMinutes": 15 }
    ]
  }
]
```

Solo `active = true`. El admin edita estos valores (Bloque F, Sprint 6); nunca crea/borra (ADR 009).

### `GET /api/v1/availability?baseServiceId=&porte=&addonIds=&data=`

- `baseServiceId`: id de un `service` con `type=BASE` (400 si no lo es).
- `porte`: `P|M|G|GG`.
- `addonIds`: opcional, csv de ids `type=ADDON` (ej. `3,5`).
- `data`: `YYYY-MM-DD`.

```json
{
  "data": "2026-08-05",
  "duracaoTotalMinutes": 75,
  "precoTotal": 74.00,
  "indisponibilidade": null,
  "slots": [
    { "horario": "09:00", "disponivel": false },
    { "horario": "09:30", "disponivel": true },
    { "horario": "10:00", "disponivel": true }
  ]
}
```

Día sin nada ofrecido — `slots` viene vacío y `indisponibilidade` explica por qué
(ver ADR 020 §6):

```json
{
  "data": "2026-08-09",
  "duracaoTotalMinutes": 75,
  "precoTotal": 74.00,
  "indisponibilidade": "DIA_INATIVO",
  "slots": []
}
```

`indisponibilidade` ∈ `DIA_INATIVO | BLOQUEADO | FORA_DA_JANELA | null`.

### `POST /api/v1/appointments`

Body — el cliente nunca manda precio ni duración, se recalculan server-side (ADR 020):

```json
{
  "baseServiceId": 1,
  "addonIds": [3],
  "porte": "M",
  "data": "2026-08-05",
  "horario": "09:30",
  "clienteNome": "Ana Souza",
  "clienteTelefone": "+55 55 99123-4567",
  "petNome": "Thor",
  "petRaca": "Vira-lata",
  "observacoes": null,
  "honeypot": ""
}
```

`honeypot` sigue el mismo patrón anti-bot de `CreateOrderRequest` (ADR de la tarea 4.15).

Response `201`:

```json
{
  "publicId": "01924f10-...",
  "status": "PENDING",
  "baseServiceNome": "Banho Essencial",
  "addons": [{ "nome": "Tosa Higiênica", "priceSnapshot": 20.00, "durationSnapshot": 20 }],
  "porte": "M",
  "startAt": "2026-08-05T09:30:00-03:00",
  "endAt": "2026-08-05T10:39:00-03:00",
  "basePriceSnapshot": 59.00,
  "totalPriceSnapshot": 79.00,
  "totalDurationMinutes": 80,
  "tempoExtra": false,
  "clienteNome": "Ana Souza",
  "petNome": "Thor"
}
```

Errores: `409` (sin cupo) y `400` (fuera de grilla/horario/ventana) devuelven el `ApiError`
estándar (`common/ApiError`) con mensaje en PT-BR. `429` con `Retry-After` si supera el rate
limit (5.8).

### `GET /api/v1/appointments/{publicId}` — vista pública reducida

Mismo shape que el `201` de arriba (`AppointmentDetail`): `publicId`, `status`, servicio +
adicionais, porte, horarios, precios, `tempoExtra`, `clienteNome` y `petNome`.

⚠️ **Sin `clienteTelefone`** — decidido al codear el Bloque E (Sprint 5): el UUID v7 del link
no es adivinable, pero por sí solo no es garantía de que no se filtre (queda en el historial
de WhatsApp del cliente, en logs de proxies). `clienteNome` sí se expone porque sin él el
cliente no podría confirmar "sí, es mi reserva". Riesgo abierto documentado en
`docs/pending-decisions.md` §11 — no bloqueante, revisar si MVP1 agrega auth de clientes
finales (Fase 2).

---

## Admin (requiere cookie JWT, `401` sin ella)

### `GET /admin/appointments?data=&status=` ou `?desde=&hasta=&status=`

`data` (día único) e `desde`/`hasta` (rango, ambos obligatórios se algum vier) são mutuamente
excludentes — se vier `data`, gana ese filtro. Sem nenhum dos dois, devuelve todo el histórico
del tenant filtrado solo por `status` (agregado Bloque A, Sprint 6, para el listado de "próximos
7 días" del admin).

```json
[
  {
    "publicId": "01924f10-...",
    "status": "PENDING",
    "startAt": "2026-08-05T09:30:00-03:00",
    "endAt": "2026-08-05T10:39:00-03:00",
    "clienteNome": "Ana Souza",
    "clienteTelefone": "+55 55 99123-4567",
    "petNome": "Thor",
    "baseServiceNome": "Banho Essencial",
    "addonsNomes": ["Tosa Higiênica"],
    "totalPriceSnapshot": 79.00,
    "tempoExtra": false
  }
]
```

### `POST /admin/appointments` — turno manual (ADR 021, Bloque A do Sprint 6)

Mesmo body de `POST /appointments` (público), sem `honeypot` — não aplica atrás do login:

```json
{
  "baseServiceId": 1,
  "addonIds": [3],
  "porte": "M",
  "data": "2026-08-05",
  "horario": "09:17",
  "clienteNome": "Ana Souza",
  "clienteTelefone": "+55 55 99123-4567",
  "petNome": "Thor",
  "petRaca": "Vira-lata",
  "observacoes": null
}
```

Relaxa 3 restrições do turno público (ADR 021): não precisa cair na grilha de 30 min, persiste
mesmo superando a capacidade e mesmo em dia bloqueado/fora de horário — em vez de `409`/`400`,
devuelve `201` com `avisos` preenchido. **Nunca** relaxa o cálculo de preço/duração — segue
server-side, igual ao turno público.

Response `201` — mesmo shape de `AdminAppointmentDetail` mais `avisos`:

```json
{
  "publicId": "01924f10-...",
  "status": "PENDING",
  "startAt": "2026-08-05T09:17:00-03:00",
  "endAt": "2026-08-05T10:17:00-03:00",
  "clienteNome": "Ana Souza",
  "clienteTelefone": "+55 55 99123-4567",
  "petNome": "Thor",
  "baseServiceNome": "Banho Essencial",
  "addonsNomes": ["Tosa Higiênica"],
  "totalPriceSnapshot": 79.00,
  "totalDurationMinutes": 80,
  "tempoExtra": false,
  "avisos": ["Este horário supera a capacidade (2)."]
}
```

`avisos: []` (nunca `null`) se nada disparar aviso. Continua só falhando `400` se o
`baseServiceId`/`addonIds`/`porte` formarem um combo inválido — o admin nunca manda preço.

### `PATCH /admin/appointments/{publicId}/status`

```json
{ "status": "CONFIRMED" }
```

Transiciones: `PENDING → CONFIRMED|CANCELLED`, `CONFIRMED → CANCELLED`. No-op idempotente si ya
está en ese estado (mismo patrón que `orders/OrderServiceImpl`).

### `PATCH /admin/appointments/{publicId}/tempo-extra`

```json
{ "tempoExtra": true }
```

Response — recalcula duración (nunca precio, ADR 011), avisa sin bloquear si supera capacidad:

```json
{
  "publicId": "01924f10-...",
  "totalDurationMinutes": 108,
  "endAt": "2026-08-05T11:18:00-03:00",
  "aviso": "Este horário supera a capacidade (2) às 10:30 com este novo horário de término."
}
```

`aviso: null` si no hay solapamiento nuevo.

### `GET /admin/services` — todos, incluidos los inativos (Bloque F, Sprint 6)

Mismo shape que el `GET /api/v1/services` público, pero sin filtrar `active` — el admin
necesita poder ver y reactivar los serviços desativados.

### `PUT /admin/services/{id}` — solo edición (405 en POST/DELETE, ADR 009)

```json
{
  "nome": "Banho Essencial",
  "descricao": "Limpeza completa, secagem profissional e perfume.",
  "active": true,
  "pricing": [
    { "size": "P", "price": 52.00, "durationMinutes": 45 },
    { "size": "M", "price": 62.00, "durationMinutes": 60 },
    { "size": "G", "price": 82.00, "durationMinutes": 90 },
    { "size": "GG", "price": 98.00, "durationMinutes": 120 }
  ]
}
```

### `GET /admin/business-hours` — los 7 días existentes (Bloque F, Sprint 6)

Mismo shape del array que devuelve el `PUT` de abajo, ordenado por `diaSemana` — necesario
para prellenar el form de horários del admin.

### `PUT /admin/business-hours` — upsert de los 7 días

```json
[
  { "diaSemana": 1, "activo": true, "abertura": "09:00", "fechamento": "17:00", "pausaInicio": null, "pausaFin": null },
  { "diaSemana": 7, "activo": false, "abertura": "09:00", "fechamento": "17:00", "pausaInicio": null, "pausaFin": null }
]
```

Validación server-side en PT-BR: `abertura < fechamento`, pausa coherente si viene.

### `GET /admin/schedule-blocks` — bloqueios existentes, ordenados por `dataDesde` (Bloque F, Sprint 6)

```json
[{ "id": 1, "dataDesde": "2026-09-07", "dataHasta": "2026-09-07", "motivo": "Feriado — Independência" }]
```

Necesario para listar los bloqueios en el admin y poder borrarlos por `id`.

### `POST /admin/schedule-blocks` / `DELETE /admin/schedule-blocks/{id}`

```json
{ "dataDesde": "2026-09-07", "dataHasta": "2026-09-07", "motivo": "Feriado — Independência" }
```

⚠️ Bloqueo por **día completo**, no por rango horario (ADR 020 — limitación conocida, mitigación
real es el turno manual del admin, implementado en Sprint 6 — ver `pending-decisions.md` §10).

---

## Notas para Sprint 6

- `horario` en requests/responses es `HH:mm` local (`America/Sao_Paulo`); `startAt`/`endAt` son
  ISO-8601 con offset explícito, nunca UTC pelado — evita que el frontend tenga que reconvertir
  zona horaria para mostrar la hora correcta.
- El wizard de 3 pasos (6.1-6.5) consume `GET /services` → `GET /availability` → `POST
  /appointments`, en ese orden — no hay endpoint combinado.
- Si algún nombre de campo cambia durante la implementación real (bloques C-G), este documento
  se actualiza en el mismo PR que lo cambia — no queda como fuente de verdad congelada si diverge
  del código.
