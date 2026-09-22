import { describe, expect, it } from 'vitest'
import { diaLegible, neto } from './lista-de-movimientos'

describe('neto', () => {
  it('suma en centavos y mantiene dos decimales', () => {
    expect(neto(['500.00', '-250.00', '-12.50'])).toBe('237.50')
  })
  it('un neto negativo lleva signo y no pierde ceros', () => {
    expect(neto(['-250.00', '0.05'])).toBe('-249.95')
  })
  it('no se rompe con montos grandes (BigInt, no number)', () => {
    expect(neto(['99999999999999999.99', '0.01'])).toBe('100000000000000000.00')
  })
})

describe('diaLegible', () => {
  it('hoy, ayer y después la fecha corta', () => {
    expect(diaLegible('2026-09-10', '2026-09-10')).toBe('Hoy')
    expect(diaLegible('2026-09-09', '2026-09-10')).toBe('Ayer')
    expect(diaLegible('2026-09-07', '2026-09-10')).toBe('7 sep 2026')
  })
})
