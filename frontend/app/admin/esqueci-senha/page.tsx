import { AuthSplitLayout } from '@/components/admin/auth-split-layout'
import { ForgotPasswordForm } from '@/components/admin/forgot-password-form'

// Tarea 7.12 — pedido de reset de senha, derivado do login existente (sem
// passar por Stitch, ver o comentário de auth-split-layout.tsx).
export default function ForgotPasswordPage() {
  return (
    <AuthSplitLayout>
      <ForgotPasswordForm />
    </AuthSplitLayout>
  )
}
