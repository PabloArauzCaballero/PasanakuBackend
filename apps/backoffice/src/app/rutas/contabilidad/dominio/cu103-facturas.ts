import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { aCentavos } from './cu101-presupuestos'
import { cargadorErpPorCursor, contextoConClave, type Importe, type Moneda } from './contrato-erp'

/**
 * CU-103 · Registrar y pagar una factura de proveedor.
 *
 * Contrato real: `POST /erp/facturas-de-proveedor` (`registrarFacturaDeProveedor`) y
 * `POST /erp/facturas-de-proveedor/{facturaId}/pagos` (`pagarFacturaDeProveedor`), y el
 * listado `GET /erp/facturas-de-proveedor` (`listarFacturasDeProveedor`, cursor (fechaEmision, id) descendente).
 *
 * **Segregación de funciones (`R-CTB-05`, `AP-CU103-02`).** Quien aprobó la factura no
 * puede autorizar su pago. La regla vive acá y la pantalla la consulta: el 422 del
 * backend confirma la decisión, nunca es la primera línea de defensa.
 */
export type EstadoFactura = 'REGISTRADA' | 'APROBADA' | 'PAGADA_PARCIAL' | 'PAGADA' | 'ANULADA'
export type FormaDePago = 'TRANSFERENCIA' | 'CHEQUE' | 'EFECTIVO'

/** `FacturaEnLista` de `erp.yaml`. El contrato NO expone quién aprobó: solo `aprobadaPorLaSesion`. */
export type FacturaDeProveedor = {
  facturaProveedorId: string
  numeroFactura: string
  terceroComercialId: string
  terceroRazonSocial: string
  ordenCompraId: string | null
  fechaEmision: string
  fechaVencimiento: string
  estado: EstadoFactura
  monto: Importe
  montoPagado: Importe
  saldoPendiente: Importe
  /**
   * Lo resuelve el BACKEND contra la sesión: cierto si esta misma cuenta aprobó la
   * factura. No se compara en el cliente porque el cliente no tiene —ni debe tener— la
   * identidad con la que el servidor decide un control de cuatro ojos.
   */
  aprobadaPorLaSesion: boolean
  /** El asiento lo escribe `nucleo-financiero` (invariante 12): viaja nulo hasta que responde. */
  asientoContableId: string | null
}

/**
 * **El autorizante del pago NO viaja en el cuerpo.** `erp.yaml` ya no declara `autorizadoPor` en `EntradaPago` (y rechaza con 400 un
 * cuerpo que lo traiga): un control de segregación de funciones cuyo «quién autorizó» lo manda el cliente es falsificable con una petición
 * hecha a mano —quien aprobó la factura solo tendría que escribir otro identificador—. El servidor lo deriva del sujeto del token de la
 * sesión (`CU103FacturaDeProveedor.pagar`, `ctx.usuarioId()`) y es ese actor el que queda escrito en `pago_a_proveedor.autorizado_por`.
 */
export type EntradaPago = { monto: string; moneda: Moneda; formaPago: FormaDePago }

/** Una factura admite pago si está aprobada o pagada en parte, y no anulada (CU-103, 5). */
export function admitePago(factura: FacturaDeProveedor): boolean {
  return (factura.estado === 'APROBADA' || factura.estado === 'PAGADA_PARCIAL') && aCentavos(factura.saldoPendiente.monto) > 0
}

/**
 * `validarSegregacionAprobacionPago` (átomo de CU-103): quien aprobó no autoriza el pago.
 * El dato lo resuelve el backend contra la sesión (`aprobadaPorLaSesion`); acá solo se
 * lee. El `AP-CU103-02` del servidor confirma la decisión, nunca es la primera defensa.
 */
export function mismoAprobadorYPagador(factura: FacturaDeProveedor): boolean {
  return factura.aprobadaPorLaSesion
}

/** Por qué esta cuenta no puede pagar esta factura. `null` si sí puede. */
export function motivoParaNoPagar(factura: FacturaDeProveedor, tienePermisoDePago: boolean): string | null {
  if (!tienePermisoDePago) return 'Registrar el pago es de Tesorería: esta cuenta no tiene CONTABILIDAD_ERP_PAGAR.'
  if (mismoAprobadorYPagador(factura)) return 'Quien aprobó la factura no puede autorizar su pago (R-CTB-05). Esta cuenta aprobó esta factura.'
  if (factura.estado === 'ANULADA') return 'La factura está anulada: no admite pagos.'
  if (factura.estado === 'REGISTRADA') return 'La factura todavía no está aprobada: se paga después de aprobarla.'
  if (!admitePago(factura)) return 'La factura no tiene saldo pendiente que pagar.'
  return null
}

/** Un pago no puede exceder el saldo pendiente (`AP-CU103-04`). */
export function pagoExcedeElSaldo(factura: FacturaDeProveedor, monto: string): boolean {
  return aCentavos(monto) > aCentavos(factura.saldoPendiente.monto)
}

/** Vencida y con saldo: la que manda en la cola de cuentas por pagar. */
export function vencida(factura: FacturaDeProveedor, hoyIso: string): boolean {
  return factura.fechaVencimiento < hoyIso.slice(0, 10) && aCentavos(factura.saldoPendiente.monto) > 0
}

/** Las facturas de proveedor, para la tabla: `GET /erp/facturas-de-proveedor` por cursor, de la emisión más reciente. */
export function cargadorDeFacturas(): CargadorDePagina<FacturaDeProveedor> {
  return cargadorErpPorCursor<FacturaDeProveedor>(inject(HttpClient), `${inject(GATEWAY)}/erp/facturas-de-proveedor`)
}

/** `POST /erp/facturas-de-proveedor/{facturaId}/pagos`, con clave nueva por intento. */
export function crearPagoDeFactura(): (facturaId: string, pago: EntradaPago) => Observable<FacturaDeProveedor> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (facturaId, pago) =>
    http.post<FacturaDeProveedor>(`${gateway}/erp/facturas-de-proveedor/${facturaId}/pagos`, pago, { context: contextoConClave() })
}
