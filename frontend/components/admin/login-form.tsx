'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { ArrowRight, Eye, EyeOff, Lock, Mail } from 'lucide-react'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { apiFetch, ApiFetchError } from '@/lib/api/client'

const loginSchema = z.object({
  email: z.email('Informe um e-mail válido.'),
  password: z.string().min(1, 'Informe sua senha.'),
})

type LoginFormValues = z.infer<typeof loginSchema>

// Tarea 4.11 (issue #32) — port de "Login Administrativo" (única pantalla
// DESKTOP do projeto). O backend de auth (1.3 JWT em cookie + 1.8 rate
// limit) já existe: este componente é só a UI. Sem "Manter conectado": o
// mock de Stitch o desenha, mas a sessão tem duração fixa — mostrar um
// controle inerte seria pior que omiti-lo. "Esqueceu a senha" agora aponta
// para /admin/esqueci-senha (tarea 7.12).
export function LoginForm() {
  const [showPassword, setShowPassword] = useState(false)
  const [loginError, setLoginError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  async function onSubmit(values: LoginFormValues) {
    setLoginError(null)
    try {
      await apiFetch('/auth/login', { method: 'POST', body: values })
      // Navegação completa: o guard do layout protegido lê a cookie no
      // server a cada request — precisa de um request novo de verdade.
      window.location.assign('/admin')
    } catch (err) {
      setLoginError(
        err instanceof ApiFetchError ? err.message : 'Não foi possível entrar. Tente novamente.'
      )
    }
  }

  return (
    <div className="w-full max-w-[440px] space-y-6 rounded-lg bg-white p-8 shadow-2xl">
      <div className="space-y-2 text-center md:text-left">
        <span className="inline-flex rounded-full bg-surface px-3 py-1 text-label uppercase tracking-tight text-ink-muted">
          Acesso Restrito
        </span>
        <h1 className="text-[26px] font-display text-navy">Entrar no painel</h1>
        <p className="text-sm text-ink-muted">Use suas credenciais administrativas</p>
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

        <Field data-invalid={!!errors.password}>
          <div className="flex items-center justify-between">
            <FieldLabel htmlFor="password">Senha</FieldLabel>
            <Link href="/admin/esqueci-senha" className="text-sm text-ink-muted hover:text-navy">
              Esqueceu a senha?
            </Link>
          </div>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 size-5 -translate-y-1/2 text-outline" />
            <Input
              id="password"
              type={showPassword ? 'text' : 'password'}
              placeholder="••••••••"
              className="pl-10 pr-10"
              {...register('password')}
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
          <FieldError errors={[errors.password]} />
        </Field>

        {loginError && <p className="text-sm text-destructive">{loginError}</p>}

        <button
          type="submit"
          disabled={isSubmitting}
          className="flex h-11 w-full items-center justify-center gap-2 rounded-md bg-navy text-sm font-medium text-white transition-opacity hover:opacity-90 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSubmitting ? 'Entrando...' : 'Entrar no painel'}
          {!isSubmitting && <ArrowRight className="size-5" />}
        </button>
      </form>
    </div>
  )
}
