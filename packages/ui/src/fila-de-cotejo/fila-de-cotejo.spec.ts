import { describe, expect, it } from 'vitest'
import { normalizar } from './fila-de-cotejo'

describe('normalizar (cotejo)', () => {
  it('ignora acentos, mayúsculas y espacios de más', () => {
    expect(normalizar('José  Pérez ')).toBe(normalizar('JOSE PEREZ'))
  })
  it('no confunde documentos distintos', () => {
    expect(normalizar('1234567 LP')).not.toBe(normalizar('1234567 SC'))
  })
})
