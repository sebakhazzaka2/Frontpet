import { defineConfig } from "vitest/config"
import react from "@vitejs/plugin-react"
import { resolve } from "node:path"

// Configuración de Vitest para unit + integration tests del frontend.
// E2E (flujos completos en navegador real) NO va acá: eso será Playwright
// más adelante, cuando existan los flujos (checkout, booking). Ver README.
export default defineConfig({
  plugins: [react()],
  test: {
    // jsdom simula el DOM en memoria → permite testear componentes React
    // sin abrir un navegador de verdad (rápido). No es 100% fiel: para eso
    // está E2E.
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    // Co-locamos los tests al lado del código: foo.ts → foo.test.ts
    include: ["**/*.{test,spec}.{ts,tsx}"],
    exclude: ["node_modules", ".next"],
    css: false,
  },
  resolve: {
    // Mismo alias "@" que tsconfig.json para que los imports funcionen igual.
    alias: {
      "@": resolve(__dirname, "./"),
    },
  },
})
