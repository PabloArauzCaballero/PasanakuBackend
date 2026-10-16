/**
 * H12.S1.M2 · separar saldos y posiciones.
 *
 * Una billetera no tiene «un saldo»: tiene tres cifras que NO se pueden gastar igual.
 * El contrato de `nucleo-financiero` (`SaldoBilletera`) hoy publica `disponible` y `retenido`;
 * el pedido de contrato del carril D (`evidencia/carril-D/contrato-requerido-nucleo.md`, que el
 * productor todavía NO implementa) agrega `titularId` y `comprometidoEnPozos`, con el sentido que
 * fija `LibroDelTitular.SaldoDelTitular`:
 *
 *   - `disponible`           lo que el libro dice que se puede usar ahora;
 *   - `comprometidoEnPozos`  PARTE del disponible afectada a aportes y pozos de grupos;
 *   - `retenido`             lo apartado por una operación en curso (aparte del disponible).
 *
 * De ahí sale lo único que es gastable de verdad: `disponible − comprometidoEnPozos`, nunca negativo
 * (la misma regla que `SaldoDelTitular.invertible()`), y un total que reconcilia por construcción:
 *
 *   gastable + comprometido + retenido = disponible + retenido = total de la cuenta.
 *
 * Si el libro NO informa lo comprometido, no se inventa: `gastable` queda en `null` y la pantalla dice
 * que no puede separar el disponible. Mostrar el disponible entero como «gastable» sin saberlo sería
 * exactamente el defecto que esta función existe para evitar. Todo es aritmética entera sobre
 * centavos (`BigInt`): ningún importe pasa por `number`.
 */

export type ImporteDelSaldo = { readonly monto: string; readonly moneda: string }

/** El `SaldoBilletera` del contrato, más lo que el carril D le pidió al núcleo (opcional hasta que exista). */
export type SaldoConPosiciones = {
  readonly cuentaId?: string
  readonly titularId?: string
  readonly disponible: ImporteDelSaldo
  readonly retenido: ImporteDelSaldo
  readonly comprometidoEnPozos?: ImporteDelSaldo
  readonly alCorteDe?: string
}

export type AdvertenciaDePosicion = 'comprometido-excede-disponible'

export type PosicionesDeSaldo = {
  readonly moneda: string
  /** El disponible tal como lo informa el libro (incluye lo comprometido en pozos). */
  readonly disponible: string
  /** `null` si el libro no informó lo comprometido: no hay forma honesta de separarlo. */
  readonly gastable: string | null
  /** Lo efectivamente afectado a pozos, acotado al disponible. `null` si no se informó. */
  readonly comprometidoEnPozos: string | null
  readonly retenido: string
  /** disponible + retenido: lo que la cuenta tiene en el libro, sea o no gastable. */
  readonly totalDeLaCuenta: string
  readonly separado: boolean
  readonly advertencias: readonly AdvertenciaDePosicion[]
}

export type MotivoDeSaldoIncoherente = 'importe-fuera-de-contrato' | 'importe-negativo' | 'monedas-distintas'

/** El saldo recibido no se puede explicar: la pantalla falla cerrada en vez de mostrar una cifra dudosa. */
export class SaldoIncoherente extends Error {
  constructor(readonly motivo: MotivoDeSaldoIncoherente, detalle: string) {
    super(`Saldo incoherente (${motivo}): ${detalle}`)
    this.name = 'SaldoIncoherente'
  }
}

const FORMA = /^-?\d+\.\d{2}$/

function centavos(i: ImporteDelSaldo, nombre: string): bigint {
  if (!FORMA.test(i.monto)) throw new SaldoIncoherente('importe-fuera-de-contrato', `${nombre} no tiene la forma ^-?\\d+\\.\\d{2}$`)
  const n = BigInt(i.monto.replace('.', ''))
  if (n < 0n) throw new SaldoIncoherente('importe-negativo', `${nombre} es negativo`)
  return n
}

function aCadena(c: bigint): string {
  const texto = c.toString().padStart(3, '0')
  return `${texto.slice(0, -2)}.${texto.slice(-2)}`
}

export function separarPosiciones(saldo: SaldoConPosiciones): PosicionesDeSaldo {
  const moneda = saldo.disponible.moneda
  const importes = [saldo.disponible, saldo.retenido, ...(saldo.comprometidoEnPozos ? [saldo.comprometidoEnPozos] : [])]
  if (importes.some((i) => i.moneda !== moneda)) {
    throw new SaldoIncoherente('monedas-distintas', 'las cifras de una misma cuenta vienen en monedas distintas y no se suman')
  }
  const disponible = centavos(saldo.disponible, 'disponible')
  const retenido = centavos(saldo.retenido, 'retenido')
  const total = aCadena(disponible + retenido)
  if (!saldo.comprometidoEnPozos) {
    return { moneda, disponible: aCadena(disponible), gastable: null, comprometidoEnPozos: null, retenido: aCadena(retenido), totalDeLaCuenta: total, separado: false, advertencias: [] }
  }
  const informado = centavos(saldo.comprometidoEnPozos, 'comprometidoEnPozos')
  const excede = informado > disponible
  const comprometido = excede ? disponible : informado
  return {
    moneda,
    disponible: aCadena(disponible),
    gastable: aCadena(disponible - comprometido),
    comprometidoEnPozos: aCadena(comprometido),
    retenido: aCadena(retenido),
    totalDeLaCuenta: total,
    separado: true,
    advertencias: excede ? ['comprometido-excede-disponible'] : [],
  }
}

/** ¿Hay algo en la cuenta? Una cuenta en cero no es una falla de consulta. */
export const cuentaEnCero = (p: PosicionesDeSaldo): boolean => p.totalDeLaCuenta === '0.00'
