import { describe, it, expect, afterEach, vi } from 'vitest'
import {
  addDays,
  appointmentDate,
  appointmentTime,
  dayOfMonth,
  fullDate,
  nextDays,
  todayInSaoPaulo,
  weekdayShort,
} from '@/lib/booking-dates'

// El riesgo real de este módulo no es el formateo, es el huso: el backend fija
// la agenda en America/Sao_Paulo y nunca usa el del sistema (SlotGrid.ZONE_ID).
// Estos tests fijan el reloj para probar justamente los bordes donde el huso
// del navegador y el de la agenda no coinciden.

afterEach(() => {
  vi.useRealTimers()
})

describe('todayInSaoPaulo', () => {
  it('devuelve YYYY-MM-DD', () => {
    expect(todayInSaoPaulo()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it('usa el día de São Paulo, no el de UTC, cuando difieren', () => {
    // 2026-08-06T01:00Z ya es 6 de agosto en UTC, pero todavía son las 22:00
    // del 5 en São Paulo (UTC-3). La tira tiene que arrancar en el 5.
    vi.setSystemTime(new Date('2026-08-06T01:00:00Z'))
    expect(todayInSaoPaulo()).toBe('2026-08-05')
  })

  it('cambia de día a las 03:00Z, que es medianoche en São Paulo', () => {
    vi.setSystemTime(new Date('2026-08-06T03:00:00Z'))
    expect(todayInSaoPaulo()).toBe('2026-08-06')
  })
})

describe('addDays', () => {
  it('suma días dentro del mes', () => {
    expect(addDays('2026-08-05', 3)).toBe('2026-08-08')
  })

  it('cruza fin de mes', () => {
    expect(addDays('2026-08-30', 3)).toBe('2026-09-02')
  })

  it('cruza fin de año', () => {
    expect(addDays('2026-12-30', 3)).toBe('2027-01-02')
  })

  it('resuelve el año bisiesto', () => {
    expect(addDays('2028-02-28', 1)).toBe('2028-02-29')
  })

  it('suma 0 sin correr el día', () => {
    expect(addDays('2026-08-05', 0)).toBe('2026-08-05')
  })
})

describe('nextDays', () => {
  it('arranca en hoy y devuelve días consecutivos', () => {
    vi.setSystemTime(new Date('2026-08-06T12:00:00Z'))
    expect(nextDays(4)).toEqual(['2026-08-06', '2026-08-07', '2026-08-08', '2026-08-09'])
  })
})

describe('display', () => {
  // Estos son la regresión de la trampa del módulo: formatear en
  // America/Sao_Paulo un string que YA es fecha de São Paulo lo correría un
  // día para atrás (medianoche UTC = 21:00 del día anterior en UTC-3).
  it('no corre el día al mostrar el número', () => {
    expect(dayOfMonth('2026-08-05')).toBe('5')
  })

  it('no corre el día al mostrar el día de la semana', () => {
    // 2026-08-05 es miércoles.
    expect(weekdayShort('2026-08-05')).toBe('qua')
  })

  it('no corre el día en el primero del mes', () => {
    expect(dayOfMonth('2026-08-01')).toBe('1')
    expect(weekdayShort('2026-08-01')).toBe('sáb')
  })

  it('formatea la fecha completa en PT-BR', () => {
    expect(fullDate('2026-08-05')).toBe('quarta-feira, 5 de agosto')
  })
})

describe('appointmentDate / appointmentTime', () => {
  // Distinto de las funciones de arriba: acá la entrada es un instante con
  // offset (AppointmentDetail.startAt), no una fecha calendario sin hora.

  it('formatea la hora en el huso de São Paulo, no en UTC', () => {
    // 12:30 UTC = 09:30 en São Paulo (UTC-3).
    expect(appointmentTime('2026-08-05T12:30:00Z')).toBe('09:30')
  })

  it('respeta un offset -03:00 explícito en el string', () => {
    expect(appointmentTime('2026-08-05T09:30:00-03:00')).toBe('09:30')
  })

  it('no corre el día al formatear cerca de medianoche UTC', () => {
    // 23:30 São Paulo del día 5 = 02:30 UTC del día 6 — la fecha mostrada
    // tiene que seguir siendo el 5, el día del turno real, no el de UTC.
    expect(appointmentDate('2026-08-06T02:30:00Z')).toBe('quarta-feira, 5 de agosto')
    expect(appointmentTime('2026-08-06T02:30:00Z')).toBe('23:30')
  })
})
