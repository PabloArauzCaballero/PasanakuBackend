import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { ActaEnLista, EventoDeRiesgoEnLista, IncidenteEnLista } from 'clientes/angular/cumplimiento'
import { GATEWAY } from '../../../nucleo/gateway'
import { cargadorPorCursor } from '../../../nucleo/tabla/cursor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { FORMA_H9, pedirPagina } from './paginas'

/**
 * Colas de solo lectura del carril A · H9 (`cumplimiento.yaml`):
 *
 * - CU-54 `listarEventosDeRiesgo` · `GET /cumplimiento/riesgos/eventos` (sin descripción ni causa raíz).
 * - CU-55 `listarIncidentesDeSeguridad` · `GET /cumplimiento/seguridad/incidentes` (sin vector ni lección).
 * - CU-94 `listarActasDeComite` · `GET /cumplimiento/comites/actas` (sin asistentes, temas ni decisiones).
 *
 * Sesionar el comité (CU-94) NO se ofrece: el permiso `GOBIERNO_SESIONAR` no está sembrado
 * (carril A, H9.S1.M4 `BLOQUEADO · DECISION_REQUIRED`) y no hay operación HTTP para hacerlo.
 */
export function cargadorDeEventosDeRiesgo(): CargadorDePagina<EventoDeRiesgoEnLista> {
  const url = `${inject(GATEWAY)}/cumplimiento/riesgos/eventos`
  return cargadorPorCursor(pedirPagina<EventoDeRiesgoEnLista>(inject(HttpClient), url, FORMA_H9, ['estados']))
}

export function cargadorDeIncidentes(): CargadorDePagina<IncidenteEnLista> {
  const url = `${inject(GATEWAY)}/cumplimiento/seguridad/incidentes`
  return cargadorPorCursor(pedirPagina<IncidenteEnLista>(inject(HttpClient), url, FORMA_H9, ['estados']))
}

/** El contrato de actas no admite `estados`: no se manda ningún filtro. */
export function cargadorDeActas(): CargadorDePagina<ActaEnLista> {
  const url = `${inject(GATEWAY)}/cumplimiento/comites/actas`
  return cargadorPorCursor(pedirPagina<ActaEnLista>(inject(HttpClient), url, FORMA_H9, []))
}
