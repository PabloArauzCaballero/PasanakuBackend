import { HttpClient, HttpParams } from '@angular/common/http'
import { firstValueFrom } from 'rxjs'
import { cargadorPorCursor, type PaginaPorCursor, type PedidoPorCursor } from '../../../nucleo/tabla/cursor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'

/**
 * Lo que comparten los tres listados de `servicios/publicidad/src/main/resources/openapi/publicidad.yaml` (carril A · H7):
 * `listarAnunciantes`, `listarCampanas` y `listarPiezasCreativas` responden `{ items, proximoFecha, proximoId }` y
 * aceptan `limite` (1–100) y el cursor `despuesDeFecha` + `despuesDeId`, **juntos o ninguno** (a medias es un 422
 * `FILTRO_DE_LISTADO_INVALIDO`). No traen total ni ordenan por columna: la tabla pagina con `cargadorPorCursor`.
 *
 * Los importes de un listado son `Dinero` (cadena + moneda): nunca un número.
 */
export type Moneda = 'BOB' | 'USD'
export type Dinero = { monto: string; moneda: Moneda }

export type CursorPublicidad = { despuesDeFecha: string; despuesDeId: string }

export type PaginaPublicidad<T> = { items: T[]; proximoFecha?: string | null; proximoId?: string | null }

/** Solo hay siguiente si el servidor mandó LAS DOS mitades del cursor; con una sola, pedir sería un 422 seguro. */
export function aPaginaPorCursor<T>(pagina: PaginaPublicidad<T>): PaginaPorCursor<T, CursorPublicidad> {
  const siguiente = pagina.proximoFecha && pagina.proximoId ? { despuesDeFecha: pagina.proximoFecha, despuesDeId: pagina.proximoId } : null
  return { items: pagina.items ?? [], siguiente }
}

/** Traduce el filtro de la URL al parámetro del contrato (`estados` en anunciantes y campañas, `estadoModeracion` en piezas). */
export type FiltroAParametros = (filtros: Readonly<Record<string, string>>, params: HttpParams) => HttpParams

export const sinFiltro: FiltroAParametros = (_f, params) => params

export function parametrosDeListado(pedido: PedidoPorCursor<CursorPublicidad>, filtro: FiltroAParametros): HttpParams {
  let params = filtro(pedido.filtros, new HttpParams().set('limite', pedido.limite))
  if (pedido.cursor) params = params.set('despuesDeFecha', pedido.cursor.despuesDeFecha).set('despuesDeId', pedido.cursor.despuesDeId)
  return params
}

/** El `cargador` de la tabla sobre un listado por cursor de publicidad. */
export function cargadorPublicidadPorCursor<T>(http: HttpClient, url: string, filtro: FiltroAParametros = sinFiltro): CargadorDePagina<T> {
  return cargadorPorCursor<T, CursorPublicidad>(async (pedido) =>
    aPaginaPorCursor(await firstValueFrom(http.get<PaginaPublicidad<T>>(url, { params: parametrosDeListado(pedido, filtro) }))),
  )
}
