import { AuthSplitLayout } from '@/components/admin/auth-split-layout'
import { LoginForm } from '@/components/admin/login-form'

// Port de "Login Administrativo" (f738782142d641f2a6fe7bf1567800c7) — única
// pantalla DESKTOP do projeto. Layout do split-view navy vive em
// auth-split-layout.tsx (extraído na tarea 7.12, compartido com
// esqueci-senha e redefinir-senha).
export default function AdminLoginPage() {
  return (
    <AuthSplitLayout>
      <LoginForm />
    </AuthSplitLayout>
  )
}
