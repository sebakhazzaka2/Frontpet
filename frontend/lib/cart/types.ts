// CartItem: lo mínimo para mostrar la sacola y armar CreateOrderItemRequest
// (backend/src/main/java/com/frontpet/orders/dto/CreateOrderItemRequest.java)
// al enviar el pedido. `variantId` es `number` porque las variantes no tienen
// public_id (ADR 013 §1) — se referencian por su id interno, igual que hace
// `product_variants`/`order_items` en la DB (ver javadoc de ProductVariantDto.id
// y de CreateOrderItemRequest).
//
// No guarda precio como fuente de verdad para el pedido: el backend siempre
// recalcula y congela el snapshot (nunca confía el precio del cliente). Acá
// `unitPrice` solo sirve para pintar la UI del carrito antes de enviar.
export interface CartItem {
  productPublicId: string
  productSlug: string
  productNome: string
  productImageUrl?: string
  variantId?: number
  variantNome?: string
  unitPrice: number
  quantidade: number
}

// Clave única de línea del carrito: mismo producto con variantes distintas
// son líneas distintas (ej. Ração 10kg y Ração 15kg del mismo produto).
export function cartItemKey(item: Pick<CartItem, 'productPublicId' | 'variantId'>): string {
  return item.variantId != null ? `${item.productPublicId}:${item.variantId}` : item.productPublicId
}
