import { EnvironmentInjector, Injectable, inject, runInInjectionContext, type Type } from '@angular/core'
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, PreloadingStrategy, RouteConfigLoadEnd, Router, type CanMatchFn, type PartialMatchRouteSnapshot, type Route } from '@angular/router'
import { EMPTY, Observable, filter, first, from, isObservable, firstValueFrom, switchMap } from 'rxjs'

/**
 * Precarga en reposo de **lo que esta sesión puede abrir** (F2, 2026-10-26). Sin estrategia, cada clic del menú
 * recién ahí pedía el código de la sección (en la medición con la red a 150 ms: de 3 a 8 archivos y hasta 4,5 s).
 *
 * Por cada ruta perezosa corre sus propias guardias `canMatch` —las mismas que decidirían la navegación— y solo si la
 * dejan pasar espera a que el navegador esté ocioso y la baja. Lo que la guardia corta (Sistemas sin el rol, una
 * pestaña sin su permiso) no se descarga. El router vuelve a llamar a la estrategia después de cada navegación, así
 * que lo que se habilita al iniciar sesión se precarga entonces. No es seguridad: el servidor decide igual.
 *
 * **De a `SIMULTANEAS` por vez.** Medido con la red estrangulada: si la persona hace clic mientras se precargan todas
 * las secciones a la vez, el código de la que pidió espera en la cola del navegador detrás de las otras (26 s con
 * 200 KB/s). Con un tope, su descarga compite con dos como mucho. El turno se libera cuando el router avisa que el
 * archivo de esa ruta llegó (`RouteConfigLoadEnd`), no cuando terminan sus hijas: esperar a las hijas, que hacen cola
 * detrás, trabaría la precarga. Y mientras hay una navegación en curso no arranca ninguna nueva: lo que la persona
 * pidió no hace cola detrás de lo que tal vez pida.
 */
@Injectable({ providedIn: 'root' })
export class PrecargaSegunSesion implements PreloadingStrategy {
  private readonly injector = inject(EnvironmentInjector)
  private enCurso = 0
  private readonly espera: (() => void)[] = []
  /** Mientras la persona navega, no arranca ninguna precarga nueva: su clic va primero. */
  private navegando = false
  private escuchando = false

  private escucharNavegacion(): void {
    if (this.escuchando) return
    this.escuchando = true
    this.injector.get(Router).events.subscribe((e) => {
      if (e instanceof NavigationStart) this.navegando = true
      else if (e instanceof NavigationEnd || e instanceof NavigationCancel || e instanceof NavigationError) {
        this.navegando = false
        while (this.enCurso < SIMULTANEAS && this.espera.length) this.espera.shift()!()
      }
    })
  }

  preload(route: Route, load: () => Observable<unknown>): Observable<unknown> {
    return from(this.permitida(route)).pipe(switchMap((ok) => (ok ? from(enReposo()).pipe(switchMap(() => this.conTurno(route, load))) : EMPTY)))
  }

  /** Corre `load` cuando hay turno libre y lo devuelve cuando llegó el archivo de `route` (o si la carga termina o falla). */
  private conTurno(route: Route, load: () => Observable<unknown>): Observable<unknown> {
    return new Observable((suscriptor) => {
      let liberado = false
      let interna: { unsubscribe(): void } | undefined
      let aviso: { unsubscribe(): void } | undefined
      const liberar = () => {
        if (liberado) return
        liberado = true
        aviso?.unsubscribe()
        this.enCurso--
        if (!this.navegando) this.espera.shift()?.()
      }
      const arrancar = () => {
        if (suscriptor.closed) return
        this.enCurso++
        aviso = this.injector
          .get(Router)
          .events.pipe(filter((e): e is RouteConfigLoadEnd => e instanceof RouteConfigLoadEnd && e.route === route), first())
          .subscribe(liberar)
        interna = load().subscribe({ next: (v) => (liberar(), suscriptor.next(v)), error: (e) => (liberar(), suscriptor.error(e)), complete: () => (liberar(), suscriptor.complete()) })
      }
      this.escucharNavegacion()
      if (this.enCurso < SIMULTANEAS && !this.navegando) arrancar()
      else this.espera.push(arrancar)
      return () => {
        interna?.unsubscribe()
        if (this.enCurso > 0 && !liberado && interna) liberar()
        const i = this.espera.indexOf(arrancar)
        if (i >= 0) this.espera.splice(i, 1)
      }
    })
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
/** Cuántas rutas se precargan a la vez. */
const SIMULTANEAS = 2

const SIN_INSTANTANEA = {} as PartialMatchRouteSnapshot

const esClase = (f: unknown) => typeof f === 'function' && /^class\b/.test(Function.prototype.toString.call(f))

/** El próximo momento ocioso del navegador (con tope, para que una pestaña ocupada no lo posponga para siempre). */
function enReposo(): Promise<void> {
  return new Promise((listo) => {
    if (typeof requestIdleCallback === 'function') requestIdleCallback(() => listo(), { timeout: 3000 })
    else setTimeout(listo, 0)
  })
}
