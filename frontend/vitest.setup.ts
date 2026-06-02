// Corre una vez antes de cada archivo de test (lo referencia vitest.config.ts).
//
// 1. Agrega los matchers de jest-dom a expect: toBeInTheDocument(),
//    toHaveTextContent(), toBeDisabled(), etc. Sin esto, expect solo tiene
//    los matchers básicos de Vitest.
import "@testing-library/jest-dom/vitest"

import { afterEach } from "vitest"
import { cleanup } from "@testing-library/react"

// 2. Desmonta el DOM renderizado después de cada test, para que un test no
//    vea lo que renderizó el anterior.
afterEach(() => {
  cleanup()
})
