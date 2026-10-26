import { formatearFecha } from '@aportaya/ui/fecha/fecha'
import { formatearMonto } from '@aportaya/ui/monto/formatear'
import { importeSinMoneda } from './importe-sin-moneda'

/**
 * Los valores de una fila **tal como se ven en la tabla**, para la búsqueda y la descarga CSV/JSON de las bandejas de
 * operación: el importe con su moneda («Bs 1.240,50»), nunca un número convertido; la fecha en hora de La Paz.
 * Un importe fuera del contrato no rompe la descarga: sale la cadena del contrato, que es lo que hay.
 */
export type ImporteConMoneda = { monto: string; moneda: string }

export function montoComoSeVe(d: ImporteConMoneda | null | undefined): string {
  if (!d) return ''
  try {
    return formatearMonto(d)
  } catch {
    return `${d.monto} ${d.moneda}`
  }
}

/** A-7: el contrato no registra la moneda. Se dice en el mismo texto, no se supone BOB. */
export function sinMonedaComoSeVe(monto: string | null | undefined): string {
  if (!monto) return ''
  try {
    return `${importeSinMoneda(monto)} (moneda no registrada)`
  } catch {
    return `${monto} (moneda no registrada)`
  }
}

export function fechaComoSeVe(iso: string | null | undefined): string {
  return iso ? formatearFecha(iso) : ''
}
