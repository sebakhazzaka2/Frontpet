import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SectionHeading } from './section-heading'

// El componente ES la garantía de consistencia entre las 8 secciones que lo
// usan (ADR 025) — estos tests cazan clases sueltas que reintroducirían las
// inconsistencias que motivaron crearlo.
describe('SectionHeading', () => {
  it('no renderiza el eyebrow si no se pasa', () => {
    render(<SectionHeading titulo="Título" />)
    expect(screen.queryByText(/—/)).not.toBeInTheDocument()
  })

  it('renderiza el eyebrow cuando se pasa', () => {
    render(<SectionHeading eyebrow="01 — Nossos Serviços" titulo="Título" />)
    expect(screen.getByText('01 — Nossos Serviços')).toBeInTheDocument()
  })

  it('tone="dark" pinta el título en blanco', () => {
    render(<SectionHeading titulo="Título" tone="dark" />)
    expect(screen.getByRole('heading', { level: 2 })).toHaveClass('text-white')
  })

  it('tone="light" (default) pinta el título en ink', () => {
    render(<SectionHeading titulo="Título" />)
    expect(screen.getByRole('heading', { level: 2 })).toHaveClass('text-ink')
  })

  it('align="center" centra el wrapper', () => {
    const { container } = render(<SectionHeading titulo="Título" align="center" />)
    expect(container.firstChild).toHaveClass('items-center', 'text-center')
  })

  it('align="left" (default) no centra', () => {
    const { container } = render(<SectionHeading titulo="Título" />)
    expect(container.firstChild).not.toHaveClass('text-center')
  })

  it('renderiza la descrição solo si se pasa', () => {
    const { rerender } = render(<SectionHeading titulo="Título" />)
    expect(screen.queryByText('Uma descrição')).not.toBeInTheDocument()

    rerender(<SectionHeading titulo="Título" descricao="Uma descrição" />)
    expect(screen.getByText('Uma descrição')).toBeInTheDocument()
  })
})
