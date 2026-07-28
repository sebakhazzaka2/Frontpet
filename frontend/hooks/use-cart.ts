'use client'

import { useCallback, useSyncExternalStore } from 'react'
import { cartItemKey, type CartItem } from '@/lib/cart/types'

// Carrito en sessionStorage, no persistido hasta el envío del pedido (CLAUDE.md
// §3/§6) — se pierde al cerrar la pestaña, y cada pestaña tiene el suyo (a
// diferencia de localStorage). No hay backend involucrado hasta el checkout.
const STORAGE_KEY = 'frontpet:carrinho'

// Store externo mínimo (sin librería — CLAUDE.md §6 "¿se resuelve con
// vanilla?"): varios componentes (CartButton en el Nav, BottomNav, la propia
// página /carrinho) necesitan leer y escribir el mismo carrito sin quedar
// anidados bajo un Context Provider. `useSyncExternalStore` (React 18+) es la
// primitiva justa para esto, y de paso resuelve la hidratación SSR/CSR sola:
// `getServerSnapshot` devuelve carrito vacío (no hay sessionStorage en el
// server), React reconcilia con el snapshot real recién después del mount,
// sin el parpadeo de un `useEffect` + estado "mounted" manual.
type Listener = () => void
const listeners = new Set<Listener>()

// Cache del snapshot actual: useSyncExternalStore exige que getSnapshot
// devuelva la MISMA referencia si los datos no cambiaron, para no entrar en
// loop de renders. Se actualiza únicamente dentro de writeStorage.
let cachedItems: CartItem[] | null = null

// Misma razón que cachedItems, pero para getServerSnapshot: un `[]` literal
// nuevo en cada call rompe la garantía de referencia estable de
// useSyncExternalStore — React lo detecta y avisa "should be cached to avoid
// an infinite loop" en cada render (confirmado con Playwright al validar el
// Bloque A). Server y cliente sin hidratar comparten el mismo carrito vacío.
const EMPTY_CART: CartItem[] = []

function readStorage(): CartItem[] {
  try {
    const raw = window.sessionStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as CartItem[]) : []
  } catch {
    // sessionStorage bloqueado (modo privado agresivo) o JSON corrupto:
    // el carrito arranca vacío en vez de romper la página.
    return []
  }
}

function writeStorage(items: CartItem[]) {
  cachedItems = items
  try {
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(items))
  } catch {
    // idem readStorage: si sessionStorage falla, el estado en memoria sigue
    // sirviendo para esta sesión de la pestaña aunque no sobreviva a un reload.
  }
  listeners.forEach((listener) => listener())
}

function subscribe(listener: Listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

function getSnapshot(): CartItem[] {
  if (cachedItems === null) {
    cachedItems = readStorage()
  }
  return cachedItems
}

function getServerSnapshot(): CartItem[] {
  return EMPTY_CART
}

export function useCart() {
  const items = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)

  // Agregar una línea ya existente (mismo produto + mesma variante) suma la
  // cantidad en vez de duplicar la línea — es el comportamiento esperado al
  // apretar "Adicionar à sacola" dos veces sobre el mismo item.
  const addItem = useCallback((item: CartItem) => {
    const current = getSnapshot()
    const key = cartItemKey(item)
    const index = current.findIndex((existing) => cartItemKey(existing) === key)

    if (index >= 0) {
      const next = [...current]
      next[index] = { ...next[index], quantidade: next[index].quantidade + item.quantidade }
      writeStorage(next)
    } else {
      writeStorage([...current, item])
    }
  }, [])

  const removeItem = useCallback((key: string) => {
    writeStorage(getSnapshot().filter((item) => cartItemKey(item) !== key))
  }, [])

  // quantidade <= 0 elimina la línea — es lo que espera un <QuantityStepper>
  // cuando el botón "-" llega a cero.
  const updateQuantity = useCallback(
    (key: string, quantidade: number) => {
      if (quantidade <= 0) {
        removeItem(key)
        return
      }
      writeStorage(getSnapshot().map((item) => (cartItemKey(item) === key ? { ...item, quantidade } : item)))
    },
    [removeItem]
  )

  const clear = useCallback(() => {
    writeStorage([])
  }, [])

  const itemCount = items.reduce((sum, item) => sum + item.quantidade, 0)
  const subtotal = items.reduce((sum, item) => sum + item.unitPrice * item.quantidade, 0)

  return { items, addItem, removeItem, updateQuantity, clear, itemCount, subtotal }
}
