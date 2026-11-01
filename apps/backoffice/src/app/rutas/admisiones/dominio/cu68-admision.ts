import { HttpClient, HttpContext, httpResource } from '@angular/common/http'
import { inject, Signal } from '@angular/core'
import type { Observable } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { CLAVE_IDEMPOTENCIA } from '../../../nucleo/idempotencia.interceptor'

/**
 * CU-68 · admisión humana. El administrador del grupo PROPONE (`proponerAdmision`), el
 * backoffice RESUELVE (`resolverAdmision`) y el historial es inmutable (`historialAdmision`).
 * Ninguna propuesta activa la membresía: solo la resolución humana de backoffice, y aun así
 * la persona queda «pendiente de firma».
 *
 * Los tipos son los de `grupos.yaml` (DecisionIngreso, EntradaDecisionIngreso) del backend,
 * que `clientes/angular/grupos` todavía no trae: el cliente generado de este repositorio es
 * anterior a ese contrato. Cuando se regenere, estos tipos se reemplazan por los generados
 * (no se agregan campos acá: lo que no está en el contrato no existe).
 */
export type FaseDeAdmision = 'PROPUESTA' | 'RESOLUCION'
export type ResultadoDeAdmision = 'ACEPTAR' | 'RECHAZAR'

export type DecisionDeIngreso = {
  id: string
  solicitudId: string
  fase: FaseDeAdmision | string
  decision: ResultadoDeAdmision | string
  actorId: string
  motivo: string
  propuestaId?: string
  participanteId?: string
  revision: number
  evidenciaAlgoritmo: string
  ocurridaEn: string
  correlacionId: string
}

export type EntradaDecisionDeIngreso = {
  decision: ResultadoDeAdmision
  motivo: string
  revisionEsperada: number
  propuestaId?: string
}

/**
 * `SolicitudDeIngreso` de `grupos.yaml` (`listarSolicitudesDeIngreso`, `GET /grupos/{grupoId}/solicitudes`): las
 * PENDIENTES de un grupo, la más antigua primero, como mucho 200. Trae el primer nombre e inicial de quien pide, no su
 * documento ni su contacto. El contrato la describe como «la cola del organizador»: si el servidor no se la abre a
 * backoffice responde 403, y la pantalla lo dice (ambigüedad registrada en el informe de la franja F).
 */
export type SolicitudDeIngreso = {
  solicitudId: string
  usuarioId: string
  cuposSolicitados: number
  mensaje?: string | null
  puntaje?: string | null
  nombreVisible?: string | null
  porEnlace?: boolean
  estado: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'EXPIRADA' | string
  fechaSolicitud: string
}

/** Las solicitudes pendientes del grupo pedido; sin grupo, no pide nada. */
export function solicitudesDelGrupo(grupoId: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<SolicitudDeIngreso[]>(() => (grupoId() ? `${gateway}/grupos/${grupoId()}/solicitudes` : undefined))
}

/** El motivo que acepta el contrato (`maxLength: 1000`). */
export const MOTIVO_MAXIMO = 1000

/** `GET /grupos/solicitudes/{id}/decisiones`: historial inmutable, en orden de revisión. */
export function historialDe(solicitudId: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<DecisionDeIngreso[]>(() => `${gateway}/grupos/solicitudes/${solicitudId()}/decisiones`)
}

/** La misma consulta, de una sola vez: es lo que se usa para saber si una resolución quedó registrada. */
export function crearConsultarHistorial(): (solicitudId: string) => Observable<DecisionDeIngreso[]> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (solicitudId) => http.get<DecisionDeIngreso[]>(`${gateway}/grupos/solicitudes/${solicitudId}/decisiones`)
}

/**
 * `POST /grupos/solicitudes/{id}/resoluciones`. La clave de idempotencia la maneja QUIEN LLAMA
 * (la pantalla): se reenvía igual en un reintento del mismo contenido y cambia si cambia el
 * contenido. Regenerarla acá duplicaría la decisión cuando la respuesta se perdió.
 */
