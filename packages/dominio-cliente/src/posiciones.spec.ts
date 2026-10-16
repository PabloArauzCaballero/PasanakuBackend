import { describe, expect, it } from 'vitest'
import { cuentaEnCero, SaldoIncoherente, separarPosiciones, type SaldoConPosiciones } from './posiciones'

/**
 * H12.S1.M2 · el contrato de `nucleo-financiero` publica `disponible` y `retenido`; `comprometidoEnPozos` y
 * `titularId` son el pedido del carril D (todavía sin productor). Estas pruebas corren contra ESE contrato
 * pedido, como doble declarado, en tres niveles: correcto, límite e inválido.
 */
const bs = (monto: string) => ({ monto, moneda: 'BOB' })
const saldo = (disponible: string, retenido: string, comprometido?: string): SaldoConPosiciones => ({
  disponible: bs(disponible),
  retenido: bs(retenido),
  ...(comprometido !== undefined ? { comprometidoEnPozos: bs(comprometido) } : {}),
})

describe('separarPosiciones · correcto', () => {
  it('separa lo gastable de lo comprometido en pozos y de lo retenido, y las tres cifras reconcilian con el total', () => {
    const p = separarPosiciones(saldo('1240.00', '60.00', '400.00'))
    expect(p).toMatchObject({ gastable: '840.00', comprometidoEnPozos: '400.00', retenido: '60.00', disponible: '1240.00', totalDeLaCuenta: '1300.00', separado: true, advertencias: [] })
    const suma = BigInt(p.gastable!.replace('.', '')) + BigInt(p.comprometidoEnPozos!.replace('.', '')) + BigInt(p.retenido.replace('.', ''))
    expect(suma).toBe(BigInt(p.totalDeLaCuenta.replace('.', '')))
  })

  it('sin nada comprometido, todo el disponible es gastable', () => {
    expect(separarPosiciones(saldo('500.00', '0.00', '0.00'))).toMatchObject({ gastable: '500.00', comprometidoEnPozos: '0.00', separado: true })
  })

  it('no pasa por number: un importe que un double no representa se conserva al centavo', () => {
    const p = separarPosiciones(saldo('9007199254740993.10', '0.01', '0.10'))
    expect(p.gastable).toBe('9007199254740993.00')
    expect(p.totalDeLaCuenta).toBe('9007199254740993.11')
  })
})

describe('separarPosiciones · límite', () => {
  it('todo comprometido: lo gastable es cero y no es negativo', () => {
    expect(separarPosiciones(saldo('400.00', '0.00', '400.00'))).toMatchObject({ gastable: '0.00', comprometidoEnPozos: '400.00', advertencias: [] })
  })

  it('comprometido mayor que el disponible: lo gastable queda en cero, lo comprometido se acota al disponible y se advierte', () => {
    const p = separarPosiciones(saldo('100.00', '10.00', '250.00'))
    expect(p).toMatchObject({ gastable: '0.00', comprometidoEnPozos: '100.00', totalDeLaCuenta: '110.00', advertencias: ['comprometido-excede-disponible'] })
  })

  it('el libro no informa lo comprometido: NO se presenta el disponible como gastable', () => {
    const p = separarPosiciones(saldo('1240.00', '0.00'))
    expect(p).toMatchObject({ gastable: null, comprometidoEnPozos: null, separado: false, disponible: '1240.00', totalDeLaCuenta: '1240.00' })
  })

  it('una cuenta en cero se reconoce como tal, con o sin lo comprometido informado', () => {
    expect(cuentaEnCero(separarPosiciones(saldo('0.00', '0.00', '0.00')))).toBe(true)
    expect(cuentaEnCero(separarPosiciones(saldo('0.00', '0.00')))).toBe(true)
    expect(cuentaEnCero(separarPosiciones(saldo('0.00', '0.01')))).toBe(false)
  })
})

describe('separarPosiciones · inválido (falla cerrada, nunca una cifra dudosa)', () => {
  const motivo = (s: SaldoConPosiciones) => {
    try {
      separarPosiciones(s)
    } catch (e) {
      expect(e).toBeInstanceOf(SaldoIncoherente)
      return (e as SaldoIncoherente).motivo
    }
    return 'no lanzó'
  }

  it('monedas distintas en una misma cuenta no se suman', () => {
    expect(motivo({ disponible: bs('10.00'), retenido: { monto: '1.00', moneda: 'USD' } })).toBe('monedas-distintas')
    expect(motivo({ disponible: bs('10.00'), retenido: bs('1.00'), comprometidoEnPozos: { monto: '1.00', moneda: 'USD' } })).toBe('monedas-distintas')
  })

  it('un importe negativo no es un saldo válido', () => {
    expect(motivo(saldo('-1.00', '0.00', '0.00'))).toBe('importe-negativo')
    expect(motivo(saldo('1.00', '0.00', '-0.50'))).toBe('importe-negativo')
  })

  it('un importe fuera de la forma del contrato (^-?\\d+\\.\\d{2}$) se rechaza', () => {
    for (const malo of ['12', '12.5', '12.345', '1e3', ' 1.00', '', 'abc', '1,00']) expect(motivo(saldo(malo, '0.00', '0.00')), malo).toBe('importe-fuera-de-contrato')
  })
})
