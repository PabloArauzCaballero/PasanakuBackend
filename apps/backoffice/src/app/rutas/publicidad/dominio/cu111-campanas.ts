import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { cargadorPublicidadPorCursor, type Dinero } from './contrato-publicidad'
import { lotesPorCursor } from './lotes-por-cursor'
import type { EntradaCampana } from 'clientes/angular/publicidad/model/entradaCampana'
import type { SalidaCampana } from 'clientes/angular/publicidad/model/salidaCampana'
import type { EntradaRechazo } from 'clientes/angular/publicidad/model/entradaRechazo'

/**
 * CU-111 · Crear y aprobar una campaña publicitaria.
 *
 * **Segregación de funciones (gate del carril)**: quien gestiona (crea, en
 * `PUBLICIDAD_ANUNCIANTES`) no es quien aprueba (`PUBLICIDAD_APROBAR_CAMPANA`,
 * `operacion.md` §gate — «el `canMatch` monta solo el lado que el rol permite»). Los
 * dos verbos viven en rutas separadas (`campanas/gestion.routes` de la pantalla de
 * gestión y `campanas/:campanaId/aprobacion` de la de aprobación), cada una con su
 * propio `canMatch`; este archivo no decide quién puede qué, solo transporta.
 *
 * Listado: `GET /publicidad/campanas` (`listarCampanas`, cursor (fechaInicio, id)). **Exige
 * `PUBLICIDAD_APROBAR_CAMPANA`: es la cola de Operaciones, no la vista del anunciante**, y sin `estados` devuelve solo
 * `EN_REVISION`. La pantalla de gestión no lo pide sin ese permiso (ver ambigüedad en el reporte de L3).
 *
 * La descripción de `crearCampana` en el YAML dice «Exige PUBLICIDAD_APROBAR_CAMPANA», lo que contradice su propio
 * resumen («el anunciante crea»). Se trata como errata del backend: acá `crearCampana` vive en la ruta de gestión
 * (`PUBLICIDAD_ANUNCIANTES`) y `aprobarCampana`/`rechazarCampana` en la de aprobación (`PUBLICIDAD_APROBAR_CAMPANA`).
 */
export type EstadoCampana = 'BORRADOR' | 'EN_REVISION' | 'ACTIVA' | 'PAUSADA' | 'RECHAZADA' | 'FINALIZADA'

/** `CampanaEnLista` de `publicidad.yaml`. */
export type Campana = {
  campanaId: string
  cuentaPublicitariaId: string
  anuncianteId: string
  nombre: string
  objetivo: 'CONVERSION' | 'DESCARGA_APP' | 'POSTULACION_GRUPO' | 'TRAFICO' | 'VISIBILIDAD_MARCA'
  presupuestoTotal: Dinero
  presupuestoConsumido: Dinero
  fechaInicio: string
  fechaFin?: string | null
  estado: EstadoCampana
  aprobadaPor?: string | null
}

export function cargarCampanas(http: HttpClient, gateway: string): CargadorDePagina<Campana> {
  return cargadorPublicidadPorCursor<Campana>(http, `${gateway}/publicidad/campanas`, (filtros, params) =>
    filtros['estado'] ? params.set('estados', filtros['estado']) : params,
  )
}

export function cargadorDeCampanas(): CargadorDePagina<Campana> {
  return cargarCampanas(inject(HttpClient), inject(GATEWAY))
}

export const ESTADOS_DE_CAMPANA: readonly EstadoCampana[] = ['BORRADOR', 'EN_REVISION', 'ACTIVA', 'PAUSADA', 'RECHAZADA', 'FINALIZADA']

/**
 * El listado por lotes de 100. Sin `estados` el contrato devuelve solo EN_REVISION: por eso «Todos» manda los seis
 * explícitos (es lo que el selector promete), y un estado elegido viaja solo.
 */
export function lotesDeCampanas() {
  return lotesPorCursor<Campana>('/publicidad/campanas', (filtros, params) => params.set('estados', filtros['estado'] || ESTADOS_DE_CAMPANA.join(',')))
}

export async function crearCampana(http: HttpClient, gateway: string, entrada: EntradaCampana): Promise<SalidaCampana> {
  return firstValueFrom(
    http.post<SalidaCampana>(`${gateway}/publicidad/campanas`, entrada, { context: contextoIdempotente() }),
  )
}

export async function aprobarCampana(http: HttpClient, gateway: string, campanaId: string): Promise<SalidaCampana> {
  return firstValueFrom(
    http.post<SalidaCampana>(`${gateway}/publicidad/campanas/${campanaId}/aprobacion`, {}, { context: contextoIdempotente() }),
  )
}

export async function rechazarCampana(http: HttpClient, gateway: string, campanaId: string, entrada: EntradaRechazo): Promise<SalidaCampana> {
  return firstValueFrom(
    http.post<SalidaCampana>(`${gateway}/publicidad/campanas/${campanaId}/rechazo`, entrada, {
      context: contextoIdempotente(),
    }),
  )
}

/** `HttpClient` solo se inyecta en `nucleo/` o `dominio/`; las pantallas de gestión y aprobación llaman a esto en contexto de inyección. */
export function accionesDeCampana() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return {
    crear: (entrada: EntradaCampana) => crearCampana(http, gateway, entrada),
    aprobar: (campanaId: string) => aprobarCampana(http, gateway, campanaId),
    rechazar: (campanaId: string, entrada: EntradaRechazo) => rechazarCampana(http, gateway, campanaId, entrada),
  }
}
