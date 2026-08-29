'use client'

import { useCallback, useSyncExternalStore } from 'react'

// Consentimento de tracking (Meta Pixel) — LGPD, ADR 024. Ao contrário do
// carrinho (hooks/use-cart.ts, sessionStorage — perde-se ao fechar a aba de
// propósito), o consentimento precisa sobreviver ao fechamento da aba e valer
// para todas as abas abertas, por isso localStorage.
const STORAGE_KEY = 'frontpet:consentimento'

// Versão do texto do banner (consent-banner.tsx). Subir este número força
// todo mundo a decidir de novo — usar quando o copy da política mudar de
// forma material, não em ajustes de redação.
const CONSENT_VERSION = 1

export type ConsentStatus = 'unknown' | 'granted' | 'denied'

interface ConsentRecord {
  v: number
  status: 'granted' | 'denied'
  ts: string
}

// Store externo mínimo, mesmo padrão de hooks/use-cart.ts (CLAUDE.md §6 —
// "se resolve com vanilla?"): vários componentes (ConsentBanner, MetaPixel,
// os trackers de evento) precisam ler o mesmo status sem um Context Provider.
type Listener = () => void
const listeners = new Set<Listener>()

// Cache da referência atual — useSyncExternalStore exige a MESMA referência
// se os dados não mudaram (ver comentário equivalente em use-cart.ts).
let cachedStatus: ConsentStatus | null = null

function readStorage(): ConsentStatus {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return 'unknown'
    const record = JSON.parse(raw) as ConsentRecord
    // Versão desatualizada da política: trata como se nunca tivesse decidido.
    if (record.v !== CONSENT_VERSION) return 'unknown'
    return record.status
  } catch {
    // localStorage bloqueado ou JSON corrompido: pede consentimento de novo
    // em vez de quebrar a página ou assumir "aceito" por padrão.
    return 'unknown'
  }
}

function writeStorage(status: ConsentStatus) {
  cachedStatus = status
  try {
    if (status === 'unknown') {
      window.localStorage.removeItem(STORAGE_KEY)
    } else {
      const record: ConsentRecord = { v: CONSENT_VERSION, status, ts: new Date().toISOString() }
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(record))
    }
  } catch {
    // idem use-cart.ts: o estado em memória segue servindo nesta aba mesmo
    // que não sobreviva a um reload.
  }
  listeners.forEach((listener) => listener())
}

function subscribe(listener: Listener) {
  listeners.add(listener)
  // localStorage é compartilhado entre abas (ao contrário do sessionStorage
  // do carrinho) — uma decisão tomada em outra aba precisa propagar aqui.
  // O evento 'storage' só dispara nas abas QUE NÃO originaram a escrita, por
  // isso writeStorage() notifica os listeners locais diretamente acima.
  const onStorageEvent = (event: StorageEvent) => {
    if (event.key === STORAGE_KEY) {
      cachedStatus = null
      listener()
    }
  }
  window.addEventListener('storage', onStorageEvent)
  return () => {
    listeners.delete(listener)
    window.removeEventListener('storage', onStorageEvent)
  }
}

function getSnapshot(): ConsentStatus {
  if (cachedStatus === null) {
    cachedStatus = readStorage()
  }
  return cachedStatus
}

// 'unknown' estável: garante que o Pixel NUNCA renderiza no SSR nem no
// primeiro render do client, antes de ler o localStorage real.
function getServerSnapshot(): ConsentStatus {
  return 'unknown'
}

export function useConsent() {
  const status = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot)

  const grant = useCallback(() => {
    writeStorage('granted')
  }, [])

  const deny = useCallback(() => {
    writeStorage('denied')
  }, [])

  return { status, grant, deny }
}
