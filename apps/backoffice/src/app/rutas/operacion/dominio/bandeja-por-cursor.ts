import { HttpClient, HttpParams } from '@angular/common/http'
import { firstValueFrom } from 'rxjs'

/**
 * Lo común a los pedidos de las bandejas de operación y tesorería (carril B · L1): todas son `GET` paginados por cursor
 * (`convenciones-contrato.md`). El estado de la pantalla (página, cursores, carga, error, filtros) lo lleva
 * `comun/listado-por-cursor.ts`; acá queda solo cómo se arma la consulta y cómo se lee el cursor de la respuesta.
 */
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

/** El filtro de servidor `estados` llega como `A,B` (o un solo valor) desde `crearListadoPorCursor`. */
export function estadosDe(filtros: Readonly<Record<string, string>>): string[] {
  return (filtros['estados'] ?? '').split(',').filter(Boolean)
}
