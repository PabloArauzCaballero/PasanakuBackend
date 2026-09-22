import { describe, expect, it } from 'vitest'
import type { TutorialDefinicion } from '@aportaya/tutoriales/tipos'
import { validarCatalogo, type CodigoDeProblema } from '@aportaya/tutoriales/validacion'

const paso = (id: string, extra: Partial<TutorialDefinicion['pasos'][number]> = {}) => ({
  id,
  titulo: `Paso ${id}`,
  descripcion: 'Texto',
  objetivo: 'algo',
  ...extra,
})

const tutorial = (id: string, extra: Partial<TutorialDefinicion> = {}): TutorialDefinicion => ({
  id,
  version: '1.0.0',
  titulo: `Tutorial ${id}`,
  descripcion: 'Descripción',
  categoria: 'Pruebas',
  dificultad: 'inicial',
  pasos: [paso('uno')],
  ...extra,
})

const codigos = (problemas: readonly { codigo: CodigoDeProblema }[]): CodigoDeProblema[] => problemas.map((p) => p.codigo)

describe('validarCatalogo', () => {
  it('un catálogo sano no tiene nada que decir', () => {
    expect(validarCatalogo([tutorial('a'), tutorial('b')], ['/tablero'])).toEqual([])
  })

  it('detecta ids duplicados: dos tutoriales con el mismo id es uno que pisa al otro', () => {
    expect(codigos(validarCatalogo([tutorial('a'), tutorial('a')]))).toContain('id-duplicado')
  })

  it('detecta un tutorial sin pasos', () => {
    expect(codigos(validarCatalogo([tutorial('a', { pasos: [] })]))).toContain('tutorial-vacio')
  })

  it('detecta pasos duplicados dentro de un tutorial', () => {
    expect(codigos(validarCatalogo([tutorial('a', { pasos: [paso('uno'), paso('uno')] })]))).toContain('paso-duplicado')
  })

  it('detecta un paso que no resalta nada ni navega', () => {
    expect(codigos(validarCatalogo([tutorial('a', { pasos: [paso('uno', { objetivo: undefined })] })]))).toContain('paso-sin-objetivo')
  })

  it('un paso sin objetivo pero que pide navegar es válido: el objetivo es la ruta', () => {
    const t = tutorial('a', { pasos: [paso('uno', { objetivo: undefined, accion: { tipo: 'navegar', ruta: '/tablero' } })] })
    expect(codigos(validarCatalogo([t], ['/tablero']))).toEqual([])
  })

  it('detecta un orden declarado que va para atrás', () => {
    const t = tutorial('a', { pasos: [paso('uno', { orden: 2 }), paso('dos', { orden: 1 })] })
    expect(codigos(validarCatalogo([t]))).toContain('orden-incorrecto')
  })

  it('detecta una ruta que el backoffice no sabe montar', () => {
    expect(codigos(validarCatalogo([tutorial('a', { ruta: '/inventada' })], ['/tablero']))).toContain('ruta-inexistente')
  })

  it('una subruta de una ruta conocida es válida', () => {
    expect(codigos(validarCatalogo([tutorial('a', { ruta: '/cumplimiento/verificaciones' })], ['/cumplimiento']))).toEqual([])
  })

  it('detecta un requisito y un siguiente que no existen', () => {
    const problemas = codigos(validarCatalogo([tutorial('a', { requisitos: ['fantasma'], siguiente: 'otro-fantasma' })]))
    expect(problemas).toContain('requisito-inexistente')
    expect(problemas).toContain('siguiente-inexistente')
  })

  it('detecta una dependencia circular entre requisitos', () => {
    const catalogo = [tutorial('a', { requisitos: ['b'] }), tutorial('b', { requisitos: ['a'] })]
    expect(codigos(validarCatalogo(catalogo))).toContain('ciclo')
  })

  it('detecta un paso que pide un permiso que el tutorial no exige: nadie podría hacerlo', () => {
    const t = tutorial('a', { permisos: ['ver:operacion'], pasos: [paso('uno', { permiso: 'ver:sistemas' })] })
    expect(codigos(validarCatalogo([t]))).toContain('permiso-incompatible')
  })

  it('devuelve TODOS los problemas, no el primero', () => {
    const catalogo = [tutorial('a', { pasos: [] }), tutorial('a', { ruta: '/inventada' })]
    expect(validarCatalogo(catalogo, ['/tablero']).length).toBeGreaterThan(1)
  })
})
