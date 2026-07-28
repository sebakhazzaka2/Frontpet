# Next 16 — cambios que nos pegan directo

> Movido desde `CLAUDE.md` §2 (2026-07-27). Trampas concretas para las tareas del ROADMAP.
> **No escribir código estilo Next 14.**
> Doc oficial: https://nextjs.org/docs/app/guides/upgrading/version-16

- **Async Request APIs (breaking).** `params`, `searchParams`, `cookies()`, `headers()` y
  `draftMode()` son **Promises**. El acceso síncrono fue eliminado del todo en la 16.
  ```tsx
  // ❌ Next 14                          // ✅ Next 16
  function Page({ params }) {            export default async function Page(
    const { slug } = params                props: PageProps<'/produtos/[slug]'>
  }                                      ) {
                                           const { slug } = await props.params
                                         }
  ```
  Pega en `/produtos/[slug]` (tarea 3.9) y en el auth guard del admin (4.11), que lee la
  cookie JWT con `cookies()`. `npx next typegen` genera los helpers `PageProps` /
  `LayoutProps` / `RouteContext` tipados por ruta.
- **`middleware.ts` → `proxy.ts`.** El nombre `middleware` está deprecado. **`proxy` corre
  solo en runtime Node, no soporta edge.** Ver la nota de Cloudflare en el ADR 016.
- **`next lint` fue eliminado.** Ya está bien en `package.json` (`"lint": "eslint"`).
  `next build` ya no linta.
- **El caching cambió.** `revalidateTag` ahora pide un segundo argumento (perfil de
  `cacheLife`). PPR salió de experimental y se activa con `cacheComponents`. **La tarea 3.11
  del ROADMAP ("ISR / revalidate 60s") está escrita para Next 14 — revisar contra la doc de
  16 antes de implementarla.**
- **`next/image`**: `images.domains` está deprecado → usar `images.remotePatterns` (necesario
  para el bucket R2 de la tarea 3.5). El default de `qualities` pasó a solo `[75]` y
  `minimumCacheTTL` de 60s a 4h.
- **Turbopack es el default** en `dev` y `build`. Los scripts ya están correctos.
