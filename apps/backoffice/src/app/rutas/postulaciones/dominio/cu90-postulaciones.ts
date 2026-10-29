import { HttpClient, httpResource } from '@angular/common/http'
import { inject, Signal } from '@angular/core'
import type { Observable } from 'rxjs'
import {
  EntradaResolucionDeHabilitacionDecisionEnum,
  EntradaRevisionDeHabilitacionDecisionEnum,
  ResumenDeHabilitacionAccionesEnum,
  type DecisionDeHabilitacion,
  type EntradaResolucionDeHabilitacion,
  type EntradaRevisionDeHabilitacion,
  type ExpedienteDeHabilitacion,
  type PaginaDeBandeja,
  type ResumenDeHabilitacion,
} from 'clientes/angular/organizador/model/models'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'

/**
 * CU-90 · Postulaciones a organizador, vistas por backoffice. Contrato real (`organizador.yaml`), todo con
 * `@Permiso("ADMIN_PLATAFORMA")`: la bandeja paginada por cursor, el expediente, la resolución (APROBAR/RECHAZAR) y la
 * revisión de una habilitación ya aprobada (CONFIRMAR/REVOCAR). Cada fila declara en `acciones` qué se puede hacer: la
 * pantalla ofrece solo eso, y el servidor vuelve a decidir igual.
 */
export type EstadoDePostulacion = 'PENDIENTE' | 'EN_REVISION' | 'APROBADA' | 'RECHAZADA'
export type { DecisionDeHabilitacion, ExpedienteDeHabilitacion, PaginaDeBandeja, ResumenDeHabilitacion }

/** El motivo que acepta el contrato (`maxLength: 1000`, `minLength: 1`). */
export const MOTIVO_MAXIMO = 1000
/** El tope de filas por página del contrato (`limite` ≤ 100). */
export const LIMITE_DE_BANDEJA = 50

/** El cursor de la bandeja: la última fila vista, o nada para la primera página. */
export type Cursor = { fecha: string; id: string } | null

/** `GET /organizadores/postulaciones/bandeja`, de la más antigua a la más nueva. */
export function bandejaDePostulaciones(estado: Signal<EstadoDePostulacion | ''>, cursor: Signal<Cursor>) {
  const gateway = inject(GATEWAY)
  return httpResource<PaginaDeBandeja>(() => {
    const params: Record<string, string> = { limite: String(LIMITE_DE_BANDEJA) }
    const e = estado()
    if (e) params['estados'] = e
    const c = cursor()
    if (c) {
      params['despuesDeFecha'] = c.fecha
      params['despuesDeId'] = c.id
    }
    return { url: `${gateway}/organizadores/postulaciones/bandeja`, params }
  })
}

/** `GET /organizadores/postulaciones/{id}/expediente`: la solicitud y su historial íntegro de decisiones. */
export function expedienteDe(solicitudId: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<ExpedienteDeHabilitacion>(() => `${gateway}/organizadores/postulaciones/${solicitudId()}/expediente`)
}

/** `POST …/resolucion`: aprobar o rechazar una postulación abierta. Idempotente por contenido. */
export function crearResolver(): (solicitudId: string, entrada: EntradaResolucionDeHabilitacion) => Observable<DecisionDeHabilitacion> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (solicitudId, entrada) =>
    http.post<DecisionDeHabilitacion>(`${gateway}/organizadores/postulaciones/${solicitudId}/resolucion`, entrada, { context: contextoIdempotente() })
}

/** `POST …/revision`: confirmar o revocar una habilitación ya aprobada. Revocar suspende al organizador. */
export function crearRevisar(): (solicitudId: string, entrada: EntradaRevisionDeHabilitacion) => Observable<DecisionDeHabilitacion> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (solicitudId, entrada) =>
    http.post<DecisionDeHabilitacion>(`${gateway}/organizadores/postulaciones/${solicitudId}/revision`, entrada, { context: contextoIdempotente() })
}

export type Accion = 'RESOLVER' | 'REVISAR'
/** Lo que se elige en el formulario: el resultado de la resolución o el de la revisión. */
export type Decision = 'APROBAR' | 'RECHAZAR' | 'CONFIRMAR' | 'REVOCAR'

/** ¿Qué formulario ofrecer? Lo que la fila declara; RESOLVER manda sobre REVISAR. */
export function accionDe(resumen: ResumenDeHabilitacion): Accion | null {
  const acciones = resumen.acciones ?? []
  if (acciones.includes(ResumenDeHabilitacionAccionesEnum.Resolver)) return 'RESOLVER'
  if (acciones.includes(ResumenDeHabilitacionAccionesEnum.Revisar)) return 'REVISAR'
  return null
}

/** Las decisiones que admite cada acción, en el orden en que se ofrecen. */
export const decisionesDe = (accion: Accion): Decision[] => (accion === 'RESOLVER' ? ['APROBAR', 'RECHAZAR'] : ['CONFIRMAR', 'REVOCAR'])

const RESOLUCION = { APROBAR: EntradaResolucionDeHabilitacionDecisionEnum.Aprobar, RECHAZAR: EntradaResolucionDeHabilitacionDecisionEnum.Rechazar } as const
const REVISION = { CONFIRMAR: EntradaRevisionDeHabilitacionDecisionEnum.Confirmar, REVOCAR: EntradaRevisionDeHabilitacionDecisionEnum.Revocar } as const

/** El cuerpo exacto del contrato para cada acción: tres campos, sin `medidos` (fuera de alcance). */
export function entradaDeResolucion(decision: 'APROBAR' | 'RECHAZAR', motivo: string, revisionEsperada: number): EntradaResolucionDeHabilitacion {
  return { decision: RESOLUCION[decision], motivo, revisionEsperada }
}
export function entradaDeRevision(decision: 'CONFIRMAR' | 'REVOCAR', motivo: string, revisionEsperada: number): EntradaRevisionDeHabilitacion {
  return { decision: REVISION[decision], motivo, revisionEsperada }
}
