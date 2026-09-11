import { describe, expect, it } from 'vitest'
import { hashDeBloque, hashDeContenido, recorrerCadena, serializarCanonico, type BloqueParaVerificar, type ValorCanonico } from './cadena'

type Contenido = Record<string, ValorCanonico>

describe('serializarCanonico', () => {
  it('ordena las claves alfabéticamente, sin importar el orden de entrada', () => {
    expect(serializarCanonico({ b: 1, a: 2 })).toBe(serializarCanonico({ a: 2, b: 1 }))
  })

  it('es determinista y sensible a cualquier cambio', () => {
    const base = serializarCanonico({ monto: '10.00', tipo: 'aporte' })
    expect(serializarCanonico({ monto: '10.00', tipo: 'aporte' })).toBe(base)
    expect(serializarCanonico({ monto: '10.01', tipo: 'aporte' })).not.toBe(base)
  })

  it('serializa arreglos y anidados', () => {
    expect(serializarCanonico({ items: [{ b: 1, a: 2 }] })).toBe('{"items":[{"a":2,"b":1}]}')
  })
})

describe('hashDeBloque y recorrerCadena', () => {
  async function bloqueGenesis(numero: number, contenido: Contenido): Promise<BloqueParaVerificar> {
    const hashContenido = await hashDeContenido(contenido)
    const hashBloque = await hashDeBloque(numero, null, hashContenido)
    return { numeroBloque: numero, hashAnterior: null, hashContenido, hashBloque, contenido }
  }

  async function encadenar(anterior: BloqueParaVerificar, numero: number, contenido: Contenido): Promise<BloqueParaVerificar> {
    const hashContenido = await hashDeContenido(contenido)
    const hashBloque = await hashDeBloque(numero, anterior.hashBloque, hashContenido)
    return { numeroBloque: numero, hashAnterior: anterior.hashBloque, hashContenido, hashBloque, contenido }
  }

  it('una cadena bien encadenada verifica íntegra', async () => {
    const b1 = await bloqueGenesis(1, { periodo: 1 })
    const b2 = await encadenar(b1, 2, { periodo: 2 })
    const b3 = await encadenar(b2, 3, { periodo: 3 })

    const resultado = await recorrerCadena([b1, b2, b3])
    expect(resultado).toEqual({ integra: true, bloquesVerificados: 3, primerBloqueFallido: null, componenteFallido: null })
  })

  it('un contenido alterado se detecta en el bloque que cambió', async () => {
    const b1 = await bloqueGenesis(1, { periodo: 1 })
    const b2 = await encadenar(b1, 2, { periodo: 2 })
    const b2Alterado: BloqueParaVerificar = { ...b2, contenido: { periodo: 999 } }

    const resultado = await recorrerCadena([b1, b2Alterado])
    expect(resultado.integra).toBe(false)
    expect(resultado.primerBloqueFallido).toBe(2)
    expect(resultado.componenteFallido).toBe('HASH_CONTENIDO')
  })

  it('un salto de numeración se reporta como SECUENCIA', async () => {
    const b1 = await bloqueGenesis(1, { periodo: 1 })
    const b2 = await encadenar(b1, 2, { periodo: 2 })
    const b4 = await encadenar(b2, 4, { periodo: 4 })

    const resultado = await recorrerCadena([b1, b2, b4])
    expect(resultado.integra).toBe(false)
    expect(resultado.componenteFallido).toBe('SECUENCIA')
  })

  it('una cadena vacía verifica íntegra con cero bloques', async () => {
    expect(await recorrerCadena([])).toEqual({ integra: true, bloquesVerificados: 0, primerBloqueFallido: null, componenteFallido: null })
  })
})
