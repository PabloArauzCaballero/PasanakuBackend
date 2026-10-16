import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http'
import { signal } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { mensajeDe } from '../../../nucleo/errores'
import { cargadorPorCursor } from '../../../nucleo/tabla/cursor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'

/**
 * Lectura paginada por cursor de los listados de configuración (A6: tarifario, mensajería, reportes). Todos responden
 * `{ items, proximo<A>, proximo<B> }` y la última página **omite** el cursor (no es `null`: `tarifas.yaml`,
 * `notificaciones.yaml` y `auditoria.yaml` no lo declaran nullable), así que el fin es «falta cualquiera de los dos».
 * El filtro `estado` de la URL (`?filtro=A,B`, `BarraDeFiltros`) viaja como `estados` repetido (`explode: true`).
 */
export type NombresDeCursor = {
  /** Los dos parámetros con que se pide la página siguiente (p. ej. `antesDe`, `antesDeId`). */
  parametros: readonly [string, string]
  /** Los dos campos de la respuesta que los traen (p. ej. `proximoAntesDe`, `proximoAntesDeId`). */
  proximo: readonly [string, string]
}

export const CURSOR_DESCENDENTE: NombresDeCursor = { parametros: ['antesDe', 'antesDeId'], proximo: ['proximoAntesDe', 'proximoAntesDeId'] }

export const FUERA_DE_CONTRATO = 'El servicio respondió algo que no esperábamos y no lo mostramos. Avisá a Sistemas con el código de seguimiento.'

export type ErrorDeLectura = { mensaje: string; trazaId?: string }

/** Lo que llega ya traducido por `nucleo/errores.interceptor.ts`, o se traduce acá con el mismo catálogo. Nunca el crudo. */
export function traducirError(error: unknown): ErrorDeLectura {
  if (error instanceof HttpErrorResponse) {
    const cuerpo = (error.error ?? {}) as Partial<{ codigo: string; trazaId: string }>
    return { mensaje: mensajeDe(cuerpo.codigo, error.status), trazaId: cuerpo.trazaId }
  }
  if (typeof error === 'object' && error !== null && typeof (error as { mensaje?: unknown }).mensaje === 'string') {
    const e = error as { mensaje: string; trazaId?: string }
    return { mensaje: e.mensaje, trazaId: e.trazaId }
  }
  return { mensaje: mensajeDe(undefined, 500) }
}

export function cargadorDeListado<T>(http: HttpClient, url: string, cursor: NombresDeCursor): CargadorDePagina<T> {
  return cargadorPorCursor<T, readonly [string, string]>(async ({ limite, cursor: actual, filtros }) => {
    let params = new HttpParams().set('limite', limite)
    if (actual) params = params.set(cursor.parametros[0], actual[0]).set(cursor.parametros[1], actual[1])
    for (const estado of (filtros['estado'] ?? '').split(',').filter(Boolean)) params = params.append('estados', estado)
    const respuesta = await firstValueFrom(http.get<Record<string, unknown>>(url, { params }))
    if (!Array.isArray(respuesta?.['items'])) throw { mensaje: FUERA_DE_CONTRATO } satisfies ErrorDeLectura
    const [a, b] = [respuesta[cursor.proximo[0]], respuesta[cursor.proximo[1]]]
    const siguiente = typeof a === 'string' && typeof b === 'string' ? ([a, b] as const) : null
    return { items: respuesta['items'] as T[], siguiente }
  })
}

/**
 * Los estados que la tabla no pinta por sí sola: el error (la tabla no atrapa un rechazo del cargador) y el vacío de la
 * primera página. Envuelve el cargador; la pantalla decide qué mostrar.
 */
export class EstadoDeListado {
  readonly error = signal<ErrorDeLectura | null>(null)
  readonly vacio = signal(false)

  envolver<T>(cargador: CargadorDePagina<T>): CargadorDePagina<T> {
    return async (pedido) => {
      try {
        const pagina = await cargador(pedido)
        this.vacio.set(pedido.pagina === 1 && pagina.filas.length === 0)
        return pagina
      } catch (error) {
        this.error.set(traducirError(error))
        return { filas: [], total: 0 }
      }
    }
  }

  reintentar(): void {
    this.error.set(null)
  }
}

/** Un campo opcional ausente se dice; nunca se completa con cero ni con un valor supuesto. */
export const NO_DISPONIBLE = 'no disponible'
