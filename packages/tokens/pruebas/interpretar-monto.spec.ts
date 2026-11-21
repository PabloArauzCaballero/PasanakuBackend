import { describe, expect, it } from 'vitest'
import { formatearMonto } from '../dinero/formatear'
import { interpretarMontoEscrito } from '../dinero/interpretar'
import vectores from '../vectores/monto-escrito.json'

type Vector = { escrito: string; contrato: string | null; nivel: string }

describe('interpretarMontoEscrito — la referencia de CampoMonto (F1.S4.M1)', () => {
  it.each(vectores as Vector[])('«$escrito» → $contrato ($nivel)', ({ escrito, contrato }) => {
    expect(interpretarMontoEscrito(escrito)).toBe(contrato)
  })

  it('cubre los tres niveles del contrato', () => {
    const niveles = new Set((vectores as Vector[]).map((v) => v.nivel))
    expect([...niveles].sort()).toEqual(['correcto', 'invalido', 'limite'])
  })

  it('nunca multiplica por cien: lo interpretado vuelve a leerse igual', () => {
    for (const { contrato } of vectores as Vector[]) {
      if (contrato === null) continue
      const mostrado = formatearMonto({ monto: contrato, moneda: 'BOB' }).replace(/^Bs /, '')
      expect(interpretarMontoEscrito(mostrado)).toBe(contrato)
    }
  })
})
