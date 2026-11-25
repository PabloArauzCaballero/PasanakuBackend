import { HttpClient, HttpContext } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type {
  EntradaCambioDeClave,
  EntradaCompletarInvitacion,
  EntradaDecisionDeRecuperacion,
  EntradaPedirRecuperacion,
  SalidaCambioDeClave,
  SalidaCompletarInvitacion,
  SalidaConsultaInvitacion,
  SalidaInicioEnrolamientoTotp,
  SalidaPedidoDeRecuperacion,
  SalidaRecuperaciones,
  SolicitudDeRecuperacion,
} from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { YA_REINTENTADA } from '../../../nucleo/sesion.interceptor'
import type { PedirPorCursor } from '../../../nucleo/tabla/cursor'
import { cursorDe, pedirJson } from '../../operacion/dominio/bandeja-por-cursor'

export type { SalidaConsultaInvitacion, SalidaInicioEnrolamientoTotp, SalidaCompletarInvitacion, SalidaCambioDeClave, SalidaPedidoDeRecuperacion, SolicitudDeRecuperacion }
export type AlcanceDeInvitacion = 'ALTA' | 'CLAVE' | 'FACTOR'
export type AlcanceDeRecuperacion = 'CLAVE' | 'FACTOR'
export type EstadoDeRecuperacion = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'EMITIDA' | 'COMPLETADA' | 'VENCIDA'

/**
 * GU-C · Cuenta del operador, contra `identidad.yaml` (tag `Cuenta`, `/cuenta/*`). Ninguna lleva `Idempotency-Key`
 * (el contrato no la pide): `aprobar` es idempotente por contenido en el servidor.
 *
 * Las cuatro operaciones públicas (`security: []`) van marcadas `YA_REINTENTADA`: un `401` ahí no es «tu sesión venció»
 * y no debe disparar un refresco con la cookie de una sesión anterior.
 */
const PUBLICA = () => ({ context: new HttpContext().set(YA_REINTENTADA, true) })

export type PuertoDeInvitacion = {
  consultar: (token: string) => Observable<SalidaConsultaInvitacion>
  iniciarFactor: (token: string) => Observable<SalidaInicioEnrolamientoTotp>
  completar: (entrada: EntradaCompletarInvitacion) => Observable<SalidaCompletarInvitacion>
}

/** El token viaja en el cuerpo, nunca en la URL (contrato). */
export function crearInvitacion(): PuertoDeInvitacion {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return {
    consultar: (token) => http.post<SalidaConsultaInvitacion>(`${gateway}/cuenta/invitacion/consultar`, { token }, PUBLICA()),
    iniciarFactor: (token) => http.post<SalidaInicioEnrolamientoTotp>(`${gateway}/cuenta/invitacion/factor`, { token }, PUBLICA()),
    completar: (entrada) => http.post<SalidaCompletarInvitacion>(`${gateway}/cuenta/invitacion/completar`, entrada, PUBLICA()),
  }
}

export function crearPedirRecuperacion(): (entrada: EntradaPedirRecuperacion) => Observable<SalidaPedidoDeRecuperacion> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (entrada) => http.post<SalidaPedidoDeRecuperacion>(`${gateway}/cuenta/recuperaciones`, entrada, PUBLICA())
}

/** Con sesión: el titular sale del token. */
export function crearCambiarClave(): (entrada: EntradaCambioDeClave) => Observable<SalidaCambioDeClave> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (entrada) => http.post<SalidaCambioDeClave>(`${gateway}/cuenta/clave`, entrada)
}

/** Bandeja (`SEGURIDAD_ACCESO_RESTABLECER`): cursor `(antesDe, antesDeId)`, de la más nueva a la más vieja. */
export function pedidorDeRecuperaciones(): PedirPorCursor<SolicitudDeRecuperacion, { a: string; b: string }> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return async ({ limite, cursor, filtros }) => {
    const p = await pedirJson<SalidaRecuperaciones>(http, `${gateway}/cuenta/recuperaciones`, {
      estado: filtros['estado'],
      limite,
      antesDe: cursor?.a,
      antesDeId: cursor?.b,
    })
    return { items: p.items, siguiente: cursorDe(p.items, p.siguienteAntesDe, p.siguienteAntesDeId) }
  }
}

export type Decision = 'aprobacion' | 'rechazo'

export function crearDecidirRecuperacion(): (id: string, decision: Decision, entrada: EntradaDecisionDeRecuperacion) => Observable<SolicitudDeRecuperacion> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (id, decision, entrada) => http.post<SolicitudDeRecuperacion>(`${gateway}/cuenta/recuperaciones/${encodeURIComponent(id)}/${decision}`, entrada)
}
