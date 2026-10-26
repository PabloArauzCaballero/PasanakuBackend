import { EnvironmentInjector, Injectable, inject, runInInjectionContext, type Type } from '@angular/core'
import { PreloadingStrategy, type CanMatchFn, type PartialMatchRouteSnapshot, type Route } from '@angular/router'
import { EMPTY, from, isObservable, firstValueFrom, switchMap, type Observable } from 'rxjs'

/**
 * Precarga en reposo de **lo que esta sesión puede abrir** (F2, 2026-10-26). Sin estrategia, cada clic del menú
 * recién ahí pedía el código de la sección (en la medición con la red a 150 ms: de 3 a 8 archivos y hasta 4,5 s).
 *
 * Por cada ruta perezosa corre sus propias guardias `canMatch` —las mismas que decidirían la navegación— y solo si la
 * dejan pasar espera a que el navegador esté ocioso y la baja. Lo que la guardia corta (Sistemas sin el rol, una
 * pestaña sin su permiso) no se descarga. El router vuelve a llamar a la estrategia después de cada navegación, así
 * que lo que se habilita al iniciar sesión se precarga entonces. No es seguridad: el servidor decide igual.
 */
@Injectable({ providedIn: 'root' })
export class PrecargaSegunSesion implements PreloadingStrategy {
  private readonly injector = inject(EnvironmentInjector)

  preload(route: Route, load: () => Observable<unknown>): Observable<unknown> {
    return from(this.permitida(route)).pipe(switchMap((ok) => (ok ? from(enReposo()).pipe(switchMap(() => load())) : EMPTY)))
  }

  private async permitida(route: Route): Promise<boolean> {
    for (const guardia of route.canMatch ?? []) {
      try {
        const r = runInInjectionContext(this.injector, () =>
          typeof guardia === 'function' && !esClase(guardia)
            ? (guardia as CanMatchFn)(route, [], SIN_INSTANTANEA)
            : (this.injector.get(guardia as Type<{ canMatch: CanMatchFn }>).canMatch(route, [], SIN_INSTANTANEA)),
        )
        const valor = isObservable(r) ? await firstValueFrom(r) : await r
        if (valor !== true) return false
      } catch {
        return false
      }
    }
    return true
  }
}

/**
 * La precarga no navega, así que no hay instantánea de la ruta: las guardias del backoffice deciden por la sesión y
 * no la leen. Una guardia que la necesitara y fallara cuenta como «no precargar» (el `catch` de arriba), nunca rompe.
 */
const SIN_INSTANTANEA = {} as PartialMatchRouteSnapshot

const esClase = (f: unknown) => typeof f === 'function' && /^class\b/.test(Function.prototype.toString.call(f))

/** El próximo momento ocioso del navegador (con tope, para que una pestaña ocupada no lo posponga para siempre). */
function enReposo(): Promise<void> {
  return new Promise((listo) => {
    if (typeof requestIdleCallback === 'function') requestIdleCallback(() => listo(), { timeout: 3000 })
    else setTimeout(listo, 0)
  })
}
