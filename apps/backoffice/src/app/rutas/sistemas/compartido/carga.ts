import { computed, type ResourceRef, type Signal } from '@angular/core'
import type { CargaDeListado } from '@aportaya/ui/listado/listado'

/** Lo que trae el error de un recurso: `ErrorTraducido` del interceptor o el error tipado de la fuente no disponible. */
type ErrorConMensaje = { mensaje: string; trazaId?: string }

const MENSAJE_GENERICO = 'Algo salió mal de nuestro lado. Probá de nuevo en un momento.'

/**
 * El mensaje que se le muestra a la persona: nunca el error crudo (regla 95.2.3). `resource()` envuelve todo
 * rechazo que no es un `Error` en un `ResourceWrappedError` con el original en `cause` (Angular 22,
 * `encapsulateResourceError`): sin desenvolverlo se perdían el mensaje traducido y el código de seguimiento.
 */
export function mensajeDeError(e: unknown, generico = MENSAJE_GENERICO): { mensaje: string; trazaId?: string } {
  if (e instanceof Error && e.cause !== undefined && !('mensaje' in e)) return mensajeDeError(e.cause, generico)
  if (e && typeof e === 'object' && 'mensaje' in e && typeof (e as ErrorConMensaje).mensaje === 'string') {
    const { mensaje, trazaId } = e as ErrorConMensaje
    return { mensaje, trazaId }
  }
  return { mensaje: generico }
}

/**
 * Traduce un `resource()`/`httpResource()` al estado que entiende `ap-listado`. Una recarga con datos
 * (`reloading`) sigue mostrando la lista: el listado no parpadea a esqueleto cada vez que se reintenta.
 */
export function cargaDe(recurso: ResourceRef<unknown>): Signal<CargaDeListado> {
  return computed<CargaDeListado>(() => {
    const estado = recurso.status()
    if (estado === 'error') {
      const sinFuente = fuenteNoDisponible(recurso.error())
      if (sinFuente) return { tipo: 'sin-fuente', mensaje: sinFuente }
      const { mensaje, trazaId } = mensajeDeError(recurso.error())
      return { tipo: 'error', mensaje, idPeticion: trazaId }
    }
    if (estado === 'loading' || (estado === 'reloading' && !recurso.hasValue())) return { tipo: 'cargando' }
    return { tipo: 'lista' }
  })
}

/** El mensaje de `ErrorFuenteNoDisponible` (la pantalla todavía no tiene contrato), desenvolviendo el error de `resource()`. */
function fuenteNoDisponible(e: unknown): string | undefined {
  if (e instanceof Error && e.cause !== undefined) return fuenteNoDisponible(e.cause)
  if (e && typeof e === 'object' && (e as { tipo?: unknown }).tipo === 'fuente-no-disponible') return (e as { mensaje: string }).mensaje
  return undefined
}

/** Las filas del recurso, o ninguna mientras carga o si falló (`value()` lanza en error). */
export function filasDe<T, R = readonly T[]>(recurso: ResourceRef<R | undefined>, extraer: (valor: R) => readonly T[] = (v) => v as unknown as readonly T[]): Signal<readonly T[]> {
  return computed(() => {
    if (!recurso.hasValue()) return []
    const v = recurso.value()
    return v === undefined ? [] : extraer(v)
  })
}
