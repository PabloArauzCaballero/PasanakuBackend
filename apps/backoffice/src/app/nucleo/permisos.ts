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

/**
 * `canMatch` del shell: sin sesión abierta no monta NINGUNA ruta del backoffice y se va
 * al ingreso. Antes el shell montaba siempre y un visitante sin cuenta llegaba a un
 * tablero vacío, sin forma de entrar.
 */
export function requiereSesion(): CanMatchFn {
  return () => {
    if (inject(Sesion).abierta()) return true
    return inject(Router).parseUrl('/ingreso')
  }
}
