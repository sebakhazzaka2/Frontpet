# Development Workflow — FrontPet

> Guía de trabajo para el desarrollo del MVP1. Stack nuevo: Next.js 14 App Router +
> Spring Boot + PostgreSQL. Objetivo: feedback rápido, código entendido línea por línea,
> Claude como copiloto real.

---

## 1. WSL — setup único, no volvés a pensar en ello

**Instalar una sola vez (en WSL):**
- VS Code en Windows con extensión **Remote - WSL** → abre el proyecto desde WSL, IntelliSense y terminal son Linux nativos
- Docker Desktop con backend WSL2 habilitado
- Java 21 + Maven en WSL (no en Windows)
- Node.js 20 LTS via `nvm` en WSL

**Workflow diario:**
```bash
# Windows Terminal → tab WSL
cd ~/dev/Frontpet && code .
# La terminal de VS Code ya está en WSL. No mezclar rutas Windows/Linux.
```

**Acceder desde celular (validación mobile real):**
```bash
ip route show | grep default   # → anota la IP del host Windows, ej. 172.22.0.1
# Celular en la misma red: http://172.22.0.1:3000
```

---

## 2. Feedback loops — correr en paralelo siempre

El ciclo idea → código → feedback tiene que ser menor a 10 segundos.

```bash
# Terminal 1 — Next.js hot reload
cd frontend && npm run dev

# Terminal 2 — Vitest watch (utils, hooks)
cd frontend && npm run test:watch

# Terminal 3 — Spring Boot con DevTools
cd backend && mvn spring-boot:run

# Una vez por sesión — PostgreSQL
docker compose -f backend/docker-compose.yml up -d
```

**Script de arranque** (guardar como `dev.sh` en la raíz):
```bash
#!/bin/bash
docker compose -f backend/docker-compose.yml up -d
(cd frontend && npm run dev) &
(cd backend && mvn spring-boot:run) &
wait
```

**Tests de integración (backend):**
Testcontainers levanta PostgreSQL real. No mockear la DB.
```bash
mvn test -pl backend
```

---

## 3. Sesión con Claude — estructura por feature

### Por cada feature no trivial:

**Paso 1 — Plan en prosa (5 min)**
Pedir: *"Antes de codear, describí: qué endpoint/query/componente, qué edge cases, qué test primero."*
Leer las ~10 líneas y aprobar antes de que tire código.

**Paso 2 — Implementación incremental**
Orden fijo: `migration → entity → repository → service → controller → test`
Una capa a la vez, no todo junto.

**Paso 3 — Verificación**
Correr tests + abrir en browser. Si algo falla, pegar el error exacto (no parafrasear).

### Lo que NO hacer:
- ❌ "Haceme todo el módulo de catálogo" → código que no entendés
- ❌ Aceptar código sin leer línea por línea
- ❌ Pedirle que commitee — vos decidís qué va a main

Usar `/plan` (plan mode) para cualquier feature que toque más de 2 archivos nuevos o lógica de negocio.

---

## 4. Next.js App Router — patrones de FrontPet

### Regla central: Server Component por defecto

| Componente | Tipo | Razón |
|---|---|---|
| Landing, catálogo, detalle | Server | Fetch de datos, SEO |
| Carrito | Client + sessionStorage | Estado local |
| Formulario de booking | Client | react-hook-form necesita el DOM |
| Animaciones | Client + `"use client"` | Framer Motion |
| Nav `(public)/layout.tsx` | Server | Sin estado |

**Fetch en Server Component (Sprint 3+):**
```typescript
// app/(public)/catalog/page.tsx
async function CatalogPage() {
  const products = await fetch(`${process.env.BACKEND_URL}/api/v1/products`, {
    next: { revalidate: 60 }
  }).then(r => r.json())

  return <ProductGrid products={products} />
}
```
TanStack Query va en Client Components únicamente (carrito, admin).

**Loading/error por ruta:**
```
app/(public)/catalog/
  page.tsx        ← componente
  loading.tsx     ← skeleton automático
  error.tsx       ← boundary ("use client")
```

