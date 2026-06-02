This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Testing (shift-left)

Estrategia: meter testing temprano, empezando por lo barato y de alto valor.
Seguimos la pirámide de testing — muchos unit tests rápidos, pocos E2E lentos.

### Qué está configurado hoy

**Vitest + Testing Library** para unit e integration tests:

- `vitest.config.ts` — config (entorno jsdom, alias `@`, dónde busca tests).
- `vitest.setup.ts` — matchers de jest-dom (`toBeInTheDocument`, etc.) + cleanup.
- Los tests viven **al lado del código**: `lib/utils.ts` → `lib/utils.test.ts`.

```bash
pnpm test          # corre todos los tests una vez (modo CI)
pnpm test:watch    # modo watch mientras desarrollás
```

Qué testear acá: lógica pura (cálculo de carrito, builder de mensajes de
WhatsApp, validaciones Zod) y componentes aislados. Corre en milisegundos,
sin navegador real (jsdom simula el DOM en memoria → rápido pero no 100% fiel).

### Qué NO está configurado todavía: E2E

**E2E (end-to-end)** prueba el flujo completo en un navegador real (abrir la
web, buscar producto, agregar al carrito, ir al checkout, verificar el link de
WhatsApp). Las herramientas líderes son **Playwright** (Microsoft) y **Cypress**.

Decisión: cuando llegue el momento, ir con **Playwright** (prueba en WebKit/Safari
real — importa porque los clientes usan iPhone — y paraleliza mejor que Cypress).

**No está montado a propósito.** Montar E2E ahora, sin flujos funcionando, es
testear el vacío. Se agrega en Sprint 1/2, cuando existan checkout y booking.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
