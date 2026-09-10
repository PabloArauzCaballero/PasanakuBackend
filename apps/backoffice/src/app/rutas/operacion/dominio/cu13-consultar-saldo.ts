import { httpResource } from '@angular/common/http'
import { inject, Signal } from '@angular/core'
import type { SaldoBilletera } from 'clientes/angular/nucleo-financiero'
import { GATEWAY } from '../../../nucleo/gateway'

/**
 * CU-13 · consultar el saldo. Un archivo por caso de uso, sobre el tipo GENERADO del
 * contrato (invariante 2). Un saldo cacheado en una consulta de soporte es un saldo que
 * se le lee al titular por teléfono: cada pantalla lo pide fresco y lo relee tras operar.
 */
export function saldoDe(cuentaId: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<SaldoBilletera>(() => `${gateway}/billetera/${cuentaId()}/saldo`)
}

/** Qué es «vacío» para el saldo: una cuenta en cero no es una falla de consulta. */
export const saldoEnCero = (s: SaldoBilletera): boolean => s.disponible.monto === '0.00' && s.retenido.monto === '0.00'
