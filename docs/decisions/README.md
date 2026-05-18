# Architectural Decision Records (ADRs)

Este directorio contiene las decisiones arquitectónicas importantes del proyecto FrontPet.

## ¿Qué es un ADR?

Un Architectural Decision Record es un documento corto que explica **por qué se tomó una
decisión técnica importante**. La idea es que en 6 meses, cuando se pregunte "¿por qué
usamos X y no Y?", la respuesta esté escrita y no haya que reconstruirla de memoria.

Cada ADR sigue la misma estructura:

- **Contexto**: cuál era el problema o la situación
- **Decisión**: qué se eligió
- **Alternativas consideradas**: qué se descartó y por qué
- **Consecuencias**: implicancias positivas y negativas

## Cuándo crear un nuevo ADR

Crear un ADR cuando:

- Se elige un componente arquitectónico (stack, framework, librería core)
- Se toma una decisión de modelado importante (multi-tenancy, eventos, etc.)
- Se descarta un patrón estándar de la industria con justificación
- Se elige un trade-off entre dos enfoques válidos

**No crear** ADRs para:

- Decisiones triviales (qué librería de date-fns usar)
- Implementaciones específicas que ya están en el código
- Cosas que se pueden cambiar en una tarde sin impacto

## Cómo crear uno

1. Copiá la plantilla de otro ADR existente
2. Numerá el siguiente disponible: `NNN-titulo-corto.md`
3. Completá las secciones
4. Marcalo con estado **Propuesta** si aún se está discutiendo
5. Una vez decidido, cambialo a **Aceptada**
6. Si en el futuro se revierte, no lo borres: marcalo como **Reemplazada por ADR-XYZ**

## Índice

| # | Título | Estado | Fecha |
|---|--------|--------|-------|
| [001](./001-stack-tecnologico.md) | Stack tecnológico | Aceptada | 2026-05-17 |
| [002](./002-multi-tenant.md) | Estrategia multi-tenant | Aceptada | 2026-05-17 |
| [003](./003-pedidos-whatsapp.md) | Pedidos vía WhatsApp click-to-chat | Aceptada | 2026-05-17 |
| [004](./004-auth-jwt-cookie.md) | Autenticación con JWT en cookie HttpOnly | Aceptada | 2026-05-17 |
| [005](./005-slots-dinamicos.md) | Cálculo dinámico de slots de booking | Aceptada | 2026-05-17 |
