import { HttpClient, httpResource } from '@angular/common/http'
import { inject, type Signal } from '@angular/core'
import type { Observable } from 'rxjs'
import type { RolCatalogo, UsuarioInternoDetalle, UsuarioInternoResumen } from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'

/**
 * Gobierno de accesos del personal interno (CU-08, GU-05/08/11/13/14) contra el contrato del PR #516 (rama
 * `claude/gu-b-cu08`, `identidad.yaml`). **Doble declarado (regla 65):** el cliente generado sale del `identidad.yaml` de
 * `dev`, que todavía no trae estas operaciones; por eso los tipos y las llamadas viven acá, con el nombre exacto de
 * cada esquema del contrato. Cuando #516 entre a `dev`, `EntradaAsignacionDeRol` & co. se importan de
 * `clientes/angular/identidad` y este archivo se achica a las llamadas. Lo verificado contra el doble está en el REPORTE.
 */

export type AmbitoDeRol = 'GLOBAL' | 'GRUPO' | 'ORGANIZACION'
export const AMBITOS_DE_ROL: readonly AmbitoDeRol[] = ['GLOBAL', 'GRUPO', 'ORGANIZACION']

/** `EntradaAsignacionDeRol` (POST `/usuarios-internos/{id}/asignaciones`). */
export type EntradaAsignacionDeRol = {
  rolId: string
  ambito: AmbitoDeRol
  ambitoId?: string | null
  vigenteHasta?: string | null
  justificacion: string
  evidenciaMfa: string
}
/** `SalidaAsignacionDeRol`. `yaExistia`: ya había una asignación viva igual (200, no escribió nada). `requiereMfa`: el destinatario aún no tiene TOTP y no podrá entrar. */
export type SalidaAsignacionDeRol = {
  asignacionId: string
  usuarioId: string
  rolCodigo: string
  ambito: AmbitoDeRol
  ambitoId?: string | null
  vigenteHasta?: string | null
  permisosEfectivos: string[]
  requiereMfa: boolean
  yaExistia: boolean
}
/** `EntradaRevocacionDeRol` (POST `/usuarios-internos/asignaciones/{asignacionId}/revocacion`). `cerrarSesiones` es `true` por omisión en el servidor. */
export type EntradaRevocacionDeRol = { motivoRevocacion: string; cerrarSesiones?: boolean; evidenciaMfa: string }
export type SalidaRevocacionDeRol = { asignacionId: string; usuarioId: string; revocadaEn: string; sesionesCerradas: number; yaEstabaRevocada: boolean }
/** `EntradaBajaUsuarioInterno` (POST `/usuarios-internos/{id}/baja`). */
export type EntradaBajaUsuarioInterno = { motivo: string; evidenciaMfa: string }
export type SalidaBajaUsuarioInterno = { usuarioId: string; estado: string; asignacionesRevocadas: number; sesionesCerradas: number; yaEstabaDeBaja: boolean }

export type SeccionDeAccesos = 'VIVOS' | 'POR_VENCER' | 'SIN_TOTP'
export const SECCIONES_DE_ACCESOS: readonly SeccionDeAccesos[] = ['VIVOS', 'POR_VENCER', 'SIN_TOTP']
/** `FilaDeAcceso`: el nombre del operador, nunca su correo ni su teléfono. */
export type FilaDeAcceso = {
  asignacionId: string
  usuarioId: string
  nombre: string
  estadoCuenta: string
  rolCodigo: string
  ambito: AmbitoDeRol
  ambitoId?: string | null
  otorgadaPor: string
  otorgadaEn: string
  vigenteHasta?: string | null
  totpConfirmado: boolean
}
export type PaginaDeAccesos = { seccion: SeccionDeAccesos; generadoEn: string; items: FilaDeAcceso[]; pagina: number; limite: number; total: number; totalPaginas: number }

/** Lo que B agregó a las operaciones que ya existían: el motivo del alta y la evidencia del segundo factor (GU-11). */
export type ConEvidencia = { evidenciaMfa?: string }
/** `UsuarioInternoResumen` de B: suma `creadoEn` (primera mitad del cursor) y `motivoBloqueo` (bloqueo indefinido = `bloqueadoHasta` nulo con motivo). */
export type UsuarioInternoDeLista = UsuarioInternoResumen & { creadoEn?: string; motivoBloqueo?: string | null }
export type UsuarioInternoFicha = UsuarioInternoDetalle & { creadoEn?: string; motivoBloqueo?: string | null }

