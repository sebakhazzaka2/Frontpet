---
description: Cierre documental del sprint actual — DoD, ADRs, deuda y estado para el próximo sprint.
---

# /sprint-close

No implementar código. Esto es cierre documental, no ejecución.

## Fases

### 1. Verificar DoD

Corré (o pedí correr) `/verify`. No cierres el sprint si el resultado es `NOT READY` sin
que el usuario lo confirme explícitamente — un sprint cerrado con build roto es peor que
uno que se cierra un día después.

### 2. Marcar tareas completadas en ROADMAP.md

Comparar contra el trabajo real (commits, archivos existentes — no contra lo que se
planeó hacer). Si algo se hizo distinto a lo planeado, anotarlo, no solo tildarlo.

### 3. ADRs

- ¿Hubo decisiones técnicas tomadas este sprint sin ADR? Listarlas.
- ¿Algún ADR existente quedó `superseded` por una decisión de este sprint? Marcarlo con
  `Status: superseded by ADR-NNNN` y agregar el enlace en el ADR nuevo también.
- Para cada ADR nuevo propuesto, no lo escribas sin esto: Contexto (máx 5 líneas) →
  Decisión → **Alternativas consideradas** (pros/cons/por qué no, mínimo 1 alternativa
  real) → Consecuencias (positivas/negativas/riesgos). Sin alternativas descartadas no es
  un ADR completo, es una nota.
- Actualizar el índice en `docs/decisions/README.md`.

### 4. Deuda técnica y documental

- Deuda técnica: código aceptado con atajos conocidos, tests faltantes, TODOs sin ticket.
- Deuda documental: ¿qué quedó desactualizado por el trabajo de este sprint? (design-system,
  db-model.png, next16-notes, etc.)
- Si algo lleva 2+ sprints como deuda sin resolverse, señalarlo aparte — es candidato a
  perder prioridad para siempre si no se aborda ahora.

### 5. WORKING-CONTEXT.md

Reescribir `docs/WORKING-CONTEXT.md` con el estado de cierre: sprint recién cerrado,
qué queda para el próximo, deuda vigente, bloqueos reales. Esto reemplaza el contenido
anterior, no lo acumula.

### 6. Proponer el próximo sprint

Basado en ROADMAP.md y en lo que efectivamente quedó pendiente (no en lo que el roadmap
asumía antes de empezar este sprint). Si el sprint se desbordó de horas, decilo y proponé
si el próximo se acorta o si el colchón absorbe la diferencia (ver presupuesto de horas
en ROADMAP.md).

## Salida

```
SPRINT CLOSE — Sprint N
========================

DoD: [READY/NOT READY] (ver /verify)

Tareas: X/Y completadas · desvíos: <lista o "ninguno">

ADRs:
- Nuevos necesarios: <lista o "ninguno">
- Superseded: <lista o "ninguno">

Deuda técnica: <lista>
Deuda documental: <lista>
Deuda arrastrada 2+ sprints: <lista o "ninguna">

WORKING-CONTEXT.md: actualizado

Próximo sprint sugerido: <resumen 2-3 líneas, remitir a ROADMAP.md para el detalle>
```
