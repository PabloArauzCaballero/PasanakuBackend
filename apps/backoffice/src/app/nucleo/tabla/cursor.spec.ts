import { describe, expect, it } from 'vitest'
import { cargadorPorCursor, type PedidoPorCursor } from './cursor'

/** Servidor falso de cursor: 5 filas, el cursor es el índice de la próxima fila (sintético). */
function servidor(filas: number) {
  const pedidos: PedidoPorCursor<number>[] = []
  const pedir = async (p: PedidoPorCursor<number>) => {
    pedidos.push(p)
    const desde = p.cursor ?? 0
    const items = Array.from({ length: Math.max(0, Math.min(p.limite, filas - desde)) }, (_, i) => desde + i)
    const siguiente = desde + items.length < filas ? desde + items.length : null
    return { items, siguiente }
  }
  return { pedir, pedidos }
}

const pedido = (pagina: number, filtros: Record<string, string> = {}) => ({ pagina, tamano: 2, orden: null, filtros })

describe('cargadorPorCursor', () => {
  it('la primera página va sin cursor y ofrece una página más si el servidor dijo que hay', async () => {
    const s = servidor(5)
    const r = await cargadorPorCursor(s.pedir)(pedido(1))
    expect(r).toEqual({ filas: [0, 1], total: 4 })
    expect(s.pedidos.at(0)).toEqual({ limite: 2, cursor: null, filtros: {} })
  })

  it('la página N se pide con el cursor que devolvió la N-1, sin recalcularlo', async () => {
    const s = servidor(5)
    const cargar = cargadorPorCursor(s.pedir)
    await cargar(pedido(1))
    const r = await cargar(pedido(2))
    expect(s.pedidos.at(1)?.cursor).toBe(2)
    expect(r.filas).toEqual([2, 3])
  })

  it('en la última página el total es exacto: no se ofrece avanzar', async () => {
    const s = servidor(5)
    const cargar = cargadorPorCursor(s.pedir)
    await cargar(pedido(1))
    await cargar(pedido(2))
    expect(await cargar(pedido(3))).toEqual({ filas: [4], total: 5 })
  })

  it('volver de la última a la primera no achica el total: las páginas recorridas siguen alcanzables', async () => {
    const s = servidor(5)
    const cargar = cargadorPorCursor(s.pedir)
    await cargar(pedido(1))
    await cargar(pedido(2))
    await cargar(pedido(3))
    expect((await cargar(pedido(2))).total).toBe(5)
    expect(await cargar(pedido(1))).toEqual({ filas: [0, 1], total: 5 })
  })

  it('sin el final conocido, volver atrás sigue ofreciendo todas las páginas con cursor', async () => {
    const s = servidor(9)
    const cargar = cargadorPorCursor(s.pedir)
    await cargar(pedido(1))
    await cargar(pedido(2))
    await cargar(pedido(3))
    expect((await cargar(pedido(1))).total).toBe(8)
  })

  it('saltar a una página sin cursor conocido recorre desde la última conocida', async () => {
    const s = servidor(5)
    const r = await cargadorPorCursor(s.pedir)(pedido(3))
    expect(s.pedidos.map((p) => p.cursor)).toEqual([null, 2, 4])
    expect(r.filas).toEqual([4])
  })

  it('cambiar los filtros reinicia los cursores', async () => {
    const s = servidor(5)
    const cargar = cargadorPorCursor(s.pedir)
    await cargar(pedido(1))
    await cargar(pedido(2))
    await cargar(pedido(2, { estado: 'ABIERTA' }))
    expect(s.pedidos.at(-2)).toEqual({ limite: 2, cursor: null, filtros: { estado: 'ABIERTA' } })
  })

  it('lista vacía: sin filas y total cero', async () => {
    expect(await cargadorPorCursor(servidor(0).pedir)(pedido(1))).toEqual({ filas: [], total: 0 })
  })
})
