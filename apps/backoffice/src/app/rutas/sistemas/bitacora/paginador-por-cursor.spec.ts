import { describe, expect, it } from 'vitest'
import { crearPaginadorPorCursor } from './paginador-por-cursor'

const respuesta = (n: number, siguiente = true) => ({ items: [], totalAproximado: 100, ...(siguiente ? { proximoAntesDe: `2026-10-${String(30 - n).padStart(2, '0')}T00:00:00Z`, proximoAntesDeId: `id-${n}` } : {}) })

describe('crearPaginadorPorCursor · paginación del servidor por cursor', () => {
  it('aceptado: la página 1 va sin cursor; Siguiente usa el cursor que trajo la respuesta', () => {
    const p = crearPaginadorPorCursor()
    expect(p.antesDe()).toBeNull()
    p.recibir(respuesta(1))
    p.irAPagina(2)
    expect(p.pagina()).toBe(2)
    expect(p.antesDeId()).toBe('id-1')
    expect(p.totalDeFilas()).toBe(100)
  })

  it('aceptado: «Ir a 3» recorre la 2 y llega a la 3; volver a la 1 no repite el recorrido', () => {
    const p = crearPaginadorPorCursor()
    p.recibir(respuesta(1))
    p.irAPagina(3)
    expect(p.pagina()).toBe(2)
    p.recibir(respuesta(2))
    expect(p.pagina()).toBe(3)
    expect(p.antesDeId()).toBe('id-2')
    p.irAPagina(1)
    expect(p.antesDe()).toBeNull()
    p.irAPagina(3)
    expect(p.pagina()).toBe(3)
  })

  it('límite: si el servidor deja de traer cursor, el recorrido se detiene en la última página', () => {
    const p = crearPaginadorPorCursor()
    p.recibir(respuesta(1))
    p.irAPagina(5)
    p.recibir(respuesta(2, false))
    expect(p.pagina()).toBe(2)
    expect(p.destino()).toBe(2)
    p.irAPagina(9)
    expect(p.destino()).toBe(2)
  })

  it('límite: cambiar filas por página o reiniciar vuelve a la página 1 sin cursores; un tamaño inválido se ignora', () => {
    const p = crearPaginadorPorCursor()
    p.recibir(respuesta(1))
    p.irAPagina(2)
    p.cambiarPorPagina(50)
    expect(p.pagina()).toBe(1)
    expect(p.porPagina()).toBe(50)
    expect(p.paginasConocidas()).toBe(1)
    p.cambiarPorPagina(0)
    expect(p.porPagina()).toBe(50)
  })

  it('inválido: sin total aproximado, el total crece con lo conocido; «Ir a» con basura va a la 1', () => {
    const p = crearPaginadorPorCursor({ porPagina: 10 })
    p.recibir({ items: [], proximoAntesDe: 'x', proximoAntesDeId: 'y' })
    expect(p.conTotal()).toBe(false)
    expect(p.totalDeFilas()).toBe(20)
    p.irAPagina(Number.NaN)
    expect(p.pagina()).toBe(1)
  })
})