**Variables de entorno:**
```env
# frontend/.env.local
BACKEND_URL=http://localhost:8080          # server-side, sin NEXT_PUBLIC_
NEXT_PUBLIC_WA_NUMBER=5XXXXXXXXXXX        # client-side, botón flotante
```

---

## 5. Spring Boot + PostgreSQL — orden de construcción

### Flyway primero, siempre
Antes de escribir una sola clase Java, escribir la migración SQL.

### Orden dentro de cada módulo:
```
1. Migración Flyway  →  V{n}__descripcion.sql
2. Entity            →  @Entity, @Table
3. Repository        →  interface extends JpaRepository<T, Long>
4. Service           →  interfaz pública + implementación
5. Controller        →  @RestController, @RequestMapping("/api/v1/...")
6. Integration test  →  @SpringBootTest + Testcontainers
```

### Estructura de módulo (ejemplo: catalog):
```
backend/src/main/java/com/frontpet/catalog/
  domain/
    Product.java
    ProductRepository.java
  application/
    ProductService.java        (interfaz)
    ProductServiceImpl.java
  api/
    ProductController.java
    ProductDto.java            (record Java 21)
    CreateProductRequest.java  (record + @Valid)
```

### Queries — tres niveles, usar el más simple:

**Nivel 1 — derived queries (90% de los casos, sin SQL):**
```java
List<Product> findByTenantId(Long tenantId);
List<Product> findByTenantIdAndCategoryId(Long tenantId, Long categoryId);
Optional<Product> findByTenantIdAndPublicId(Long tenantId, UUID publicId);
```

**Nivel 2 — @Query JPQL** (cuando el derived se vuelve ilegible):
```java
@Query("SELECT p FROM Product p WHERE p.tenantId = :tid AND p.active = true ORDER BY p.createdAt DESC")
List<Product> findActive(@Param("tid") Long tenantId);
```

**Nivel 3 — native SQL** (aggregations complejas, mini-dashboard Sprint 7):
```java
@Query(value = "SELECT product_id, COUNT(*) as cnt FROM order_item GROUP BY product_id ORDER BY cnt DESC LIMIT 5", nativeQuery = true)
List<Object[]> findTop5ProductIds();
```

Sprint 3 y 4: solo Nivel 1. Nivel 2 y 3 aparecen en Sprint 7.

---

## 6. Anti-patrones a evitar (específicos a este stack)

**Next.js:**
- ❌ `"use client"` en pages — hace que todo el árbol sea cliente, pierde SSR y SEO
- ❌ `useEffect` para fetch de datos iniciales — usar Server Components
- ❌ Importar una librería de animaciones en un Server Component

**Spring Boot / JPA:**
- ❌ `@Transactional` en el Controller — va en el Service
- ❌ Lazy loading sin `JOIN FETCH` explícito → N+1 queries silenciosos
- ❌ Hardcodear `tenant_id = 1` → siempre desde el contexto auth

---

## 7. Orden de build julio-agosto

### Pre-julio
- Cerrar decisiones abiertas de `docs/db-skeleton.md`
- Schema final → DBdiagram.io → `docs/db-model.png`
- Resolver inconsistencia de fuentes (Fredoka vs DM Serif en tailwind.config.ts)

### Julio
| Semana | Foco |
|---|---|
| 1-2 | Backend Sprint 3: migraciones V1-V4, módulos tenant/identity/catalog, endpoints GET products |
| 3-4 | Frontend Sprint 3: catálogo con fetch real, detalle de producto |
| 5 | Sprint 4: carrito sessionStorage + pedidos WhatsApp + POST /orders |

### Agosto
| Semana | Foco |
|---|---|
| 6-7 | Admin: login JWT + CRUD productos |
| 8-9 | Booking: slots dinámicos (backend) + wizard 3 pasos (frontend) |
| 10 | Sprint Despliegue: Hetzner + Coolify + Caddy + dominio |
| 11-12 | Colchón: polish, Meta Pixel, mini-dashboard, Plausible |

---

**Última actualización**: 2026-06-24
