import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { cargadorErpPorCursor, contextoConClave, type Importe } from './contrato-erp'

/**
 * CU-102 · Dar de alta un tercero comercial y su orden de compra.
 *
 * Contrato real: `POST /erp/terceros` (`darDeAltaTercero`), `POST /erp/ordenes-de-compra`
 * (`crearOrdenDeCompra`) y `POST /erp/ordenes-de-compra/{ordenId}/aprobacion`
 * (`aprobarOrdenDeCompra`), y el listado `GET /erp/ordenes-de-compra` (`listarOrdenesDeCompra`, cursor (creadaEn, id)
 * descendente). La respuesta es mínima: no trae la descripción libre de la orden ni el documento del tercero.
 *
 * El alta del tercero y la de la orden son **dos operaciones**, tal como declara la nota
 * de divergencia de `erp.yaml`: el tercero se reusa entre órdenes.
 */
export type EstadoOrden = 'BORRADOR' | 'APROBADA' | 'RECIBIDA_PARCIAL' | 'RECIBIDA_TOTAL' | 'CANCELADA'

/** `OrdenEnLista` de `erp.yaml`. */
export type OrdenDeCompra = {
  ordenCompraId: string
  numero: string
  terceroComercialId: string
  terceroRazonSocial: string
  centroCostoId?: string | null
  centroCostoNombre?: string | null
  estado: EstadoOrden
  monto: Importe
  creadaEn: string
}

export type SalidaOrden = { ordenCompraId: string; estado: EstadoOrden; monto: string }

/**
 * Ninguna factura sin orden autorizada antes (CU-102, postcondición). Una orden en
 * `BORRADOR` no se puede recibir ni facturar: el backend responde
 * `AP-CU102-03 ORDEN_SIN_APROBACION`, y la interfaz lo dice antes de pedirlo.
 */
export function puedeFacturarse(orden: OrdenDeCompra): boolean {
  return orden.estado === 'APROBADA' || orden.estado === 'RECIBIDA_PARCIAL' || orden.estado === 'RECIBIDA_TOTAL'
}

export function motivoParaNoFacturar(orden: OrdenDeCompra): string | null {
  if (puedeFacturarse(orden)) return null
  if (orden.estado === 'BORRADOR') return 'La orden sigue en borrador: hay que aprobarla antes de poder facturarla.'
  return 'La orden está cancelada: no admite facturas nuevas.'
}

/** Solo una orden en `BORRADOR` se aprueba; aprobar una cancelada se rechaza (CU-102, 3a). */
export function puedeAprobarse(orden: OrdenDeCompra): boolean {
  return orden.estado === 'BORRADOR'
}

/** Las órdenes de compra, para la tabla: `GET /erp/ordenes-de-compra` por cursor, de la más reciente a la más antigua. */
export function cargadorDeOrdenes(): CargadorDePagina<OrdenDeCompra> {
  return cargadorErpPorCursor<OrdenDeCompra>(inject(HttpClient), `${inject(GATEWAY)}/erp/ordenes-de-compra`)
}

/** `POST /erp/ordenes-de-compra/{ordenId}/aprobacion`, con clave nueva por intento. */
export function crearAprobacionDeOrden(): (ordenId: string) => Observable<SalidaOrden> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (ordenId) =>
    http.post<SalidaOrden>(`${gateway}/erp/ordenes-de-compra/${ordenId}/aprobacion`, {}, { context: contextoConClave() })
}
