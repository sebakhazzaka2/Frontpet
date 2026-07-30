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
| [005](./005-slots-dinamicos.md) | Cálculo dinámico de slots de booking | Aceptada, actualizada por 020 | 2026-05-17 |
| [006](./006-frontend-layout-structure.md) | Estructura de layouts del frontend | Aceptada | 2026-05-18 |
| [007](./007-product-language-ptbr.md) | Idioma del producto: portugués brasileño (PT-BR) | Aceptada | 2026-05-20 |
| [008](./008-agendamentos-sin-whatsapp.md) | Agendamentos persisten en DB sin abrir WhatsApp en el submit | Aceptada | 2026-05-25 |
| [009](./009-servicos-fixos-capacidade-simples.md) | Serviços fixos en DB seed + capacidade simples (no profissionais nominais) | Parcial. reemplazada por 011 | 2026-05-25 |
| [010](./010-whatsapp-templates-confirmacao.md) | Templates de mensagem pré-formatada para confirmação por WhatsApp | Aceptada | 2026-05-25 |
| [011](./011-modelo-servicos-banhos-adicionais.md) | Modelo de serviços: banhos base + adicionais, preço/duração por porte | Aceptada | 2026-07-09 |
| [012](./012-booking-sin-integracion-erp.md) | Booking sin integración ERP: la web es la autoridad de disponibilidad | Aceptada | 2026-07-09 |
| [013](./013-modelo-datos-mvp1.md) | Modelo de datos MVP1: decisiones de esquema y reutilización | Aceptada | 2026-07-13 |
| [014](./014-tailwind-v4-css-first.md) | Config de Tailwind CSS-first (`@theme` en `globals.css`) | Aceptada | 2026-07-16 |
| [015](./015-toolchain-wsl-node-pnpm.md) | Toolchain de desarrollo: WSL, Node y pnpm | Aceptada | 2026-07-16 |
| [016](./016-deploy-frontend-vps-coolify.md) | Deploy: VPS único con Coolify, Cloudflare como CDN | Aceptada | 2026-07-16 |
| [017](./017-metodologia-frontend-first-hibrido.md) | Metodología: frontend-first híbrido con datos estáticos tipados | Aceptada | 2026-07-17 |
| [018](./018-r2-presigned-upload-flow.md) | Flujo de firmado para upload de imágenes a Cloudflare R2 | Aceptada | 2026-07-27 |
| [019](./019-rate-limit-login-en-memoria.md) | Rate limit del login: en memoria, solo por IP | Aceptada | 2026-07-28 |
| [020](./020-algoritmo-slots.md) | Algoritmo de cálculo de disponibilidad (Sprint 5) | Aceptada | 2026-07-29 |
