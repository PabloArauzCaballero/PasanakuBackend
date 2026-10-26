import { HttpClient, HttpContext, HttpParams } from '@angular/common/http'
import { firstValueFrom } from 'rxjs'
import { mensajeDe, type ErrorTraducido } from '../../../nucleo/errores'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import { cargadorPorCursor, type PaginaPorCursor, type PedidoPorCursor } from '../../../nucleo/tabla/cursor'
import type { CargadorDePagina, PaginaServidor, PedidoDePagina } from '../../../nucleo/tabla/tipos'

/**
 * Lo que `servicios/erp/src/main/resources/openapi/erp.yaml` comparte entre sus operaciones, y el adaptador de LISTADO
 * por cursor que usan las cinco bandejas del ERP (CU-101 a CU-105).
 *
 * **Contrato de listado (carril A · A6).** `listarPartidasPresupuestarias`, `listarOrdenesDeCompra`,
 * `listarFacturasDeProveedor`, `listarCuentasPorCobrar` y `listarActivosFijos` responden
 * `{ items, proximoAntesDe, proximoAntesDeId }`, en orden descendente fijo del servidor; aceptan `limite` (1–100),
 * `estados` (lista separada por comas) y el cursor `antesDe` + `antesDeId`, que van **juntos o ninguno** (a medias es un
 * 422 `FILTRO_DE_LISTADO_INVALIDO`). No traen total ni ordenan por columna: la tabla pagina con `cargadorPorCursor`
 * (`nucleo/tabla/cursor.ts`) y no ofrece ordenar, porque ordenar una sola página sería mentir sobre el resto.
 */
export type Moneda = 'BOB' | 'USD'

/** Importe como CADENA decimal, igual que el esquema `Monto` de `erp.yaml`. Nunca un number. */
export type Importe = { monto: string; moneda: Moneda }

/**
 * El importe para un CSV/JSON: la cadena decimal del contrato tal cual, con su moneda («1240.50 BOB»). Nunca pasa por
 * `Number`: lo que se exporta es exactamente lo que respondió el servidor (regla 91.1.1).
 */
export function textoDeImporte(importe: Importe | null | undefined): string {
  return importe ? `${importe.monto} ${importe.moneda}` : ''
}

/** El contexto HTTP que exige toda operación con efecto de `erp.yaml` (invariante 7). */
export function contextoConClave(): HttpContext {
  return contextoIdempotente()
}

function comparar<T>(a: T, b: T, clave: keyof T): number {
  const [va, vb] = [a[clave], b[clave]]
  if (va && typeof va === 'object' && 'monto' in va && vb && typeof vb === 'object' && 'monto' in vb) {
    return Number((va as { monto: string }).monto) - Number((vb as { monto: string }).monto)
  }
  return String(va ?? '').localeCompare(String(vb ?? ''))
}

/**
 * Pagina, ordena y filtra en memoria una lista ya cargada. Ninguna bandeja del ERP la usa desde que el contrato pagina
 * por cursor; queda porque su comportamiento tiene prueba propia (`cu103-facturas.spec.ts`).
 */
export function paginarEnElAdaptador<T extends object>(
  todos: readonly T[],
  pedido: PedidoDePagina,
  filtrar: (fila: T, filtros: Readonly<Record<string, string>>) => boolean,
  ordenPorOmision?: (a: T, b: T) => number,
): PaginaServidor<T> {
  const filtrados = todos.filter((f) => filtrar(f, pedido.filtros))
  const ordenados = pedido.orden
    ? [...filtrados].sort((a, b) => {
        const signo = pedido.orden!.sentido === 'asc' ? 1 : -1
        return signo * comparar(a, b, pedido.orden!.clave as keyof T)
      })
    : ordenPorOmision
      ? [...filtrados].sort(ordenPorOmision)
      : filtrados
  const inicio = (pedido.pagina - 1) * pedido.tamano
  return { filas: ordenados.slice(inicio, inicio + pedido.tamano), total: ordenados.length }
}

/** El cursor de los listados del ERP: fecha (o fecha-hora) e identificador de la última fila, siempre juntos. */
export type CursorErp = { antesDe: string; antesDeId: string }

/** La forma común de `PaginaDePartidas`, `PaginaDeOrdenes`, `PaginaDeFacturas`, `PaginaDeCuentasPorCobrar` y `PaginaDeActivos`. */
export type PaginaErp<T> = { items: T[]; proximoAntesDe?: string | null; proximoAntesDeId?: string | null }

/**
 * Una página del contrato a la forma de `cargadorPorCursor`. Solo hay siguiente si el servidor mandó LAS DOS mitades del
 * cursor: con una sola no se puede pedir la próxima página sin un 422, así que se trata como el final.
 */
export function aPaginaPorCursor<T>(pagina: PaginaErp<T>): PaginaPorCursor<T, CursorErp> {
  const siguiente = pagina.proximoAntesDe && pagina.proximoAntesDeId ? { antesDe: pagina.proximoAntesDe, antesDeId: pagina.proximoAntesDeId } : null
  return { items: pagina.items ?? [], siguiente }
}

/** Los parámetros de la consulta: `limite`, el filtro `estado` de la URL como `estados`, y el cursor completo si hay. */
export function parametrosDeListado(pedido: PedidoPorCursor<CursorErp>): HttpParams {
  let params = new HttpParams().set('limite', pedido.limite)
  const estado = pedido.filtros['estado']
  if (estado) params = params.set('estados', estado)
  if (pedido.cursor) params = params.set('antesDe', pedido.cursor.antesDe).set('antesDeId', pedido.cursor.antesDeId)
  return params
}

/** El `cargador` que pide `TablaDeDatosVirtualizada`, sobre un listado por cursor del ERP. */
export function cargadorErpPorCursor<T>(http: HttpClient, url: string): CargadorDePagina<T> {
  return cargadorPorCursor<T, CursorErp>(async (pedido) =>
    aPaginaPorCursor(await firstValueFrom(http.get<PaginaErp<T>>(url, { params: parametrosDeListado(pedido) }))),
  )
}

/** El error ya traducido por `erroresInterceptor` de un `httpResource`, o `null`. Nunca el cuerpo crudo del backend. */
export function errorDelRecurso(recurso: { error: () => unknown }): ErrorTraducido | null {
  const e = recurso.error()
  if (!e) return null
  return e && typeof e === 'object' && 'mensaje' in e ? (e as ErrorTraducido) : { mensaje: mensajeDe(undefined, 500) }
}

/** Filtro por un campo de estado, con el valor que la `BarraDeFiltros` dejó en la URL. */
export function filtroPorEstado<T extends { estado: string }>(fila: T, filtros: Readonly<Record<string, string>>): boolean {
  return !filtros['estado'] || fila.estado === filtros['estado']
}
