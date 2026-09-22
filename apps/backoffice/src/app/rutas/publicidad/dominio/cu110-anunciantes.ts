import { HttpClient, HttpContext, httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { CLAVE_IDEMPOTENCIA, claveDeIdempotencia } from '../../../nucleo/idempotencia.interceptor'
import type { CargadorDePagina, PaginaServidor } from '../../../nucleo/tabla/tipos'
import type { EntradaAnunciante } from 'clientes/angular/publicidad/model/entradaAnunciante'
import type { SalidaAnunciante } from 'clientes/angular/publicidad/model/salidaAnunciante'
import type { EntradaSocio } from 'clientes/angular/publicidad/model/entradaSocio'
import type { SalidaSocio } from 'clientes/angular/publicidad/model/salidaSocio'

/**
 * CU-110 · Dar de alta un anunciante y su cuenta publicitaria.
 *
 * **Supuesto declarado**: `servicios/publicidad/openapi/publicidad.yaml` publica
 * `POST /publicidad/anunciantes` pero no un `GET` de listado — mismo hueco que ya
 * declaró `B1` para D-15 (`d15-solicitudes-escaladas.ts`). Se asume, con el mismo
 * criterio, `GET /publicidad/anunciantes` como extensión natural del recurso que el
 * POST ya define. Cuando el contrato real lo publique, solo cambia este archivo.
 */
export type Anunciante = SalidaAnunciante & {
  tipo: EntradaAnunciante['tipo']
  razonSocialFacturacion: string
  moneda: EntradaAnunciante['moneda']
  limiteGastoMensual: string | null
  estado: 'ACTIVA' | 'SUSPENDIDA'
}

export function cargarAnunciantes(http: HttpClient, gateway: string): CargadorDePagina<Anunciante> {
  return async (pedido) => {
    const todos = await firstValueFrom(http.get<Anunciante[]>(`${gateway}/publicidad/anunciantes`))
    const ordenados = pedido.orden
      ? [...todos].sort((a, b) => {
          const clave = pedido.orden!.clave as keyof Anunciante
          const [va, vb] = [String(a[clave] ?? ''), String(b[clave] ?? '')]
          return pedido.orden!.sentido === 'asc' ? va.localeCompare(vb) : vb.localeCompare(va)
        })
      : todos
    const inicio = (pedido.pagina - 1) * pedido.tamano
    const pagina: PaginaServidor<Anunciante> = { filas: ordenados.slice(inicio, inicio + pedido.tamano), total: ordenados.length }
    return pagina
  }
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
    http.post<SalidaSocio>(`${gateway}/publicidad/socios-comerciales`, entrada, { context: new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia()) }),
  )
}

export async function verificarSocio(http: HttpClient, gateway: string, socioId: string): Promise<SalidaSocio> {
  return firstValueFrom(
    http.post<SalidaSocio>(`${gateway}/publicidad/socios-comerciales/${socioId}/verificacion`, {}, { context: new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia()) }),
  )
}

export async function darDeAltaAnunciante(http: HttpClient, gateway: string, entrada: EntradaAnunciante): Promise<SalidaAnunciante> {
  return firstValueFrom(
    http.post<SalidaAnunciante>(`${gateway}/publicidad/anunciantes`, entrada, { context: new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia()) }),
  )
}

/** `HttpClient` solo se inyecta en `nucleo/` o `dominio/` (gate «sin red en vista»); la pantalla llama a esto en contexto de inyección. */
export function accionesDeAnunciante() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return { darDeAlta: (entrada: EntradaAnunciante) => darDeAltaAnunciante(http, gateway, entrada) }
}
