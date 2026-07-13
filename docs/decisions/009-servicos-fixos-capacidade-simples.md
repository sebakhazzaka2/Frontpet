# ADR 009 — Serviços fijos en DB seed + capacidade simples en lugar de profissionais nominais

**Estado**: Parcialmente reemplazada — ver Actualización 2026-07-09
**Fecha**: 2026-05-25
**Sprint**: 2

---

## Actualización 2026-07-09 (post-respuestas del cliente)

Tras relevar la operación real de FrontPet, **el catálogo concreto de servicios de este ADR
quedó obsoleto**. Ya no son "3 serviços fixos" (Banho & Tosa, Tosa Higiênica, Spa Premium),
sino un modelo de **2 banhos base + adicionais con preço/duração por porte**. Ese modelo se
documenta en **[ADR 011](./011-modelo-servicos-banhos-adicionais.md)**, que reemplaza la
sección "Serviços" de este documento.

**Lo que de este ADR SIGUE VIGENTE** (no cambia):
- **Capacidade simples = un único número por tenant** (no profissionais nominais). Confirmado
  con el cliente: 2 atendimentos en simultáneo para cualquier servicio. Sin agenda por
  profesional en MVP1.
- **Admin edita, no cria ni deleta** servicios desde la UI.
- **Horários con full CRUD** y bloqueos por excepción.

**Lo que queda obsoleto** (ver ADR 011):
- La lista concreta de "3 serviços fixos".
- El supuesto de un único `duration_minutes` plano por servicio → ahora preço y duração
  dependen del **porte** (P/M/G/GG).

---

## Contexto

Al diseñar la pantalla de gestión de serviços + horários del admin (Prompt 11 del flujo
Stitch), apareció la pregunta: ¿qué nivel de CRUD necesitamos para serviços y profissionais
en MVP1?

CLAUDE.md sección 7 dice literalmente "CRUD de servicios y horarios" como parte del scope
admin. Una lectura estricta sugiere:

- Admin puede **crear** nuevos serviços
- Admin puede **crear** nuevos profissionais y asignarlos a serviços
- Cada profissional tiene su propio horário e capacidades

Una lectura pragmática considera el cliente piloto (FrontPet, petshop chico em Santana do
Livramento) y la operación real:

- FrontPet ofrece **3 serviços fixos** (Banho & Tosa, Tosa Higiênica, Spa Premium). Esos
  serviços están en el Instagram desde el día uno y no cambian.
- El equipo es **1-2 pessoas que fazem tudo** — no hay especialización tipo "Carla solo
  hace banho, Marcos solo hace tosa".
- Construir CRUD completo para algo que se va a usar 1 vez al año (ou nunca) es
  over-engineering.

Además, hay tensión con la visión futura multi-tenant: ¿la simplificación de hoy bloquea
el SaaS de mañana?

---

## Decisión

**MVP1 trata serviços como una lista fija en DB seed (sin crear desde admin), y modela
"capacidade de atendimento" como un único número por tenant en lugar de profissionais
nominais.**

### Concretamente

1. **Serviços**:
   - DB seed con los 3 serviços canônicos de FrontPet: `Banho & Tosa`, `Tosa Higiênica`,
     `Spa Premium` (e o tenant podría tener una lista distinta si fuera otro tipo de
     negocio).
   - Admin puede **editar** cualquier serviço (preço, duração, descrição, imagem,
     ativo/inativo, destaque).
   - Admin **NO puede criar** novos serviços desde a UI em MVP1. Si necesitan agregar
     uno, se hace por DB seed update o por una operação manual del dev.
   - Admin **NO puede deletar** serviços históricamente (riesgo de quebrar bookings
     históricos). Para "remover" um serviço, se desactiva via toggle `ativo=false` —
     dejaba de aparecer en el catálogo público pero permanece referenciable por
     bookings antiguos.

2. **Profissionais**:
   - **No existe el concepto de "profissional nominal" en MVP1**.
   - En lugar, hay un único campo `tenant.config.capacidade_atendimento` (integer,
     default 1) que representa cuántos pets pueden atenderse em paralelo.
   - El cálculo dinámico de slots (ver ADR 005) usa esa capacidade para decidir si
     un horário está disponible:
     ```
     slot_disponivel = (capacidade - count(bookings_overlapando)) > 0
     ```
   - El tab "Profissionais" del admin **se elimina**. La configuração da capacidade
     vive en `/admin/configuracoes` (página simples com settings da loja).

3. **Horários de atendimento**:
   - Full CRUD: admin puede editar dias (Lun-Dom), franjas horárias por dia, intervalos
     (pausa para almoço), excepciones (feriados, cierres especiais).
   - **Validação obrigatória**:
     - `start_time < end_time` en cada franja.
     - Sin solapamientos entre franjas del mismo dia.
     - Excepciones con fecha válida.
   - El cálculo de slots (ADR 005) consume esta configuração dinâmicamente.

---

## Alternativas consideradas

### ❌ Full CRUD de serviços + profissionais nominais

**A favor**:
- Lectura literal del scope CLAUDE.md ("CRUD de servicios")
- Flexibilidad máxima
- "Más profesional" visualmente

**En contra**:
- FrontPet (cliente piloto) **no tiene la operação que justifique** profissionais
  nominais — son 1-2 personas que hacen todo
- 2 semanas adicionais de dev (modal de criação + tabela `professionals` +
  `professional_services` + `professional_schedule` + lógica de asignación de bookings
  a profissionais + UI de gestión).
