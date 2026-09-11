import { httpResource } from '@angular/common/http'
import { inject } from '@angular/core'
import { GATEWAY } from '../../../nucleo/gateway'

/**
 * CU-52 · Atender un reclamo en plazo (D-18: el Punto de Reclamo tiene puerta en la
 * app; el backoffice tenía la bandeja, esto es su lectura).
 *
 * **Supuesto declarado**: `servicios/cumplimiento/openapi/cumplimiento.yaml` reserva el
 * prefijo `/reclamos` (línea 5 del archivo) pero todavía no publica la operación de
 * bandeja (`GET /reclamos`) ni la de respuesta (`POST /reclamos/{id}/respuesta`). Los
 * campos de abajo son los que declara `SalidaCU52` en el propio CU-52, más `categoria`,
 * `canalIngreso` y `responsableId` que el flujo principal exige guardar. Se pide al
 * carril de `cumplimiento` (backend) escribir la operación; hasta entonces esta pantalla
 * llama al gateway con esta forma y el contrato real puede diferir en el nombre de ruta.
 */
export type EstadoReclamo = 'INGRESADO' | 'EN_ANALISIS' | 'RESPONDIDO' | 'CERRADO' | 'ELEVADO'

export type ReclamoDeBandeja = {
  reclamoId: string
  codigo: string
  categoria: 'COMISION' | 'OPERACION_NO_RECONOCIDA' | 'SALDO' | 'SERVICIO' | 'DATOS_PERSONALES' | 'GRUPO'
  canalIngreso: 'APP' | 'WEB' | 'TELEFONO' | 'PRESENCIAL' | 'CORREO'
  montoReclamado?: { monto: string; moneda: string }
  fechaIngreso: string
  plazoRespuesta: string
  diasHabilesPlazo: number
  estado: EstadoReclamo
  responsableId: string | null
  responsableNombre: string | null
}

/** La bandeja completa: la ordena por vencimiento quien la consume (regla del gate). */
export function bandejaDeReclamos() {
  const gateway = inject(GATEWAY)
  return httpResource<ReclamoDeBandeja[]>(() => `${gateway}/reclamos`)
}

/** Un reclamo vencido sin respuesta es el que manda en la cola (CU-52, 5c). */
export const vencido = (r: ReclamoDeBandeja, ahoraIso: string): boolean =>
  new Date(r.plazoRespuesta).getTime() < new Date(ahoraIso).getTime() && r.estado !== 'CERRADO' && r.estado !== 'RESPONDIDO'

/** Ordena por plazo de vencimiento ascendente: el que vence antes va primero. */
export function ordenadosPorVencimiento(reclamos: readonly ReclamoDeBandeja[]): ReclamoDeBandeja[] {
  return [...reclamos].sort((a, b) => new Date(a.plazoRespuesta).getTime() - new Date(b.plazoRespuesta).getTime())
}
