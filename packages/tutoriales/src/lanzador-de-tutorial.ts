import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { NavigationEnd, Router } from '@angular/router'
import { filter, map, startWith } from 'rxjs'
import { Boton } from '@aportaya/ui/boton/boton'
import { MotorDeTutoriales } from './motor'
import { ProgresoDeTutoriales } from './progreso'
import { RegistroDeTutoriales } from './registro'
import { sePuedeContinuar } from './avance'
import { textosDelRecorrido } from './textos-del-recorrido'

/**
 * **El tour contextual**: un botón en la cabecera que abre el tutorial de la pantalla
 * en la que uno está. Aparece solo si hay uno para esa ruta —no se ofrece ayuda que no
 * existe— y dice «Continuar» si quedó algo a medias.
 *
 * Está en la cabecera y no dentro de cada pantalla a propósito: así ninguna pantalla
 * tiene que saber que los tutoriales existen, y agregar el tutorial de una pantalla
 * nueva no toca esa pantalla.
 */
@Component({
  selector: 'ap-lanzador-de-tutorial',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton],
  template: `
    @if (tutorial(); as t) {
      <ap-boton variante="fantasma" tamano="sm" (pulsado)="abrir()">
        {{ continuar() ? 'Continuar el tutorial' : textos.lanzador }}
      </ap-boton>
    }
  `,
  styles: `:host { display: inline-flex; }`,
})
export class LanzadorDeTutorial {
  protected readonly textos = textosDelRecorrido
  private readonly registro = inject(RegistroDeTutoriales)
  private readonly motor = inject(MotorDeTutoriales)
  private readonly progresos = inject(ProgresoDeTutoriales)
  private readonly router = inject(Router)

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  )

  protected readonly tutorial = computed(() => this.registro.paraLaRuta(this.url()))
  protected readonly continuar = computed(() => {
    const t = this.tutorial()
    return t !== undefined && sePuedeContinuar(this.progresos.de(t.id), t)
  })

  constructor() {
    // El catálogo llega por `import()` y el avance por el almacén: los pide quien de
    // verdad los necesita, no el shell. Hasta que lleguen, el botón simplemente no está.
    void this.registro.cargar()
    this.progresos.cargar()
  }

  protected abrir(): void {
    const t = this.tutorial()
    if (t === undefined) return
    const progreso = this.progresos.de(t.id)
    if (this.continuar() && progreso !== undefined) void this.motor.continuar(t.id, progreso)
    else void this.motor.iniciar(t.id, progreso?.repeticiones ?? 0)
  }
}
