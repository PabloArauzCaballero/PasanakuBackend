import { describe, expect, it } from 'vitest'
import { tonoDeCifra, type Cifra } from './cifras'

const cifra = (valor: string, tono?: Cifra['tono']): Cifra => ({ etiqueta: 'Caídos', valor, icono: 'alerta', tono })

describe('tonoDeCifra', () => {
  it('correcto: con algo que contar, el aviso y el error se pintan', () => {
    expect(tonoDeCifra(cifra('2', 'error'))).toBe('error')
    expect(tonoDeCifra(cifra('1', 'aviso'))).toBe('aviso')
  })

  it('límite: en cero, aviso y error pasan a neutro; ok, info y sin tono quedan como están', () => {
    expect(tonoDeCifra(cifra('0', 'error'))).toBe('neutro')
    expect(tonoDeCifra(cifra('0', 'aviso'))).toBe('neutro')
    expect(tonoDeCifra(cifra('0', 'ok'))).toBe('ok')
    expect(tonoDeCifra(cifra('0', 'info'))).toBe('info')
    expect(tonoDeCifra(cifra('0'))).toBe('neutro')
  })

  it('inválido: un valor que no es número (como «—») conserva su tono', () => {
    expect(tonoDeCifra(cifra('—', 'error'))).toBe('error')
  })
})
