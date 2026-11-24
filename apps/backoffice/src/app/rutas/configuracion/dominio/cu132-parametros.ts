import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type { EntradaNombresDeUsuariosInternos } from 'clientes/angular/identidad/model/entradaNombresDeUsuariosInternos'
import type { NombreDeUsuarioInterno } from 'clientes/angular/identidad/model/nombreDeUsuarioInterno'
import type { EntradaVersionParametro } from 'clientes/angular/tarifas/model/entradaVersionParametro'
import type { ParametroFinanciero } from 'clientes/angular/tarifas/model/parametroFinanciero'
import type { VersionDeParametro } from 'clientes/angular/tarifas/model/versionDeParametro'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'

export type EntradaVersionDeParametro = EntradaVersionParametro
export type { NombreDeUsuarioInterno, ParametroFinanciero, VersionDeParametro }

/** El contrato de identidad acepta hasta 50 ids por lote (`maxItems`). */
export const MAXIMO_DE_OPERADORES_POR_LOTE = 50

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

/**
 * Identidad devuelve el nombre de los operadores internos ACTIVOS de un lote y omite el resto: un id que no vuelve
 * no es un error, es «no disponible». Es una lectura: no lleva clave de idempotencia aunque vaya por POST (el POST
 * existe para que los ids no viajen en la URL).
 */
export const resolverNombresDeOperadores = (http: HttpClient, gateway: string, ids: string[]): Observable<NombreDeUsuarioInterno[]> =>
  http.post<NombreDeUsuarioInterno[]>(`${gateway}/usuarios-internos/nombres`, { usuarioIds: ids } satisfies EntradaNombresDeUsuariosInternos)

export function crearResolverNombresDeOperadores(): (ids: string[]) => Observable<NombreDeUsuarioInterno[]> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (ids) => resolverNombresDeOperadores(http, gateway, ids)
}

/** Los ids de quien propuso y quien aprobó, sin repetir y en orden de aparición. */
export function operadoresDe(versiones: VersionDeParametro[]): string[] {
  const ids = new Set<string>()
  for (const v of versiones) {
    if (v.propuestoPor) ids.add(v.propuestoPor)
    if (v.aprobadoPor) ids.add(v.aprobadoPor)
  }
  return [...ids]
}

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
