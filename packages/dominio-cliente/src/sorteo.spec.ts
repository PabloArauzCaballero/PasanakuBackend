import { describe, expect, it } from 'vitest'
import { barajarDeterminista, hashDelCompromiso, verificarCompromiso } from './sorteo'
import vectores from '../vectores/sorteo.vectores.json'

type Vector = {
  semilla: string
  entropias: string[]
  hashComprometido: string
  cupos: number[]
  ordenEsperado: number[]
}

/**
 * Coincide byte a byte con `plataforma/comun-dominio` ·
 * `SorteoVerificable` (Java): mismo algoritmo (Fisher-Yates + SHA-256 modular),
 * mismos vectores. Ver `vectores/sorteo.vectores.json` y su README sobre el origen
 * de estos valores: se calcularon con Python replicando línea por línea el
 * algoritmo Java (los vectores dorados generados por la propia prueba Java no
 * existen todavía — hueco declarado en el informe del carril).
 */
describe('sorteo verificable (CU-60/CU-61) — vectores dorados', () => {
  for (const v of vectores as Vector[]) {
    it(`semilla "${v.semilla}": compromiso y orden coinciden con el vector`, async () => {
      const hash = await hashDelCompromiso(v.semilla, v.entropias)
      expect(hash).toBe(v.hashComprometido)
      expect(await verificarCompromiso(v.semilla, v.entropias, v.hashComprometido)).toBe(true)

      const orden = await barajarDeterminista(v.semilla, v.cupos)
      expect(orden).toEqual(v.ordenEsperado)
    })
  }

  it('una semilla distinta no verifica el mismo compromiso', async () => {
    const hash = await hashDelCompromiso('semilla-real', ['ana-7'])
    expect(await verificarCompromiso('otra-semilla', ['ana-7'], hash)).toBe(false)
  })

  it('el barajado es una permutación pura: mismo tamaño, mismos elementos, no muta la entrada', async () => {
    const cupos = [1, 2, 3, 4, 5]
    const orden = await barajarDeterminista('s', cupos)
    expect(orden).toHaveLength(cupos.length)
    expect([...orden].sort()).toEqual([...cupos].sort())
    expect(cupos).toEqual([1, 2, 3, 4, 5])
  })

  it('sin semilla, no hay sorteo', async () => {
    await expect(barajarDeterminista('', [1, 2])).rejects.toThrow()
    await expect(hashDelCompromiso('', [])).rejects.toThrow()
  })

  it('sin hash comprometido, no verifica (no explota)', async () => {
    expect(await verificarCompromiso('s', [], undefined)).toBe(false)
  })
})
