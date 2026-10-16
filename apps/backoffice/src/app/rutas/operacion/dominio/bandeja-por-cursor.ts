import { HttpClient, HttpParams } from '@angular/common/http'
import { signal, type Signal } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { cargadorPorCursor, type PedirPorCursor } from '../../../nucleo/tabla/cursor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'

/**
 * Lo común a las bandejas de operación y tesorería (carril B · L1): todas son `GET` paginados por cursor
 * (`convenciones-contrato.md`) que la `TablaDeDatosVirtualizada` consume por número de página.
 *
 * La tabla no atrapa el error de su cargador (`nucleo/tabla`, fuera de este lote): si el pedido falla, la promesa
 * se rechaza y la pantalla queda en blanco. Esta envoltura lo atrapa, lo expone como estado de la pantalla
 * (carga · datos · vacío · error · denegado, regla 95.2) y le devuelve a la tabla una página vacía.
 */
export type EstadoDeBandeja = 'cargando' | 'datos' | 'vacio' | 'error' | 'denegado'

export type Bandeja<T> = {
  cargador: CargadorDePagina<T>
  estado: Signal<EstadoDeBandeja>
  error: Signal<ErrorTraducido | null>
  /** Cambia en cada reintento: la pantalla vuelve a montar la tabla con `@for (v of [version()]; track v)`. */
  version: Signal<number>
  reintentar: () => void
}

export function bandejaPorCursor<T, C>(pedir: PedirPorCursor<T, C>): Bandeja<T> {
  const estado = signal<EstadoDeBandeja>('cargando')
  const error = signal<ErrorTraducido | null>(null)
  const version = signal(0)
  const base = cargadorPorCursor(pedir)
  const cargador: CargadorDePagina<T> = async (pedido) => {
    estado.set('cargando')
    error.set(null)
    try {
      const pagina = await base(pedido)
      estado.set(pagina.filas.length === 0 && pedido.pagina <= 1 ? 'vacio' : 'datos')
      return pagina
    } catch (e) {
      const traducido = e as ErrorTraducido
      error.set(traducido)
      estado.set(traducido?.estado === 403 ? 'denegado' : 'error')
      return { filas: [], total: 0 }
    }
  }
  return { cargador, estado, error, version, reintentar: () => version.update((v) => v + 1) }
}

type ValorDeParametro = string | number | readonly string[] | null | undefined

/** `GET` con parámetros de consulta; omite los vacíos (el contrato rechaza un cursor a medias, no uno ausente). */
export function pedirJson<R>(http: HttpClient, url: string, parametros: Record<string, ValorDeParametro>): Promise<R> {
  let params = new HttpParams()
  for (const [clave, valor] of Object.entries(parametros)) {
    if (valor === null || valor === undefined || valor === '') continue
    if (Array.isArray(valor)) for (const v of valor) params = params.append(clave, v)
    else params = params.set(clave, String(valor))
  }
  return firstValueFrom(http.get<R>(url, { params }))
}

/** Un cursor solo existe con sus dos mitades y con filas en la página: si no, no hay próxima. */
export function cursorDe<A, B>(items: readonly unknown[], a: A | null | undefined, b: B | null | undefined): { a: A; b: B } | null {
  return items.length > 0 && a != null && b != null ? { a, b } : null
}

/** Los filtros de estado viajan en la URL como `?filtro=A,B` (BarraDeFiltros); el cargador los recibe como `estados`. */
export function estadosDe(filtros: Readonly<Record<string, string>>): string[] {
  return (filtros['estados'] ?? '').split(',').filter(Boolean)
}
