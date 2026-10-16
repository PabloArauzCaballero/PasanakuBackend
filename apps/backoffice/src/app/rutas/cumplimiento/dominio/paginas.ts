import { HttpClient, HttpParams } from '@angular/common/http'
import { firstValueFrom } from 'rxjs'
import type { PedirPorCursor } from '../../../nucleo/tabla/cursor'

/**
 * Cómo pide páginas cada bandeja de `cumplimiento.yaml`. Hay dos familias y no se mezclan:
 *
 * - **H5 (CU-44)**: cursor ascendente con su propio nombre (`despuesDeFecha` / `despuesDePlazo`
 *   + `despuesDeId`) y filtros múltiples `explode: false` → `estados=ABIERTA,EN_ANALISIS`.
 * - **H9 (CU-43/45/54/55/56/94)**: cursor descendente `antesDe` + `antesDeId`, respuesta
 *   `proximoAntesDe` + `proximoAntesDeId`, y `estados` con `explode: true` → `estados=A&estados=B`.
 *
 * El cursor es un PAR (fecha, id): si el servidor devuelve solo uno, no hay próxima página
 * —mandar uno solo es «cursor a medias» y el servidor lo rechaza con su FILTRO_INVALIDO—.
 */
export type FormaDePagina = {
  parametroFecha: string
  parametroId: string
  campoFecha: string
  campoId: string
  /** `true` = `explode: false` del contrato (una sola clave con comas). */
  multiplesConComa: boolean
}

export const FORMA_H9: FormaDePagina = {
  parametroFecha: 'antesDe',
  parametroId: 'antesDeId',
  campoFecha: 'proximoAntesDe',
  campoId: 'proximoAntesDeId',
  multiplesConComa: false,
}

export type CursorDePar = { fecha: string; id: string }

/** Lista blanca de filtros que viajan; cualquier otra clave de la tabla se ignora (no se inventan parámetros). */
export function pedirPagina<T>(
  http: HttpClient,
  url: string,
  forma: FormaDePagina,
  filtrosPermitidos: readonly string[],
): PedirPorCursor<T, CursorDePar> {
  return async ({ limite, cursor, filtros }) => {
    let params = new HttpParams().set('limite', limite)
    for (const clave of filtrosPermitidos) {
      const valores = (filtros[clave] ?? '').split(',').filter(Boolean)
      if (!valores.length) continue
      if (forma.multiplesConComa) params = params.set(clave, valores.join(','))
      else for (const v of valores) params = params.append(clave, v)
    }
    if (cursor) params = params.set(forma.parametroFecha, cursor.fecha).set(forma.parametroId, cursor.id)
    const pagina = await firstValueFrom(http.get<Record<string, unknown> & { items: T[] }>(url, { params }))
    const fecha = pagina[forma.campoFecha]
    const id = pagina[forma.campoId]
    const siguiente = typeof fecha === 'string' && typeof id === 'string' ? { fecha, id } : null
    return { items: pagina.items ?? [], siguiente }
  }
}
