import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { PaginaDeReversos } from 'clientes/angular/nucleo-financiero/model/paginaDeReversos'
import type { ReversoEnLista } from 'clientes/angular/nucleo-financiero/model/reversoEnLista'
import { GATEWAY } from '../../../../nucleo/gateway'
import type { PedirPorCursor } from '../../../../nucleo/tabla/cursor'
import { bandejaPorCursor, cursorDe, estadosDe, pedirJson, type Bandeja } from '../../dominio/bandeja-por-cursor'

export type { ReversoEnLista }

/**
 * CU-14 · reversos de transacciones, solo lectura (`listarReversos`, `GET /contabilidad/reversos`, AUDITORIA_LEER).
 * Cursor `(antesDe, antesDeId)` descendente; `estados` es `style: form, explode: false` (una clave, valores con coma);
 * sin estados el servidor devuelve todos. `montoReversado` es cadena SIN moneda (A-7).
 */
type Cursor = { a: string; b: string }

export function pedirReversos(http: HttpClient, gateway: string): PedirPorCursor<ReversoEnLista, Cursor> {
  return async ({ limite, cursor, filtros }) => {
    const estados = estadosDe(filtros)
    const p = await pedirJson<PaginaDeReversos>(http, `${gateway}/contabilidad/reversos`, {
      estados: estados.length ? estados.join(',') : undefined,
      limite,
      antesDe: cursor?.a,
      antesDeId: cursor?.b,
    })
    return { items: p.items, siguiente: cursorDe(p.items, p.proximoAntesDe, p.proximoAntesDeId) }
  }
}

export function bandejaDeReversos(): Bandeja<ReversoEnLista> {
  return bandejaPorCursor(pedirReversos(inject(HttpClient), inject(GATEWAY)))
}