/** Tamaño de página del listado por cursor (`limite` 1–100, 50 por omisión en el servidor). */
export const TAMANO_DE_PAGINA_DE_USUARIOS = 50
export type CursorDeUsuarios = { antesDe: string; antesDeId: string }

/** Una página del listado de usuarios internos por cursor (GU-17). Menos filas que `limite` = última página. */
export function paginaDeUsuariosInternos(http: HttpClient, gateway: string, cursor: CursorDeUsuarios | null, limite = TAMANO_DE_PAGINA_DE_USUARIOS): Observable<UsuarioInternoDeLista[]> {
  const params: Record<string, string> = { limite: String(limite) }
  if (cursor) {
    params['antesDe'] = cursor.antesDe
    params['antesDeId'] = cursor.antesDeId
  }
  return http.get<UsuarioInternoDeLista[]>(`${gateway}/usuarios-internos`, { params })
}

/** El cursor de la página siguiente: fecha de alta e id de la última fila recibida. Nulo si esa fila no trae `creadoEn`. */
export function cursorDespuesDe(filas: readonly UsuarioInternoDeLista[]): CursorDeUsuarios | null {
  const ultima = filas.at(-1)
  return ultima?.creadoEn ? { antesDe: ultima.creadoEn, antesDeId: ultima.id } : null
}

export function asignarRolDeOperador(http: HttpClient, gateway: string, usuarioId: string, entrada: EntradaAsignacionDeRol): Observable<SalidaAsignacionDeRol> {
  return http.post<SalidaAsignacionDeRol>(`${gateway}/usuarios-internos/${usuarioId}/asignaciones`, entrada, { context: contextoIdempotente() })
}

export function revocarAsignacionDeRol(http: HttpClient, gateway: string, asignacionId: string, entrada: EntradaRevocacionDeRol): Observable<SalidaRevocacionDeRol> {
  return http.post<SalidaRevocacionDeRol>(`${gateway}/usuarios-internos/asignaciones/${asignacionId}/revocacion`, entrada, { context: contextoIdempotente() })
}

export function darDeBajaUsuarioInterno(http: HttpClient, gateway: string, usuarioId: string, entrada: EntradaBajaUsuarioInterno): Observable<SalidaBajaUsuarioInterno> {
  return http.post<SalidaBajaUsuarioInterno>(`${gateway}/usuarios-internos/${usuarioId}/baja`, entrada, { context: contextoIdempotente() })
}

/** Los tres actos con servicio propio, listos para inyectar en un componente. */
export function accionesDeAccesos() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return {
    asignar: (usuarioId: string, entrada: EntradaAsignacionDeRol) => asignarRolDeOperador(http, gateway, usuarioId, entrada),
    revocar: (asignacionId: string, entrada: EntradaRevocacionDeRol) => revocarAsignacionDeRol(http, gateway, asignacionId, entrada),
    darDeBaja: (usuarioId: string, entrada: EntradaBajaUsuarioInterno) => darDeBajaUsuarioInterno(http, gateway, usuarioId, entrada),
    paginaDeUsuarios: (cursor: CursorDeUsuarios | null) => paginaDeUsuariosInternos(http, gateway, cursor),
  }
}

/** El informe de accesos (GU-08): una sección y una página por vez, orden determinista del servidor. */
export function informeDeAccesos(seccion: Signal<SeccionDeAccesos>, pagina: Signal<number>, limite = 25) {
  const gateway = inject(GATEWAY)
  return httpResource<PaginaDeAccesos>(() => `${gateway}/usuarios-internos/accesos/informe?seccion=${seccion()}&pagina=${pagina()}&limite=${limite}`)
}

/** Los roles que se le pueden otorgar a una persona interna: sin participante, organizador, anunciante, `SERVICIO_*` ni ámbito GRUPO (GU-13). */
export function rolesAsignables() {
  const gateway = inject(GATEWAY)
  return httpResource<RolCatalogo[]>(() => `${gateway}/roles?asignablesAPersonalInterno=true`)
}
