import { describe, expect, it } from 'vitest'
import type { PaginaServidor, PedidoDePagina } from '../../../nucleo/tabla/tipos'
import { crearBandeja } from './bandeja-por-cursor'

type Fila = { id: string; nombre: string; estado: string }
const FILAS: Fila[] = [
  { id: '1', nombre: 'José Pérez', estado: 'ABIERTA' },
  { id: '2', nombre: 'Ana Torres', estado: 'ESCALADA' },
]

/** Doble del cargador por cursor (declarado): registra cada pedido y deja resolverlo o rechazarlo a mano. */
function doble() {
  const pedidos: { pedido: PedidoDePagina; resolver: (p: PaginaServidor<Fila>) => void; rechazar: (e: unknown) => void }[] = []
  const cargador = (pedido: PedidoDePagina) =>
    new Promise<PaginaServidor<Fila>>((resolver, rechazar) => pedidos.push({ pedido, resolver, rechazar }))
  return { cargador, pedidos }
}
const asentar = () => new Promise((r) => setTimeout(r, 0))

describe('crearBandeja · bandeja de cumplimiento paginada por cursor', () => {
  it('correcto: carga al crearse con la página 1, 25 filas y sin filtros; queda en «lista»', async () => {
    const { cargador, pedidos } = doble()
    const b = crearBandeja<Fila>({ cargador, camposDeBusqueda: ['nombre'] })
    expect(b.carga().tipo).toBe('cargando')
    expect(pedidos[0]!.pedido).toEqual({ pagina: 1, tamano: 25, orden: null, filtros: {} })
    pedidos[0]!.resolver({ filas: FILAS, total: 2 })
    await asentar()
    expect(b.carga().tipo).toBe('lista')
    expect(b.filtradas().map((f) => f.id)).toEqual(['1', '2'])
    expect(b.hayMas()).toBe(false)
  })

  it('correcto: un filtro viaja al servidor y vuelve a la página 1; buscar filtra la página cargada sin pedir nada', async () => {
    const { cargador, pedidos } = doble()
    const b = crearBandeja<Fila>({ cargador, camposDeBusqueda: ['nombre'] })
    pedidos[0]!.resolver({ filas: FILAS, total: 50 })
    await asentar()
    b.irAPagina(2)
    expect(pedidos[1]!.pedido.pagina).toBe(2)
    b.filtrar('estados', 'ESCALADA')
    expect(pedidos[2]!.pedido).toMatchObject({ pagina: 1, filtros: { estados: 'ESCALADA' } })
    pedidos[2]!.resolver({ filas: FILAS, total: 2 })
    await asentar()
    b.buscar('jose')
    expect(pedidos).toHaveLength(3)
    expect(b.filtradas().map((f) => f.nombre)).toEqual(['José Pérez'])
    expect(b.hayCriterios()).toBe(true)
  })

  it('límite: más filas en el servidor → «hay más»; cambiar filas por página reinicia a la 1; limpiar sin filtro de servidor no recarga', async () => {
    const { cargador, pedidos } = doble()
    const b = crearBandeja<Fila>({ cargador, camposDeBusqueda: ['nombre'], porPagina: 2 })
    pedidos[0]!.resolver({ filas: FILAS, total: 4 })
    await asentar()
    expect(b.hayMas()).toBe(true)
    b.cambiarPorPagina(100)
    expect(pedidos[1]!.pedido).toMatchObject({ pagina: 1, tamano: 100 })
    b.cambiarPorPagina(100)
    b.cambiarPorPagina(0)
    expect(pedidos).toHaveLength(2)
    b.buscar('ana')
    b.limpiar()
    expect(pedidos).toHaveLength(2)
    expect(b.busqueda()).toBe('')
  })

  it('límite: una respuesta atrasada no pisa a la más nueva', async () => {
    const { cargador, pedidos } = doble()
    const b = crearBandeja<Fila>({ cargador, camposDeBusqueda: ['nombre'] })
    b.filtrar('estados', 'ABIERTA')
    pedidos[1]!.resolver({ filas: [FILAS[0]!], total: 1 })
    pedidos[0]!.resolver({ filas: FILAS, total: 2 })
    await asentar()
    expect(b.filas().map((f) => f.id)).toEqual(['1'])
  })

  it('inválido: 403 es «denegado»; otro rechazo es «error» con su traza y el texto propio de cumplimiento; recargar repite', async () => {
    const { cargador, pedidos } = doble()
    const b = crearBandeja<Fila>({ cargador, camposDeBusqueda: ['nombre'] })
    pedidos[0]!.rechazar({ estado: 403, mensaje: 'x' })
    await asentar()
    expect(b.carga().tipo).toBe('denegado')
    void b.recargar()
    pedidos[1]!.rechazar({ estado: 422, codigo: 'AP-CU44-05', mensaje: 'crudo', trazaId: 't-1' })
    await asentar()
    const c = b.carga()
    expect(c.tipo === 'error' && c.error).toMatchObject({ trazaId: 't-1', mensaje: expect.stringContaining('El filtro de la lista no es válido') })
    void b.recargar()
    expect(pedidos).toHaveLength(3)
  })
})
