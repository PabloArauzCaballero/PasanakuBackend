import { inject } from '@angular/core'
import { CanMatchFn, Router } from '@angular/router'
import { Sesion } from './sesion'

/**
 * `canMatch` funcional por permiso: si el rol no lo tiene, la ruta **no monta** (regla 4
 * del shell — «el rol oculta, no protege»; el servidor es quien de verdad decide). Sin
 * permiso, se redirige al tablero en vez de mostrar una pantalla vacía sin explicación.
 */
export function requierePermiso(permiso: string): CanMatchFn {
  return () => {
    const sesion = inject(Sesion)
    if (sesion.puede(permiso)) return true
    const router = inject(Router)
    return router.parseUrl('/tablero')
  }
}
