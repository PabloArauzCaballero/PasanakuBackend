import { HttpClient, httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'

/**
 * Gobierno de roles personalizados (GU-16, CU-135) y de cuentas de servicio (GU-18, CU-136). Los tipos son los de
 * `identidad.yaml`; cada acto lleva la `evidenciaMfa` que entrega `ap-dialogo-de-acto` y su motivo. La credencial de
 * una cuenta de servicio solo existe en la respuesta de la rotación: este módulo no la guarda en ninguna parte.
 */
export type EstadoDeCambioDeRol = 'APLICADO' | 'PENDIENTE_DE_APROBACION' | 'RECHAZADA'
export type SalidaDeRol = { estado: EstadoDeCambioDeRol; rolId?: string | null; solicitudId?: string | null }
export type EntradaDeCreacionDeRol = { codigo: string; nombre: string; descripcion?: string | null; permisos: string[]; motivo: string; evidenciaMfa: string }
export type EntradaDeEdicionDeRol = { nombre: string; descripcion?: string | null; permisos: string[]; motivo: string; evidenciaMfa: string }
export type EntradaConMotivo = { motivo: string; evidenciaMfa: string }
export type SolicitudDeRol = {
  id: string
  tipo: 'CREAR' | 'EDITAR' | 'RETIRAR'
  rolId?: string | null
  codigo: string
  nombre: string
  descripcion?: string | null
  permisos: string[]
  motivo: string
  propuestaPor: string
  propuestaEn: string
}

export type CuentaDeServicio = {
  id: string
  usuarioId: string
  rolCodigo: string
  nombre: string
  estado: 'ACTIVA' | 'REVOCADA'
  venceEn: string
  creadaEn: string
  credencialRotadaEn: string
  revocadaEn?: string | null
}
export type EntradaDeAltaDeCuentaDeServicio = { usuarioId: string; nombre: string; venceEn: string; motivo: string; evidenciaMfa: string }
export type SalidaDeAltaDeCuentaDeServicio = { cuentaId: string; usuarioId: string; rolCodigo: string; venceEn: string }
export type EntradaDeRotacion = { venceEn?: string | null; motivo: string; evidenciaMfa: string }
export type SalidaDeRotacion = { cuentaId: string; credencial: string; venceEn: string; sesionesCerradas: number }
export type SalidaDeRevocacion = { cuentaId: string; asignacionesRevocadas: number; sesionesCerradas: number }

/** Los niveles del catálogo que obligan a cuatro ojos (`permiso.nivel_riesgo`; A1 del plan del lote F). */
export const RIESGO_DE_CUATRO_OJOS: readonly string[] = ['ALTO', 'CRITICO']

export function solicitudesDeRol() {
  const gateway = inject(GATEWAY)
  return httpResource<SolicitudDeRol[]>(() => `${gateway}/roles/solicitudes`)
}

export function cuentasDeServicio() {
  const gateway = inject(GATEWAY)
  return httpResource<CuentaDeServicio[]>(() => `${gateway}/cuentas-servicio`)
}

/** Los actos con servicio propio, listos para inyectar en un componente. */
export function accionesDeRoles() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  const post = <S>(ruta: string, cuerpo: object): Observable<S> => http.post<S>(`${gateway}${ruta}`, cuerpo, { context: contextoIdempotente() })
  return {
    crear: (entrada: EntradaDeCreacionDeRol) => post<SalidaDeRol>('/roles', entrada),
    editar: (rolId: string, entrada: EntradaDeEdicionDeRol) =>
      http.patch<SalidaDeRol>(`${gateway}/roles/${rolId}`, entrada, { context: contextoIdempotente() }),
    retirar: (rolId: string, entrada: EntradaConMotivo) => post<SalidaDeRol>(`/roles/${rolId}/retiro`, entrada),
    aprobar: (solicitudId: string, entrada: EntradaConMotivo) => post<SalidaDeRol>(`/roles/solicitudes/${solicitudId}/aprobacion`, entrada),
    rechazar: (solicitudId: string, entrada: EntradaConMotivo) => post<SalidaDeRol>(`/roles/solicitudes/${solicitudId}/rechazo`, entrada),
  }
}

export function accionesDeCuentasDeServicio() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  const post = <S>(ruta: string, cuerpo: object): Observable<S> => http.post<S>(`${gateway}${ruta}`, cuerpo, { context: contextoIdempotente() })
  return {
    registrar: (entrada: EntradaDeAltaDeCuentaDeServicio) => post<SalidaDeAltaDeCuentaDeServicio>('/cuentas-servicio', entrada),
    rotar: (cuentaId: string, entrada: EntradaDeRotacion) => post<SalidaDeRotacion>(`/cuentas-servicio/${cuentaId}/rotacion`, entrada),
    revocar: (cuentaId: string, entrada: EntradaConMotivo) => post<SalidaDeRevocacion>(`/cuentas-servicio/${cuentaId}/revocacion`, entrada),
  }
}
