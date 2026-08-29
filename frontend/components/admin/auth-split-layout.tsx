import Image from 'next/image'
import Link from 'next/link'
import { ArrowLeft, Calendar, Package, ShoppingBag } from 'lucide-react'
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

interface AuthSplitLayoutProps {
  children: React.ReactNode
}

// Extraído de app/admin/login/page.tsx (tarea 7.12): el split-view navy —
// <aside> com HIGHLIGHTS só em md+, logo mobile, link "Sou cliente, voltar
// para a loja" e o © — é idêntico nas 3 telas de auth (login,
// esqueci-senha, redefinir-senha). Server Component, sem 'use client': só
// muda o form que cada página passa como children. Não passou por Stitch —
// as 2 telas novas são derivadas desta, que já existia (porte de "Login
// Administrativo", a única tela DESKTOP do projeto).
export function AuthSplitLayout({ children }: AuthSplitLayoutProps) {
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

        {children}

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
