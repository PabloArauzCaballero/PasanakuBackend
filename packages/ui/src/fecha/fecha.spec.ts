import { describe, expect, it } from 'vitest'
import { formatearDia, formatearFecha } from './fecha'

/** Mismo formato que `Fecha.formatear` en Flutter: hora de La Paz, zona visible. */
describe('formatearFecha', () => {
  it('pasa de UTC a La Paz (−4) y muestra la zona', () => {
    expect(formatearFecha('2026-09-10T18:05:00Z')).toBe('10 sep 2026, 14:05 (La Paz)')
  })
  it('cruza el día hacia atrás cuando corresponde', () => {
    expect(formatearFecha('2026-09-11T02:30:00Z')).toBe('10 sep 2026, 22:30 (La Paz)')
  })
  it('sin hora deja solo la fecha', () => {
    expect(formatearFecha('2026-01-01T03:00:00Z', false)).toBe('31 dic 2025')
  })
})

/** Un `format: date` no tiene zona: correrlo a La Paz lo mostraría un día antes. */
describe('formatearDia', () => {
  it('muestra el mismo día que dice el contrato', () => {
    expect(formatearDia('2027-05-10')).toBe('10 may 2027')
  })
  it('el primero de enero no cae en el año anterior', () => {
    expect(formatearDia('2027-01-01')).toBe('1 ene 2027')
  })
})
