import { HttpClient, httpResource } from '@angular/common/http'
import { inject, type Signal } from '@angular/core'
import type { Observable } from 'rxjs'
import type {
  EntradaActualizarUsuarioInterno,
  EntradaAltaUsuarioInterno,
  EntradaAsignarRolesUsuarioInterno,
  EntradaDesbloqueoUsuarioInterno,
  PermisoCatalogo,
  RolCatalogo,
  UsuarioInternoDetalle,
  UsuarioInternoResumen,
} from 'clientes/angular/identidad'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'

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

export function crearUsuarioInterno(
  http: HttpClient,
  gateway: string,
  entrada: EntradaAltaUsuarioInterno,
): Observable<UsuarioInternoDetalle> {
  return http.post<UsuarioInternoDetalle>(`${gateway}/usuarios-internos`, entrada, {
    context: contextoIdempotente(),
  })
}

export function actualizarUsuarioInterno(
  http: HttpClient,
  gateway: string,
  id: string,
  entrada: EntradaActualizarUsuarioInterno,
): Observable<UsuarioInternoDetalle> {
  return http.patch<UsuarioInternoDetalle>(`${gateway}/usuarios-internos/${id}`, entrada)
}

export function asignarRolesUsuarioInterno(
  http: HttpClient,
  gateway: string,
  id: string,
  entrada: EntradaAsignarRolesUsuarioInterno,
): Observable<UsuarioInternoDetalle> {
  return http.put<UsuarioInternoDetalle>(`${gateway}/usuarios-internos/${id}/roles`, entrada)
}

export function desbloquearUsuarioInterno(
  http: HttpClient,
  gateway: string,
  id: string,
  motivo: string,
): Observable<UsuarioInternoDetalle> {
  const payload: EntradaDesbloqueoUsuarioInterno = { motivo }
  return http.post<UsuarioInternoDetalle>(`${gateway}/usuarios-internos/${id}/desbloquear`, payload)
}

export function creadorUsuarioInterno() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (entrada: EntradaAltaUsuarioInterno) => crearUsuarioInterno(http, gateway, entrada)
}

export function actualizadorUsuarioInterno() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (id: string, entrada: EntradaActualizarUsuarioInterno) =>
    actualizarUsuarioInterno(http, gateway, id, entrada)
}

export function asignadorRolesUsuarioInterno() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (id: string, entrada: EntradaAsignarRolesUsuarioInterno) =>
    asignarRolesUsuarioInterno(http, gateway, id, entrada)
}

export function desbloqueadorUsuarioInterno() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (id: string, motivo: string) =>
    desbloquearUsuarioInterno(http, gateway, id, motivo)
}
