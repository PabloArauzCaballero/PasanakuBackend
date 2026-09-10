import { describe, expect, it } from 'vitest'
import { desformatearMonto, formatearMonto, prefijoDe } from '../dinero/formatear'
import vectores from '../vectores/monto.json'

describe('formatearMonto — la referencia de los dos mundos', () => {
  it('arma la cifra como la maqueta la muestra', () => {
    expect(formatearMonto({ monto: '1240.00', moneda: 'BOB' })).toBe('Bs 1.240,00')
    expect(formatearMonto({ monto: '1234567.89', moneda: 'BOB' })).toBe('Bs 1.234.567,89')
    expect(formatearMonto({ monto: '-1240.50', moneda: 'BOB' })).toBe('-Bs 1.240,50')
    expect(formatearMonto({ monto: '10.00', moneda: 'USD' })).toBe('USD 10,00')
    expect(prefijoDe('BOB')).toBe('Bs')
  })

  it('rechaza lo que no tiene la forma del contrato', () => {
    for (const malo of ['1.5', '1240', '1.234,00', '']) {
      expect(() => formatearMonto({ monto: malo, moneda: 'BOB' })).toThrow(/fuera del contrato/)
    }
  })

  it('propiedad: formatear y deshacer devuelve el mismo importe, sobre los vectores', () => {
    expect(vectores.length).toBeGreaterThanOrEqual(5000)
    for (const v of vectores) {
      expect(formatearMonto(v)).toBe(v.esperado)
      expect(desformatearMonto(v.esperado)).toBe(v.monto)
    }
  })
})
