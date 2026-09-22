import { describe, expect, it } from 'vitest'
import { puedeConfirmar, restauracionVencida } from './datos-simulados'
import type { Respaldo } from './puertos'

const base = (probada: string | null): Respaldo => ({ id: 'r', origen: 'x', tomadoEl: '2026-09-11 02:00', ultimaRestauracionProbadaEl: probada })

describe('restauracionVencida', () => {
  const hoy = new Date('2026-09-11T00:00:00Z')

  it('nunca probada: vencida', () => {
    expect(restauracionVencida(base(null), hoy)).toBe(true)
  })

  it('probada hace más de 30 días: vencida', () => {
    expect(restauracionVencida(base('2026-06-15'), hoy)).toBe(true)
  })

  it('probada hace menos de 30 días: no vencida', () => {
    expect(restauracionVencida(base('2026-08-29'), hoy)).toBe(false)
  })

  it('probada exactamente hace 30 días: no vencida (el corte es "más de 30")', () => {
    const hace30 = new Date(hoy.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
    expect(restauracionVencida(base(hace30), hoy)).toBe(false)
  })
})

describe('puedeConfirmar · doble persona para un interruptor que toca dinero', () => {
  it('quien pidió el cambio no puede confirmarlo solo/a', () => {
    expect(puedeConfirmar('j.perez@aportaya.bo', 'j.perez@aportaya.bo')).toBe(false)
    expect(puedeConfirmar('j.perez@aportaya.bo', 'J.PEREZ@aportaya.bo')).toBe(false)
  })

  it('una persona distinta sí puede confirmarlo', () => {
    expect(puedeConfirmar('j.perez@aportaya.bo', 'm.rios@aportaya.bo')).toBe(true)
  })

  it('un correo vacío no confirma nada', () => {
    expect(puedeConfirmar('j.perez@aportaya.bo', '')).toBe(false)
  })
})
