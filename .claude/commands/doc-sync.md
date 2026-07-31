---
description: Sincronizar la documentación del proyecto, separando lo permanente de lo efímero.
---

# /doc-sync

Objetivo: eliminar drift entre código y documentación. Mantener Single Source of Truth.

No modifiques arquitectura ni planificación. Solo sincronización documental.
No implementes código.

## Fases

### 1. Detectar drift

Comparar contra el estado real del repo (no contra lo que el doc dice que es):

- `ROADMAP.md`: tareas marcadas como pendientes que ya están hechas (o viceversa) —
  revisar commits y estructura real, no solo texto
- `CLAUDE.md` §4 (Estructura del repo): ¿sigue reflejando `frontend/` y `backend/` reales?
- `docs/decisions/*.md`: ¿algún ADR quedó contradicho por una decisión posterior sin
  marcarse como `superseded by`?
- `docs/pending-decisions.md`: ¿alguna decisión "pendiente" ya se resolvió en el código?
- `docs/design-system.md`: ¿coincide con `frontend/app/globals.css` (fuente canónica)?

### 2. Clasificar cada hallazgo (permanente vs. efímero)

Antes de decidir dónde escribir algo, clasificalo:

| Si es... | Va en... |
|---|---|
| Regla o decisión estable, válida indefinidamente | `CLAUDE.md` |
| Decisión arquitectónica tomada, con alternativas y trade-offs | ADR en `docs/decisions/` |
| Estado actual que cambia sprint a sprint (qué está en curso, bloqueado, deuda con fecha) | `docs/WORKING-CONTEXT.md` |
| Decisión NO tomada todavía, que no bloquea | `docs/pending-decisions.md` |
| Progreso de tareas planificadas | `ROADMAP.md` |

Si algo hoy vive en `CLAUDE.md` pero es estado efímero (por ejemplo, un `⚠️` que dice
"pendiente de confirmación del cliente" o "esto es temporal hasta X"), **movelo** a
`WORKING-CONTEXT.md` en vez de solo actualizarlo in situ.

### 3. Filtro de admisión antes de agregar algo nuevo a CLAUDE.md

No agregues nada a CLAUDE.md sin pasar los 3 criterios:

1. **¿Aparece en 2+ lugares o va a repetirse en 2+ sprints?** Si es un caso único, no es regla.
2. **¿Es accionable?** Se puede escribir como "hacé X" / "no hagas Y" — no "X es importante".
3. **¿Tiene riesgo de violación articulable en una frase?** Si no podés decir qué se rompe
   al ignorarlo, no es una regla, es una observación.

Para cada candidato, asigná un veredicto y decilo explícitamente en el reporte:

- **Agregar** — pasa los 3 criterios, no está cubierto
- **Ya cubierto** — el contenido ya existe, aunque con otras palabras (citá dónde)
- **Demasiado específico** — debería vivir en un ADR o en `pending-decisions.md`, no en CLAUDE.md
- **Revisar** — contradice algo que ya está escrito (señalalo, no lo sobrescribas sin avisar)

### 4. Actualizar WORKING-CONTEXT.md

Reescribí (no acumules) `docs/WORKING-CONTEXT.md` con el estado real: branch, qué sprint
está en curso y qué falta de él, deuda documental conocida vigente, qué bloquea o no
bloquea. Borrá entradas que dejaron de ser ciertas — no las dejes "resueltas" ahí.

### 5. Design system

Si `globals.css` cambió y `docs/design-system.md` no, actualizar. Marcar explícitamente
si el drift es en las secciones 6-7 (ya documentado como desactualizado — confirmar si
sigue así o se corrigió).

### 6. Referencias obsoletas

Buscar menciones a archivos/rutas/comandos que ya no existen (renombrados, borrados,
migrados). No asumas — verificá con Glob/Grep antes de reportarlo como obsoleto.

## Salida

```
DOC-SYNC REPORT
===============

Archivos modificados:
- <archivo> — <por qué cambió, 1 línea>

Candidatos a CLAUDE.md:
- <candidato> — veredicto: [Agregar/Ya cubierto/Demasiado específico/Revisar]

WORKING-CONTEXT.md: [reescrito / sin cambios]

Inconsistencias encontradas (sin resolver todavía):
- ...

Recomendaciones opcionales:
- ...
```
