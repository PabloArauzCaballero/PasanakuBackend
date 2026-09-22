import { HttpClient, HttpContext } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { CLAVE_IDEMPOTENCIA, claveDeIdempotencia } from '../../../nucleo/idempotencia.interceptor'
import type { CargadorDePagina, PaginaServidor } from '../../../nucleo/tabla/tipos'
import type { EntradaRevision } from 'clientes/angular/publicidad/model/entradaRevision'
import type { SalidaRevision } from 'clientes/angular/publicidad/model/salidaRevision'

/**
 * CU-112 · Moderar una pieza creativa. **Gate del carril: la moderación es previa a la
 * entrega.** Ninguna pantalla de este carril programa un anuncio (CU-113 no está en el
 * alcance de F14 más que el panel de desempeño de lectura — ver `docs/Flujo de
 * pantallas · backoffice administrador.md` §6), así que no existe, hoy, ni una ruta ni
 * un botón que lleve una pieza a un anuncio sin pasar por acá. `puedeUsarseEnUnAnuncio`
 * es la función que cualquier pantalla futura que programe anuncios tendría que
 * consultar (regla R-PUB-04): se prueba en el spec de este archivo y es la evidencia
 * del gate.
 *
 * **Supuesto declarado**: no hay `GET` de listado en el contrato — se asume
 * `GET /publicidad/piezas-creativas?estadoModeracion=PENDIENTE` como extensión natural
 * del recurso, mismo criterio que D-15 y CU-110/111.
 */
export type PiezaCreativa = {
  piezaCreativaId: string
  anuncianteId: string
  anuncianteNombre: string
  subidaPor: string
  titulo: string
  texto: string | null
  urlRecurso: string
  tipoRecurso: 'IMAGEN' | 'VIDEO'
  estadoModeracion: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA'
}

/** R-PUB-04: una pieza que no está APROBADA no entra a ningún anuncio. */
export function puedeUsarseEnUnAnuncio(pieza: Pick<PiezaCreativa, 'estadoModeracion'>): boolean {
  return pieza.estadoModeracion === 'APROBADA'
}

/** R-PUB-05: quien sube no se autoaprueba. La UI lo refuerza; el servidor lo decide (AP-CU112-03). */
export function puedeModerar(pieza: Pick<PiezaCreativa, 'subidaPor'>, operadorId: string): boolean {
  return pieza.subidaPor !== operadorId
}

export function cargarPiezasPendientes(http: HttpClient, gateway: string): CargadorDePagina<PiezaCreativa> {
  return async (pedido) => {
    const todas = await firstValueFrom(http.get<PiezaCreativa[]>(`${gateway}/publicidad/piezas-creativas`, { params: { estadoModeracion: 'PENDIENTE' } }))
    const ordenadas = pedido.orden
      ? [...todas].sort((a, b) => {
          const clave = pedido.orden!.clave as keyof PiezaCreativa
          const [va, vb] = [String(a[clave] ?? ''), String(b[clave] ?? '')]
          return pedido.orden!.sentido === 'asc' ? va.localeCompare(vb) : vb.localeCompare(va)
        })
      : todas
    const inicio = (pedido.pagina - 1) * pedido.tamano
    const pagina: PaginaServidor<PiezaCreativa> = { filas: ordenadas.slice(inicio, inicio + pedido.tamano), total: ordenadas.length }
    return pagina
  }
}

export function cargadorDePiezasPendientes(): CargadorDePagina<PiezaCreativa> {
  return cargarPiezasPendientes(inject(HttpClient), inject(GATEWAY))
}

export async function moderarPieza(http: HttpClient, gateway: string, piezaId: string, entrada: EntradaRevision): Promise<SalidaRevision> {
  return firstValueFrom(
    http.post<SalidaRevision>(`${gateway}/publicidad/piezas-creativas/${piezaId}/revision`, entrada, {
      context: new HttpContext().set(CLAVE_IDEMPOTENCIA, claveDeIdempotencia()),
    }),
  )
}

/** `HttpClient` solo se inyecta en `nucleo/` o `dominio/`; la cola de moderación llama a esto en contexto de inyección. */
export function accionesDeModeracion() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return { moderar: (piezaId: string, entrada: EntradaRevision) => moderarPieza(http, gateway, piezaId, entrada) }
}
