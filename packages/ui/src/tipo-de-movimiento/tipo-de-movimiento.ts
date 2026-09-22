import type { NombreDeIcono } from '../icono/icono'

/** Los tipos que el extracto distingue. **El ícono dice qué pasó** (regla 1), el signo lo dice el monto. */
export type TipoDeMovimiento = 'aporte' | 'entrega' | 'recarga' | 'retiro' | 'mora' | 'comision'

export const ICONO_DE_MOVIMIENTO: Record<TipoDeMovimiento, NombreDeIcono> = {
  aporte: 'aporte',
  entrega: 'entrega',
  recarga: 'recarga',
  retiro: 'retiro',
  mora: 'mora',
  comision: 'comision',
}

export const NOMBRE_DE_MOVIMIENTO: Record<TipoDeMovimiento, string> = {
  aporte: 'Aporte',
  entrega: 'Entrega',
  recarga: 'Recarga',
  retiro: 'Retiro',
  mora: 'Mora',
  comision: 'Comisión',
}
