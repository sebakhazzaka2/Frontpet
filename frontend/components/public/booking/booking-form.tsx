'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { ApiFetchError } from '@/lib/api/client'
import { createAppointment, type CreateAppointmentRequest } from '@/lib/api/appointments'
import { bookingDetailsSchema, type BookingDetailsFormValues } from '@/lib/schemas/booking'
import type { Porte } from '@/lib/api/services'
import { fullDate } from '@/lib/booking-dates'

interface BookingFormProps {
  baseServiceId: number
  baseServiceNome: string
  addonIds: number[]
  porte: Porte
  data: string
  horario: string
  onBack: () => void
  // 409 (sem cupo): outro cliente reservou o mesmo horário enquanto este
  // preenchia o form — o wizard volta ao Passo 2 com a grade recarregada
  // (AC do issue #61), não deixa o usuário num form morto.
  onSlotUnavailable: () => void
}

// Passo 3 do wizard (Bloque D, issue #61) — RHF + zodResolver, mesmo padrão
// de checkout-form.tsx (primeiro form do repo). `baseServiceId`/`addonIds`/
// `porte`/`data`/`horario` vêm do estado do wizard (BookingWizard), nunca de
// input do usuário nesta etapa — preço/duração são recalculados server-side
// de qualquer forma (ADR 020), mas os IDs do combo têm que ser os mesmos que
// o usuário viu no resumo do Passo 1.
export function BookingForm({
  baseServiceId,
  baseServiceNome,
  addonIds,
  porte,
  data,
  horario,
  onBack,
  onSlotUnavailable,
}: BookingFormProps) {
  const router = useRouter()
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [retryAfterSeconds, setRetryAfterSeconds] = useState<number | null>(null)

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<BookingDetailsFormValues>({
    resolver: zodResolver(bookingDetailsSchema),
    defaultValues: {
      clienteNome: '',
      clienteTelefone: '',
      petNome: '',
      petRaca: '',
      observacoes: '',
      honeypot: '',
    },
  })

  async function onSubmit(values: BookingDetailsFormValues) {
    setSubmitError(null)
    setRetryAfterSeconds(null)

    const request: CreateAppointmentRequest = {
      baseServiceId,
      addonIds: addonIds.length > 0 ? addonIds : undefined,
      porte,
      data,
      horario,
      clienteNome: values.clienteNome,
      clienteTelefone: values.clienteTelefone,
      petNome: values.petNome,
      petRaca: values.petRaca || undefined,
      observacoes: values.observacoes || undefined,
      honeypot: values.honeypot ?? '',
    }

    try {
      const appointment = await createAppointment(request)
      router.push(`/agendamento/${appointment.publicId}`)
    } catch (err) {
      if (!(err instanceof ApiFetchError)) {
        setSubmitError('Não foi possível confirmar o agendamento. Tente novamente em instantes.')
        return
      }

      if (err.status === 409) {
        onSlotUnavailable()
        return
      }

      if (err.status === 429) {
        setRetryAfterSeconds(err.retryAfterSeconds ?? null)
        setSubmitError(err.message)
        return
      }

      if (err.fieldErrors) {
        for (const [field, message] of Object.entries(err.fieldErrors)) {
          if (field in values) {
            setError(field as keyof BookingDetailsFormValues, { message })
          }
        }
        setSubmitError('Verifique os campos destacados.')
      } else {
        setSubmitError(err.message)
      }
    }
  }

  return (
    <div>
      <h2 className="mb-6 text-h3 font-display text-navy">Conte um pouco sobre você e seu pet</h2>

      <div className="mb-6 flex items-start gap-3 rounded-lg bg-muted p-4">
        <div>
          <p className="text-label font-medium text-ink">Resumo do Agendamento</p>
          <p className="text-sm text-ink-muted">{baseServiceNome}</p>
          <p className="text-sm font-bold text-navy">
            {fullDate(data)}, {horario}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <Field data-invalid={!!errors.clienteNome}>
          <FieldLabel htmlFor="clienteNome">Seu nome completo</FieldLabel>
          <Input id="clienteNome" placeholder="Como podemos te chamar?" {...register('clienteNome')} />
          <FieldError errors={[errors.clienteNome]} />
        </Field>

        <Field data-invalid={!!errors.clienteTelefone}>
          <FieldLabel htmlFor="clienteTelefone">WhatsApp</FieldLabel>
          <Input
            id="clienteTelefone"
            type="tel"
            placeholder="(55) 99999-9999"
            {...register('clienteTelefone')}
          />
          <FieldError errors={[errors.clienteTelefone]} />
        </Field>

        <div className="grid grid-cols-2 gap-4">
          <Field data-invalid={!!errors.petNome}>
            <FieldLabel htmlFor="petNome">Nome do pet</FieldLabel>
            <Input id="petNome" placeholder="Ex: Rex" {...register('petNome')} />
            <FieldError errors={[errors.petNome]} />
          </Field>

          <Field data-invalid={!!errors.petRaca}>
            <FieldLabel htmlFor="petRaca">Raça</FieldLabel>
            <Input id="petRaca" placeholder="Ex: Poodle" {...register('petRaca')} />
            <FieldError errors={[errors.petRaca]} />
          </Field>
        </div>

        <Field data-invalid={!!errors.observacoes}>
          <FieldLabel htmlFor="observacoes">Observações (opcional)</FieldLabel>
          <Textarea
            id="observacoes"
            placeholder="Ex: Ele é um pouco arisco com outros cães."
            rows={3}
            {...register('observacoes')}
          />
          <FieldError errors={[errors.observacoes]} />
        </Field>

        {/* Honeypot: invisible para humanos, sin label — mismo patrón anti-bot
            de checkout-form.tsx (tarea 4.15/5.8). */}
        <input
          type="text"
          tabIndex={-1}
          autoComplete="off"
          aria-hidden="true"
          className="absolute h-0 w-0 opacity-0"
          {...register('honeypot')}
        />

        {submitError && (
          <p className="text-sm text-destructive">
            {submitError}
            {retryAfterSeconds !== null && ` Tente novamente em ${retryAfterSeconds}s.`}
          </p>
        )}

        <div className="mt-4 flex items-center gap-4">
          <button
            type="button"
            onClick={onBack}
            className="h-11 rounded-md border border-outline px-5 font-medium text-navy transition-transform active:scale-95"
          >
            Voltar
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="h-12 flex-[2] rounded-full bg-orange font-label text-white shadow-md transition-transform active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isSubmitting ? 'Enviando...' : 'Confirmar agendamento'}
          </button>
        </div>
      </form>
    </div>
  )
}
