import { HttpClient, HttpResponse } from '@angular/common/http'
import { inject } from '@angular/core'
import { map, type Observable } from 'rxjs'
import type { SalidaEstadoFinanciero } from 'clientes/angular/erp'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoConClave } from './contrato-erp'

/**
 * CU-106 · Generar el estado financiero del período.
 *
 * Contrato real: `POST /erp/periodos/{periodoId}/estados-financieros`
 * (`generarEstadoFinanciero`), que devuelve `estadoFinancieroId`, `tipo`, `cuadra`,
 * **`hashContenido`** y `generadoEn`.
 *
 * **El hash lo calcula el backend y el cliente NO lo recalcula nunca.** Un estado
 * financiero es una fotografía guardada e inmutable (CU-106, postcondición): si el
 * cliente recompusiera el documento o volviera a hashearlo, el hash dejaría de probar
 * nada — probaría lo que el cliente armó, no lo que el backend selló. Este módulo solo
 * transporta el hash del backend y lo **compara** con el que acompaña al documento
 * descargado. No hay ninguna llamada a `crypto.subtle`, ni a ninguna otra función de
 * hash, en todo el carril.
 *
 * **Hueco declarado — pedido pendiente al carril `5A` (backend `erp`).** `erp.yaml` no
 * publica la descarga del documento: solo la generación. Se pide
 * `GET /erp/estados-financieros/{id}/documento`, que devuelva el archivo sellado y
 * repita su hash en la cabecera `X-Hash-Contenido`. Hasta que exista, esta función
 * llama a esa ruta y, si la cabecera no viene, la descarga queda marcada
 * `verificado: false` y la pantalla lo dice — nunca se finge una verificación.
 */
export type TipoDeEstadoFinanciero = SalidaEstadoFinanciero['tipo']

/** La salida de la generación es el tipo GENERADO de `clientes/angular/erp`: trae `hashContenido`. */
export type EstadoFinancieroGenerado = SalidaEstadoFinanciero

export type DocumentoDescargado = {
  estadoFinancieroId: string
  /** El hash del backend, tal cual vino en la respuesta de generación. Nunca recalculado acá. */
  hashContenido: string
  archivo: Blob
  nombreDeArchivo: string
  /** Cierto solo si el backend repitió el hash junto al documento y coincidió. */
  verificado: boolean
}

/** La cabecera en la que el backend repite el hash del documento que está entregando. */
export const CABECERA_DE_HASH = 'X-Hash-Contenido'

export class DocumentoDiscrepante extends Error {
  constructor(readonly esperado: string, readonly recibido: string) {
    super('El documento descargado no corresponde al estado financiero generado.')
    this.name = 'DocumentoDiscrepante'
  }
}

/** «balance-general-2026-03-a1b2c3d4.pdf»: el hash del backend queda en el nombre del archivo. */
export function nombreDeArchivo(generado: EstadoFinancieroGenerado, periodoNombre: string): string {
  const tipo = generado.tipo.toLowerCase().replace(/_/g, '-')
  const periodo = periodoNombre.toLowerCase().replace(/[^a-z0-9]+/g, '-')
  return `${tipo}-${periodo}-${generado.hashContenido.slice(0, 8)}.pdf`
}

/**
 * Arma la descarga a partir de lo que devolvió el backend en las DOS llamadas: el hash
 * sale de la respuesta de generación, y se compara con el que acompaña al documento.
 * Si difieren, no se entrega el archivo: se levanta `DocumentoDiscrepante`.
 */
export function armarDescarga(
  generado: EstadoFinancieroGenerado,
  respuesta: HttpResponse<Blob>,
  periodoNombre: string,
): DocumentoDescargado {
  const hashDelDocumento = respuesta.headers.get(CABECERA_DE_HASH)
  if (hashDelDocumento !== null && hashDelDocumento !== generado.hashContenido) {
    throw new DocumentoDiscrepante(generado.hashContenido, hashDelDocumento)
  }
  return {
    estadoFinancieroId: generado.estadoFinancieroId,
    hashContenido: generado.hashContenido,
    archivo: respuesta.body ?? new Blob(),
    nombreDeArchivo: nombreDeArchivo(generado, periodoNombre),
    verificado: hashDelDocumento === generado.hashContenido,
  }
}

/** Un período abierto solo da un estado provisorio; el definitivo es posterior al cierre (2a). */
export function esProvisorio(periodoCerrado: boolean): boolean {
  return !periodoCerrado
}

/** `POST /erp/periodos/{periodoId}/estados-financieros`, con clave nueva por intento. */
export function crearGeneracion(): (periodoId: string, tipo: TipoDeEstadoFinanciero) => Observable<EstadoFinancieroGenerado> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (periodoId, tipo) =>
    http.post<EstadoFinancieroGenerado>(`${gateway}/erp/periodos/${periodoId}/estados-financieros`, { tipo }, { context: contextoConClave() })
}

/** `GET /erp/estados-financieros/{id}/documento` (pedido pendiente al backend). */
export function crearDescarga(): (generado: EstadoFinancieroGenerado, periodoNombre: string) => Observable<DocumentoDescargado> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (generado, periodoNombre) =>
    http
      .get(`${gateway}/erp/estados-financieros/${generado.estadoFinancieroId}/documento`, { observe: 'response', responseType: 'blob' })
      .pipe(map((respuesta) => armarDescarga(generado, respuesta, periodoNombre)))
}
