import { HttpClient, httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import type { SalidaCierre } from 'clientes/angular/erp'
import { GATEWAY } from '../../../nucleo/gateway'
import { contextoConClave, type Importe } from './contrato-erp'

/**
 * CU-100 · Abrir y cerrar el período contable.
 *
 * Contrato real: `POST /erp/ejercicios` (`abrirEjercicioFiscal`) y
 * `POST /erp/periodos/{periodoId}/cierre` (`cerrarPeriodoContable`). La LECTURA de los
 * períodos no existe todavía en `erp.yaml`: ver el hueco declarado en `contrato-erp.ts`.
 *
 * **La regla del gate de este carril vive acá, no en la pantalla**: un período cerrado
 * NO admite asentar. El modelo lo rechaza (`AP-CU103-01 PERIODO_CERRADO`), y la interfaz
 * tiene que decirlo antes, con el motivo a la vista — nunca ofrecer el botón y dejar que
 * el 422 explique. `puedeAsentarEn` es la única fuente de esa decisión: la usan tanto la
 * pantalla de períodos como el alta de factura de proveedor (CU-103).
 */
export type EstadoPeriodo = 'ABIERTO' | 'CERRADO'

export type PeriodoContable = {
  periodoId: string
  ejercicioFiscalId: string
  anio: number
  mes: number
  nombre: string
  estado: EstadoPeriodo
  cerradoEn: string | null
  totalDebe: Importe | null
  totalHaber: Importe | null
}

/** La salida del cierre es el tipo GENERADO de `clientes/angular/erp`: no se redeclara. */
export type { SalidaCierre }

/** Un período cerrado no admite ningún asiento nuevo: es irreversible por diseño (CU-100). */
export function puedeAsentarEn(periodo: PeriodoContable): boolean {
  return periodo.estado === 'ABIERTO'
}

/**
 * Por qué no se puede asentar. Devuelve `null` cuando sí se puede: la interfaz nunca
 * muestra un botón deshabilitado sin motivo visible al lado.
 */
export function motivoParaNoAsentar(periodo: PeriodoContable): string | null {
  return puedeAsentarEn(periodo)
    ? null
    : 'Período cerrado: no se puede asentar en él. La corrección va en el período abierto siguiente, con glosa que lo referencie.'
}

/** El más antiguo sin cerrar del ejercicio: el único que el backend acepta cerrar (AP-CU100-02). */
export function periodoMasAntiguoAbierto(periodos: readonly PeriodoContable[]): PeriodoContable | null {
  const abiertos = periodos.filter((p) => p.estado === 'ABIERTO')
  if (abiertos.length === 0) return null
  return [...abiertos].sort((a, b) => a.anio - b.anio || a.mes - b.mes)[0] ?? null
}

/** Solo el más antiguo abierto se puede cerrar; el resto, con su motivo a la vista. */
export function puedeCerrar(periodo: PeriodoContable, periodos: readonly PeriodoContable[]): boolean {
  return periodoMasAntiguoAbierto(periodos)?.periodoId === periodo.periodoId
}

/** El cuadre del mes: `total_debe` distinto de `total_haber` es un incidente, no un cierre (3a). */
export function cuadra(periodo: PeriodoContable): boolean {
  if (!periodo.totalDebe || !periodo.totalHaber) return false
  return Number(periodo.totalDebe.monto) === Number(periodo.totalHaber.monto)
}

/** Los períodos del ejercicio. Pedido pendiente al backend: `GET /erp/ejercicios/{id}/periodos`. */
export function periodosDelEjercicio(ejercicioId: () => string) {
  const gateway = inject(GATEWAY)
  return httpResource<PeriodoContable[]>(() => `${gateway}/erp/ejercicios/${ejercicioId()}/periodos`)
}

export const sinPeriodos = (periodos: PeriodoContable[] | undefined): boolean => (periodos?.length ?? 0) === 0

/** `POST /erp/periodos/{periodoId}/cierre`, con clave de idempotencia nueva por intento. */
export function crearCierreDePeriodo(): (periodoId: string, glosa: string) => Observable<SalidaCierre> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (periodoId, glosa) =>
    http.post<SalidaCierre>(`${gateway}/erp/periodos/${periodoId}/cierre`, { glosa }, { context: contextoConClave() })
}
