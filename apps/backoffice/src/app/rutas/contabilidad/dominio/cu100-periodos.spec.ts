import { describe, expect, it } from 'vitest'
import {
  cuadra,
  motivoParaNoAsentar,
  periodoMasAntiguoAbierto,
  puedeAsentarEn,
  puedeCerrar,
  type PeriodoContable,
} from './cu100-periodos'

function periodo(mes: number, estado: 'ABIERTO' | 'CERRADO', debe = '10.00', haber = '10.00'): PeriodoContable {
  return {
    periodoId: `p-${mes}`,
    ejercicioFiscalId: 'e-1',
    anio: 2026,
    mes,
    nombre: `${mes}/2026`,
    estado,
    cerradoEn: estado === 'CERRADO' ? '2026-02-01T10:00:00Z' : null,
    totalDebe: { monto: debe, moneda: 'BOB' },
    totalHaber: { monto: haber, moneda: 'BOB' },
  }
}

describe('CU-100 · la regla de un período cerrado', () => {
  it('un período ABIERTO admite asentar y no tiene motivo que mostrar', () => {
    expect(puedeAsentarEn(periodo(1, 'ABIERTO'))).toBe(true)
    expect(motivoParaNoAsentar(periodo(1, 'ABIERTO'))).toBeNull()
  })

  it('un período CERRADO no admite asentar, y el motivo dice dónde va la corrección', () => {
    expect(puedeAsentarEn(periodo(1, 'CERRADO'))).toBe(false)
    expect(motivoParaNoAsentar(periodo(1, 'CERRADO'))).toContain('período abierto siguiente')
  })
})

describe('CU-100 · el orden estricto de cierre (AP-CU100-02)', () => {
  const meses = [periodo(3, 'ABIERTO'), periodo(1, 'CERRADO'), periodo(2, 'ABIERTO')]

  it('el más antiguo abierto es febrero, no marzo, aunque marzo venga primero en la lista', () => {
    expect(periodoMasAntiguoAbierto(meses)?.mes).toBe(2)
  })

  it('solo el más antiguo abierto se puede cerrar', () => {
    expect(puedeCerrar(periodo(2, 'ABIERTO'), meses)).toBe(true)
    expect(puedeCerrar(periodo(3, 'ABIERTO'), meses)).toBe(false)
  })

  it('sin ningún período abierto no hay nada que cerrar', () => {
    expect(periodoMasAntiguoAbierto([periodo(1, 'CERRADO')])).toBeNull()
  })
})

describe('CU-100 · el cuadre del mes (AP-CU100-03)', () => {
  it('cuadra cuando debe y haber coinciden', () => {
    expect(cuadra(periodo(1, 'ABIERTO', '1200.50', '1200.50'))).toBe(true)
  })

  it('no cuadra con un centavo de diferencia: es un incidente, no un cierre', () => {
    expect(cuadra(periodo(1, 'ABIERTO', '1200.50', '1200.51'))).toBe(false)
  })

  it('sin totales todavía no se puede afirmar que cuadre', () => {
    expect(cuadra({ ...periodo(1, 'ABIERTO'), totalDebe: null, totalHaber: null })).toBe(false)
  })
})
