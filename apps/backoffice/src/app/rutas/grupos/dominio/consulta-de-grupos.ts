import { httpResource } from '@angular/common/http'
import { inject, Signal } from '@angular/core'
import type { CupoDeBackoffice, DetalleDeGrupoDeBackoffice, GrupoDeBackoffice } from 'clientes/angular/grupos/model/models'
import { GATEWAY } from '../../../nucleo/gateway'

/**
 * Los grupos vistos desde el backoffice (2026-10-29). Contrato real (`grupos.yaml`): `GET /grupos/backoffice` y
 * `GET /grupos/backoffice/{grupoId}`, solo lectura, con `ADMIN_PLATAFORMA` o `AUDITORIA_LEER`. Datos del grupo y,
 * por cupo, alias y estado: nada de contacto ni importes de una persona.
 */
export type { CupoDeBackoffice, DetalleDeGrupoDeBackoffice, GrupoDeBackoffice }

/** `GET /grupos/backoffice`: todos los grupos (hasta 200), filtrables por estado. */
export function gruposDeBackoffice(estado: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<GrupoDeBackoffice[]>(() => {
    const params: Record<string, string> = {}
    const e = estado()
    if (e) params['estado'] = e
    return { url: `${gateway}/grupos/backoffice`, params }
  })
}

/** `GET /grupos/backoffice/{grupoId}`: el grupo y sus cupos. */
export function grupoDeBackoffice(grupoId: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<DetalleDeGrupoDeBackoffice>(() => `${gateway}/grupos/backoffice/${grupoId()}`)
}

/**
 * Los estados de un grupo, tal como los fija `ck_grupo_estado` (`sql/10_tablas/02_grupos_turnos/grupo.sql:37`). El
 * contrato los deja como texto libre: si la base suma uno, el filtro no lo ofrece pero el listado lo muestra igual.
 */
export const ESTADOS_DE_GRUPO = [
  'BORRADOR',
  'ABIERTO_A_INSCRIPCION',
  'CONFORMADO',
  'ACTIVO',
  'EN_CURSO',
  'SUSPENDIDO',
  'FINALIZADO',
  'DISUELTO_ANTICIPADAMENTE',
  'CANCELADO',
] as const
