'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { ArrowRight, Eye, EyeOff, Lock } from 'lucide-react'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { apiFetch, ApiFetchError } from '@/lib/api/client'

const resetPasswordSchema = z
  .object({
    // min(8)/max(72): espelha ResetPasswordRequest do backend — 72 porque o
    // BCrypt trunca silenciosamente além disso (ver o record no Java).
    passwordField: z
      .string()
      .min(8, 'A senha deve ter pelo menos 8 caracteres.')
      .max(72, 'A senha deve ter no máximo 72 caracteres.'),
    confirmPasswordField: z.string().min(1, 'Confirme sua nova senha.'),
  })
  .refine((data) => data.passwordField === data.confirmPasswordField, {
    message: 'As senhas não coincidem.',
    path: ['confirmPasswordField'],
  })

type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>

interface ResetPasswordFormProps {
  token: string
}

// Tarea 7.12. Sem auto-login após o sucesso, de propósito: obriga a
// demonstrar que se sabe a senha nova, e auto-logar bateria de frente com o
// corte de password_changed_at no mesmo segundo (ADR 022 / JwtAuthFilter) —
// a sessão nasceria já invalidada.
export function ResetPasswordForm({ token }: ResetPasswordFormProps) {
  const router = useRouter()
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [success, setSuccess] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { passwordField: '', confirmPasswordField: '' },
  })

  async function onSubmit(values: ResetPasswordFormValues) {
    setFormError(null)
    try {
      await apiFetch('/auth/reset-password', {
        method: 'POST',
        // A chave "password" aqui é o contrato com ResetPasswordRequest do
        // backend — não é o mesmo identificador do campo local do form.
        body: { token, password: values.passwordField },
      })
      setSuccess(true)
    } catch (err) {
      setFormError(
        err instanceof ApiFetchError
          ? err.message
          : 'Não foi possível redefinir sua senha. Tente novamente.'
      )
    }
  }

  if (success) {
    return (
      <div className="w-full max-w-[440px] space-y-6 rounded-lg bg-white p-8 shadow-2xl">
        <div className="space-y-2 text-center md:text-left">
          <span className="inline-flex rounded-full bg-surface px-3 py-1 text-label uppercase tracking-tight text-ink-muted">
            Senha redefinida
          </span>
          <h1 className="text-[26px] font-display text-navy">Tudo certo!</h1>
        </div>
        <p className="text-sm text-ink-muted">
          Sua senha foi redefinida com sucesso. Entre com sua nova senha para continuar.
        </p>
        <button
          type="button"
          onClick={() => router.push('/admin/login')}
          className="flex h-11 w-full items-center justify-center gap-2 rounded-md bg-navy text-sm font-medium text-white transition-opacity hover:opacity-90"
        >
          Ir para o login
          <ArrowRight className="size-5" />
        </button>
      </div>
    )
  }

  return (
    <div className="w-full max-w-[440px] space-y-6 rounded-lg bg-white p-8 shadow-2xl">
      <div className="space-y-2 text-center md:text-left">
        <span className="inline-flex rounded-full bg-surface px-3 py-1 text-label uppercase tracking-tight text-ink-muted">
          Acesso Restrito
        </span>
        <h1 className="text-[26px] font-display text-navy">Defina uma nova senha</h1>
        <p className="text-sm text-ink-muted">Escolha uma senha com pelo menos 8 caracteres</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Field data-invalid={!!errors.passwordField}>
          <FieldLabel htmlFor="passwordField">Nova senha</FieldLabel>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 size-5 -translate-y-1/2 text-outline" />
            <Input
              id="passwordField"
              type={showPassword ? 'text' : 'password'}
              placeholder="••••••••"
              className="pl-10 pr-10"
              {...register('passwordField')}
            />
            <button
              type="button"
              onClick={() => setShowPassword((value) => !value)}
              aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-outline hover:text-navy"
            >
              {showPassword ? <EyeOff className="size-5" /> : <Eye className="size-5" />}
            </button>
          </div>
          <FieldError errors={[errors.passwordField]} />
        </Field>

        <Field data-invalid={!!errors.confirmPasswordField}>
          <FieldLabel htmlFor="confirmPasswordField">Confirmar nova senha</FieldLabel>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 size-5 -translate-y-1/2 text-outline" />
            <Input
              id="confirmPasswordField"
              type={showConfirmPassword ? 'text' : 'password'}
              placeholder="••••••••"
              className="pl-10 pr-10"
              {...register('confirmPasswordField')}
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword((value) => !value)}
              aria-label={showConfirmPassword ? 'Ocultar senha' : 'Mostrar senha'}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-outline hover:text-navy"
            >
              {showConfirmPassword ? <EyeOff className="size-5" /> : <Eye className="size-5" />}
            </button>
          </div>
          <FieldError errors={[errors.confirmPasswordField]} />
        </Field>

        {formError && (
          <div className="space-y-2">
            <p className="text-sm text-destructive">{formError}</p>
            <Link
              href="/admin/esqueci-senha"
              className="text-sm font-medium text-navy hover:opacity-80"
            >
              Solicitar um novo link
            </Link>
          </div>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
          className="flex h-11 w-full items-center justify-center gap-2 rounded-md bg-navy text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSubmitting ? 'Salvando...' : 'Redefinir senha'}
          {!isSubmitting && <ArrowRight className="size-5" />}
        </button>
      </form>
    </div>
  )
}
