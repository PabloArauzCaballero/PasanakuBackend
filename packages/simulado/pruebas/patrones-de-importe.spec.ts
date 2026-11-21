import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

/**
 * F1.L6.M04 · Un `pattern` de un contrato que no acepta ningún valor real es un contrato roto
 * que el mock «cumple» con basura. Pasó en `cumplimiento.yaml`: `'^-?\\d+\\.\\d{2}$'` entre
 * comillas simples deja la barra escapada en el regex, que rechaza `"100.00"`.
 */
const GENERADO = resolve(__dirname, '../generado')
const contratos = existsSync(GENERADO) ? readdirSync(GENERADO).filter((a) => a.endsWith('.json')) : []

type Patron = { contrato: string; ruta: string; patron: string }

function patrones(): Patron[] {
  const salida: Patron[] = []
  for (const archivo of contratos) {
    const recorrer = (nodo: unknown, ruta: string) => {
      if (!nodo || typeof nodo !== 'object') return
      const objeto = nodo as Record<string, unknown>
      if (typeof objeto['pattern'] === 'string') salida.push({ contrato: archivo, ruta, patron: objeto['pattern'] })
      for (const [clave, valor] of Object.entries(objeto)) recorrer(valor, `${ruta}/${clave}`)
    }
    recorrer(JSON.parse(readFileSync(resolve(GENERADO, archivo), 'utf8')), '')
  }
  return salida
}

describe('patrones de los contratos', () => {
  it('hay contratos generados para revisar', () => expect(contratos.length).toBeGreaterThan(0))

  it('ningún patrón deja una barra escapada delante de una clase (\\\\d, \\\\.)', () => {
    const rotos = patrones().filter((p) => /\\\\[d.]/.test(p.patron))
    expect(rotos).toEqual([])
  })

  it('todo patrón de importe acepta un importe real', () => {
    const deImporte = patrones().filter((p) => /^\^(-\?)?\\d\+\\\.\\d\{2\}\$$/.test(p.patron))
    expect(deImporte.length).toBeGreaterThan(0)
    for (const p of deImporte) expect(new RegExp(p.patron).test('100.00'), `${p.contrato}${p.ruta}`).toBe(true)
  })

  it('cumplimiento: los límites de diligencia y de sandbox no admiten negativos (91.2.2)', () => {
    const limites = patrones().filter(
      (p) =>
        p.contrato === 'cumplimiento.json' &&
        /SalidaDiligencia\/properties\/limitesNuevos|SalidaAlcance\/properties\/limitesSandbox/.test(p.ruta),
    )
    expect(limites).toHaveLength(2)
    for (const p of limites) {
      expect(new RegExp(p.patron).test('100.00')).toBe(true)
      expect(new RegExp(p.patron).test('-1.00')).toBe(false)
    }
  })
})
