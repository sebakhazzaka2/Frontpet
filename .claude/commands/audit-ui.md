---
description: Auditar la calidad visual completa comparando la implementación contra Stitch.
---

# /audit-ui

No modificar nada todavía. Es auditoría.

## Fases

### 1. Comparar contra Stitch

Para la(s) pantalla(s) en cuestión, comparar el `code.html` del proyecto Stitch
(`projects/3403942466915386698`) contra la implementación real en `frontend/`.

### 2. Revisar contra tokens canónicos

No compares "a ojo" — verificá contra la fuente real:

- Spacing: ¿son todos múltiplos de 4? (`mt-[13px]` u otro valor arbitrario = hallazgo)
- Radios: ¿coinciden con la tabla canónica de `@theme` — `sm` 4px badges, `md` 8px
  botones/inputs/chips, `lg` 16px cards, `xl` 24px modales, `full` pills? (ADR 014)
- Colores: ¿son los 5 tokens de `globals.css` o hay hex hardcodeado?
- Tipografía: ¿Fredoka/Plus Jakarta Sans, pesos 400/500/600 (nunca 700+)?

### 3. Revisar estructura y jerarquía

- Jerarquía visual, alineaciones, consistencia entre componentes similares
- Componentes: ¿se reusó lo existente en `components/ui/` o se duplicó?

### 4. Responsive

Validar a 320/768/1024px (1440 si la pantalla tiene layout desktop propio — hoy solo
Login lo tiene, el resto es mobile 780px).

### 5. Accesibilidad y microinteracciones

Contraste, labels, estados de foco, transiciones (framer-motion) coherentes con el resto
del sistema.

## Salida

```
UI AUDIT — <pantalla/componente>
=================================

CRÍTICO (rompe usabilidad o el design system):
- <hallazgo> — token/regla violado

ALTO (desvío visible de Stitch, sin justificación documentada):
- <hallazgo>

MEDIO (mejora de consistencia, no urgente):
- <hallazgo>

BAJO (pulido):
- <hallazgo>

Total: X hallazgos priorizados
```

No implementar los cambios en esta pasada — entregar solo la lista priorizada.