- Risk de bugs: doble-booking entre profissionais, slots calculados incorrectamente, etc.
- Carga cognitiva para el admin (FrontPet owner): cargar "Carla, Marcos, Juliana" y
  asignar serviços a cada uno cuando en realidad cualquiera hace todo.
- Reescritura de copy + UX más compleja en el wizard de booking público (¿selecciona
  profissional el cliente?).

**Veredicto**: over-engineering claro para el cliente piloto. La complejidad no se
justifica con valor de negocio.

### ❌ Capacidade configurable por dia + por serviço

**A favor**:
- Mais granular que un único `capacidade`

**En contra**:
- Adiciona N×M dimensiones (dias × serviços) sin demanda real
- FrontPet no opera así — la capacidade es la mesma todo el dia, todo serviço

**Veredicto**: complejidad sin payoff. Defer a Fase 2 si aparece la necesidad real.

### ❌ Hard-coded serviços en código (sem tabela)

**A favor**:
- Máxima simplicidad: serviços viven como constantes JS/Java

**En contra**:
- Quebrá la regla multi-tenant — cada tenant tendría serviços distintos
- Admin no puede editar preço, descrição, imagem desde el panel
- Cualquier cambio requer redeploy

**Veredicto**: rompe multi-tenancy, restringe edición.

### ❌ CRUD completo só de serviços (sem profissionais)

**A favor**:
- Sigue siendo simples (sin profissionais)
- Permite criar novos serviços via admin

**En contra**:
- Marginal value: FrontPet no va a criar serviços novos via panel
- UI extra (botão "Novo serviço", modal de criação, validações de duplicados) sin uso
- Risk: admin crea "Banho Express" y "Banho Rápido" como duplicados, cliente se confunde
- Si llega el dia que necesitan agregar um serviço novo, é mas seguro hacerlo por
  release controlado

**Veredicto**: micro-feature que agrega risk + complejidad sin beneficio claro.

---

## Consecuencias

### Positivas

- **Menos código que escribir, debugear y mantener** en MVP1.
- **Carga cognitiva menor** para el admin (FrontPet owner): no tiene que gestionar
  profissionais ni decidir asignaciones de serviços.
- **Booking wizard público más simples**: el cliente no elige profissional, solo data e
  horário. Una capa menos de UX.
- **Slot calculation más simples** en backend: capacidade es un número, no una matriz
  de profissionais × horários.
- **Migration futura limpia**: cuando se necesite (Fase 2+), se agrega tabla
  `professionals` + columna nullable `professional_id` em `bookings`, sin tocar nada
  existente.

### Negativas

- Admin **no puede criar novos serviços** desde la UI em MVP1. Si FrontPet quiere
  agregar "Tosa para Gatos" en 6 meses, hay que coordinar con el dev para DB seed
  update.
- No hay **tracking por profissional** (cuántos atendimentos hizo Carla esta semana,
  etc.). Para FrontPet com 1-2 personas no tiene sentido; para tenants futuros más
  grandes, sí — se evalúa en Fase 2.
- El admin no puede **diferenciar capacidades por especialidade** (ej. "tenemos 2
  banhistas pero 1 sola tosadora"). Capacidade es un número plano.

### Mitigaciones

- **Documentar en CLAUDE.md** que `tenant.config.capacidade_atendimento` representa o
  número de bookings simultâneos máximo. Default 1.
- **Cuando llegue Fase 2** y haya tenants que pidan profissionais nominais, se agregan
  sin breaking changes:
  ```sql
  CREATE TABLE professionals (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR NOT NULL,
    role VARCHAR,
    created_at TIMESTAMP DEFAULT NOW()
  );

  ALTER TABLE bookings
    ADD COLUMN professional_id UUID REFERENCES professionals(id);
  ```
  Tenants existentes (FrontPet) continúan funcionando con `professional_id = NULL` y
  usando capacidade plana. Tenants nuevos pueden activar profissionais nominais.

- **Si en MVP1 mismo aparece la necesidad de differentiar capacidades**, se evalúa
  agregar `service.capacity_override` (nullable) — cada serviço opcionalmente tiene
  su propia capacidade que sobrescribe la del tenant. Es ~2h de dev, no es una
  reescritura.

---

## Implicancias para multi-tenancy futura

**La decisión de capacidade simples NO complejiza el SaaS multi-tenant futuro.**

Lo que hace al sistema multi-tenant-ready es:

- `tenant_id` en toda tabla del dominio (ya está en CLAUDE.md sección 2)
- Queries filtradas por `tenant_id` siempre
- Auth carga el `tenant_id` del usuario logueado

Eso es ortogonal a cómo se modela capacidade. La capacidade simples es **una elección
de feature scope para el cliente piloto**, no una decisión arquitectónica multi-tenant.

Cuando llegue el SaaS y aparezca un tenant que necesite profissionais nominais
(ej. salão de beleza humano con 5 cabeleireiros especializados), se aplica la migration
limpia descrita arriba y ese tenant activa el feature. Los otros tenants (FrontPet,
petshops pequeños) siguen con capacidade simples sin cambios.

---

## Notas para el futuro

En Fase 2, cuando aparezca demanda real:

- **Profissionais nominales** con horarios individuales (1 sprint).
- **Asignación de serviços por profissional** (medio sprint adicional).
- **Booking público con "elegir profissional"** opcional (medio sprint UX).
- **Dashboard admin con métricas por profissional** (medio sprint).
- **Capacidade por serviço** si se demanda (~2h).

Total Fase 2 estimado: 2-3 sprints adicionais.

Antes de implementar Fase 2, validar con datos reales (al menos 3 tenants pidiendo el
feature, no anticipar).
