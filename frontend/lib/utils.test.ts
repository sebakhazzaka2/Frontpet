import { describe, it, expect } from "vitest"
import { cn, formatPrice } from "@/lib/utils"

// Test de ejemplo / smoke test: prueba el helper `cn` (merge de clases Tailwind).
// Sirve como plantilla del patrón de unit test. Cuando aparezca lógica de
// negocio (cálculo de carrito, builder de mensajes de WhatsApp, validaciones
// Zod), se testea igual: archivo `.test.ts` al lado del código.
describe("cn", () => {
  it("junta clases", () => {
    expect(cn("px-2", "py-1")).toBe("px-2 py-1")
  })

  it("resuelve clases Tailwind en conflicto (gana la última)", () => {
    expect(cn("px-2", "px-4")).toBe("px-4")
  })

  it("ignora valores falsy (útil para clases condicionales)", () => {
    expect(cn("px-2", false, null, undefined, "py-1")).toBe("px-2 py-1")
  })
})

describe("formatPrice", () => {
  it("formatea en BRL con vírgula decimal", () => {
    expect(formatPrice(185)).toBe("R$ 185,00")
  })

  it("redondea a 2 decimais", () => {
    expect(formatPrice(42.9)).toBe("R$ 42,90")
  })
})
