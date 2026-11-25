import { HttpClient, httpResource } from '@angular/common/http'
import { inject, type Signal } from '@angular/core'
import type { Observable } from 'rxjs'
import type {
  EntradaActualizarUsuarioInterno,
  EntradaAltaUsuarioInterno,
  EntradaDesbloqueoUsuarioInterno,
  PermisoCatalogo,
  RolCatalogo,
  UsuarioInternoDetalle,
  UsuarioInternoResumen,
} from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import type { ConEvidencia } from './gobierno-de-accesos'

export function usuariosInternos(filtros: {
  estado: Signal<string>
  rol: Signal<string>
  busqueda: Signal<string>
}) {
  const gateway = inject(GATEWAY)
  return httpResource<UsuarioInternoResumen[]>(() => {
    const params = new URLSearchParams()
    if (filtros.estado() && filtros.estado() !== 'TODOS') params.set('estado', filtros.estado())
    if (filtros.rol()) params.set('rol', filtros.rol())
    if (filtros.busqueda()) params.set('busqueda', filtros.busqueda())
    const q = params.toString()
    const base = `${gateway}/usuarios-internos`
    return q ? `${base}?${q}` : base
  })
}

export function usuarioInterno(id: Signal<string | null>) {
  const gateway = inject(GATEWAY)
  return httpResource<UsuarioInternoDetalle>(() => {
    const idVal = id()
    return idVal ? `${gateway}/usuarios-internos/${idVal}` : undefined
  })
}

export function catalogoRoles() {
  const gateway = inject(GATEWAY)
  return httpResource<RolCatalogo[]>(() => `${gateway}/roles`)
}

export function catalogoPermisos() {
  const gateway = inject(GATEWAY)
  return httpResource<PermisoCatalogo[]>(() => `${gateway}/permisos`)
}

/** El alta de B (GU-11) suma el motivo y la evidencia del segundo factor al cuerpo que ya existía. */
export type EntradaDeAlta = EntradaAltaUsuarioInterno & { motivo?: string } & ConEvidencia
export type EntradaDeActualizacion = EntradaActualizarUsuarioInterno & ConEvidencia

export function crearUsuarioInterno(
  http: HttpClient,
  gateway: string,
  entrada: EntradaDeAlta,
): Observable<UsuarioInternoDetalle> {
  return http.post<UsuarioInternoDetalle>(`${gateway}/usuarios-internos`, entrada, {
    context: contextoIdempotente(),
  })
}

export function actualizarUsuarioInterno(
  http: HttpClient,
  gateway: string,
  id: string,
  entrada: EntradaDeActualizacion,
): Observable<UsuarioInternoDetalle> {
  return http.patch<UsuarioInternoDetalle>(`${gateway}/usuarios-internos/${id}`, entrada)
}

export function desbloquearUsuarioInterno(
  http: HttpClient,
  gateway: string,
  id: string,
  motivo: string,
  evidenciaMfa?: string,
): Observable<UsuarioInternoDetalle> {
  const payload: EntradaDesbloqueoUsuarioInterno & ConEvidencia = evidenciaMfa ? { motivo, evidenciaMfa } : { motivo }
  return http.post<UsuarioInternoDetalle>(`${gateway}/usuarios-internos/${id}/desbloquear`, payload)
}

export function creadorUsuarioInterno() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (entrada: EntradaDeAlta) => crearUsuarioInterno(http, gateway, entrada)
}

export function actualizadorUsuarioInterno() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (id: string, entrada: EntradaDeActualizacion) =>
    actualizarUsuarioInterno(http, gateway, id, entrada)
}

export function desbloqueadorUsuarioInterno() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (id: string, motivo: string, evidenciaMfa?: string) =>
    desbloquearUsuarioInterno(http, gateway, id, motivo, evidenciaMfa)
}

/**
 * Los departamentos que ofrece el alta (los mismos que tenía el formulario). El contrato no publica un catálogo de
 * departamentos (`departamento` es texto libre de 2–60 caracteres en `EntradaAltaUsuarioInterno`): si el backend
 * agrega uno, esto se reemplaza por su `GET`.
 */
export const DEPARTAMENTOS_INTERNOS = ['OPERACIONES', 'RIESGOS', 'CUMPLIMIENTO', 'TECNOLOGIA', 'COBRANZAS', 'GOBERNANZA_DATOS', 'AUDITORIA', 'EJECUTIVO'] as const
