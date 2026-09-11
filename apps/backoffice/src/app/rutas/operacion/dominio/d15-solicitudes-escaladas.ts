import { httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import { GATEWAY } from '../../../nucleo/gateway'

/**
 * D-15 · «F7 suma la vista de solicitudes escaladas cuando el organizador deja vencer
 * el plazo» (`planes/20` §D-15). La solicitud nace en CU-68 (`solicitud_ingreso`,
 * `PENDIENTE`) y el organizador tiene 48 horas para resolverla.
 *
 * **Supuesto declarado**: ni `servicios/grupos/openapi/grupos.yaml` ni CU-68 publican
 * todavía el endpoint de escalamiento (`GET /grupos/solicitudes-escaladas`) — CU-68 solo
 * define `POST /grupos/:id/solicitudes` y la resolución del organizador. La forma de
 * abajo sale de los campos que CU-68 ya declara para `solicitud_ingreso`
 * (`solicitudId`, `puntajeCompatibilidad` con su desglose, `fechaLimite`) más el dato
 * que este escalamiento necesariamente agrega: desde cuándo está vencida y a quién
 * (el equipo de operación) le toca resolver en su lugar. Se pide a `grupos` (backend)
 * la operación; el nombre de ruta puede cambiar cuando exista el contrato real.
 */
export type FactorDePuntaje = { motivo: string; aFavor: boolean }

export type SolicitudEscalada = {
  solicitudId: string
  grupoId: string
  grupoCodigo: string
  usuarioId: string
  usuarioNombre: string
  canal: 'QR' | 'CODIGO'
  puntajeCompatibilidad: number
  factores: FactorDePuntaje[]
  fechaLimiteOrganizador: string
  escaladaEn: string
  estado: 'ESCALADA' | 'RESUELTA'
}

export function colaDeSolicitudesEscaladas() {
  const gateway = inject(GATEWAY)
  return httpResource<SolicitudEscalada[]>(() => `${gateway}/grupos/solicitudes-escaladas`)
}

export function ordenadasPorVencimiento(solicitudes: readonly SolicitudEscalada[]): SolicitudEscalada[] {
  return [...solicitudes].sort((a, b) => new Date(a.fechaLimiteOrganizador).getTime() - new Date(b.fechaLimiteOrganizador).getTime())
}
