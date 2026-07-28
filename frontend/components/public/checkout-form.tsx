'use client'

import Link from 'next/link'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Send } from 'lucide-react'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Checkbox } from '@/components/ui/checkbox'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useCart } from '@/hooks/use-cart'
import { createOrder, type CreateOrderRequest, type FormaPagamento } from '@/lib/api/orders'
import { ApiFetchError } from '@/lib/api/client'
import { checkoutSchema, type CheckoutFormValues } from '@/lib/schemas/checkout'
import { buildOrderMessage } from '@/lib/whatsapp/templates'
import { buildWhatsAppLink } from '@/lib/data/site'

const FORMA_PAGAMENTO_LABELS: Record<FormaPagamento, string> = {
  DINHEIRO: 'Dinheiro',
  PIX: 'PIX',
  CARTAO_DEBITO: 'Cartão de débito',
  CARTAO_CREDITO: 'Cartão de crédito',
}

// Port de "Sua Sacola", sección "Resumo do Pedido" (checkout embebido — Stitch
// dibuja solo nome/telefone/modalidade; acá se agregan endereço, forma de
// pagamento e horário porque ADR 003 (act. 2026-07-09/28) y a migração real
// os exigem — ver drift #3 do plano do Sprint 4). Primer form del repo:
// React Hook Form + zodResolver (zod v4).
export function CheckoutForm() {
  const { items, subtotal, clear } = useCart()
  const [submitError, setSubmitError] = useState<string | null>(null)

  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CheckoutFormValues>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: {
      clienteNome: '',
      clienteTelefone: '',
      modalidade: 'ENTREGA',
      enderecoEntrega: '',
      formaPagamento: undefined,
      horarioEntrega: '',
      consentimentoLgpd: false,
      honeypot: '',
    },
  })

  const modalidade = useWatch({ control, name: 'modalidade' })

  async function onSubmit(values: CheckoutFormValues) {
    setSubmitError(null)

    const request: CreateOrderRequest = {
      clienteNome: values.clienteNome,
      clienteTelefone: values.clienteTelefone,
      modalidade: values.modalidade,
      enderecoEntrega: values.modalidade === 'ENTREGA' ? values.enderecoEntrega : undefined,
      formaPagamento: values.formaPagamento,
      horarioEntrega: values.horarioEntrega || undefined,
      consentimentoLgpd: values.consentimentoLgpd,
      honeypot: values.honeypot ?? '',
      items: items.map((item) => ({
        productPublicId: item.productPublicId,
        variantId: item.variantId,
        quantidade: item.quantidade,
      })),
    }

    try {
      const order = await createOrder(request)
      // El pedido ya quedó persistido acá — clear() y el redirect a wa.me
      // pasan DESPUÉS, nunca antes (ADR 003: se guarda aunque el cliente no
      // llegue a enviar el WhatsApp).
      clear()
      const message = buildOrderMessage(order)
      window.location.assign(buildWhatsAppLink(message))
    } catch (err) {
      if (err instanceof ApiFetchError && err.fieldErrors) {
        for (const [field, message] of Object.entries(err.fieldErrors)) {
          if (field in values) {
            setError(field as keyof CheckoutFormValues, { message })
          }
        }
        setSubmitError('Verifique os campos destacados.')
      } else {
        setSubmitError('Não foi possível enviar o pedido. Tente novamente em instantes.')
      }
    }
  }

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="flex flex-col gap-4 rounded-xl border border-white bg-surface-container p-4 shadow-card"
    >
      <h2 className="font-display text-h2-mobile text-navy">Resumo do Pedido</h2>

      <Field data-invalid={!!errors.clienteNome}>
        <FieldLabel htmlFor="clienteNome">Nome completo</FieldLabel>
        <Input id="clienteNome" placeholder="Como devemos te chamar?" {...register('clienteNome')} />
        <FieldError errors={[errors.clienteNome]} />
      </Field>

      <Field data-invalid={!!errors.clienteTelefone}>
        <FieldLabel htmlFor="clienteTelefone">Telefone</FieldLabel>
        <Input
          id="clienteTelefone"
          type="tel"
          placeholder="(00) 00000-0000"
          {...register('clienteTelefone')}
        />
        <FieldError errors={[errors.clienteTelefone]} />
      </Field>

      <Field data-invalid={!!errors.modalidade}>
        <FieldLabel>Modalidade</FieldLabel>
        <Controller
          control={control}
          name="modalidade"
          render={({ field }) => (
            <RadioGroup
              value={field.value}
              onValueChange={field.onChange}
              className="grid grid-cols-2 gap-1 rounded-md border border-outline bg-white p-1"
            >
              {(['ENTREGA', 'RETIRADA'] as const).map((value) => (
                <label
                  key={value}
                  className={
                    field.value === value
                      ? 'flex cursor-pointer items-center justify-center gap-2 rounded-md bg-navy py-2 text-sm font-medium text-white'
                      : 'flex cursor-pointer items-center justify-center gap-2 rounded-md py-2 text-sm text-ink-muted transition-colors hover:bg-surface'
                  }
                >
                  <RadioGroupItem value={value} className="sr-only" />
                  {value === 'ENTREGA' ? 'Entrega' : 'Retirada'}
                </label>
              ))}
            </RadioGroup>
          )}
        />
        <FieldError errors={[errors.modalidade]} />
      </Field>

      {modalidade === 'ENTREGA' && (
        <Field data-invalid={!!errors.enderecoEntrega}>
          <FieldLabel htmlFor="enderecoEntrega">Endereço de entrega</FieldLabel>
          <Input
            id="enderecoEntrega"
            placeholder="Rua, número, bairro"
            {...register('enderecoEntrega')}
          />
          <FieldError errors={[errors.enderecoEntrega]} />
        </Field>
      )}

      <Field data-invalid={!!errors.formaPagamento}>
        <FieldLabel>Forma de pagamento</FieldLabel>
        <Controller
          control={control}
          name="formaPagamento"
          render={({ field }) => (
            <Select value={field.value} onValueChange={field.onChange}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Como você vai pagar?" />
              </SelectTrigger>
              <SelectContent>
                {Object.entries(FORMA_PAGAMENTO_LABELS).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        />
        <FieldError errors={[errors.formaPagamento]} />
      </Field>

      <Field>
        <FieldLabel htmlFor="horarioEntrega">Horário preferido (opcional)</FieldLabel>
        <Input
          id="horarioEntrega"
          placeholder="Ex: à tarde, depois das 18h..."
          {...register('horarioEntrega')}
        />
      </Field>

      {/* Honeypot (tarea 4.15): invisible para humanos, sin label — un bot
          que preenche tudo por seletor de tipo de campo cai aqui. */}
      <input
        type="text"
        tabIndex={-1}
        autoComplete="off"
        aria-hidden="true"
        className="absolute h-0 w-0 opacity-0"
        {...register('honeypot')}
      />

      <div className="flex flex-col gap-2 border-t border-outline/30 pt-4 text-ink-muted">
        <div className="flex items-center justify-between text-sm">
          <span>Subtotal</span>
          <span>{new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(subtotal)}</span>
        </div>
      </div>

      <Field orientation="horizontal" data-invalid={!!errors.consentimentoLgpd}>
        <Controller
          control={control}
          name="consentimentoLgpd"
          render={({ field }) => (
            <Checkbox
              id="consentimentoLgpd"
              checked={field.value}
              onCheckedChange={field.onChange}
            />
          )}
        />
        <FieldLabel htmlFor="consentimentoLgpd" className="text-sm font-normal">
          Li e aceito a{' '}
          <Link href="/privacidade" target="_blank" className="underline hover:text-navy">
            política de privacidade
          </Link>
        </FieldLabel>
      </Field>
      <FieldError errors={[errors.consentimentoLgpd]} />

      <div className="rounded-lg bg-navy/5 p-3 text-caption text-navy">
        Essa finalização vai te levar a uma mensagem pré-formatada no WhatsApp — qualquer dúvida
        pode ser adicionada na mensagem antes de enviar.
      </div>

      {submitError && <p className="text-sm text-destructive">{submitError}</p>}

      <button
        type="submit"
        disabled={isSubmitting || items.length === 0}
        className="flex h-14 items-center justify-center gap-2 rounded-xl bg-wa font-display text-lg font-semibold text-white transition-all hover:opacity-90 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Send className="size-5" />
        {isSubmitting ? 'Enviando...' : 'Finalizar pelo WhatsApp'}
      </button>
    </form>
  )
}
