---
description: Auditar el proyecto como Senior Engineer, con severidad y ubicación exacta por hallazgo.
---

# /release-check

No implementar nada. Es auditoría, no ejecución.

## Alcance

Revisar:

- **Correctness**: bugs, edge cases sin manejar, estados inconsistentes
- **Code smells**: violaciones de §5/§6 de CLAUDE.md (abstracciones prematuras,
  lazy loading de ORM sin pensar, lógica de petshop hardcodeada, etc.)
- **Deuda técnica**: atajos conocidos, TODOs sin resolver
- **Deuda visual**: desvíos de Stitch no documentados, spacing fuera de la escala de 4,
  radios fuera de la tabla canónica (ADR 014)
- **Performance**: queries N+1, imágenes sin `<Image>`, bundle size
- **Seguridad**: inputs sin validar, JWT fuera de cookie HttpOnly, secrets expuestos
- **Accesibilidad**: contraste, labels, navegación por teclado
- **Responsive**: 320/768/1024/1440px
- **Documentación**: drift entre código y docs (podés delegar el detalle a `/doc-sync`,
  acá solo señalá que existe)
- **Consistencia**: convenciones de CLAUDE.md §5 aplicadas de forma pareja

## Reglas

- Cada hallazgo necesita: archivo + línea (o componente/pantalla) + por qué importa en
  1 frase. Sin ubicación exacta, no es un hallazgo, es una sospecha — decilo como tal.
- No repitas hallazgos ya conocidos y documentados en `docs/pending-decisions.md` sin
  aportar algo nuevo.
- Priorizá por impacto real en este proyecto (petshop piloto, USD 500, deadline 30/09),
  no por severidad genérica de OWASP/Lighthouse.

## Salida

```
RELEASE CHECK
=============

CRÍTICO (bloquea entrega o rompe en producción):
- [archivo:línea] <hallazgo> — <por qué>

ALTO (afecta UX o corrección, no bloquea):
- [archivo:línea] <hallazgo> — <por qué>

MEDIO (deuda que conviene resolver, no urgente):
- [archivo:línea] <hallazgo> — <por qué>

BAJO (nice-to-have, considerar solo si sobra tiempo):
- [archivo:línea] <hallazgo> — <por qué>

Ya conocido (en pending-decisions.md, sin novedad): <lista o "ninguno">

Total: X hallazgos (Y crítico, Z alto, W medio, V bajo)
```
