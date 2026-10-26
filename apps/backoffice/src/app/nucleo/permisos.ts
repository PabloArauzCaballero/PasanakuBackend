import { inject } from '@angular/core'
import { toObservable } from '@angular/core/rxjs-interop'
import { CanMatchFn, RedirectFunction, Router } from '@angular/router'
import { filter, map, take } from 'rxjs'
import { MENU, type Entrada } from './menu'
import { destinoDe } from './navegacion'
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
 * `canMatch` del shell: espera a que `Sesion` termine de resolver (`AuthBootstrap` corre
 * antes de esto por `provideAppInitializer`, pero un reintento manual desde la pantalla de
 * error puede dejar el estado en `RESTORING` mientras el usuario ya está navegando). En
 * `AUTHENTICATED` monta; en `ANONYMOUS` va al ingreso preservando la ruta pedida
 * (`volverA`); en `ERROR` va a la pantalla de arranque con errror, nunca a un login
 * silencioso mientras el estado es desconocido.
 */
export function requiereSesion(): CanMatchFn {
  return (_ruta, segmentos) => {
    const sesion = inject(Sesion)
    const router = inject(Router)

    return toObservable(sesion.estado).pipe(
      filter((estado) => estado !== 'UNKNOWN' && estado !== 'RESTORING'),
      take(1),
      map((estado) => {
        if (estado === 'AUTHENTICATED') return true
        const rutaPedida = '/' + segmentos.map((s) => s.path).join('/')
        if (estado === 'ERROR') return router.createUrlTree(['/arranque'], { queryParams: { volverA: rutaPedida } })
        return router.createUrlTree(['/ingreso'], { queryParams: { volverA: rutaPedida } })
      }),
    )
  }
}

/**
 * `redirectTo` de la raíz de una sección: a la primera pestaña que **esta** sesión puede abrir (`menu.ts`), no a una
 * fija que la guardia de esa pestaña rebotaría al tablero. Sin ninguna, al `respaldo` de siempre.
 */
export function aterrizaje(seccion: Entrada['ruta'], respaldo: string): RedirectFunction {
  return () => {
    const entrada = MENU.flatMap((g) => g.entradas).find((e) => e.ruta === seccion)
    return (entrada && destinoDe(entrada, inject(Sesion))) ?? respaldo
  }
}
