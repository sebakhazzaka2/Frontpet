import { describe, it, expect } from "vitest"
import { canTransitionStatus } from "@/lib/status-transitions"

describe("canTransitionStatus", () => {
  it("permite PENDING -> CONFIRMED e PENDING -> CANCELLED", () => {
    expect(canTransitionStatus("PENDING", "CONFIRMED")).toBe(true)
    expect(canTransitionStatus("PENDING", "CANCELLED")).toBe(true)
  })

  it("permite CONFIRMED -> CANCELLED, mas não CONFIRMED -> PENDING", () => {
    expect(canTransitionStatus("CONFIRMED", "CANCELLED")).toBe(true)
    expect(canTransitionStatus("CONFIRMED", "PENDING")).toBe(false)
  })

  it("não permite nenhuma transição a partir de CANCELLED", () => {
    expect(canTransitionStatus("CANCELLED", "PENDING")).toBe(false)
    expect(canTransitionStatus("CANCELLED", "CONFIRMED")).toBe(false)
  })

  it("permite manter o mesmo status (opção já selecionada)", () => {
    expect(canTransitionStatus("PENDING", "PENDING")).toBe(true)
    expect(canTransitionStatus("CONFIRMED", "CONFIRMED")).toBe(true)
    expect(canTransitionStatus("CANCELLED", "CANCELLED")).toBe(true)
  })
})
