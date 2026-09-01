import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ReviewCard } from './review-card'
import type { GoogleReview } from '@/lib/google-places'

// Convierte los requisitos de atribución del ToS de Google Places en tests
// (ADR 025), para que un refactor visual no los borre sin que nadie lo note
// — que es exactamente lo que pasó con la integración legacy.

const REVIEW: GoogleReview = {
  id: 'places/x/reviews/1',
  authorName: 'Elisangela Brito',
  authorUri: 'https://google.com/maps/contrib/1',
  photoUri: null,
  rating: 5,
  text: 'Excelente. Meu Pet volta limpo e bem tratado.',
  relativeTime: '4 semanas atrás',
  reviewUri: 'https://maps.google.com/reviews/1',
}

describe('ReviewCard', () => {
  it('el nombre del autor linkea a su perfil de Google', () => {
    render(<ReviewCard review={REVIEW} />)
    const link = screen.getByRole('link', { name: /Elisangela Brito/ })
    expect(link).toHaveAttribute('href', REVIEW.authorUri)
  })

  it('tiene un link a la review completa en Google', () => {
    render(<ReviewCard review={REVIEW} />)
    const link = screen.getByRole('link', { name: /Ler no Google/ })
    expect(link).toHaveAttribute('href', REVIEW.reviewUri)
  })

  it('muestra 5 slots de estrella con el rating accesible', () => {
    render(<ReviewCard review={{ ...REVIEW, rating: 3 }} />)
    expect(screen.getByText('Avaliação: 3 de 5 estrelas')).toBeInTheDocument()
  })

  it('sin authorUri, el nombre se muestra sin link', () => {
    render(<ReviewCard review={{ ...REVIEW, authorUri: null }} />)
    expect(screen.queryByRole('link', { name: /Elisangela Brito/ })).not.toBeInTheDocument()
    expect(screen.getByText('Elisangela Brito')).toBeInTheDocument()
  })

  it('sin photoUri, cae al avatar de iniciales', () => {
    render(<ReviewCard review={REVIEW} />)
    expect(screen.getByText('EB')).toBeInTheDocument()
  })

  it('sin reviewUri, no muestra el link "Ler no Google"', () => {
    render(<ReviewCard review={{ ...REVIEW, reviewUri: null }} />)
    expect(screen.queryByRole('link', { name: /Ler no Google/ })).not.toBeInTheDocument()
  })

  it('muestra el texto de la review', () => {
    render(<ReviewCard review={REVIEW} />)
    expect(screen.getByText(REVIEW.text)).toBeInTheDocument()
  })
})
