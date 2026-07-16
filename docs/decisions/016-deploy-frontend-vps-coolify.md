# ADR 016 — Deploy: VPS único con Coolify, Cloudflare como CDN

**Estado**: Aceptada
**Fecha**: 2026-07-16
**Sprint**: previo al Sprint Despliegue (semana 5)
**Reemplaza**: la decisión "Cloudflare Pages para el frontend" que asumía CLAUDE.md sección 2

---

## Contexto

El plan original (mayo 2026) era: backend en un VPS Hetzner CX22 con Coolify, frontend en
**Cloudflare Pages**. Al verificar ese supuesto en julio 2026 contra el stack real
(Next 16.2.6), se cayó:

- **`@cloudflare/next-on-pages` está deprecado.** Cloudflare hoy recomienda el adapter de
  **OpenNext sobre Workers**, no Pages.
- OpenNext dice soportar todas las minor de Next 16, pero el
  [issue #13755](https://github.com/cloudflare/workers-sdk/issues/13755) documenta un
  catch-22: el `proxy.ts` de Next 16 corre **solo** en runtime Node e importa `async_hooks`,
  que no existe en Workers. El issue está **cerrado por falta de respuesta del reportante,
  no por estar resuelto**.

En paralelo se revisó si convenía cambiar de proveedor por latencia. El cliente opera en
Santana do Livramento (RS, Brasil) y el sitio es de ventas y conversión: la preocupación era
que fotos y páginas cargaran rápido.

### El dato que reordenó la decisión

**Cloudflare tiene un PoP en Porto Alegre (código POA)**, en Rio Grande do Sul, a ~500 km del
cliente. Es el CDN con más ubicaciones de Brasil (30) y de LATAM (55).

Con Cloudflare adelante, el reparto real de responsabilidades queda así:

| Qué | Se sirve desde | ¿Depende del origin? |
|---|---|---|
| Landing (estática, prerenderizada) | Cloudflare POA | **No** |
| Catálogo (ISR) | Cloudflare POA | **No** |
| Fotos de productos (R2 + `next/image`) | Cloudflare POA | **No** |
| Carrito (sessionStorage) | el browser, cero red | **No** |
| `GET /availability`, `POST /orders`, admin | el VPS | **Sí** |

**Todo el camino de venta y conversión está en el edge.** La latencia al origin solo afecta
a un puñado de requests: los slots de booking, el POST del pedido, y el panel admin (que lo
usa el dueño, no los clientes).

Además: el catálogo **no es grande**. MVP1 arranca con 10 productos seed y el cliente es un
petshop de barrio. Optimizar el origin por volumen de catálogo sería optimización prematura.

---

## Decisión

### 1. Un solo VPS sirve backend y frontend, vía Coolify

Nada de Cloudflare Pages ni Workers para el frontend. Coolify buildea y sirve Next 16 nativo,
al lado de Spring Boot y Postgres.

**Efecto lateral importante**: esto **saca a OpenNext del camino crítico**. Sin adapter no hay
issue #13755, no hay conflicto con `proxy.ts`, no hay spike de 2 hs que hacer. Cloudflare queda
solo donde funciona sin fricción: DNS, CDN y R2.

### 2. Hetzner **CX32** (4 vCPU / 8 GB), no CX22

El CX22 (2 vCPU / 4 GB) del plan original no alcanza:

| Componente | RAM aprox. |
|---|---|
| Coolify (Docker, su Postgres, Redis, Soketi) | ~2 GB |
| JVM de Spring Boot | ~512 MB – 1 GB |
| Postgres de la app | ~256 – 512 MB |
| Next.js (build + runtime) | ~150 – 300 MB |

En 4 GB no entra con holgura. El CX32 cuesta ~€3/mes más que el CX22 — menos que un café,
contra un día entero peleando OOM kills durante el sprint.

### 3. Región **US East (Ashburn)**, no Alemania

Latencias aproximadas a Rio Grande do Sul (estimaciones de conocimiento general, **medir antes
de comprar**):

| Región | Latencia aprox. |
|---|---|
| Hetzner Alemania (default del plan viejo) | ~200 ms |
| **Hetzner US East (Ashburn)** | **~130 ms** |
| Vultr / AWS São Paulo | ~30 ms |

Ashburn son ~70 ms gratis contra Alemania. Y como solo afecta a las llamadas de API (no al
contenido cacheado), no justifica pagar por São Paulo — ver alternativas.

---

## Consecuencias

**Positivas**
- Un solo proveedor de cómputo, un solo lugar donde mirar logs.
- Sin adapters, sin trampas de versión de Next, sin vendor nuevo que aprender.
- El Sprint Despliegue se simplifica: desaparece la tarea D.6 (Cloudflare Pages) y el spike
  de OpenNext que iba a hacer falta.
- Costo de infra estable y bajo (~€7.50/mes) contra un proyecto de USD 500 one-time.

**Negativas / riesgos aceptados**
- **Un solo punto de falla**: si el VPS se cae, se cae todo — sitio y API. Mitigación parcial:
  Cloudflare sigue sirviendo lo cacheado un rato. Aceptable para MVP1 de un negocio local.
- **~130 ms de latencia** en las llamadas de API. Aceptado: son pocos requests y ninguno está
  en el camino de conversión.
- **El VPS hace el trabajo de `next/image`** (optimización de imágenes cuesta CPU). Los
  resultados los cachea Cloudflare, así que el costo es solo la primera vez por imagen.
- Coolify es nuevo para el equipo → leer su doc **antes** del sprint, no durante (ya estaba
  anotado como riesgo en el ROADMAP).

**Reversibilidad**: alta. Si al medir en producción la latencia de la API molesta de verdad,
mover el backend a São Paulo (Vultr) es un deploy, no un rediseño. Se decide con datos, no
por anticipado.

---

## Alternativas descartadas

- **Cloudflare Workers + OpenNext**: es lo que Cloudflare recomienda, pero el conflicto con
  `proxy.ts` de Next 16 no está confirmado como resuelto. Máxima superficie de riesgo, cero
  beneficio compensatorio para este proyecto.
- **Vercel**: técnicamente perfecto (ellos hacen Next), pero el tier Hobby es de uso **no
  comercial** y FrontPet es un proyecto pago. Pro son ~USD 20/mes = USD 240/año contra un
  contrato de USD 500 one-time. No cierra.
- **AWS (sa-east-1 São Paulo)**: la mejor latencia (~30 ms), pero requiere EC2/ECS + RDS +
  ALB + ECR + VPC + IAM. Solo el load balancer son ~USD 18/mes; el total realista es USD
  60-100/mes, que se come el valor del contrato en menos de 6 meses. Y el Sprint Despliegue
  tiene **10 horas** presupuestadas: aprender IAM y VPC no entra. Reevaluar **si** el
  producto llega a ser el SaaS multi-tenant de la visión de largo plazo.
- **Vultr São Paulo**: ~30 ms de latencia, pero ~USD 48/mes por 8 GB — 6x el costo de Hetzner
  — a cambio de acelerar requests que los clientes casi no hacen.
- **Seguir con CX22 y ajustar memoria**: se puede tirar Coolify y usar docker-compose + Caddy
  a mano, ahorrando ~2 GB. Pero se pierde auto-deploy y UI de logs, que es justamente lo que
  hace que un dev solo pueda operar esto. €3/mes es más barato que ese tiempo.
