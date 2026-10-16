import { signal } from '@angular/core'
import type { ErrorTraducido } from '../../../nucleo/errores'
import type { CargadorDePagina } from '../../../nucleo/tabla/tipos'
import { traducirError } from './errores-de-cumplimiento'

export type EstadoDeBandeja = 'cargando' | 'datos' | 'vacio' | 'error' | 'denegado'

/**
 * Los cuatro estados de una bandeja (+ denegado) alrededor del cargador de la tabla.
 *
 * `TablaDeDatosVirtualizada` (que no se toca) solo muestra «Cargando…» y no captura un
 * rechazo del cargador: sin este envoltorio un 403 o un 422 dejaban la tabla vacía y muda,
 * que se lee como «no hay nada». Acá el rechazo se convierte en estado visible y la tabla
 * recibe una página vacía. Solo la ÚLTIMA carga pedida escribe el estado: una respuesta
 * atrasada no pisa una más nueva.
 */
export function observarBandeja<T>(base: CargadorDePagina<T>) {
  const estado = signal<EstadoDeBandeja>('cargando')
  const error = signal<ErrorTraducido | null>(null)
  /** Cambia para volver a montar la tabla (reintentar, o refrescar tras una acción). */
  const montaje = signal(0)
  let ultima = 0

  const cargador: CargadorDePagina<T> = async (pedido) => {
    const esta = ++ultima
    estado.set('cargando')
    try {
      const pagina = await base(pedido)
      if (esta === ultima) {
        error.set(null)
        estado.set(pagina.filas.length === 0 && pedido.pagina <= 1 ? 'vacio' : 'datos')
      }
      return pagina
    } catch (e) {
      if (esta === ultima) {
        const traducido = traducirError(e)
        error.set(traducido)
        estado.set(traducido.estado === 403 ? 'denegado' : 'error')
      }
      return { filas: [], total: 0 }
    }
  }

  return {
    cargador,
    estado: estado.asReadonly(),
    error: error.asReadonly(),
    montaje: montaje.asReadonly(),
    recargar: () => montaje.update((v) => v + 1),
  }
}

/** Filtros de selección múltiple viajan en la tabla como una cadena separada por comas (la tabla solo admite cadenas). */
export function filtrosDe(elegidos: Readonly<Record<string, readonly string[]>>): Record<string, string> {
  const filtros: Record<string, string> = {}
  for (const [clave, valores] of Object.entries(elegidos)) if (valores.length) filtros[clave] = valores.join(',')
  return filtros
}
