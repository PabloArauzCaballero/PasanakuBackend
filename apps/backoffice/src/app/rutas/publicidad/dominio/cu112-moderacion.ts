import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoIdempotente } from '../../../nucleo/idempotencia.interceptor'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { cargadorPublicidadPorCursor } from './contrato-publicidad'
import { lotesPorCursor } from './lotes-por-cursor'
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
 * Listado: `GET /publicidad/piezas-creativas` (`listarPiezasCreativas`, cursor (creadaEn, id), sin
 * `estadoModeracion` devuelve PENDIENTE; exige `PUBLICIDAD_MODERAR`). El contrato **no** dice quién subió la pieza ni
 * el nombre del anunciante: R-PUB-05 (quien sube no se autoaprueba) lo decide el servidor al moderar
 * (`AP-CU112-03 MODERADOR_ES_EL_ANUNCIANTE`), y la pantalla muestra ese rechazo; no hay dato en el cliente para
 * anticiparlo ni se inventa uno.
 */
export type EstadoModeracion = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA'

/** `PiezaEnLista` de `publicidad.yaml`. */
export type PiezaCreativa = {
  piezaCreativaId: string
  anuncianteId: string
  titulo: string
  texto?: string | null
  urlRecurso: string
  tipoRecurso: 'IMAGEN' | 'VIDEO'
  estadoModeracion: EstadoModeracion
  creadaEn: string
}

/** Código con el que el servidor rechaza que quien subió la pieza la modere (R-PUB-05). */
export const MODERADOR_ES_EL_ANUNCIANTE = 'AP-CU112-03'

/** R-PUB-04: una pieza que no está APROBADA no entra a ningún anuncio. */
export function puedeUsarseEnUnAnuncio(pieza: Pick<PiezaCreativa, 'estadoModeracion'>): boolean {
  return pieza.estadoModeracion === 'APROBADA'
}

/** La cola: `estadoModeracion=PENDIENTE` explícito (es también el valor por omisión del contrato). */
export function cargarPiezasPendientes(http: HttpClient, gateway: string): CargadorDePagina<PiezaCreativa> {
  return cargadorPublicidadPorCursor<PiezaCreativa>(http, `${gateway}/publicidad/piezas-creativas`, (_f, params) =>
    params.set('estadoModeracion', 'PENDIENTE'),
  )
}

export function cargadorDePiezasPendientes(): CargadorDePagina<PiezaCreativa> {
  return cargarPiezasPendientes(inject(HttpClient), inject(GATEWAY))
}

/**
 * La cola por lotes de 100. `estadoModeracion` acepta un solo valor y por omisión es PENDIENTE (la cola); elegir
 * APROBADA o RECHAZADA muestra lo ya resuelto, sin acciones.
 */
export function lotesDePiezas() {
  return lotesPorCursor<PiezaCreativa>('/publicidad/piezas-creativas', (filtros, params) => params.set('estadoModeracion', filtros['estado'] || 'PENDIENTE'))
}

export async function moderarPieza(http: HttpClient, gateway: string, piezaId: string, entrada: EntradaRevision): Promise<SalidaRevision> {
  return firstValueFrom(
    http.post<SalidaRevision>(`${gateway}/publicidad/piezas-creativas/${piezaId}/revision`, entrada, {
      context: contextoIdempotente(),
    }),
  )
}

/** `HttpClient` solo se inyecta en `nucleo/` o `dominio/`; la cola de moderación llama a esto en contexto de inyección. */
export function accionesDeModeracion() {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return { moderar: (piezaId: string, entrada: EntradaRevision) => moderarPieza(http, gateway, piezaId, entrada) }
}
