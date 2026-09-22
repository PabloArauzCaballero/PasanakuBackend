import { describe, expect, it } from 'vitest'
import { validarCatalogo } from './validacion'
import type { TutorialDefinicion } from './tipos'

/**
 * H5.S3.M2 (PR13-Ci.Frontend) -- el package `@aportaya/tutoriales` no tenía NINGUNA
 * prueba propia; su `typecheck` se delegaba enteramente a quien lo consume
 * (`echo 'lo typechequean las dos apps...'`). `validarCatalogo` es texto puro (sin
 * Angular, sin DOM, según su propio comentario) y es exactamente lo que un package
 * debería poder probar solo: el motor entero se apoya en que esta función encuentre
 * TODO lo que está mal en un catálogo, no lo primero.
 */

function paso(parcial: Partial<TutorialDefinicion['pasos'][number]> = {}) {
  return {
    id: 'p1',
    titulo: 'Paso',
    descripcion: 'desc',
    objetivo: 'algo-en-pantalla',
    accion: { tipo: 'ninguna' as const },
    ...parcial,
  }
}

function tutorial(parcial: Partial<TutorialDefinicion> = {}): TutorialDefinicion {
  return {
    id: 't1',
    version: '1',
    titulo: 'Tutorial',
    descripcion: 'desc',
    categoria: 'general',
    dificultad: 'inicial',
    pasos: [paso()],
    ...parcial,
  }
}

describe('validarCatalogo', () => {
  it('un catálogo sano no reporta ningún problema', () => {
    expect(validarCatalogo([tutorial()])).toEqual([])
  })

  it('un tutorial sin pasos es "tutorial-vacio"', () => {
    const problemas = validarCatalogo([tutorial({ pasos: [] })])
    expect(problemas).toContainEqual(
      expect.objectContaining({ codigo: 'tutorial-vacio', tutorialId: 't1' }),
    )
  })

  it('dos tutoriales con el mismo id son "id-duplicado"', () => {
    const problemas = validarCatalogo([tutorial(), tutorial()])
    expect(problemas.map((p) => p.codigo)).toContain('id-duplicado')
  })

  it('un requisito que no existe en el catálogo es "requisito-inexistente"', () => {
    const problemas = validarCatalogo([tutorial({ requisitos: ['fantasma'] })])
    expect(problemas).toContainEqual(
      expect.objectContaining({ codigo: 'requisito-inexistente', detalle: expect.stringContaining('fantasma') }),
    )
  })

  it('un ciclo de requisitos (A exige B, B exige A) se detecta y no cuelga', () => {
    const a = tutorial({ id: 'a', requisitos: ['b'] })
    const b = tutorial({ id: 'b', requisitos: ['a'] })
    const problemas = validarCatalogo([a, b])
    expect(problemas.map((p) => p.codigo)).toContain('ciclo')
  })

  it('una ruta que no está en la lista de rutas conocidas es "ruta-inexistente"', () => {
    const problemas = validarCatalogo([tutorial({ ruta: '/no-existe' })], ['/operacion'])
    expect(problemas).toContainEqual(
      expect.objectContaining({ codigo: 'ruta-inexistente' }),
    )
  })

  it('una ruta conocida por prefijo NO es un problema', () => {
    const problemas = validarCatalogo(
      [tutorial({ ruta: '/operacion/billetera' })],
      ['/operacion'],
    )
    expect(problemas).toEqual([])
  })

  it('un paso sin objetivo y sin acción de navegar es "paso-sin-objetivo"', () => {
    const problemas = validarCatalogo([
      tutorial({ pasos: [paso({ objetivo: undefined, accion: undefined })] }),
    ])
    expect(problemas.map((p) => p.codigo)).toContain('paso-sin-objetivo')
  })

  it('el orden declarado fuera de secuencia es "orden-incorrecto"', () => {
    const problemas = validarCatalogo([
      tutorial({
        pasos: [paso({ id: 'p1', orden: 2 }), paso({ id: 'p2', orden: 1 })],
      }),
    ])
    expect(problemas.map((p) => p.codigo)).toContain('orden-incorrecto')
  })
})
