import { z } from 'zod'

// Primer form del repo (issue #30) — API de zod v4, distinta a v3 (CLAUDE.md
// §2): sin z.string().email() deprecado, mensajes de error como segundo
// argumento string simple. Validado contra la doc oficial de zod 4.4, no
// copiado de snippets v3.
//
// Campos alineados 1:1 con CreateOrderRequest.java (backend/.../orders/dto/),
// no con los 3 campos que dibuja Stitch — ver drift #3 del plan de Sprint 4 y
// ADR 003 (act. 2026-07-28).
export const checkoutSchema = z
  .object({
    clienteNome: z
      .string()
      .trim()
      .min(2, 'Informe seu nome completo.')
      .max(160, 'Nome muito longo.'),
    clienteTelefone: z
      .string()
      .trim()
      .min(8, 'Informe um telefone válido.')
      .max(30, 'Telefone muito longo.')
      .regex(/^[\d\s()+-]+$/, 'Use apenas números, espaços e símbolos como + ( ) -.'),
    modalidade: z.enum(['ENTREGA', 'RETIRADA'], {
      message: 'Selecione a modalidade de entrega.',
    }),
    enderecoEntrega: z.string().trim().max(500).optional(),
    formaPagamento: z.enum(['DINHEIRO', 'PIX', 'CARTAO_DEBITO', 'CARTAO_CREDITO'], {
      message: 'Selecione a forma de pagamento.',
    }),
    horarioEntrega: z.string().trim().max(120).optional(),
    consentimentoLgpd: z.boolean().refine((value) => value === true, {
      message: 'É necessário aceitar a política de privacidade.',
    }),
    // Honeypot (tarea 4.15): campo invisible para humanos vía CSS — un
    // formulario real siempre lo manda vacío. Sin mensaje de error visible
    // a propósito: si un bot lo completa, no debe enterarse de por qué falló.
    // Sin `.default()`: el default real lo provee `defaultValues` de
    // react-hook-form — con `.default()` acá, el tipo de entrada de zod
    // (`string | undefined`) diverge del de salida (`string`), y
    // `useForm<CheckoutFormValues>` exige que ambos coincidan.
    honeypot: z.string().max(0).optional(),
  })
  .superRefine((data, ctx) => {
    if (data.modalidade === 'ENTREGA' && (!data.enderecoEntrega || data.enderecoEntrega.length < 5)) {
      ctx.addIssue({
        code: 'custom',
        path: ['enderecoEntrega'],
        message: 'Informe o endereço completo para entrega.',
      })
    }
  })

export type CheckoutFormValues = z.infer<typeof checkoutSchema>
