import { httpResource } from '@angular/common/http'
import { inject, Signal } from '@angular/core'
import { separarPosiciones, SaldoIncoherente, type ImporteDelSaldo, type PosicionesDeSaldo } from '@aportaya/dominio-cliente/posiciones'
import type { SaldoBilletera } from 'clientes/angular/nucleo-financiero'
import { GATEWAY } from '../../../nucleo/gateway'

/**
 * CU-13 · consultar el saldo. Un archivo por caso de uso, sobre el tipo GENERADO del
 * contrato (invariante 2). Un saldo cacheado en una consulta de soporte es un saldo que
 * se le lee al titular por teléfono: cada pantalla lo pide fresco y lo relee tras operar.
 *
 * H12.S1.M2 · el contrato publicado (`SaldoBilletera`) trae `disponible` y `retenido`. Lo que el
 * carril D le pidió al núcleo —`titularId` y `comprometidoEnPozos`, ver
 * `evidencia/carril-D/contrato-requerido-nucleo.md`— todavía no lo publica el productor: llega como
 * opcional y, mientras falte, la pantalla NO afirma cuánto es gastable (ver `posicionesDe`).
 */
export type SaldoConPosiciones = SaldoBilletera & { titularId?: string; comprometidoEnPozos?: ImporteDelSaldo }

export function saldoDe(cuentaId: Signal<string>) {
  const gateway = inject(GATEWAY)
  return httpResource<SaldoConPosiciones>(() => `${gateway}/billetera/${cuentaId()}/saldo`)
}

/** Qué es «vacío» para el saldo: una cuenta en cero no es una falla de consulta. */
export const saldoEnCero = (s: SaldoConPosiciones): boolean =>
  s.disponible.monto === '0.00' && s.retenido.monto === '0.00' && (s.comprometidoEnPozos?.monto ?? '0.00') === '0.00'

export type LecturaDePosiciones = { ok: true; posiciones: PosicionesDeSaldo } | { ok: false }

/** Separa el saldo en gastable / comprometido / retenido. Si las cifras no se pueden explicar, falla cerrado. */
export function posicionesDe(s: SaldoConPosiciones): LecturaDePosiciones {
  try {
    return { ok: true, posiciones: separarPosiciones(s) }
  } catch (e) {
    if (e instanceof SaldoIncoherente) return { ok: false }
    throw e
  }
}
