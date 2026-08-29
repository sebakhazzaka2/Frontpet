import { z } from 'zod'

// Passo 3 do wizard de agendamento (issue #61) — zod v4, mismo critério de
// lib/schemas/checkout.ts (CLAUDE.md §2: API distinta a v3, mensagens como
// segundo argumento string).
//
// Campos alinhados 1:1 com CreateAppointmentRequest.java (backend/.../booking/dto/),
// não com o mock de Stitch — o form real de "Passo 3 (Fluxo Interno)" já bate
// com esses 5 campos, sem drift como o checkout teve com pagamento/frete.
export const bookingDetailsSchema = z.object({
  clienteNome: z
    .string()
    .trim()
    .min(2, 'Informe seu nome completo.')
    .max(160, 'Nome muito longo.'),
  clienteTelefone: z
    .string()
    .trim()
    .min(8, 'Informe um WhatsApp válido.')
    .max(30, 'Telefone muito longo.')
    .regex(/^[\d\s()+-]+$/, 'Use apenas números, espaços e símbolos como + ( ) -.'),
  petNome: z
    .string()
    .trim()
    .min(1, 'Informe o nome do seu pet.')
    .max(80, 'Nome muito longo.'),
  petRaca: z.string().trim().max(80).optional(),
  observacoes: z.string().trim().max(500).optional(),
  // Checkbox LGPD (ADR 024) — o booking captura nome, telefone, pet e
  // observações desde o Sprint 5/6, mas nunca ganhou o mesmo consentimento
  // explícito que checkout.ts já tem desde a tarea 4.16.
  consentimentoLgpd: z.boolean().refine((value) => value === true, {
    message: 'É necessário aceitar a política de privacidade.',
  }),
  // Honeypot (mismo patrón anti-bot de checkout.ts / CreateOrderRequest,
  // tarea 4.15/5.8) — sin .default(), el default real lo provee
  // defaultValues de react-hook-form.
  honeypot: z.string().max(0).optional(),
})

export type BookingDetailsFormValues = z.infer<typeof bookingDetailsSchema>
