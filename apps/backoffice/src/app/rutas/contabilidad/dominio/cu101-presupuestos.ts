import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { cargadorErpPorCursor, type Importe } from './contrato-erp'

/**
 * CU-101 · Presupuestar por centro de costo.
 *
 * Contrato real: `POST /erp/presupuestos` (`crearPresupuesto`),
 * `POST /erp/presupuestos/{presupuestoId}/aprobacion` (`aprobarPresupuesto`) y el listado
 * `GET /erp/partidas-presupuestarias` (`listarPartidasPresupuestarias`, cursor (inicio del período, id) descendente).
 * La cuenta contable viaja como id: su nombre es de nucleo-financiero y erp no cruza esquemas, así que la pantalla
 * no la nombra. El período viaja como año y mes: el nombre lo arma la UI (`nombreDelPeriodo`).
 *
 * El presupuesto **mide, no bloquea** (CU-101, 3a y 4a): pasarse de lo presupuestado no
 * rechaza la factura, registra la desviación para que alguien la explique. Por eso la
 * pantalla resalta la desviación y nunca deshabilita nada por ella.
 */
export type EstadoPresupuesto = 'BORRADOR' | 'APROBADO' | 'CERRADO'

/** `PartidaEnLista` de `erp.yaml`. */
export type PartidaPresupuestaria = {
  partidaId: string
  presupuestoId: string
  presupuestoNombre: string
  estado: EstadoPresupuesto
  centroCostoId: string
  centroCostoNombre: string
  cuentaContableId: string
  periodoContableId: string
  anio: number
  mes: number
  fechaInicioPeriodo: string
  montoPresupuestado: Importe
  montoEjecutado: Importe
}

/**
 * `monto_ejecutado - monto_presupuestado`, puro y en cadena decimal — el mismo átomo
 * `calcularDesviacion` que declara CU-101. Se calcula con enteros de centavos para no
 * pasar nunca por un doble (invariante 4); el resultado vuelve a ser cadena, y quien lo
 * dibuja es `Monto`, no este módulo.
 */
export function calcularDesviacion(partida: PartidaPresupuestaria): Importe {
  const centavos = aCentavos(partida.montoEjecutado.monto) - aCentavos(partida.montoPresupuestado.monto)
  return { monto: deCentavos(centavos), moneda: partida.montoEjecutado.moneda }
}

/** Gastó más de lo autorizado: es un hecho que se muestra, nunca un rechazo. */
export function sobreEjecutada(partida: PartidaPresupuestaria): boolean {
  return aCentavos(partida.montoEjecutado.monto) > aCentavos(partida.montoPresupuestado.monto)
}

/** «1240.55» → 124055. Sin coma flotante: la parte decimal del contrato es siempre de dos dígitos. */
export function aCentavos(monto: string): number {
  const negativo = monto.startsWith('-')
  const [entera, decimal = '00'] = monto.replace('-', '').split('.')
  const centavos = Number(entera) * 100 + Number(decimal.padEnd(2, '0').slice(0, 2))
  return negativo ? -centavos : centavos
}

/** 124055 → «1240.55». No es formato de presentación: es la cadena del contrato. */
export function deCentavos(centavos: number): string {
  const signo = centavos < 0 ? '-' : ''
  const absoluto = Math.abs(centavos)
  return `${signo}${Math.trunc(absoluto / 100)}.${String(absoluto % 100).padStart(2, '0')}`
}

/** Las partidas, para la tabla: `GET /erp/partidas-presupuestarias` por cursor, en el orden del servidor. */
export function cargadorDePartidas(): CargadorDePagina<PartidaPresupuestaria> {
  return cargadorErpPorCursor<PartidaPresupuestaria>(inject(HttpClient), `${inject(GATEWAY)}/erp/partidas-presupuestarias`)
}
