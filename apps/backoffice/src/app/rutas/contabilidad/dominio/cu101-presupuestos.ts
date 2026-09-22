import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { cargadorDeLista, type Importe } from './contrato-erp'

/**
 * CU-101 · Presupuestar por centro de costo.
 *
 * Contrato real: `POST /erp/presupuestos` (`crearPresupuesto`) y
 * `POST /erp/presupuestos/{presupuestoId}/aprobacion` (`aprobarPresupuesto`). La lectura
 * de partidas no existe todavía (ver el hueco declarado en `contrato-erp.ts`).
 *
 * El presupuesto **mide, no bloquea** (CU-101, 3a y 4a): pasarse de lo presupuestado no
 * rechaza la factura, registra la desviación para que alguien la explique. Por eso la
 * pantalla resalta la desviación y nunca deshabilita nada por ella.
 */
export type EstadoPresupuesto = 'BORRADOR' | 'APROBADO' | 'CERRADO'

export type PartidaPresupuestaria = {
  partidaId: string
  presupuestoId: string
  presupuestoNombre: string
  centroCostoNombre: string
  cuentaContableNombre: string
  periodoNombre: string
  estado: EstadoPresupuesto
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

/** Las partidas del ejercicio, para la tabla. Pedido pendiente: `GET /erp/partidas-presupuestarias`. */
export function cargadorDePartidas(): CargadorDePagina<PartidaPresupuestaria> {
  const gateway = inject(GATEWAY)
  return cargadorDeLista<PartidaPresupuestaria>(
    inject(HttpClient),
    `${gateway}/erp/partidas-presupuestarias`,
    (p, filtros) => !filtros['estado'] || p.estado === filtros['estado'],
    (a, b) => Number(sobreEjecutada(b)) - Number(sobreEjecutada(a)) || a.periodoNombre.localeCompare(b.periodoNombre),
  )
}
