# ADR 007 — Idioma del producto: portugués brasileño (PT-BR)

**Estado**: Aceptada
**Fecha**: 2026-05-20
**Sprint**: 2

---

## Contexto

Al arrancar el diseño visual del MVP1 (frontend con Stitch + Tailwind), apareció una
inconsistencia entre los prototipos visuales del repo y la realidad del cliente:

- **Prototipos** (`docs/prototypes/FrontPet.jsx` y `frontpet-landing.html`): copy en español.
- **Instagram real del cliente** (@frontpet.br): copy 100% en portugués brasileño
  ("Banho & Tosa profissional, rações premium com entrega GRÁTIS em Rivera e Livramento").
- **CLAUDE.md** hasta v1.3: no especificaba idioma del producto, solo del repo.

FrontPet opera en Santana do Livramento (Rio Grande do Sul, Brasil), ciudad fronteriza
con Rivera (Uruguay). La zona es bilingüe receptiva — un uruguayo de Rivera entiende
perfectamente portugués escrito — pero el cliente eligió comunicarse en PT-BR con su
audiencia. Su negocio físico y digital ya opera en ese idioma.

La decisión bloquea el resto del trabajo visual: el copy de la UI cambia todo
(microcopy de botones, validaciones, nombres de servicios, mensajes pre-cargados de
WhatsApp, emails de notificación, etc.).

---

## Decisión

**El producto se entrega en portugués brasileño (PT-BR) únicamente para MVP1.**

Esto aplica a:

- Todo el copy de la UI pública (landing, catálogo, servicios, booking, carrito).
- Todo el copy del panel admin (login, dashboard, CRUDs, listados).
- Mensajes pre-cargados de WhatsApp (`wa.me?text=...`).
- Validaciones de formulario y mensajes de error visibles al usuario.
- Metadata SEO (`<title>`, `meta description`, OG tags).
- Emails transaccionales (si aplica en Sprint 4+).
- Nombres canónicos de servicios siguiendo el Instagram del cliente:
  - "Banho & Tosa" (no "Baño y Corte")
  - "Rações" (no "Alimentos")
  - Etc.

**No cambia** (siguen en español o inglés):

- ADRs, documentación técnica en `docs/`
- `CLAUDE.md` y memoria de Claude
- Comentarios de código
- Mensajes de commit (siguen siendo Conventional Commits en inglés)
- Nombres de variables, funciones, tablas, columnas (siguen en inglés según sección 5
  del CLAUDE.md)
- Logs internos y mensajes de error de backend hacia logs

---

## Alternativas consideradas

### ❌ Español único

**A favor**:
- Match con los prototipos existentes
- Comodidad del desarrollador (es hispanohablante)
- Cubre clientes uruguayos de Rivera de forma "nativa"

**En contra**:
- No matchea con la audiencia primaria del cliente (Brasil)
- No matchea con su Instagram, su comunicación actual ni su localidad legal
- Genera disonancia: cliente recibe pedidos en portugués por WhatsApp pero el sitio
  está en español
- Reduce confianza para el visitante brasileño (lectura forzada en idioma extranjero)

**Veredicto**: la conveniencia del desarrollador no justifica desalinear el producto
de la audiencia y la operación real del cliente.

### ❌ Bilingüe PT-BR/ES con selector desde MVP1

**A favor**:
- Cubre las dos audiencias de la zona frontera
- "Profesionaliza" la app

**En contra**:
- Doble trabajo de copy en cada pantalla
- Requiere infraestructura de i18n (`next-intl` o similar) desde el día uno
- Decisión de cuál es el idioma por defecto sigue abierta
- Va contra la regla "no abstracciones sin 3 casos de uso reales" (CLAUDE.md sección 6)
- No hay evidencia todavía de que la audiencia uruguaya pida la versión en español

**Veredicto**: over-engineering para MVP1. Se evalúa en Fase 2+ si aparece demanda real.

### ❌ Detección automática por geolocalización / `Accept-Language`

**A favor**:
- "Inteligente"

**En contra**:
- Falsos positivos masivos en zona frontera (un brasileño puede tener el celular en
  español si vive en Uruguay y viceversa)
- Sin selector manual, frustra al usuario
- Aún requiere mantener dos copys
- Suma complejidad sin valor claro

**Veredicto**: anti-patrón conocido de i18n. Descartado.

---

## Consecuencias

### Positivas

- Alineación total con la comunicación actual del cliente y su audiencia.
- Reduce fricción para el visitante brasileño (idioma nativo).
- Simplifica el MVP1: una sola fuente de copy, sin abstracción de i18n.
- Permite que los nombres de servicios y categorías del backend sean en PT-BR sin
  traducción intermedia.
- Coherencia con el Instagram del cliente y los mensajes de WhatsApp manuales.

### Negativas

- Los prototipos visuales (`docs/prototypes/`) quedan desactualizados en idioma.
  Quedan como referencia visual, no de copy.
- Desarrollador (hispanohablante) debe verificar copy en PT-BR con el cliente antes
  de cada release. Riesgo de errores idiomáticos sutiles ("você" vs "tu", flexiones,
  etc.).
- Clientes uruguayos de Rivera leen el sitio en portugués. Si bien la mayoría lo
  entiende, hay un porcentaje que puede sentirlo como barrera.

### Mitigaciones

- Pasarle al cliente todo el copy final antes de release para revisión.
- Considerar contratar un revisor nativo PT-BR para una pasada de QA antes del Sprint
  Despliegue.
- Mantener un glosario PT-BR en `docs/copy-glossary.md` con términos canónicos
  ("Agendar horário", "Pedir pelo WhatsApp", "Banho & Tosa", "Adicionar ao carrinho",
  etc.) que el desarrollador y Claude puedan referenciar para no inventar variantes.
- En Fase 2, si hay datos analíticos que muestran bounce alto desde IPs uruguayas,
  reevaluar la decisión y considerar bilingüe.

---

## Notas para el futuro

Cuando se justifique (datos de Plausible muestran tráfico uruguayo significativo +
bounce alto + feedback explícito del cliente):

- Agregar selector ES/PT con `next-intl` (Fase 2).
- Estructurar copy en archivos `messages/pt-BR.json` y `messages/es.json`.
- Default idioma según `Accept-Language` con fallback PT-BR y selector visible
  en el nav.
- Backend: agregar campo `locale` opcional en `tenant.config` para soportar la
  futura visión multi-tenant donde otros negocios puedan elegir su idioma.

**Estimación de Fase 2 con i18n**: 1 sprint adicional + revisión de copy completo.
