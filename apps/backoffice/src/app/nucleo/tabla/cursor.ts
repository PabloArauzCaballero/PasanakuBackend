import type { CargadorDePagina, PaginaServidor, PedidoDePagina } from './tipos'

/**
 * **Bandejas paginadas por cursor (carril A, `convenciones-contrato.md`).** Todas las listas del backoffice responden
 * `{ items, proximo…, proximoId }`: no traen total ni número de página, y con `proximoId = null` no hay más. La
 * `TablaDeDatosVirtualizada` pide por número de página y muestra un total: este adaptador traduce una cosa a la otra
 * sin tocar la tabla ni inventar un total que el servidor no dio.
 *
 * - La página N se pide con el cursor que devolvió la N-1 (guardado acá). Si se salta a una página cuyo cursor no se
 *   conoce, se recorre desde la última conocida: nunca se calcula un cursor.
 * - El total que ve la tabla sale de **todo lo recorrido**, no de la página actual: con el final conocido es
 *   «páginas completas + largo de la última»; sin él, «lo más lejos que se llegó + una página». Volver de la 3 a la 1 no
 *   achica el total: antes lo recalculaba desde la página pedida y las siguientes quedaban inalcanzables.
 * - Cambiar filtros, orden o tamaño reinicia los cursores: un cursor solo vale para la consulta que lo produjo.
 */
export type PaginaPorCursor<T, C> = { items: T[]; siguiente: C | null }

export type PedidoPorCursor<C> = { limite: number; cursor: C | null; filtros: Readonly<Record<string, string>> }

export type PedirPorCursor<T, C> = (pedido: PedidoPorCursor<C>) => Promise<PaginaPorCursor<T, C>>

export function cargadorPorCursor<T, C>(pedir: PedirPorCursor<T, C>): CargadorDePagina<T> {
  let consulta = ''
  // cursores[n] = cursor con el que se pide la página n + 1 (la primera va sin cursor).
  let cursores: (C | null)[] = [null]
  let fin: number | null = null
  let largoDelFin = 0
  let maximoVisto = 0

  return async (pedido: PedidoDePagina): Promise<PaginaServidor<T>> => {
    const clave = JSON.stringify([pedido.tamano, pedido.orden, Object.entries(pedido.filtros).sort()])
    if (clave !== consulta) {
      consulta = clave
      cursores = [null]
      fin = null
      largoDelFin = 0
      maximoVisto = 0
    }
    const objetivo = Math.max(0, pedido.pagina - 1)
    let indice = Math.min(objetivo, cursores.length - 1)
    let pagina = await pedir({ limite: pedido.tamano, cursor: cursores[indice] ?? null, filtros: pedido.filtros })
    while (indice < objetivo && pagina.siguiente !== null) {
      cursores[indice + 1] = pagina.siguiente
      indice++
      pagina = await pedir({ limite: pedido.tamano, cursor: pagina.siguiente, filtros: pedido.filtros })
    }
    if (pagina.siguiente !== null) cursores[indice + 1] = pagina.siguiente
    else {
      fin = indice
      largoDelFin = pagina.items.length
    }
    maximoVisto = Math.max(maximoVisto, indice * pedido.tamano + pagina.items.length)
    const total = fin === null ? maximoVisto + pedido.tamano : fin * pedido.tamano + largoDelFin
    return { filas: pagina.items, total }
  }
}
