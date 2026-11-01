import { Injectable, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { NavigationEnd, Router } from '@angular/router'
import { filter, map } from 'rxjs'

/**
 * La URL de la vista, como señal, **una sola vez para todo el shell**. Antes cabecera, menú y pestañas se suscribían
 * cada uno por su lado a `router.events` (tres `toSignal` recalculando migas, entrada y pestaña en cada evento); ahora
 * leen esta, que solo cambia al terminar una navegación.
 */
@Injectable({ providedIn: 'root' })
export class UrlActual {
  private readonly router = inject(Router)
  readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  )
}
