import Image from 'next/image'
import Link from 'next/link'
import { WhatsAppIcon } from '@/components/shared/whatsapp-icon'
import { InstagramIcon } from '@/components/shared/instagram-icon'
import { INSTAGRAM_HANDLE, INSTAGRAM_URL, SITE_CITY, buildWhatsAppLink } from '@/lib/data/site'
import logoHorizontal from '@/public/brand/frontpet-logo-horizontal.png'

const LINKS_UTEIS = [
  { href: '/', label: 'Início' },
  { href: '/produtos', label: 'Produtos' },
  { href: '/agendamento', label: 'Agendar horário' },
] as const

// "Rodapé Sincronizado" de Stitch (issue #10). La columna "Serviços" del mock
// listaba links inventados ("Spa Pet", que no existe en el catálogo real —
// ver ADR 011) sobre una dirección placeholder ("Rua Exemplo, 123"). No se
// porta: solo entran datos confirmados (ciudad del cliente, @frontpet.br).
export function Footer() {
  const whatsappHref = buildWhatsAppLink('Olá! Gostaria de mais informações.')
  const year = new Date().getFullYear()

  return (
    // pb-20/md:pb-12: BottomNav es fixed y vive fuera de <main> (layout.tsx),
    // así que su pb-20 no protege al Footer — se repite acá el mismo valor.
    <footer className="bg-navy px-6 pt-12 pb-20 text-white md:pb-12 lg:px-8">
      <div className="mx-auto flex max-w-content flex-col gap-10 md:flex-row md:justify-between">
        <div className="flex flex-col gap-3">
          <Image
            src={logoHorizontal}
            alt="FrontPet Petshop"
            className="h-10 w-auto object-contain"
            sizes="200px"
          />
          <p className="max-w-[280px] text-sm text-white/80">
            Banho, tosa e produtos premium para o seu pet, em {SITE_CITY}.
          </p>
          <div className="flex gap-3">
            <a
              href={INSTAGRAM_URL}
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Instagram da FrontPet"
              className="flex size-10 items-center justify-center rounded-full bg-white/10 transition-opacity hover:opacity-80"
            >
              <InstagramIcon className="size-4" />
            </a>
            <a
              href={whatsappHref}
              target="_blank"
              rel="noopener noreferrer"
              aria-label="Falar pelo WhatsApp"
              className="flex size-10 items-center justify-center rounded-full bg-white/10 transition-opacity hover:opacity-80"
            >
              <WhatsAppIcon className="size-4" />
            </a>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-10">
          <div>
            <h3 className="text-label uppercase tracking-wide text-white/60">Links úteis</h3>
            <ul className="mt-4 flex flex-col gap-2">
              {LINKS_UTEIS.map((link) => (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className="text-sm text-white/85 transition-opacity hover:opacity-80"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="text-label uppercase tracking-wide text-white/60">Contato</h3>
            <ul className="mt-4 flex flex-col gap-2 text-sm text-white/85">
              <li>{SITE_CITY}</li>
              <li>
                <a
                  href={INSTAGRAM_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:opacity-80"
                >
                  {INSTAGRAM_HANDLE}
                </a>
              </li>
              <li>
                <a
                  href={whatsappHref}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:opacity-80"
                >
                  WhatsApp
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div className="mx-auto mt-10 max-w-content border-t border-white/10 pt-6 text-center text-caption text-white/60">
        © {year} FrontPet Petshop. Todos os direitos reservados.
      </div>
    </footer>
  )
}
