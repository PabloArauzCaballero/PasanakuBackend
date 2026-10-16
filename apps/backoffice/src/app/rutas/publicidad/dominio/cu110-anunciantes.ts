import { HttpClient, httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { cargadorPublicidadPorCursor, type Dinero } from './contrato-publicidad'
import type { EntradaAnunciante } from 'clientes/angular/publicidad/model/entradaAnunciante'
import type { SalidaAnunciante } from 'clientes/angular/publicidad/model/salidaAnunciante'
import type { EntradaSocio } from 'clientes/angular/publicidad/model/entradaSocio'
import type { SalidaSocio } from 'clientes/angular/publicidad/model/salidaSocio'

/**
 * CU-110 · Dar de alta un anunciante y su cuenta publicitaria.
 *
 * Contrato real: `GET /publicidad/anunciantes` (`listarAnunciantes`, cursor (creadoEn, id) descendente, exige
 * `PUBLICIDAD_ANUNCIANTES`) y `POST /publicidad/anunciantes` (`darDeAltaAnunciante`). El listado trae la razón social de
 * facturación y la cuenta publicitaria con su límite; no trae datos personales del organizador ni del socio.
 */
export type EstadoAnunciante = 'ACTIVO' | 'SUSPENDIDO' | 'DADO_DE_BAJA'
export type EstadoCuentaPublicitaria = 'ACTIVA' | 'SUSPENDIDA' | 'CERRADA'

/** `AnuncianteEnLista` de `publicidad.yaml`. El límite es `Dinero` con su moneda, o `null` si la cuenta no tiene techo. */
export type Anunciante = {
  anuncianteId: string
  tipo: 'ORGANIZADOR' | 'SOCIO_COMERCIAL'
  razonSocialFacturacion: string
  estado: EstadoAnunciante
  creadoEn: string
  cuentaPublicitariaId?: string | null
  estadoCuenta?: EstadoCuentaPublicitaria | null
  limiteGastoMensual?: Dinero | null
}

export function cargarAnunciantes(http: HttpClient, gateway: string): CargadorDePagina<Anunciante> {
  return cargadorPublicidadPorCursor<Anunciante>(http, `${gateway}/publicidad/anunciantes`, (filtros, params) =>
    filtros['estado'] ? params.set('estados', filtros['estado']) : params,
  )
}

export function cargadorDeAnunciantes(): CargadorDePagina<Anunciante> {
  return cargarAnunciantes(inject(HttpClient), inject(GATEWAY))
}

export function recursoDeSocio(socioId: () => string | undefined) {
  const gateway = inject(GATEWAY)
  return httpResource<SalidaSocio>(() => (socioId() ? `${gateway}/publicidad/socios-comerciales/${socioId()}` : undefined))
}

export async function postularSocio(http: HttpClient, gateway: string, entrada: EntradaSocio): Promise<SalidaSocio> {
  return firstValueFrom(
    http.post<SalidaSocio>(`${gateway}/publicidad/socios-comerciales`, entrada, { context: contextoIdempotente() }),
  )
}

export async function verificarSocio(http: HttpClient, gateway: string, socioId: string): Promise<SalidaSocio> {
  return firstValueFrom(
    http.post<SalidaSocio>(`${gateway}/publicidad/socios-comerciales/${socioId}/verificacion`, {}, { context: contextoIdempotente() }),
  )
}

export async function darDeAltaAnunciante(http: HttpClient, gateway: string, entrada: EntradaAnunciante): Promise<SalidaAnunciante> {
  return firstValueFrom(
    http.post<SalidaAnunciante>(`${gateway}/publicidad/anunciantes`, entrada, { context: contextoIdempotente() }),
  )
}

/** `HttpClient` solo se inyecta en `nucleo/` o `dominio/` (gate «sin red en vista»); la pantalla llama a esto en contexto de inyección. */
export function accionesDeAnunciante() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return { darDeAlta: (entrada: EntradaAnunciante) => darDeAltaAnunciante(http, gateway, entrada) }
}
