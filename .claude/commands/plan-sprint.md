---
description: Analiza un sprint completo antes de implementarlo y genera la estrategia óptima de ejecución.
---

# Objetivo

Entrá en Plan Mode.

NO escribas código.
NO propongas implementaciones todavía.

Tu único objetivo es diseñar el mejor plan posible para ejecutar este sprint minimizando tiempo, retrabajo y conflictos.

Pensá como un Tech Lead preparando el trabajo para un equipo senior.

---

# Contexto

Leé primero:

- ROADMAP.md
- CLAUDE.md

Luego abrí únicamente la documentación necesaria (ADRs, workflow, design docs, pending-decisions, etc.).

No cargues documentación innecesaria.

---

# Análisis

No asumas que el ROADMAP es perfecto.

Revisá críticamente:

- dependencias
- riesgos
- tareas ocultas
- deuda técnica
- posibles simplificaciones
- posibles mejoras de arquitectura
- inconsistencias con ADRs
- documentación desactualizada

Si encontrás una mejor estrategia, explicá por qué.

---

# Para cada tarea

Analizá:

- objetivo
- archivos que probablemente cambien
- dependencias
- riesgo
- complejidad
- impacto sobre otras tareas

Indicá si:

✅ independiente

⚠️ parcialmente paralelizable

❌ debe hacerse en secuencia

---

# Paralelización

Buscá oportunidades reales de multitasking.

No propongas múltiples tabs por defecto.

Solo recomendalos cuando:

- modifican áreas distintas
- minimizan conflictos de merge
- reducen el tiempo total

Si conviene, proponé:

- cantidad óptima de tabs
- qué hace cada uno
- orden de integración
- si conviene usar git worktree

Si NO conviene, decilo explícitamente.

---

# Arquitectura

Detectá:

- ADRs que probablemente deban actualizarse
- nuevos ADRs necesarios
- documentación que quedará desactualizada
- posibles cambios permanentes para CLAUDE.md

No los implementes.
Solo señalalos.

---

# Riesgos e issues

Identificá:

- bloqueantes
- decisiones pendientes
- dependencias externas
- posibles bugs
- deuda técnica que convenga resolver antes
---

# Estimación

Generá una estimación propia.

No copies la del ROADMAP si no coincide con el trabajo real.

Explicá dónde está el costo.

---

## Acceptance Criteria

Definí criterios verificables para considerar el sprint terminado.

Los criterios deben ser comprobables y alineados con el DoD del proyecto.
---

# Entregable

Devolvé únicamente:

## Resumen ejecutivo

## Orden recomendado

## Estrategia de implementación

## Estrategia de ramas

## Estrategia de multitasking (si aplica)

## Riesgos e Issues

## Documentación que probablemente cambie

## Checklist antes de empezar

## Acceptance Criteria

No implementes nada.

Esperá aprobación antes de pasar a la ejecución.