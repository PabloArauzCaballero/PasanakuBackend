import { describe, expect, it } from 'vitest'
import { avanceGeneral, cerrado, enPaso, estadoDe, fraccionDe, progresoInicial, requisitosPendientes, sePuedeContinuar, vigente } from '@aportaya/tutoriales/avance'
import type { ProgresoDeTutorial, TutorialDefinicion } from '@aportaya/tutoriales/tipos'

const AHORA = '2026-09-17T12:00:00.000Z'
const T: TutorialDefinicion = {
  id: 'a',
  version: '1.0.0',
  titulo: 'A',
  descripcion: 'd',
  categoria: 'c',
  dificultad: 'inicial',
  pasos: [
    { id: 'p1', titulo: 't', descripcion: 'd', objetivo: 'x' },
    { id: 'p2', titulo: 't', descripcion: 'd', objetivo: 'x' },
    { id: 'p3', titulo: 't', descripcion: 'd', objetivo: 'x' },
    { id: 'p4', titulo: 't', descripcion: 'd', objetivo: 'x' },
  ],
}

describe('el avance de un tutorial', () => {
  it('arranca en el primer paso, en progreso y sin terminar', () => {
    const p = progresoInicial(T, AHORA)
    expect(p).toMatchObject({ estado: 'en-progreso', indice: 0, pasoId: 'p1', terminadoEn: null, repeticiones: 0 })
  })

  it('completar suma una repetición; omitir no', () => {
    const inicial = progresoInicial(T, AHORA)
    expect(cerrado(inicial, 'completado', AHORA).repeticiones).toBe(1)
    expect(cerrado(inicial, 'omitido', AHORA).repeticiones).toBe(0)
  })

  it('la fracción sale del paso, y completado es siempre 1', () => {
    const enDos = enPaso(progresoInicial(T, AHORA), 2, 'p3', AHORA)
    expect(fraccionDe(enDos, T)).toBe(0.5)
    expect(fraccionDe(cerrado(enDos, 'completado', AHORA), T)).toBe(1)
  })

  it('sin progreso, el tutorial está pendiente y en cero', () => {
    expect(estadoDe(undefined, T)).toBe('pendiente')
    expect(fraccionDe(undefined, T)).toBe(0)
  })

  it('cambiar la versión invalida lo hecho: vuelve a ofrecerse', () => {
    const hecho = cerrado(progresoInicial(T, AHORA), 'completado', AHORA)
    expect(estadoDe(hecho, T)).toBe('completado')
    const nuevaVersion = { ...T, version: '2.0.0' }
    expect(vigente(hecho, nuevaVersion)).toBe(false)
    expect(estadoDe(hecho, nuevaVersion)).toBe('pendiente')
  })

  it('se continúa lo que quedó a medias en la versión vigente, pero no lo recién empezado', () => {
    const enDos = enPaso(progresoInicial(T, AHORA), 2, 'p3', AHORA)
    expect(sePuedeContinuar(enDos, T)).toBe(true)
    expect(sePuedeContinuar(enDos, { ...T, version: '2.0.0' })).toBe(false)
    expect(sePuedeContinuar(progresoInicial(T, AHORA), T)).toBe(false)
  })

  it('lo abandonado a mitad de camino también se puede retomar: es lo que promete el diálogo de salida', () => {
    const abandonado = cerrado(enPaso(progresoInicial(T, AHORA), 2, 'p3', AHORA), 'omitido', AHORA)
    expect(sePuedeContinuar(abandonado, T)).toBe(true)
    // Abandonado en el primer paso no tiene nada que retomar.
    expect(sePuedeContinuar(cerrado(progresoInicial(T, AHORA), 'omitido', AHORA), T)).toBe(false)
  })

  it('el avance general cuenta completados sobre disponibles; omitido no cuenta como hecho', () => {
    const otro: TutorialDefinicion = { ...T, id: 'b' }
    const mapa = new Map<string, ProgresoDeTutorial>([
      ['a', cerrado(progresoInicial(T, AHORA), 'completado', AHORA)],
      ['b', cerrado(progresoInicial(otro, AHORA), 'omitido', AHORA)],
    ])
    expect(avanceGeneral([T, otro], mapa)).toBe(0.5)
  })

  it('los requisitos pendientes son los que todavía no se completaron', () => {
    const base: TutorialDefinicion = { ...T, id: 'base' }
    const conRequisito: TutorialDefinicion = { ...T, id: 'con', requisitos: ['base'] }
    const porId = new Map([['base', base]])
    expect(requisitosPendientes(conRequisito, porId, new Map()).map((r) => r.id)).toEqual(['base'])
    const hechos = new Map([['base', cerrado(progresoInicial(base, AHORA), 'completado', AHORA)]])
    expect(requisitosPendientes(conRequisito, porId, hechos)).toEqual([])
  })
})
