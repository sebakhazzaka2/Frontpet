import Image from 'next/image'
import Link from 'next/link'
import { ArrowLeft, Calendar, Package, ShoppingBag } from 'lucide-react'
import { LoginForm } from '@/components/admin/login-form'
import logoHorizontal from '@/public/brand/frontpet-logo-horizontal.png'

const HIGHLIGHTS = [
  {
    icon: ShoppingBag,
    title: 'Pedidos do dia',
    description: 'Veja em tempo real os pedidos chegando pelo WhatsApp.',
  },
  {
    icon: Calendar,
    title: 'Agendamentos',
    description: 'Confirme, reagende ou cancele direto pelo painel.',
  },
  {
    icon: Package,
    title: 'Produtos',
    description: 'Venda e disponibilize seus produtos com facilidade.',
  },
] as const

// Port de "Login Administrativo" (f738782142d641f2a6fe7bf1567800c7) — única
// pantalla DESKTOP del proyecto (as otras 18 são MOBILE 780px). Split view:
// painel navy à esquerda só em md+ (marketing/contexto), form à direita
// sempre visível. Fora de app/admin/(protected)/ a propósito — ver comentário
// do layout guard sobre por que login não pode estar dentro dele.
export default function AdminLoginPage() {
  return (
    <div className="flex min-h-screen bg-navy">
      <aside className="hidden flex-1 flex-col items-center justify-center border-r border-white/10 p-16 md:flex">
        <div className="w-full max-w-lg space-y-12">
          <Image src={logoHorizontal} alt="FrontPet" className="h-16 w-auto object-contain" priority />

          <div className="space-y-6">
            <span className="inline-flex rounded-full bg-white/10 px-4 py-1 text-label uppercase tracking-wider text-white">
              Painel Administrativo
            </span>
            <h1 className="text-[36px] font-display leading-tight text-white">
              Gerencie sua loja com tranquilidade
            </h1>

            <ul className="space-y-6 pt-2">
              {HIGHLIGHTS.map(({ icon: Icon, title, description }) => (
                <li key={title} className="flex items-start gap-4">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-md bg-white/10 text-white">
                    <Icon className="size-5" />
                  </div>
                  <div className="flex flex-col">
                    <span className="text-h3 font-display leading-tight text-white">{title}</span>
                    <span className="text-body-sm text-white/70">{description}</span>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </aside>

      <main className="flex flex-1 flex-col items-center justify-center gap-8 px-6 py-16">
        <Image
          src={logoHorizontal}
          alt="FrontPet"
          className="h-12 w-auto object-contain md:hidden"
          priority
        />

        <LoginForm />

        <div className="w-full max-w-[440px] border-t border-white/10 pt-6">
          <Link
            href="/"
            className="flex items-center justify-center gap-2 text-body-sm text-white/70 transition-colors hover:text-white"
          >
            <ArrowLeft className="size-4" />
            Sou cliente, voltar para a loja
          </Link>
        </div>

        <p className="text-caption text-white/50">© 2026 FrontPet. Sistema de Gestão Interna.</p>
      </main>
    </div>
  )
}
