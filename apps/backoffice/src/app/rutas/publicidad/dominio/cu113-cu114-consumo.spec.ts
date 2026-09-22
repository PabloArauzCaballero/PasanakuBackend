import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Gate del carril: «el desempeño mostrado cuadra con lo facturado en CU-114» —
 * `pantalla-de-desempeno.ts` y `pantalla-de-liquidacion.ts` importan `recursoDeConsumo`
 * de este mismo archivo, nunca calculan el total por su cuenta. Se prueba a nivel de
 * fuente (no de red) porque lo que hay que garantizar es que **no exista una segunda
 * ruta de cálculo**, no un valor concreto.
 */
describe('CU-113/CU-114 · un solo origen de dato para desempeño y liquidación', () => {
  it('las dos pantallas importan recursoDeConsumo del mismo dominio compartido', () => {
    const desempeno = readFileSync(join(__dirname, '../desempeno/pantalla-de-desempeno.ts'), 'utf8')
    const liquidacion = readFileSync(join(__dirname, '../liquidacion/pantalla-de-liquidacion.ts'), 'utf8')
    expect(desempeno).toContain("from '../dominio/cu113-cu114-consumo'")
    expect(liquidacion).toContain("from '../dominio/cu113-cu114-consumo'")
    expect(desempeno).toContain('recursoDeConsumo')
    expect(liquidacion).toContain('recursoDeConsumo')
  })

  it('ninguna de las dos pantallas define su propio cálculo de total (grep negativo)', () => {
    const desempeno = readFileSync(join(__dirname, '../desempeno/pantalla-de-desempeno.ts'), 'utf8')
    const liquidacion = readFileSync(join(__dirname, '../liquidacion/pantalla-de-liquidacion.ts'), 'utf8')
    for (const archivo of [desempeno, liquidacion]) {
      expect(archivo).not.toMatch(/\.reduce\(|\.sum\(/)
    }
  })
})
