import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type { CierreDiarioEnLista } from 'clientes/angular/nucleo-financiero/model/cierreDiarioEnLista'
import type { EntradaCierreDiario } from 'clientes/angular/nucleo-financiero/model/entradaCierreDiario'
import type { PaginaDeCierresDiarios } from 'clientes/angular/nucleo-financiero/model/paginaDeCierresDiarios'
import type { SalidaCierreDiario } from 'clientes/angular/nucleo-financiero/model/salidaCierreDiario'
import { GATEWAY } from '../../../../nucleo/gateway'
import { contextoIdempotente } from '../../../../nucleo/idempotencia.interceptor'
import type { PedirPorCursor } from '../../../../nucleo/tabla/cursor'
import { bandejaPorCursor, cursorDe, pedirJson, type Bandeja } from '../../dominio/bandeja-por-cursor'

export type { CierreDiarioEnLista, EntradaCierreDiario, SalidaCierreDiario }

/**
 * CU-51 · cierre diario (`nucleo-financiero.yaml`, carril A · H8):
 * - `listarCierresDiarios` `GET /contabilidad/cierres-diarios` (AUDITORIA_LEER): cursor `(antesDe, antesDeId)` descendente.
 *   Los importes son cadena SIN moneda (A-7).
 * - `ejecutarCierreDiario` `POST /contabilidad/cierres-diarios` (rol CONTABILIDAD, `Idempotency-Key`): el cliente manda
 *   solo `{ fecha }`; las cifras las informa aportes y el cuadre sale de las conciliaciones del día. Idempotente por
 *   fecha: un día ya cerrado devuelve 200 con `yaExistia = true`.
 */
type Cursor = { a: string; b: string }

export function pedirCierresDiarios(http: HttpClient, gateway: string): PedirPorCursor<CierreDiarioEnLista, Cursor> {
  return async ({ limite, cursor }) => {
    const p = await pedirJson<PaginaDeCierresDiarios>(http, `${gateway}/contabilidad/cierres-diarios`, { limite, antesDe: cursor?.a, antesDeId: cursor?.b })
    return { items: p.items, siguiente: cursorDe(p.items, p.proximoAntesDe, p.proximoAntesDeId) }
  }
}

export function bandejaDeCierresDiarios(): Bandeja<CierreDiarioEnLista> {
  return bandejaPorCursor(pedirCierresDiarios(inject(HttpClient), inject(GATEWAY)))
}

export function crearEjecutarCierreDiario(): (entrada: EntradaCierreDiario) => Observable<SalidaCierreDiario> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (entrada) => http.post<SalidaCierreDiario>(`${gateway}/contabilidad/cierres-diarios`, entrada, { context: contextoIdempotente() })
}

/**
 * Hoy en La Paz (UTC−4 todo el año, sin horario de verano) como `AAAA-MM-DD`. El contrato rechaza cerrar hoy o un día
 * futuro (`AP-CU51-06`, zona America/La_Paz): la pantalla lo advierte antes, el servidor decide.
 */
export function hoyEnLaPaz(ahora: Date = new Date()): string {
  return new Date(ahora.getTime() - 4 * 60 * 60 * 1000).toISOString().slice(0, 10)
}