export function crearResolver(): (solicitudId: string, entrada: EntradaDecisionDeIngreso, clave: string) => Observable<DecisionDeIngreso> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (solicitudId, entrada, clave) =>
    http.post<DecisionDeIngreso>(`${gateway}/grupos/solicitudes/${solicitudId}/resoluciones`, entrada, {
      context: new HttpContext().set(CLAVE_IDEMPOTENCIA, clave),
    })
}

/** ¿Es la propuesta vigente? La última decisión del historial, si es una propuesta. */
export function propuestaVigente(historial: readonly DecisionDeIngreso[]): DecisionDeIngreso | null {
  const ultima = historial.at(-1)
  return ultima?.fase === 'PROPUESTA' ? ultima : null
}

/** La resolución humana, si ya ocurrió: cierra la solicitud. */
export function resolucionDe(historial: readonly DecisionDeIngreso[]): DecisionDeIngreso | null {
  return historial.find((d) => d.fase === 'RESOLUCION') ?? null
}

/** Desde la revisión del último registro se calcula `revisionEsperada`: 0 si no hay historial. */
export const revisionActual = (historial: readonly DecisionDeIngreso[]): number => historial.at(-1)?.revision ?? 0

/**
 * ¿La resolución que quedó registrada es la que se intentó? Se compara por la propuesta que
 * resuelve y por el resultado: es lo que distingue «mi envío sí llegó» de «otra persona
 * resolvió mientras mi respuesta se perdía».
 */
export function esMiResolucion(historial: readonly DecisionDeIngreso[], propuestaId: string, decision: ResultadoDeAdmision, motivo: string): boolean {
  const r = resolucionDe(historial)
  return r !== null && r.propuestaId === propuestaId && r.decision === decision && r.motivo === motivo
}

/** Resumen de una solicitud de ingreso para la bandeja del backoffice. */
export type SolicitudDeAdmision = {
  solicitudId: string
  usuarioId: string
  postulante: string
  grupoId: string
  grupoCodigo: string
  cuposSolicitados: number
  mensaje?: string
  puntaje?: string | number
  porEnlace?: boolean
  estado: 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA' | 'CANCELADA' | 'EXPIRADA' | string
  fechaSolicitud: string
}

/**
 * `GET /grupos/solicitudes`: Bandeja general de solicitudes de ingreso del backoffice.
 */
export function solicitudesDeAdmision() {
  const gateway = inject(GATEWAY)
  return httpResource<SolicitudDeAdmision[]>(() => `${gateway}/grupos/solicitudes`)
}

/** Lo que acepta `POST /grupos/solicitudes/{id}/decision` (`EntradaDecisionDeIngreso`: motivo de 5 a 300). */
export type EntradaDecisionDirecta = { decision: ResultadoDeAdmision; motivo: string }
export type SalidaDecisionDirecta = { solicitudId: string; estado: 'APROBADA' | 'RECHAZADA' | string; participanteId?: string | null; cupoId?: string | null }
export const MOTIVO_DIRECTO_MINIMO = 5
export const MOTIVO_DIRECTO_MAXIMO = 300

/**
 * `POST /grupos/solicitudes/{id}/decision` (`decidirSolicitudDeIngreso`): la puerta directa, la misma del
 * organizador. Backoffice la usa cuando no hay propuesta que resolver. Con la admisión humana obligatoria el
 * servidor la cierra con 422 `AP-CU68-15`. Repetir la MISMA decisión devuelve lo ya resuelto: reintentar con la
 * misma clave no duplica nada.
 */
export function crearDecidirDirecto(): (solicitudId: string, entrada: EntradaDecisionDirecta, clave: string) => Observable<SalidaDecisionDirecta> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (solicitudId, entrada, clave) =>
    http.post<SalidaDecisionDirecta>(`${gateway}/grupos/solicitudes/${solicitudId}/decision`, entrada, {
      context: new HttpContext().set(CLAVE_IDEMPOTENCIA, clave),
    })
}

/** El estado de una solicitud en la bandeja (`ACEPTADA`, `RECHAZADA`, `PENDIENTE`…), o null si no figura. */
export const estadoEnBandeja = (bandeja: readonly SolicitudDeAdmision[], solicitudId: string): string | null =>
  bandeja.find((s) => s.solicitudId === solicitudId)?.estado ?? null
