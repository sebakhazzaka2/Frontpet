# ADR 002 — Estrategia multi-tenant

**Fecha**: 2026-05-17
**Estado**: Aceptada
**Autor**: [tu nombre]

---

## Contexto

El MVP1 tiene **un solo cliente** (FrontPet), pero la visión a largo plazo es que la
misma plataforma sirva a múltiples negocios locales (otros petshops, peluquerías,
barberías, gimnasios, etc.).

Si en el MVP1 se modela todo como single-tenant y después hay que retrofitear
multi-tenancy, el costo es altísimo: migraciones de datos masivas, refactor de cada
query, riesgo de leaks entre tenants.

Si en el MVP1 se construye toda la infraestructura multi-tenant (subdominios, signup
de negocios, branding configurable), se duplica el tiempo de desarrollo sin agregar
valor a FrontPet.

## Decisión

**Multi-tenant ready en el modelo de datos, single-tenant en la operación.**

Específicamente:

1. **Toda tabla del dominio lleva una columna `tenant_id`** desde el día uno, indexada,
   parte del WHERE en todos los queries.
2. **Modelo de aislamiento elegido**: shared database, shared schema, tenant discriminator.
3. **Filtro automático**: implementar un `@TenantFilter` global con Hibernate que
   aplique el `WHERE tenant_id = ?` sin necesidad de pasarlo explícitamente.
4. **Resolución del tenant**: del JWT del admin (campo `tenantId` en el claim).
5. **Una sola fila en la tabla `tenants`** en MVP1: FrontPet.
6. **No se construye infraestructura multi-tenant operativa**:
   - Sin subdominios dinámicos
   - Sin signup de negocios nuevos
   - Sin branding configurable por tenant
   - Sin panel super-admin

## Alternativas consideradas

### ❌ Sin tenant_id en MVP1, agregarlo después

**A favor**: 2-3 días menos de trabajo inicial.

**En contra**:
- Migración futura altamente compleja (datos existentes a migrar, queries a refactorear)
- Riesgo alto de errores de aislamiento en la migración
- Mala práctica conocida — todos los SaaS exitosos lo agregan desde el día uno

### ❌ Multi-tenant operativo completo en MVP1

**A favor**: Producto listo para vender a otros clientes desde día uno.

**En contra**:
- Duplica el tiempo de desarrollo (~6 semanas extras)
- No aporta valor a FrontPet
- Especulativo — la abstracción correcta de multi-tenancy aparece cuando hay 3+ tenants reales

### ❌ Schema separado por tenant (esquema diferente en Postgres)

**A favor**: Aislamiento físico mayor.

**En contra**:
- Migraciones más complejas
- Peor manejo cuando hay muchos tenants
- No justificado para datos no regulados de negocios locales

### ❌ Database separada por tenant

**A favor**: Aislamiento total.

**En contra**:
- Costo operativo enorme
- Solo justificado en escenarios enterprise/regulados

## Consecuencias

### Positivas
- MVP1 entregable en tiempo razonable
- Cero refactor cuando llegue el segundo cliente
- Cada query del backend ya filtra por tenant_id de forma transparente
- Habilita escalabilidad horizontal futura

### Negativas
- Todas las tablas tienen una columna extra (costo mínimo)
- Hay que recordar setear el `tenantId` en el contexto al autenticar
- Posible bug si se olvida el filtro en un query nativo (mitigable con tests)

### Mitigaciones
- Tests explícitos que intenten leer datos cruzados entre tenants y verifiquen que retornan vacío
- Code review obligatorio en cualquier query con `nativeQuery = true`
- `TenantContext` thread-local resuelto al inicio de cada request

## Notas para el futuro

Cuando llegue el momento de activar multi-tenant operativo (segundo cliente real):

- Subdominios via wildcard DNS (`*.tudominio.com`)
- Middleware que resuelve el tenant del subdominio antes de la autenticación
- Panel super-admin separado para gestionar tenants
- Branding por tenant con campo `tenant.theme` (JSONB)
- Módulos habilitables con `tenant.enabled_modules` (JSON array)

**Estimación de Fase 5 multi-tenant**: 4-6 semanas adicionales.
