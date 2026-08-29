import Link from 'next/link'
import { AuthSplitLayout } from '@/components/admin/auth-split-layout'
import { ResetPasswordForm } from '@/components/admin/reset-password-form'

interface ResetPasswordPageProps {
  searchParams: Promise<{ token?: string }>
}

// Tarea 7.12. Next 16: searchParams é uma Promise (docs/next16-notes.md).
// O token se lê aqui, no server, e se passa por prop — evita um
// useSearchParams em cliente, que obrigaria a envolver o form num
// <Suspense>, e mantém o Client Component no mínimo (CLAUDE.md §5).
export default async function ResetPasswordPage({ searchParams }: ResetPasswordPageProps) {
  const { token } = await searchParams

  return (
    <AuthSplitLayout>
      {token ? (
        <ResetPasswordForm token={token} />
      ) : (
        <div className="w-full max-w-[440px] space-y-6 rounded-lg bg-white p-8 shadow-2xl">
          <div className="space-y-2 text-center md:text-left">
            <span className="inline-flex rounded-full bg-surface px-3 py-1 text-label uppercase tracking-tight text-ink-muted">
              Link inválido
            </span>
            <h1 className="text-[26px] font-display text-navy">Não foi possível abrir este link</h1>
          </div>
          <p className="text-sm text-ink-muted">
            Link inválido. Solicite um novo link de redefinição.
          </p>
          <Link
            href="/admin/esqueci-senha"
            className="flex h-11 w-full items-center justify-center gap-2 rounded-md bg-navy text-sm font-medium text-white transition-opacity hover:opacity-90"
          >
            Solicitar novo link
          </Link>
        </div>
      )}
    </AuthSplitLayout>
  )
}
