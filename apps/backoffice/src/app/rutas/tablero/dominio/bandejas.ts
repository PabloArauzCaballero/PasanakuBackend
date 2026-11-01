import { HttpClient, HttpErrorResponse } from '@angular/common/http'
import type { NombreDeIcono } from '@aportaya/ui/icono/icono'
import { inject } from '@angular/core'
import { firstValueFrom } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import { mensajeDe } from '../../../nucleo/errores'

/**
 * H1.S4.M1 (B2, AMB-B3) · «Requiere tu acción». **No hay endpoint de indicadores ni contadores** y ninguna bandeja trae
 * total (`convenciones-contrato.md` §4: `items` + cursor). Por eso el tablero pide la **primera página con `limite=1`** de
 * cada bandeja real y dice solo lo que esa respuesta prueba: «hay pendientes» (vino al menos una fila) o «al día» (vino
 * vacía). Nunca un número: el servidor no lo dio.
 *
 * Cada bandeja se consulta **sin filtros**: su filtro por omisión del contrato ya es «lo que alguien tiene que mirar»
 * (citado en `definicion`). Ruta y permiso salen del YAML de cada servicio:
 * - `listarRetirosEnRevision` · `GET /billetera/retiros` · RETIRO_APROBAR (nucleo-financiero.yaml:390)
 * - `listarBandejaDeReclamos` · `GET /reclamos` · RECLAMO_VER, sin `estados` = INGRESADO y EN_ANALISIS (cumplimiento.yaml:468)
 * - `listarBandejaDeAlertas` · `GET /cumplimiento/alertas` · CUMPLIMIENTO_ALERTAS, sin `estados` = ABIERTA y EN_ANALISIS (cumplimiento.yaml:570)
 * - `listarBandejaDeCasos` · `GET /cumplimiento/casos` · CUMPLIMIENTO_CASOS, sin `estados` = no cerrados (cumplimiento.yaml:618)
 * - `listarDesembolsos` · `GET /desembolsos` · ENTREGA_EJECUTAR o AUDITORIA_LEER, por omisión RECHAZADA, DEVUELTA_POR_BANCO
 *   y EN_PROCESO (entregas.yaml:236)
 */
export type DefinicionDeBandeja = {
  id: 'retiros' | 'reclamos' | 'alertas' | 'casos' | 'desembolsos'
  titulo: string
  definicion: string
  /** Basta uno: es lo que el contrato acepta (p. ej. desembolsos: ENTREGA_EJECUTAR o AUDITORIA_LEER). */
  permisos: readonly string[]
  /** La pantalla de la bandeja dentro del backoffice. */
  ruta: string
  /** La operación del contrato, relativa al gateway. */
  recurso: string
  icono: NombreDeIcono
}

export const BANDEJAS: readonly DefinicionDeBandeja[] = [
  {
    id: 'retiros',
    titulo: 'Retiros en revisión',
    definicion: 'Retiros que esperan la segunda firma de otra persona con permiso.',
    permisos: ['RETIRO_APROBAR'],
    ruta: '/operacion/retiros',
    recurso: '/billetera/retiros',
    icono: 'retiro',
  },
  {
    id: 'reclamos',
    titulo: 'Reclamos abiertos',
    definicion: 'Reclamos ingresados o en análisis, el que vence antes primero.',
    permisos: ['RECLAMO_VER'],
    ruta: '/operacion/reclamos',
    recurso: '/reclamos',
    icono: 'mensaje',
  },
  {
    id: 'alertas',
    titulo: 'Alertas de monitoreo',
    definicion: 'Alertas abiertas o en análisis que esperan a un analista.',
    permisos: ['CUMPLIMIENTO_ALERTAS'],
    ruta: '/cumplimiento/alertas',
    recurso: '/cumplimiento/alertas',
    icono: 'alerta',
  },
  {
    id: 'casos',
    titulo: 'Casos de investigación',
    definicion: 'Casos no cerrados, el de plazo más cercano primero.',
    permisos: ['CUMPLIMIENTO_CASOS'],
    ruta: '/cumplimiento/casos',
    recurso: '/cumplimiento/casos',
    icono: 'carpeta',
  },
  {
    id: 'desembolsos',
    titulo: 'Desembolsos con excepción',
    definicion: 'Órdenes rechazadas, devueltas por el banco o todavía en proceso.',
    permisos: ['ENTREGA_EJECUTAR', 'AUDITORIA_LEER'],
    ruta: '/operacion/desembolsos',
    recurso: '/desembolsos',
    icono: 'entrega',
  },
]

export type ErrorDeLectura = { mensaje: string; trazaId?: string }

/** Lo único que una primera página con `limite=1` permite afirmar. */
export type LecturaDeBandeja = { tipo: 'pendientes' } | { tipo: 'al-dia' } | { tipo: 'error'; error: ErrorDeLectura }

/** La respuesta no trae `items` como lista: está fuera de contrato y no se interpreta como «al día». */
export const FUERA_DE_CONTRATO = 'La bandeja respondió algo que no esperábamos. No lo damos por «al día»: avisá a Sistemas.'

/**
 * El interceptor (`nucleo/errores.interceptor.ts`) ya entrega el error traducido; sin él (o fuera de HTTP) se traduce acá
 * con el mismo catálogo. Nunca el mensaje crudo del backend.
 */
export function traducirError(error: unknown): ErrorDeLectura {
  if (error instanceof HttpErrorResponse) {
    const cuerpo = (error.error ?? {}) as Partial<{ codigo: string; trazaId: string }>
    return { mensaje: mensajeDe(cuerpo.codigo, error.status), trazaId: cuerpo.trazaId }
  }
  if (typeof error === 'object' && error !== null && typeof (error as { mensaje?: unknown }).mensaje === 'string') {
    const e = error as { mensaje: string; trazaId?: string }
    return { mensaje: e.mensaje, trazaId: e.trazaId }
  }
  return { mensaje: mensajeDe(undefined, 500) }
}

export async function leerBandeja(http: HttpClient, gateway: string, bandeja: DefinicionDeBandeja): Promise<LecturaDeBandeja> {
  try {
    const respuesta = await firstValueFrom(http.get<{ items?: unknown }>(`${gateway}${bandeja.recurso}`, { params: { limite: 1 } }))
    if (!Array.isArray(respuesta?.items)) return { tipo: 'error', error: { mensaje: FUERA_DE_CONTRATO } }
    return respuesta.items.length > 0 ? { tipo: 'pendientes' } : { tipo: 'al-dia' }
  } catch (error) {
    return { tipo: 'error', error: traducirError(error) }
  }
}

/** Fábrica en contexto de inyección: la pantalla no importa `HttpClient` (checker «sin red en vista»). */
export function lectorDeBandejas(): (bandeja: DefinicionDeBandeja) => Promise<LecturaDeBandeja> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (bandeja) => leerBandeja(http, gateway, bandeja)
}
