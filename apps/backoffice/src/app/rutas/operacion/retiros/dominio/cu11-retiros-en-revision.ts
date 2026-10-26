import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { PaginaRetirosEnRevision } from 'clientes/angular/nucleo-financiero/model/paginaRetirosEnRevision'
import type { RetiroEnRevision } from 'clientes/angular/nucleo-financiero/model/retiroEnRevision'
import { GATEWAY } from '../../../../nucleo/gateway'
import type { PedirPorCursor } from '../../../../nucleo/tabla/cursor'
import { cursorDe, pedirJson } from '../../dominio/bandeja-por-cursor'

export type { RetiroEnRevision }

/**
 * CU-11 · bandeja de retiros que esperan la segunda firma (`listarRetirosEnRevision`, `GET /billetera/retiros`,
 * permiso `RETIRO_APROBAR`). Cursor `(despuesDeFecha, despuesDeId)` ascendente, del más antiguo al más nuevo.
 * La fila no trae nombre ni cuenta de destino (respuesta mínima del contrato) y la pantalla tampoco muestra los
 * identificadores de persona/cuenta que sí trae (`solicitadaPor`, `cuentaBilleteraId`): no hacen falta para decidir.
 */
type Cursor = { a: string; b: string }

export function pedirRetirosEnRevision(http: HttpClient, gateway: string): PedirPorCursor<RetiroEnRevision, Cursor> {
  return async ({ limite, cursor }) => {
    const p = await pedirJson<PaginaRetirosEnRevision>(http, `${gateway}/billetera/retiros`, {
      limite,
      despuesDeFecha: cursor?.a,
      despuesDeId: cursor?.b,
    })
    return { items: p.items, siguiente: cursorDe(p.items, p.proximoFecha, p.proximoId) }
  }
}

/** Fábrica en contexto de inyección: la pantalla no importa `HttpClient` (checker «sin red en vista»). */
export function pedidorDeRetirosEnRevision(): PedirPorCursor<RetiroEnRevision, Cursor> {
  return pedirRetirosEnRevision(inject(HttpClient), inject(GATEWAY))
}
