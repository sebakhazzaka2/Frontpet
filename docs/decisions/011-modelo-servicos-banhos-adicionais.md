# ADR 011 — Modelo de serviços: banhos base + adicionais, preço/duração por porte

**Estado**: Aceptada
**Fecha**: 2026-07-09
**Sprint**: 2
**Reemplaza**: la sección "Serviços" de [ADR 009](./009-servicos-fixos-capacidade-simples.md)

---

## Contexto

El [ADR 009](./009-servicos-fixos-capacidade-simples.md) asumía "3 serviços fixos" con un
único `duration_minutes` plano por servicio. Al relevar la operación real de FrontPet, el
modelo resultó bastante más rico:

- **2 banhos base**:
  - **Esencial**: baño completo.
  - **Premium**: incluye lo del esencial + corte de unha + limpeza dental + spray bucal +
    limpeza de ouvidos. (Son parte del premium, no adicionais sueltos.)
- **Adicionais** (opcionais, se suman a un banho base): tosa higiênica, tosa completa,
  carding (remoção de pelos mortos), hidratação, banho antisséptico, banho antipulga.
  Próximamente cromoterapia.
- **Preço y duração varían por porte**: **P** (inclui filhotes), **M**, **G**, **GG**.
- El cliente mencionó que el **tipo de pelo** también afecta la duração. Se decidió **NO**
  modelarlo como driver de preço en MVP1 (queda como dato del animal); el modelo se diseña
  de forma que agregarlo después sea una columna más, sin romper nada (ver "Extensibilidad").
- **Un turno = 1 banho base + N adicionais.** No hay turnos con dos banhos base.

Precios y duraciones concretas **se cargan más adelante**, cuando se desarrolle el módulo de
booking. Este ADR fija la **estructura**, no los valores.

---

## Decisión

**Un único catálogo `services` con un `type` (`BASE` | `ADDON`), y una tabla de tarifas
`service_pricing` que resuelve preço + duração por porte.** El turno referencia 1 servicio
base y, opcionalmente, varios adicionais vía tabla intermedia.

### Estructura conceptual

```
services
  - id, tenant_id
  - type            BASE | ADDON
  - name            ("Banho Esencial", "Tosa Higiênica", …)
  - description
  - active          (soft-delete: se desactiva, no se borra)

service_pricing     -- resuelve el "por porte"
  - id, service_id (FK)
  - size            P | M | G | GG
  - price
  - duration_minutes
  - UNIQUE (service_id, size)

appointment                       -- 1 banho base por turno
  - ... , base_service_id (FK a services type=BASE)
  - size            (el porte del animal en ese turno; congela qué tarifa aplica)

appointment_addon                 -- N adicionais por turno (N:M)
  - appointment_id (FK)
  - service_id     (FK a services type=ADDON)
```

- La **duración total del turno** (lo que ocupa el cupo en el cálculo de slots del
  [ADR 005](./005-slots-dinamicos.md)) = `duration_minutes` del banho base para ese porte
  **+** suma de `duration_minutes` de cada adicional para ese porte.
- El **preço total** = preço del base por porte + suma de adicionais por porte. Se
  **snapshotea** en el turno al confirmar (mismo criterio que `order_item`), para que un
  cambio de tarifa futuro no altere turnos históricos.

### Por qué un solo catálogo con `type` y no dos tablas

Base y adicional comparten exactamente los mismos atributos (nombre, descripción, activo,
tarifa por porte). Separarlos en dos tablas duplicaría `service_pricing`. Un `type`
discrimina y mantiene una sola tabla de tarifas. Si en Fase 2 divergen mucho, se separan.

---

## Alternativas consideradas

### ❌ `duration_minutes` y `price` planos en `services` (modelo ADR 009 original)
No soporta variación por porte. Descartado por la operación real.

### ❌ Columnas `price_p, price_m, price_g, price_gg` en `services`
Denormaliza el porte en columnas. Rompe al agregar un porte nuevo o el tipo de pelo (habría
que ALTER TABLE por cada dimensión). La tabla `service_pricing` por filas escala mejor.

### ❌ Adicionais como booleanos en `appointment` (chk_corte_unha, chk_hidratacao…)
Hardcodea el catálogo de adicionais en columnas. Contra la regla de no hardcodear y no
permite que el admin active/desactive adicionais. Descartado.

---

## Actualización 2026-07-25 — `tempo_extra`: +20% de duração (admin)

Se agrega `appointments.tempo_extra BOOLEAN DEFAULT FALSE`. Lo marca **el admin**, al
crear/confirmar el turno — nunca el cliente en el booking público — para casos de **primeira
vez do cão sendo banhado** o **cão difícil de banhar**.

Cuando `tempo_extra = TRUE`, `total_duration_minutes` (que ya existía) se calcula:

```
base  = duration(banho_base, porte) + Σ duration(adicionais, porte)
total = tempo_extra ? round(base * 1.2) : base
```

No se agrega una columna separada para "duración sin el extra": el desglose pre-multiplicador
ya es reconstruible desde `base_price_snapshot` + `appointment_addons.duration_snapshot` por
línea, así que guardar un tercer número sería redundante.

**Caso borde aceptado, no validado en MVP1**: si el admin marca `tempo_extra` en un turno que
ya estaba creado (en vez de al momento de confirmarlo), la duración nueva podría superponerse
con el siguiente turno agendado. Se resuelve igual que la mitigación del ADR 012 (bloqueo/ajuste
manual por el admin) — no amerita una validación automática de conflicto para este caso.

## Extensibilidad (tipo de pelo, futuro)

Cuando se valide que el tipo de pelo pesa en preço/duração, se agrega a `service_pricing`:

```sql
ALTER TABLE service_pricing ADD COLUMN coat_type VARCHAR;  -- nullable
-- y la UNIQUE pasa a (service_id, size, coat_type)
```

Las tarifas existentes quedan con `coat_type = NULL` (aplican a cualquier pelo) y las nuevas
lo especifican. Cero breaking change. Por eso se eligió tarifas por filas y no por columnas.

---

## Consecuencias

### Positivas
- Soporta la operación real (banhos base + adicionais + porte) sin hardcodear.
- Extensible a tipo de pelo (o cualquier dimensión de tarifa) sin reescribir.
- Reutiliza el criterio de snapshot de precio de orders → consistencia en todo el sistema.

### Negativas
- El cálculo de duración de un turno deja de ser un campo y pasa a ser una suma (base +
  adicionais). Hay que testearlo bien contra el slot calculation del ADR 005.
- Cargar el catálogo inicial + tarifas por porte es más trabajo de seed que 3 filas planas.

### Notas
- Capacidad sigue siendo el número plano del ADR 009 (= 2). Sin cambios.
- El "servicio extra 1 pet/horario solo de mañana" que mencionó el cliente **no** se modela
  en MVP1 (no es el foco). Si se necesita, es un `capacity_override` por servicio (ADR 009
  ya previó esa mitigación).
