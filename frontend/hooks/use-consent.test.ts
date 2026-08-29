import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// vi.resetModules() + import dinâmico por teste: use-consent.ts cacheia o
// snapshot num módulo-level `let cachedStatus` (mesmo padrão de
// hooks/use-cart.ts) — sem isso, o cache de um teste vazaria pro próximo
// mesmo depois de limpar o localStorage.
async function importFresh() {
  vi.resetModules()
  return import('./use-consent')
}

describe('useConsent', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  afterEach(() => {
    window.localStorage.clear()
  })

  it('empieza en "unknown" cuando no hay nada guardado', async () => {
    const { useConsent } = await importFresh()
    const { result } = renderHook(() => useConsent())
    expect(result.current.status).toBe('unknown')
  })

  it('grant() persiste "granted" en localStorage con versión y timestamp', async () => {
    const { useConsent } = await importFresh()
    const { result } = renderHook(() => useConsent())

    act(() => {
      result.current.grant()
    })

    await waitFor(() => expect(result.current.status).toBe('granted'))

    const raw = window.localStorage.getItem('frontpet:consentimento')
    expect(raw).not.toBeNull()
    const record = JSON.parse(raw!)
    expect(record.status).toBe('granted')
    expect(record.v).toBe(1)
    expect(typeof record.ts).toBe('string')
  })

  it('deny() persiste "denied"', async () => {
    const { useConsent } = await importFresh()
    const { result } = renderHook(() => useConsent())

    act(() => {
      result.current.deny()
    })

    await waitFor(() => expect(result.current.status).toBe('denied'))
  })

  it('un hook nuevo lee el status ya persistido (sobrevive a un remount)', async () => {
    const { useConsent } = await importFresh()
    const first = renderHook(() => useConsent())
    act(() => {
      first.result.current.grant()
    })
    await waitFor(() => expect(first.result.current.status).toBe('granted'))

    const second = renderHook(() => useConsent())
    expect(second.result.current.status).toBe('granted')
  })

  it('un registro con versión de política vieja vuelve a pedir consentimiento', async () => {
    window.localStorage.setItem(
      'frontpet:consentimento',
      JSON.stringify({ v: 0, status: 'granted', ts: new Date().toISOString() })
    )
    const { useConsent } = await importFresh()
    const { result } = renderHook(() => useConsent())
    expect(result.current.status).toBe('unknown')
  })

  it('JSON corrupto en localStorage no rompe: vuelve a "unknown"', async () => {
    window.localStorage.setItem('frontpet:consentimento', '{not-json')
    const { useConsent } = await importFresh()
    const { result } = renderHook(() => useConsent())
    expect(result.current.status).toBe('unknown')
  })
})
