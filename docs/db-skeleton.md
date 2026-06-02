# Esqueleto de tablas (tentativo) — pre-reunión cliente

> **Estado**: borrador. Las ramas marcadas como **🔀 DECISIÓN #n** se cierran con las
> respuestas del cliente en la reunión. Una vez cerradas, se finaliza el schema, se
> regenera `db-model.png` y recién ahí se escribe la migración Flyway.
> No es fuente de verdad todavía.

## Notas transversales (valen para todas las tablas)

- `tenant_id` en toda tabla de dominio (multi-tenant ready — CLAUDE.md §3).
- `public_id` UUID v7 solo en entidades expuestas públicamente; el resto BIGSERIAL interno.
- Los campos `_snapshot` en `order_item` son lo que habilita facturación / top-productos en
  Fase 2 **sin** tabla `events`.
- Estados de pedido y reserva: solo `PENDING / CONFIRMED / CANCELLED` (CLAUDE.md §6).

---

## TENANT / CONFIG

```
tenant
  id              BIGSERIAL PK
  public_id       UUID v7
  nome, endereço, whatsapp_destino
  config (JSONB):
    · capacidade_atendimento      ← 🔀 DECISIÓN #3 (¿número único o por servicio?)
    · anticipacao_min/max_reserva ← regla de reserva (#11)
  created_at
```

## IDENTITY / AUTH

```
admin_user
  id, tenant_id FK
  email, password_hash, role
  created_at
```

## CATALOG

```
category          (Rações, Petiscos, Higiene…)
  id, tenant_id, nome, slug

species           (Cães, Gatos, Aves…)          ← ¿tabla o enum?
  id, tenant_id, nome

brand             ← 🔀 DECISIÓN opcional (¿filtra por marca? #10)
  id, tenant_id, nome

product
  id, tenant_id, public_id UUID v7
  nome, descrição
  brand_id FK?                        ← depende de brand
  stock INT?                          ← 🔀 DECISIÓN #6 (¿maneja stock?)
  price NUMERIC(10,2)?                ← 🔀 DECISIÓN #1: si NO hay variantes, el precio vive acá
  created_at

product_variant   ← 🔀 DECISIÓN #1: SOLO si vende variantes (ração 3/10/15kg)
  id, product_id FK
  nome_variante (ej. "10kg"), price, stock?

product_category  ← 🔀 ¿un producto en varias categorías? (N:N) o FK simple
  product_id, category_id

product_species   ← 🔀 ¿sirve para Cães Y Gatos? (N:N)
  product_id, species_id

product_image     (galería — ya está en scope)
  id, product_id FK, url, orden
```

## BOOKING

```
service           (3 fijos: Banho&Tosa, Tosa Higiênica, Spa Premium)
  id, tenant_id, public_id
  nome, descrição
  price?, duration_min?               ← 🔀 DECISIÓN #2: si NO varía por porte, viven acá

service_price_by_size  ← 🔀 DECISIÓN #2: SOLO si varía por porte
  service_id FK
  porte (pequeno/médio/grande), price, duration_min

business_hours    ← 🔀 DECISIÓN #4 (estructura de horarios)
  id, tenant_id
  dia_semana (0–6)
  abertura, fechamento
  (¿dos rangos por pausa de mediodía? → 2 filas o columnas extra)

business_hours_exception  (feriados, vacaciones, bloqueos)
  id, tenant_id, data, motivo

booking
  id, tenant_id, public_id UUID v7
  service_id FK
  inicio, fim                  (fim = inicio + duración → para solapamientos)
  porte?                       ← 🔀 DECISIÓN #2 y #5 (porte define precio/duración)
  pet_nome?, pet_raça?         ← 🔀 DECISIÓN #5 (¿registra mascota?)
  cliente_nome, cliente_telefone   (sin auth de cliente)
  status        (PENDING/CONFIRMED/CANCELLED)
  created_at, confirmed_at, cancelled_at   ← timestamps para tasa de cancelación futura
```

## ORDERS

```
order
  id, tenant_id, public_id UUID v7
  cliente_nome, cliente_telefone
  endereco_entrega?            ← 🔀 DECISIÓN (¿entrega a domicilio? #9)
  status        (PENDING/CONFIRMED/CANCELLED)
  created_at, confirmed_at, cancelled_at
  (Total = Subtotal — frete "a combinar", no se persiste valor de frete)

order_item
  id, order_id FK
  product_id / product_variant_id FK   ← depende de DECISIÓN #1
  nome_snapshot                (nombre al momento del pedido)
  unit_price_snapshot          ← precio congelado, NO FK al precio actual
  quantidade
```

---

## Puntos de decisión abiertos (resumen)

| # | Decisión | Tabla(s) afectada(s) | Default propuesto |
|---|----------|----------------------|-------------------|
| 1 | ¿Variantes de producto? | `product` vs `product_variant`, `order_item` | Sí (rações por peso) |
| 2 | ¿Precio/duración por porte? | `service` vs `service_price_by_size`, `booking` | Sí (peluquería canina) |
| 3 | ¿Capacidad única o por servicio? | `tenant.config` | Número único (ADR 009) |
| 4 | Estructura de horarios | `business_hours` | 1 rango por día, revisar pausa |
| 5 | ¿Registra mascota? | `booking` (pet_*, porte) | Al menos porte |
| 6 | ¿Maneja stock? | `product.stock`, `product_variant.stock` | No en MVP1 |
| — | ¿Filtra por marca? | `brand`, `product.brand_id` | A confirmar |
| — | ¿Entrega a domicilio? | `order.endereco_entrega` | A confirmar |
| — | ¿Producto en varias categorías/especies? | `product_category`, `product_species` | N:N |
