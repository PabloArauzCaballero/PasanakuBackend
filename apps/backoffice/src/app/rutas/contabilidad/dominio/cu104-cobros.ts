import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { aCentavos, deCentavos } from './cu101-presupuestos'
import { cargadorErpPorCursor, contextoConClave, type Importe } from './contrato-erp'

/**
 * CU-104 · Cobrar una cuenta por cobrar.
 *
 * Contrato real: `POST /erp/cuentas-por-cobrar` (`abrirCuentaPorCobrar`) y
 * `POST /erp/cuentas-por-cobrar/{cuentaId}/cobros` (`registrarCobro`), y el listado
 * `GET /erp/cuentas-por-cobrar` (`listarCuentasPorCobrar`, cursor (fechaVencimiento, id) descendente).
 *
 * `erp.yaml` declara además que **no hay operación para marcar una cuenta como
 * incobrable**: la tabla es append-only y el estado se decide al insertar. La pantalla
 * por lo tanto muestra `INCOBRABLE` pero no ofrece declararlo.
 */
export type EstadoCuentaPorCobrar = 'PENDIENTE' | 'COBRADA_PARCIAL' | 'COBRADA' | 'INCOBRABLE'
export type FormaDeCobro = 'TRANSFERENCIA' | 'QR' | 'TARJETA' | 'EFECTIVO'

export type OrigenCuentaPorCobrar = 'FACTURA_PUBLICIDAD' | 'OTRO' | 'VENTA_SERVICIO'

/** `CuentaPorCobrarEnLista` de `erp.yaml`. Una cuenta puede no tener tercero: ambos campos son nulables. */
export type CuentaPorCobrar = {
  cuentaPorCobrarId: string
  origenTipo: OrigenCuentaPorCobrar
  origenId: string
  terceroComercialId?: string | null
  terceroRazonSocial?: string | null
  fechaVencimiento: string
  estado: EstadoCuentaPorCobrar
  monto: Importe
  cobrado: Importe
  saldoPendiente: Importe
}

/** `calcularSaldoPendiente` (átomo de CU-104): `monto - monto_cobrado`, en centavos enteros. */
export function saldoPendienteDe(cuenta: CuentaPorCobrar): Importe {
  return { monto: deCentavos(aCentavos(cuenta.monto.monto) - aCentavos(cuenta.cobrado.monto)), moneda: cuenta.monto.moneda }
}

/** Una cuenta incobrable no se cobra (`AP-CU104-03`); una ya cobrada tampoco tiene qué cobrar. */
export function admiteCobro(cuenta: CuentaPorCobrar): boolean {
  return cuenta.estado !== 'INCOBRABLE' && cuenta.estado !== 'COBRADA' && aCentavos(cuenta.saldoPendiente.monto) > 0
}

export function motivoParaNoCobrar(cuenta: CuentaPorCobrar, tienePermiso: boolean): string | null {
  if (!tienePermiso) return 'Registrar el cobro es de Tesorería: esta cuenta no tiene CONTABILIDAD_ERP_COBRAR.'
  if (cuenta.estado === 'INCOBRABLE') return 'Cuenta declarada incobrable: no admite cobros.'
  if (!admiteCobro(cuenta)) return 'La cuenta no tiene saldo pendiente que cobrar.'
  return null
}

/** El cobro no puede exceder el saldo: el excedente no se acredita a otra cuenta (`AP-CU104-02`). */
export function cobroExcedeElSaldo(cuenta: CuentaPorCobrar, monto: string): boolean {
  return aCentavos(monto) > aCentavos(cuenta.saldoPendiente.monto)
}

/** Las cuentas por cobrar, para la tabla: `GET /erp/cuentas-por-cobrar` por cursor, en el orden del servidor. */
export function cargadorDeCuentas(): CargadorDePagina<CuentaPorCobrar> {
  return cargadorErpPorCursor<CuentaPorCobrar>(inject(HttpClient), `${inject(GATEWAY)}/erp/cuentas-por-cobrar`)
}

/** `POST /erp/cuentas-por-cobrar/{cuentaId}/cobros`, con clave nueva por intento. */
export function crearCobro(): (cuentaId: string, cobro: { monto: string; formaCobro: FormaDeCobro }) => Observable<CuentaPorCobrar> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (cuentaId, cobro) =>
    http.post<CuentaPorCobrar>(`${gateway}/erp/cuentas-por-cobrar/${cuentaId}/cobros`, cobro, { context: contextoConClave() })
}
