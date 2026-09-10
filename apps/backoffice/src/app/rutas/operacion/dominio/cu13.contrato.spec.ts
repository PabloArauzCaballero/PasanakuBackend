import { ejemploDe } from '@aportaya/simulado'
import type { SaldoBilletera } from 'clientes/angular/nucleo-financiero'
import { describe, expect, it } from 'vitest'

/** El ejemplo del contrato encaja en el tipo generado y los importes son cadena. */
describe('CU-13 · contrato', () => {
  for (const escenario of ['ok', 'vacio', 'adverso'] as const) {
    it(`consultarSaldo · ${escenario}`, () => {
      const e = ejemploDe('nucleo-financiero', 'consultarSaldo', escenario)
      const saldo = e.cuerpo as SaldoBilletera
      expect(e.estado).toBe(200)
      expect(saldo.disponible.monto).toMatch(/^-?\d+\.\d{2}$/)
      expect(saldo.retenido.monto).toMatch(/^-?\d+\.\d{2}$/)
      expect(typeof saldo.disponible.monto).toBe('string')
    })
  }
})
