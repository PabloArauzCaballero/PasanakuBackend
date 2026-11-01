import { describe, expect, it } from 'vitest'
import type { PedidoPorCursor, PaginaPorCursor } from '../../../nucleo/tabla/cursor'
import { crearListadoPorCursor } from './listado-por-cursor'

/**
 * `crearListadoPorCursor` contra un **doble del contrato** `PedirPorCursor` (sin HTTP): una colección sintética de 25
 * filas que el doble pagina por cursor numérico, filtra por `estados` y puede fallar o responder tarde a pedido.
 */
type Fila = { id: string; nombre: string; estado: 'A' | 'B'; tipo: 'x' | 'y' }
const TODAS: Fila[] = Array.from({ length: 25 }, (_, i) => ({ id: `f${i + 1}`, nombre: i === 3 ? 'José Pérez' : `fila ${i + 1}`, estado: i % 2 ? 'B' : 'A', tipo: i < 10 ? 'x' : 'y' }))
const espera = () => new Promise((r) => setTimeout(r, 0))

function doble(filas: Fila[] = TODAS) {
  const pedidos: PedidoPorCursor<number>[] = []
  let falla: { estado: number; mensaje: string; trazaId?: string } | null = null
  const demoras: (() => void)[] = []
  let demorar = false
  const pedir = async (p: PedidoPorCursor<number>): Promise<PaginaPorCursor<Fila, number>> => {
    pedidos.push(p)
    if (demorar) await new Promise<void>((r) => demoras.push(r))
    if (falla) throw falla
    const estados = (p.filtros['estados'] ?? '').split(',').filter(Boolean)
    const base = estados.length ? filas.filter((f) => estados.includes(f.estado)) : filas
    const desde = p.cursor ?? 0
    const items = base.slice(desde, desde + p.limite)
    return { items, siguiente: desde + p.limite < base.length ? desde + p.limite : null }
  }
  return { pedir, pedidos, fallar: (e: typeof falla) => (falla = e), demorar: (v: boolean) => (demorar = v), soltar: () => demoras.splice(0).forEach((r) => r()) }
}

function crear(d = doble()) {
  const l = crearListadoPorCursor<Fila, number>({
    pedir: d.pedir,
    camposDeBusqueda: ['nombre', 'id'],
    filtrosLocales: [{ nombre: 'tipo', etiqueta: 'Tipo', valor: (f) => f.tipo }],
    filtrosDeServidor: [{ nombre: 'estados', etiqueta: 'Estado', opciones: [{ valor: 'A', texto: 'A' }, { valor: 'B', texto: 'B' }] }],
  })
  return { l, d }
}

describe('crearListadoPorCursor · [doble del contrato PedirPorCursor]', () => {
  it('aceptado: primera página de 10 sin cursor; Siguiente pide con el cursor del servidor; Ir a 3 recorre sin inventar cursor', async () => {
    const { l, d } = crear()
    expect(l.estado()).toBe('cargando')
    await espera()
    expect(l.estado()).toBe('lista')
    expect(d.pedidos[0]).toMatchObject({ limite: 10, cursor: null })
    expect(l.local.visibles().map((f) => f.id)).toEqual(TODAS.slice(0, 10).map((f) => f.id))
    // Vistas 10 + una página más porque el servidor dijo que hay próxima.
    expect(l.totalConocido()).toBe(20)
    l.irAPagina(2)
    await espera()
    expect(d.pedidos.at(-1)).toMatchObject({ cursor: 10 })
    l.irAPagina(3)
    await espera()
    expect(d.pedidos.at(-1)).toMatchObject({ cursor: 20 })
    expect(l.local.visibles()).toHaveLength(5)
    expect(l.totalConocido()).toBe(25)
  })

  it('aceptado: cambiar filas por página vuelve a la 1 y pide con el nuevo límite; repetir el mismo tamaño no pide', async () => {
    const { l, d } = crear()
    await espera()
    l.irAPagina(2)
    await espera()
    l.cambiarPorPagina(50)
    await espera()
    expect(l.pagina()).toBe(1)
    expect(d.pedidos.at(-1)).toMatchObject({ limite: 50, cursor: null })
    const antes = d.pedidos.length
    l.cambiarPorPagina(50)
    l.irAPagina(1)
    expect(d.pedidos.length).toBe(antes)
  })

  it('aceptado: el filtro de servidor viaja como `estados` y vuelve a la página 1; el local y la búsqueda (sin acentos) no piden', async () => {
    const { l, d } = crear()
    await espera()
    l.irAPagina(2)
    await espera()
    l.filtrar('estados', 'B')
    await espera()
    expect(l.pagina()).toBe(1)
    expect(d.pedidos.at(-1)?.filtros).toEqual({ estados: 'B' })
    expect(l.local.visibles().every((f) => f.estado === 'B')).toBe(true)
    const antes = d.pedidos.length
    l.filtrar('estados', '')
    await espera()
    l.buscar('jose')
    l.filtrar('tipo', 'x')
    expect(d.pedidos.length).toBe(antes + 1)
    expect(l.local.filtradas().map((f) => f.nombre)).toEqual(['José Pérez'])
    expect(l.selectores().map((s) => s.nombre)).toEqual(['estados', 'tipo'])
  })

  it('límite: colección vacía ⇒ lista sin filas y sin página siguiente; «Limpiar» con solo criterios locales no vuelve a pedir', async () => {
    const { l, d } = crear(doble([]))
    await espera()
    expect(l.estado()).toBe('lista')
    expect(l.local.total()).toBe(0)
    expect(l.totalConocido()).toBe(0)
    l.buscar('nada')
    l.limpiar()
    expect(d.pedidos).toHaveLength(1)
    expect(l.hayCriterios()).toBe(false)
  })

  it('límite: una sola fila ⇒ una página; búsqueda sin coincidencias ⇒ 0 filtradas con criterios activos', async () => {
    const { l } = crear(doble(TODAS.slice(0, 1)))
    await espera()
    expect(l.totalConocido()).toBe(1)
    l.buscar('zzz')
    expect(l.local.total()).toBe(0)
    expect(l.hayCriterios()).toBe(true)
  })

  it('error: 500 ⇒ estado error con el ErrorTraducido; «recargar» repite el pedido y se recupera', async () => {
    const d = doble()
    d.fallar({ estado: 500, mensaje: 'No pudimos completar la operación.', trazaId: 't-1' })
    const { l } = crear(d)
    await espera()
    expect(l.estado()).toBe('error')
    expect(l.error()?.trazaId).toBe('t-1')
    d.fallar(null)
    l.recargar()
    await espera()
    expect(l.estado()).toBe('lista')
    expect(d.pedidos).toHaveLength(2)
  })

  it('error: 403 ⇒ denegado (no error genérico)', async () => {
    const d = doble()
    d.fallar({ estado: 403, mensaje: 'x' })
    const { l } = crear(d)
    await espera()
    expect(l.estado()).toBe('denegado')
  })

  it('error: una respuesta vieja que llega después de cambiar de página se descarta', async () => {
    const d = doble()
    const { l } = crear(d)
    await espera()
    d.demorar(true)
    l.irAPagina(2)
    l.irAPagina(1)
    d.soltar()
    await espera()
    d.soltar()
    await espera()
    expect(l.pagina()).toBe(1)
    expect(l.local.visibles()[0]?.id).toBe('f1')
  })
})
