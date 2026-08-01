// Fechas del wizard de agendamento. Todo el módulo trabaja con strings
// `YYYY-MM-DD` — el mismo formato que manda y recibe el backend.
//
// ⚠️ Por qué no se usa `new Date()` a secas en ningún lado: el backend fija
// la agenda en `America/Sao_Paulo` (SlotGrid.ZONE_ID) y nunca usa el huso del
// sistema, justamente porque la JVM del contenedor corre en UTC. El cliente
// tiene el problema espejo: el navegador de un usuario en otro huso (o el
// render de Next en el servidor, que corre en UTC) calcularía un "hoy"
// distinto al del backend y la tira de fechas arrancaría un día corrido.
const ZONE = 'America/Sao_Paulo'

// 'en-CA' formatea como YYYY-MM-DD, que es exactamente el formato del
// contrato — evita armar el string a mano desde getFullYear/getMonth.
const ISO_IN_ZONE = new Intl.DateTimeFormat('en-CA', {
  timeZone: ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

/** Hoy según el huso de la agenda, no según el del navegador. */
export function todayInSaoPaulo(): string {
  return ISO_IN_ZONE.format(new Date())
}

/**
 * Suma días a un `YYYY-MM-DD`. La aritmética va en UTC a propósito: UTC no
 * tiene DST, así que sumar 24 h siempre cae en el día calendario siguiente.
 * Hacerlo en horario local rompería en los dos días del año en que São Paulo
 * cambia de hora (si vuelve el horario de verano en Brasil).
 */
export function addDays(isoDate: string, days: number): string {
  const parsed = new Date(`${isoDate}T00:00:00Z`)
  parsed.setUTCDate(parsed.getUTCDate() + days)
  return parsed.toISOString().slice(0, 10)
}

/** Los próximos `count` días a partir de hoy, inclusive. */
export function nextDays(count: number): string[] {
  const today = todayInSaoPaulo()
  return Array.from({ length: count }, (_, i) => addDays(today, i))
}

// ⚠️ Las funciones de display formatean en UTC, no en ZONE. No es un olvido:
// `isoDate` ya ES la fecha calendario de São Paulo. Al parsearla como
// medianoche UTC y volver a formatearla en São Paulo (UTC-3) daría las 21:00
// del día anterior — el 05/08 se mostraría como "04". Parseado y formateado
// ambos en UTC, el día calendario se preserva.
const WEEKDAY_SHORT = new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC', weekday: 'short' })
const DAY_OF_MONTH = new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC', day: 'numeric' })
const FULL_DATE = new Intl.DateTimeFormat('pt-BR', {
  timeZone: 'UTC',
  weekday: 'long',
  day: 'numeric',
  month: 'long',
})

/** "seg", "ter", … sin el punto que agrega o Intl em pt-BR. */
export function weekdayShort(isoDate: string): string {
  return WEEKDAY_SHORT.format(new Date(`${isoDate}T00:00:00Z`)).replace('.', '')
}

export function dayOfMonth(isoDate: string): string {
  return DAY_OF_MONTH.format(new Date(`${isoDate}T00:00:00Z`))
}

/** "segunda-feira, 5 de agosto" — para o resumo do Passo 3. */
export function fullDate(isoDate: string): string {
  return FULL_DATE.format(new Date(`${isoDate}T00:00:00Z`))
}
