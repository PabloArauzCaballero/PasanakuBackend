import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type { ConciliacionEnLista } from 'clientes/angular/nucleo-financiero/model/conciliacionEnLista'
import type { EntradaConciliacionCustodia } from 'clientes/angular/nucleo-financiero/model/entradaConciliacionCustodia'
import type { PaginaDeConciliaciones } from 'clientes/angular/nucleo-financiero/model/paginaDeConciliaciones'
import type { SalidaConciliacionCustodia } from 'clientes/angular/nucleo-financiero/model/salidaConciliacionCustodia'
import { GATEWAY } from '../../../../nucleo/gateway'
import { contextoIdempotente } from '../../../../nucleo/idempotencia.interceptor'
import type { PedirPorCursor } from '../../../../nucleo/tabla/cursor'
import { bandejaPorCursor, cursorDe, pedirJson, type Bandeja } from '../../dominio/bandeja-por-cursor'

export type { ConciliacionEnLista, EntradaConciliacionCustodia, SalidaConciliacionCustodia }

/**
 * CU-50 · conciliación de custodia (`nucleo-financiero.yaml`, carril A · H8):
 * - `listarConciliaciones` `GET /custodia/conciliaciones` (AUDITORIA_LEER): cursor `(antesDe, antesDeId)` descendente.
 * - `conciliarCustodia` `POST /custodia/conciliaciones` (rol TESORERIA, `Idempotency-Key`). El saldo emitido lo
 *   calcula el servidor: el cliente manda solo cuenta, día, saldo del extracto y tránsito. Reintentar con los mismos
 *   saldos devuelve la misma conciliación; otros saldos para el mismo día es `AP-CU50-03`.
 */
type Cursor = { a: string; b: string }

export function pedirConciliaciones(http: HttpClient, gateway: string): PedirPorCursor<ConciliacionEnLista, Cursor> {
  return async ({ limite, cursor }) => {
    const p = await pedirJson<PaginaDeConciliaciones>(http, `${gateway}/custodia/conciliaciones`, { limite, antesDe: cursor?.a, antesDeId: cursor?.b })
    return { items: p.items, siguiente: cursorDe(p.items, p.proximoAntesDe, p.proximoAntesDeId) }
  }
}

export function bandejaDeConciliaciones(): Bandeja<ConciliacionEnLista> {
  return bandejaPorCursor(pedirConciliaciones(inject(HttpClient), inject(GATEWAY)))
}

export function crearConciliarCustodia(): (entrada: EntradaConciliacionCustodia) => Observable<SalidaConciliacionCustodia> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (entrada) => http.post<SalidaConciliacionCustodia>(`${gateway}/custodia/conciliaciones`, entrada, { context: contextoIdempotente() })
}

/** «0.00» o «-0.00»: cuadra. Comparación de cadena del contrato, sin pasar por número. */
export const esCero = (monto: string): boolean => /^-?0+\.00$/.test(monto)
