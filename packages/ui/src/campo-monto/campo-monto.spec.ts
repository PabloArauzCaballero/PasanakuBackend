import { describe, expect, it } from 'vitest'
import { aCadenaDelContrato } from './campo-monto'

/** Lo que la persona escribe → la cadena del contrato, sin pasar por `number`. */
describe('aCadenaDelContrato', () => {
  it.each([
    ['1.240,00', '1240.00'],
    ['1240', '1240.00'],
    ['12,5', '12.50'],
    ['0,05', '0.05'],
    ['99999999999999999999', '99999999999999999999.00'],
  ])('%s → %s', (entrada, esperado) => expect(aCadenaDelContrato(entrada)).toBe(esperado))

  it.each(['abc', '12,345', '1.2.3,4', '', '-5'])('rechaza %s', (entrada) => expect(aCadenaDelContrato(entrada)).toBeNull())
})
