# ADR 017 — Metodología de implementación: frontend-first híbrido con datos estáticos tipados

**Estado**: Aceptada
**Fecha**: 2026-07-17
**Sprint**: transversal (aplica de Sprint 2 en adelante)

---

## Contexto

Con el schema de DB cerrado (ADR 013) y las 19 pantallas ya diseñadas en Stitch, apareció la
pregunta de **cómo ejecutar la construcción**: ¿feature por feature full-stack (slice
vertical), o toda la UI primero con datos estáticos y el backend después?

La tentación fue el **frontend-first puro**: "hacemos todo Stitch con datos estáticos, después
todo el backend, después el swap de datos". Es atractivo para un dev solo (menos
context-switching entre Java y Next, la librería de componentes se construye una vez, y da un
producto visible/demoable temprano — bueno para la confianza del cliente).

Pero el frontend-first **puro** tiene dos problemas graves contra las fechas del proyecto
(hito de cobro 05/09 = *tienda vendiendo*; entrega 30/09):

1. **Big-bang de integración**: diferir *todo* el swap de datos al final junta todos los
   riesgos de integración (forma real de DTOs, loading/error states, Async APIs de Next 16,
   flujo de cookie de auth, paginación/filtros que el mock disimula) en el peor momento
   posible — contra el deadline.
2. **Empuja el backend más riesgoso (booking) al final**: el cálculo de slots + concurrencia
   es el riesgo concentrado del MVP (ADR 005, 009, 011) y **no se des-arriesga con frontend
   estático**. El ROADMAP explícitamente pide *adelantar* ese backend, no diferirlo.

Además, "tienda vendiendo" el 05/09 necesita el catálogo con **datos reales** (los ~40
productos) + carrito + `wa.me`, no un shell estático.

---

## Decisión

**Frontend-first híbrido.** Se adopta el estilo frontend-first **para la superficie pública /
marketing y la librería de componentes**, pero con excepciones explícitas y sin big-bang.

### Orden para la superficie pública
`design system → librería de componentes → vistas públicas con datos estáticos → deploy`.
Eficiente, demoable, y aprovecha que Stitch ya tiene el diseño.

### Excepciones que NO se difieren al final
- **Booking backend**: se codea temprano (el ROADMAP ya lo marca como "adelantar Sprint 5").
  Es el riesgo concentrado; el frontend estático de booking no lo mitiga.
- **Admin**: se hace **vertical**, pegado a su backend. Es CRUD — su razón de ser es
  manipular datos reales; el frontend estático de admin es poco demostrable y el que más
  rework necesita en el swap.

### Reglas transversales
- **Swap por superficie, no big-bang**: cada vista pasa de datos estáticos a la API real
  cuando aterriza su backend, no todo junto al final.
- **Datos estáticos tipados contra los DTOs reales** (derivados del schema, ADR 013), en
  `frontend/lib/data/`. Con la forma correcta desde el día uno, el swap es **cambiar la
  fuente, no reescribir el componente**.
- **Componente que no existe → crearlo antes de usarlo. Nunca duplicar** uno existente.

### Relación con el ROADMAP
Esto **no reemplaza el plan de sprints** — lo refina. El ROADMAP ya front-loadea la landing
(Sprint 2) antes de los sprints de backend, y mantiene slices verticales para catálogo (3),
pedidos+admin (4) y booking (5-6). El ADR fija *el estilo de ejecución* dentro de esos
sprints y la disciplina de datos, no un reordenamiento.

---

## Alternativas consideradas

### ❌ Frontend-first puro (todo UI estático → todo backend → swap final)
Big-bang de integración contra el deadline + booking diferido + un "deploy" intermedio que
sería un shell no-funcional cuando el hito de cobro pide venta real. Rechazado.

### ❌ Vertical estricto (cada feature full-stack de una)
Válido y seguro para la integración, pero desperdicia dos ventajas concretas de este
proyecto: que Stitch ya tiene el diseño listo (batchear el port es eficiente) y que construir
la librería de componentes una vez reduce el context-switching de un dev solo. El híbrido
conserva esas ventajas donde aplican (público) sin pagar el costo del big-bang.

---

## Consecuencias

### Positivas
- Producto público visible/demoable temprano — activo de confianza y de venta para el cliente.
- Librería de componentes construida una vez y reusada.
- Integración distribuida por superficie, no concentrada al final.
- El riesgo real (booking) se ataca temprano, no contra la entrega.

### Negativas / a vigilar
- **Requiere disciplina de tipar los datos estáticos contra los DTOs desde el día uno.** Si
  se inventan con forma cómoda para el mock, el "swap" se vuelve "reescribir componentes".
- El **deploy intermedio** despliega un preview no-funcional (landing sin backend de venta).
  Hay que framearlo al cliente como *preview*, no como "la tienda ya está online".
- El "tienda vendiendo" del 05/09 depende de que el **path de datos reales del catálogo**
  (seed o catálogo mínimo) exista para esa fecha, no solo placeholders.

### Reglas operativas (viven también en CLAUDE.md §5 para que apliquen en cada sesión)
1. Componente inexistente → crearlo antes de usarlo; nunca duplicar.
2. Datos en `frontend/lib/data/`, nunca hardcodeados, tipados contra los DTOs reales.
