import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type { EntradaVersionParametro } from 'clientes/angular/tarifas/model/entradaVersionParametro'
import type { ParametroFinanciero } from 'clientes/angular/tarifas/model/parametroFinanciero'
import type { VersionDeParametro } from 'clientes/angular/tarifas/model/versionDeParametro'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'

export type EntradaVersionDeParametro = EntradaVersionParametro
export type { ParametroFinanciero, VersionDeParametro }

const base = (gateway: string) => `${gateway}/tarifas/parametros`

export const cargarParametrosFinancieros = (http: HttpClient, gateway: string): Observable<ParametroFinanciero[]> =>
  http.get<ParametroFinanciero[]>(base(gateway))

export const cargarHistorialDeParametro = (http: HttpClient, gateway: string, codigo: string): Observable<VersionDeParametro[]> =>
  http.get<VersionDeParametro[]>(`${base(gateway)}/${encodeURIComponent(codigo)}/versiones`)

export const proponerVersionDeParametro = (
  http: HttpClient,
  gateway: string,
  codigo: string,
  cuerpo: EntradaVersionDeParametro,
): Observable<VersionDeParametro> =>
  http.post<VersionDeParametro>(`${base(gateway)}/${encodeURIComponent(codigo)}/versiones`, cuerpo as EntradaVersionParametro, { context: contextoIdempotente() })

export const aprobarVersionDeParametro = (http: HttpClient, gateway: string, id: string): Observable<VersionDeParametro> =>
  http.post<VersionDeParametro>(`${base(gateway)}/versiones/${encodeURIComponent(id)}/aprobacion`, null, { context: contextoIdempotente() })

export const cargadorDeParametrosFinancieros = () => cargarParametrosFinancieros(inject(HttpClient), inject(GATEWAY))

export function crearCargarHistorialDeParametro(): (codigo: string) => Observable<VersionDeParametro[]> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (codigo) => cargarHistorialDeParametro(http, gateway, codigo)
}

export function crearProponerVersionDeParametro(): (codigo: string, cuerpo: EntradaVersionDeParametro) => Observable<VersionDeParametro> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (codigo, cuerpo) => proponerVersionDeParametro(http, gateway, codigo, cuerpo)
}

export function crearAprobarVersionDeParametro(): (id: string) => Observable<VersionDeParametro> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (id) => aprobarVersionDeParametro(http, gateway, id)
}
