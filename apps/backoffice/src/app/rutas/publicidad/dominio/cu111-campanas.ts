import { HttpClient, HttpContext } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { CLAVE_IDEMPOTENCIA, claveDeIdempotencia } from '../../../nucleo/idempotencia.interceptor'
import type { CargadorDePagina, PaginaServidor } from '../../../nucleo/tabla/tipos'
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
 * **Supuesto declarado**: igual que en `cu110-anunciantes.ts`, no hay `GET` de listado
 * en el contrato — se asume `GET /publicidad/campanas` como extensión del recurso.
 * La descripción de `crearCampana` en el YAML dice «Exige PUBLICIDAD_APROBAR_CAMPANA»,
 * lo que contradice su propio resumen («el anunciante crea») y a `aprobarCampana`/
 * `rechazarCampana`, que sí dicen «Operaciones aprueba/rechaza» bajo el mismo permiso.
 * Se trata como una errata de copia del backend: acá `crearCampana` exige
 * `PUBLICIDAD_ANUNCIANTES` (gestión) y `aprobarCampana`/`rechazarCampana` exigen
 * `PUBLICIDAD_APROBAR_CAMPANA` (aprobación) — es la única lectura consistente con la
 * segregación de funciones que pide este mismo carril. Se deja anotado para que
 * Backend lo confirme o corrija el contrato.
 */
export type Campana = SalidaCampana & { nombre: string; moneda: string; fechaInicio: string }

export function cargarCampanas(http: HttpClient, gateway: string): CargadorDePagina<Campana> {
  return async (pedido) => {
    const todas = await firstValueFrom(http.get<Campana[]>(`${gateway}/publicidad/campanas`))
    const filtradas = pedido.filtros['estado'] ? todas.filter((c) => c.estado === pedido.filtros['estado']) : todas
    const ordenadas = pedido.orden
      ? [...filtradas].sort((a, b) => {
          const clave = pedido.orden!.clave as keyof Campana
          const [va, vb] = [String(a[clave] ?? ''), String(b[clave] ?? '')]
          return pedido.orden!.sentido === 'asc' ? va.localeCompare(vb) : vb.localeCompare(va)
        })
      : filtradas
    const inicio = (pedido.pagina - 1) * pedido.tamano
    const pagina: PaginaServidor<Campana> = { filas: ordenadas.slice(inicio, inicio + pedido.tamano), total: ordenadas.length }
    return pagina
  }
}

export function cargadorDeCampanas(): CargadorDePagina<Campana> {
  return cargarCampanas(inject(HttpClient), inject(GATEWAY))
}

export async function crearCampana(http: HttpClient, gateway: string, entrada: EntradaCampana): Promise<SalidaCampana> {
  return firstValueFrom(
    http.post<SalidaCampana>(`${gateway}/publicidad/campanas`, entrada, { context: new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia()) }),
  )
}

export async function aprobarCampana(http: HttpClient, gateway: string, campanaId: string): Promise<SalidaCampana> {
  return firstValueFrom(
    http.post<SalidaCampana>(`${gateway}/publicidad/campanas/${campanaId}/aprobacion`, {}, { context: new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia()) }),
  )
}

export async function rechazarCampana(http: HttpClient, gateway: string, campanaId: string, entrada: EntradaRechazo): Promise<SalidaCampana> {
  return firstValueFrom(
    http.post<SalidaCampana>(`${gateway}/publicidad/campanas/${campanaId}/rechazo`, entrada, {
      context: new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia()),
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
