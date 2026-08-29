'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { ArrowLeft, ArrowRight, Mail } from 'lucide-react'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { apiFetch, ApiFetchError } from '@/lib/api/client'

const forgotPasswordSchema = z.object({
  email: z.email('Informe um e-mail válido.'),
})

type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>

// Tarea 7.12 — pedido de reset de senha. O backend SEMPRE responde 202,
// exista ou não a conta (anti-enumeração, ADR 022): por isso o estado de
// sucesso mostra o mesmo texto genérico independente do e-mail digitado —
// não faz sentido a UI delatar aqui o que o backend já esconde.
export function ForgotPasswordForm() {
  const [sent, setSent] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  })

  async function onSubmit(values: ForgotPasswordFormValues) {
    setFormError(null)
    try {
      await apiFetch('/auth/forgot-password', { method: 'POST', body: values })
      setSent(true)
    } catch (err) {
      if (err instanceof ApiFetchError && err.status === 429) {
        setFormError('Muitas tentativas. Tente novamente em alguns minutos.')
        return
      }
      setFormError(
        err instanceof ApiFetchError ? err.message : 'Não foi possível enviar o link. Tente novamente.'
      )
    }
  }

  if (sent) {
    return (
      <div className="w-full max-w-[440px] space-y-6 rounded-lg bg-white p-8 shadow-2xl">
        <div className="space-y-2 text-center md:text-left">
          <span className="inline-flex rounded-full bg-surface px-3 py-1 text-label uppercase tracking-tight text-ink-muted">
            Verifique seu e-mail
          </span>
          <h1 className="text-[26px] font-display text-navy">Link enviado</h1>
        </div>
        <p className="text-sm text-ink-muted">
          Se houver uma conta com esse e-mail, você vai receber um link para redefinir sua senha
          em alguns minutos. Confira também a caixa de spam. O link vale por 60 minutos.
        </p>
        <Link
          href="/admin/login"
          className="flex items-center justify-center gap-2 text-sm font-medium text-navy hover:opacity-80"
        >
          <ArrowLeft className="size-4" />
          Voltar para o login
        </Link>
      </div>
    )
  }

  return (
    <div className="w-full max-w-[440px] space-y-6 rounded-lg bg-white p-8 shadow-2xl">
      <div className="space-y-2 text-center md:text-left">
        <span className="inline-flex rounded-full bg-surface px-3 py-1 text-label uppercase tracking-tight text-ink-muted">
          Acesso Restrito
        </span>
        <h1 className="text-[26px] font-display text-navy">Esqueceu sua senha?</h1>
        <p className="text-sm text-ink-muted">
          Informe seu e-mail e enviaremos um link para redefinir sua senha.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Field data-invalid={!!errors.email}>
          <FieldLabel htmlFor="email">E-mail</FieldLabel>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 size-5 -translate-y-1/2 text-outline" />
            <Input
              id="email"
              type="email"
              placeholder="admin@frontpet.com.br"
              className="pl-10"
              {...register('email')}
            />
          </div>
          <FieldError errors={[errors.email]} />
        </Field>

        {formError && <p className="text-sm text-destructive">{formError}</p>}

        <button
          type="submit"
          disabled={isSubmitting}
          className="flex h-11 w-full items-center justify-center gap-2 rounded-md bg-navy text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSubmitting ? 'Enviando...' : 'Enviar link de redefinição'}
          {!isSubmitting && <ArrowRight className="size-5" />}
        </button>

        <Link
          href="/admin/login"
          className="flex items-center justify-center gap-2 text-sm text-ink-muted hover:text-navy"
        >
          <ArrowLeft className="size-4" />
          Voltar para o login
        </Link>
      </form>
    </div>
  )
}
