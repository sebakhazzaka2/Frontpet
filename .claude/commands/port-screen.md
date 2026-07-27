---
description: Portar una pantalla de Stitch a componentes reales del repo (pipeline de docs/stitch-implementation-workflow.md)
---

Portá la pantalla de Stitch indicada en: $ARGUMENTS
(si falta el screen ID, listá las pantallas con `mcp__stitch__list_screens` del proyecto
`projects/3403942466915386698` y pedí confirmación de cuál).

Es trabajo de ejecución: escribí código, no devuelvas solo un análisis.

## Contexto — leelo, no lo rederives

Antes de tocar Stitch, cargá las fuentes canónicas (no dupliques su contenido en tu
razonamiento — aplicalas):

1. **Proceso y criterios**: `docs/stitch-implementation-workflow.md` §1 (etapas, incluye
   el chequeo de scope de la Etapa 0) y §3 (checklist de validación).
2. **Tokens**: `frontend/app/globals.css`, bloque `@theme` — colores, tipografía, radios,
   sombras. No existe `tailwind.config.ts` y no se recrea.
3. **Traducción de radios**: ADR 014 (`docs/decisions/014-tailwind-v4-css-first.md`),
   sección 6 + Actualización 2026-07-27. Primero verificá si el `code.html` de la pantalla
   define un `borderRadius` propio: si no, aplica la tabla del CDN v3; si sí, derivá la
   traducción como hizo la landing.
4. **Iconos**: tabla Material Symbols → `lucide-react` en `docs/port-landing-stitch.md`
   §6 (única sección viva de ese doc). Ícono nuevo → buscá el 1:1 en lucide y
   **agregalo a esa tabla**, no lo resuelvas solo en el componente.
5. **Reglas de diseño y de datos**: `CLAUDE.md` §5 (paleta, escala, PT-BR, metodología) —
   ya lo tenés cargado.

## Reglas duras del port

- Colores fuera de los tokens, tamaños fuera de escala, contenido placeholder de Stitch
  (direcciones/teléfonos/servicios de mockup): **se marcan como decisión abierta, no se
  inventan tokens ni datos**. Lo ya resuelto (tokens, radios, íconos mapeados) se aplica
  directo, sin preguntar.
- Sin foto real: patrón `PawPrint` sobre `bg-surface` (como `product-card.tsx` /
  `service-card.tsx`), nunca stock hotlinkeado.
- Datos: si existe DTO real en `backend/src/main/java/com/frontpet/**/dto/`, el tipo en
  `lib/data/` copia esos nombres de campo; si no, tipo provisorio con comentario de qué
  DTO lo reemplaza y en qué sprint (ROADMAP.md).
- Reusá lo existente (`WhatsAppIcon`, `buildWhatsAppLink` de `@/lib/data/site`,
  `formatPrice` de `@/lib/utils`) — no dupliques componentes.
- Server Component por defecto; `'use client'` solo con comentario de una línea con la razón.
- **NO commitees** (el commit lo hace el desarrollador). **NO toques** Nav, Footer ni
  componentes existentes salvo que la tarea lo pida. **NO instales** dependencias sin
  marcarlo como pregunta abierta.

## Al terminar

Validá con el checklist de `docs/stitch-implementation-workflow.md` §3 y reportá:
archivos tocados · desvíos de Stitch sin resolución previa y por qué · íconos/tokens
nuevos (para revisarlos antes de darlos por canónicos) · resultado de build/lint/test ·
rutas de screenshots a 320/768/1024px (1440 si hay layout desktop propio) · preguntas
abiertas. Registrá el port en la tabla "Historial de pilotos" del workflow doc.
