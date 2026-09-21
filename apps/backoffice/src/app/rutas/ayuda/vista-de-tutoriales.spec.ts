import { describe, expect, it } from 'vitest'
import type { ProgresoDeTutorial, TutorialDefinicion } from '@aportaya/tutoriales/tipos'
import { componerLista, recomendado, type Filtro } from '@aportaya/tutoriales/centro/vista-de-tutoriales'

const t = (id: string, extra: Partial<TutorialDefinicion> = {}): TutorialDefinicion => ({
  id,
  version: '1',
  titulo: id,
  descripcion: 'descripción de ' + id,
  categoria: 'General',
  dificultad: 'inicial',
  pasos: [
    { id: 'p1', titulo: 'a', descripcion: 'b', objetivo: 'x' },
    { id: 'p2', titulo: 'a', descripcion: 'b', objetivo: 'x' },
  ],
  ...extra,
})

const progreso = (id: string, extra: Partial<ProgresoDeTutorial> = {}): ProgresoDeTutorial => ({
  tutorialId: id,
  version: '1',
  estado: 'completado',
  pasoId: 'p2',
  indice: 2,
  iniciadoEn: 'x',
  terminadoEn: 'y',
  ultimaInteraccion: 'y',
  repeticiones: 1,
  ...extra,
})

const TODO: Filtro = { texto: '', estado: 'todos', categoria: null }

describe('componerLista', () => {
  it('sin filtro devuelve todo, con su estado resuelto', () => {
    const lista = componerLista([t('a'), t('b')], new Map([['a', progreso('a')]]), TODO)
    expect(lista.map((f) => [f.tutorial.id, f.estado])).toEqual([
      ['b', 'pendiente'],
      ['a', 'completado'],
    ])
  })

  it('ordena: obligatorio, en progreso, pendiente, omitido y al final lo hecho', () => {
    const catalogo = [t('hecho'), t('pendiente'), t('medias'), t('obligatorio', { obligatorio: true }), t('omitido')]
    const avance = new Map([
      ['hecho', progreso('hecho')],
      ['medias', progreso('medias', { estado: 'en-progreso', indice: 1 })],
      ['omitido', progreso('omitido', { estado: 'omitido', indice: 1 })],
    ])
    expect(componerLista(catalogo, avance, TODO).map((f) => f.tutorial.id)).toEqual(['obligatorio', 'medias', 'pendiente', 'omitido', 'hecho'])
  })

  it('busca por título, descripción y categoría, sin importar mayúsculas', () => {
    const catalogo = [t('billetera', { titulo: 'Mirar la Billetera' }), t('campana', { categoria: 'Publicidad' })]
    expect(componerLista(catalogo, new Map(), { ...TODO, texto: 'billetera' }).map((f) => f.tutorial.id)).toEqual(['billetera'])
    expect(componerLista(catalogo, new Map(), { ...TODO, texto: 'publicidad' }).map((f) => f.tutorial.id)).toEqual(['campana'])
  })

  it('con varias palabras exige todas', () => {
    const catalogo = [t('a', { titulo: 'Crear una campaña' }), t('b', { titulo: 'Crear un anunciante' })]
    expect(componerLista(catalogo, new Map(), { ...TODO, texto: 'crear campaña' }).map((f) => f.tutorial.id)).toEqual(['a'])
  })

  it('el filtro de pendientes incluye los omitidos: dejarlo a medias no es hacerlo', () => {
    const catalogo = [t('a'), t('b')]
    const avance = new Map([['a', progreso('a', { estado: 'omitido' })]])
    expect(componerLista(catalogo, avance, { ...TODO, estado: 'pendiente' }).map((f) => f.tutorial.id)).toEqual(['b', 'a'])
  })

  it('filtra por obligatorios y por categoría', () => {
    const catalogo = [t('a', { obligatorio: true }), t('b', { categoria: 'Operación' })]
    expect(componerLista(catalogo, new Map(), { ...TODO, estado: 'obligatorio' }).map((f) => f.tutorial.id)).toEqual(['a'])
    expect(componerLista(catalogo, new Map(), { ...TODO, categoria: 'Operación' }).map((f) => f.tutorial.id)).toEqual(['b'])
  })

  it('marca como continuable lo que quedó a mitad de camino', () => {
    const avance = new Map([['a', progreso('a', { estado: 'en-progreso', indice: 1 })]])
    expect(componerLista([t('a')], avance, TODO)[0]?.continuable).toBe(true)
  })

  it('el recomendado es el primero sin requisitos pendientes', () => {
    const catalogo = [t('avanzado', { requisitos: ['base'] }), t('base')]
    const lista = componerLista(catalogo, new Map(), TODO)
    expect(recomendado(lista)?.tutorial.id).toBe('base')
  })

  it('sin nada que hacer, no hay recomendado', () => {
    const lista = componerLista([t('a')], new Map([['a', progreso('a')]]), TODO)
    expect(recomendado(lista)).toBeUndefined()
  })
})
