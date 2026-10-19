import { HttpClient } from '@angular/common/http'
import { inject } from '@angular/core'
import type { Observable } from 'rxjs'
import { GATEWAY } from '../../../nucleo/gateway'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { aCentavos } from './cu101-presupuestos'
import { cargadorErpPorCursor, contextoConClave, type Importe } from './contrato-erp'

/**
 * CU-105 · Depreciar un activo fijo.
 *
 * Contrato real: `POST /erp/activos/{activoId}/depreciaciones` (`depreciarActivo`) y
 * `POST /erp/periodos/{periodoId}/depreciaciones` (`correrDepreciacionDelPeriodo`, la
 * corrida del mes, idempotente), y el inventario `GET /erp/activos` (`listarActivosFijos`, cursor
 * (fechaAdquisicion, id) descendente).
 *
 * `valor_en_libros` es `GENERATED` en la base (CU-105, 3): **no se recalcula acá**. La
 * cuota de línea recta la calcula el backend; la pantalla muestra lo que devolvió.
 */
export type EstadoActivo = 'ACTIVO' | 'DADO_DE_BAJA' | 'VENDIDO'

export type ActivoFijo = {
  activoFijoId: string
  codigo: string
  descripcion: string
  categoriaNombre: string
  vidaUtilMeses: number
  fechaAdquisicion: string
  estado: EstadoActivo
  costoAdquisicion: Importe
  valorResidual: Importe
  depreciacionAcumulada: Importe
  valorEnLibros: Importe
  /** Cierto si ya tiene `depreciacion_activo` en el período que contiene hoy (`AP-CU105-01`). Lo resuelve el servidor. */
  depreciadoEnElPeriodoVigente: boolean
}

export type SalidaDepreciacion = {
  activoFijoId: string
  depreciacionId: string
  monto: string
  valorEnLibros: string
  totalmenteDepreciado: boolean
}

/** El valor en libros llegó al residual: el activo deja de generar cuota (`AP-CU105-02`). */
export function agotado(activo: ActivoFijo): boolean {
  return aCentavos(activo.valorEnLibros.monto) <= aCentavos(activo.valorResidual.monto)
}

/** Solo un activo en uso, no agotado y sin cuota en el período admite depreciarse. */
export function admiteDepreciacion(activo: ActivoFijo): boolean {
  return activo.estado === 'ACTIVO' && !agotado(activo) && !activo.depreciadoEnElPeriodoVigente
}

export function motivoParaNoDepreciar(activo: ActivoFijo, tienePermiso: boolean): string | null {
  if (!tienePermiso) return 'Depreciar exige CONTABILIDAD_ERP_ACTIVOS_FIJOS: esta cuenta no lo tiene.'
  if (activo.estado !== 'ACTIVO') return 'El activo está fuera de uso: no se deprecia.'
  if (agotado(activo)) return 'El valor en libros ya llegó al residual: no genera depreciación nueva.'
  if (activo.depreciadoEnElPeriodoVigente) return 'Ya tiene cuota calculada en este período.'
  return null
}

/** El inventario de activos, para la tabla: `GET /erp/activos` por cursor, de la adquisición más reciente. */
export function cargadorDeActivos(): CargadorDePagina<ActivoFijo> {
  return cargadorErpPorCursor<ActivoFijo>(inject(HttpClient), `${inject(GATEWAY)}/erp/activos`)
}

/** `POST /erp/activos/{activoId}/depreciaciones`, con clave nueva por intento. */
export function crearDepreciacion(): (activoId: string, periodoContableId: string) => Observable<SalidaDepreciacion> {
  const http = inject(HttpClient)
  const gateway = inject(GATEWAY)
  return (activoId, periodoContableId) =>
    http.post<SalidaDepreciacion>(`${gateway}/erp/activos/${activoId}/depreciaciones`, { periodoContableId }, { context: contextoConClave() })
}
