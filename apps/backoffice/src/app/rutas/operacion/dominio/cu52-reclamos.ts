import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type { EntradaRespuestaReclamo } from 'clientes/angular/cumplimiento/model/entradaRespuestaReclamo'
import type { FichaDeReclamo } from 'clientes/angular/cumplimiento/model/fichaDeReclamo'
import type { PaginaDeReclamos } from 'clientes/angular/cumplimiento/model/paginaDeReclamos'
import { ReclamoEnBandejaEstadoEnum, type ReclamoEnBandeja } from 'clientes/angular/cumplimiento/model/reclamoEnBandeja'
import type { SalidaRespuestaReclamo } from 'clientes/angular/cumplimiento/model/salidaRespuestaReclamo'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import type { PedirPorCursor } from '../../../nucleo/tabla/cursor'
import { bandejaPorCursor, cursorDe, estadosDe, pedirJson, type Bandeja } from './bandeja-por-cursor'

export type { EntradaRespuestaReclamo, FichaDeReclamo, ReclamoEnBandeja, SalidaRespuestaReclamo }
export type EstadoReclamo = `${ReclamoEnBandejaEstadoEnum}`
export type ResultadoReclamo = `${EntradaRespuestaReclamo['resultado']}`

/**
 * CU-52 · Punto de Reclamo, contra `cumplimiento.yaml` (carril A · H3):
 * - `listarBandejaDeReclamos` `GET /reclamos` (RECLAMO_VER): cursor `(despuesDeLimite, despuesDeId)`, ordenado por el
 *   límite vigente EN EL SERVIDOR (acá no se reordena ni se recalcula ningún plazo). Sin `estados` trae los abiertos.
 * - `consultarReclamo` `GET /reclamos/{id}` (RECLAMO_ATENDER): la ficha con el texto, sin la identidad de quien reclama.
 * - `responderReclamo` `POST /reclamos/{id}/respuesta` (RECLAMO_ATENDER, `Idempotency-Key`).
 * `montoReclamado` es cadena SIN moneda (A-7): no se supone BOB.
 */
type Cursor = { a: string; b: string }

/** El texto que acepta el contrato (`maxLength: 4000`). */
export const RESPUESTA_MAXIMA = 4000

export function pedirReclamos(http: HttpClient, gateway: string): PedirPorCursor<ReclamoEnBandeja, Cursor> {
  return async ({ limite, cursor, filtros }) => {
    const estados = estadosDe(filtros)
    const p = await pedirJson<PaginaDeReclamos>(http, `${gateway}/reclamos`, {
      // `style: form, explode: false`: una sola clave con los valores separados por coma.
      estados: estados.length ? estados.join(',') : undefined,
      limite,
      despuesDeLimite: cursor?.a,
      despuesDeId: cursor?.b,
    })
    return { items: p.items, siguiente: cursorDe(p.items, p.proximoLimite, p.proximoId) }
  }
}

/** Fábrica en contexto de inyección: la pantalla no importa `HttpClient` (checker «sin red en vista»). */
export function bandejaDeReclamos(): Bandeja<ReclamoEnBandeja> {
  return bandejaPorCursor(pedirReclamos(inject(HttpClient), inject(GATEWAY)))
}

export function crearConsultarReclamo(): (reclamoId: string) => Observable<FichaDeReclamo> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (reclamoId) => http.get<FichaDeReclamo>(`${gateway}/reclamos/${reclamoId}`)
}

/** La clave de idempotencia la gobierna el contenido: reintentar lo mismo tras perder la respuesta no duplica. */
export function crearResponderReclamo(): (reclamoId: string, entrada: EntradaRespuestaReclamo) => Observable<SalidaRespuestaReclamo> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (reclamoId, entrada) =>
    http.post<SalidaRespuestaReclamo>(`${gateway}/reclamos/${reclamoId}/respuesta`, entrada, { context: contextoIdempotente() })
}

/** Un reclamo cerrado o respondido ya no se responde desde la bandeja. */
export const sePuedeResponder = (r: ReclamoEnBandeja): boolean =>
  r.estado !== ReclamoEnBandejaEstadoEnum.Cerrado && r.estado !== ReclamoEnBandejaEstadoEnum.Respondido
