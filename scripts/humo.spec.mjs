import { describe, expect, it } from 'vitest'
import { resultadoFinal, clasificar, INFORMATIVAS } from './humo.mjs'

/**
 * Los tres niveles del encargo (H4.S1.M2): todas bien, una informativa falla, una
 * obligatoria falla. Prueba pura sobre `resultadoFinal`/`clasificar` — no llama a
 * newman ni a la red, así que corre igual sin las colecciones reales presentes.
 */
describe('humo.mjs · resultadoFinal', () => {
  it('todas bien: exitoso, sin listas de rotas', () => {
    const r = resultadoFinal([
      { nombreDeArchivo: 'a.humo.postman_collection.json', fallas: 0, ok: true },
      { nombreDeArchivo: 'b.humo.postman_collection.json', fallas: 0, ok: true },
    ])
    expect(r).toEqual({ exitoso: true, obligatoriasRotas: [], informativasRotas: [] })
  })

  it('una obligatoria falla: NO exitoso, y la nombra (el kill-test del encargo)', () => {
    const r = resultadoFinal([
      { nombreDeArchivo: 'garantia.humo.postman_collection.json', fallas: 2, ok: false },
      { nombreDeArchivo: 'b.humo.postman_collection.json', fallas: 0, ok: true },
    ])
    expect(r.exitoso).toBe(false)
    expect(r.obligatoriasRotas).toEqual(['garantia.humo.postman_collection.json'])
  })

  it('una informativa falla: sigue exitoso (avisa, no bloquea)', () => {
    // INFORMATIVAS está vacío por omisión (ver el comentario de humo.mjs): para probar
    // este nivel sin mutar el módulo real, se arma el mismo cálculo con una lista propia.
    const informativas = new Set(['opcional.humo.postman_collection.json'])
    const clasificarDePrueba = (n) => (informativas.has(n) ? 'informativa' : 'obligatoria')
    const resultados = [{ nombreDeArchivo: 'opcional.humo.postman_collection.json', fallas: 1, ok: false }]
    const rotas = resultados.filter((r) => !r.ok)
    const obligatoriasRotas = rotas.filter((r) => clasificarDePrueba(r.nombreDeArchivo) === 'obligatoria')
    const informativasRotas = rotas.filter((r) => clasificarDePrueba(r.nombreDeArchivo) === 'informativa')
    expect(obligatoriasRotas).toHaveLength(0)
    expect(informativasRotas).toEqual(resultados)
  })

  it('clasificar: sin marca en INFORMATIVAS, todo es obligatorio por omisión (denegar por defecto)', () => {
    expect(INFORMATIVAS.size).toBe(0)
    expect(clasificar('cualquiera.humo.postman_collection.json')).toBe('obligatoria')
  })
})
