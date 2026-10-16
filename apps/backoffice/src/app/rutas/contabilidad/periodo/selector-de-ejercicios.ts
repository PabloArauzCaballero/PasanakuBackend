import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { crearListadoDeEjercicios, type EjercicioFiscal } from '../dominio/cu100-periodos'
import { textosContabilidad } from '../textos'

/**
 * CU-100 · El selector de ejercicios fiscales (`GET /erp/ejercicios`, del más reciente al más antiguo). Cada ejercicio es
 * un ENLACE a `periodo/:ejercicioId`: elegir es una decisión de la persona y queda en la URL, nunca una que tome la
 * pantalla. Pagina con el cursor `antesDeAnio` que devuelve el servidor («Ver ejercicios anteriores»): no se calcula.
 */
@Component({
  selector: 'ap-selector-de-ejercicios',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, RouterLink],
  template: `
    <nav [attr.aria-label]="t.ejercicios">
      <h2>{{ t.ejercicios }}</h2>
      @if (error(); as e) {
        <ap-alerta tono="error">
          {{ e.mensaje }}
          @if (e.trazaId) { <span class="traza">Código de seguimiento: {{ e.trazaId }}</span> }
        </ap-alerta>
      }
      @if (ejercicios().length > 0) {
        <ul>
          @for (e of ejercicios(); track e.ejercicioId) {
            <li>
              <a [routerLink]="'/contabilidad/periodo/' + e.ejercicioId" [attr.aria-current]="e.ejercicioId === elegido() ? 'page' : null">{{
                t.ejercicio(e.anio, t.estados[e.estado])
              }}</a>
            </li>
          }
        </ul>
      } @else if (cargando()) {
        <p role="status">{{ t.cargandoEjercicios }}</p>
      } @else if (!error()) {
        <p class="nota">{{ t.sinEjercicios }}</p>
      }
      @if (proximoAntesDeAnio() !== null) {
        <ap-boton [cargando]="cargando()" (pulsado)="cargar()">{{ t.masEjercicios }}</ap-boton>
      }
    </nav>
  `,
  styles: `
    nav { display: grid; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.05em; }
    ul { list-style: none; margin: 0; padding: 0; display: flex; flex-wrap: wrap; gap: var(--s2) var(--s4); }
    a { display: inline-flex; align-items: center; min-height: var(--area-tactil); color: var(--text); }
    a[aria-current='page'] { font-weight: 600; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .nota { color: var(--text-2); margin: 0; }
    .traza { display: block; margin-top: var(--s1); color: var(--text-2); font-size: .85em; }
  `,
})
export class SelectorDeEjercicios {
  protected readonly t = textosContabilidad.periodo
  /** El ejercicio que la pantalla está mostrando, para marcarlo con `aria-current`. */
  readonly elegido = input<string | undefined>(undefined)
  private readonly pedir = crearListadoDeEjercicios()
  protected readonly ejercicios = signal<EjercicioFiscal[]>([])
  protected readonly proximoAntesDeAnio = signal<number | null>(null)
  protected readonly cargando = signal(false)
  protected readonly error = signal<ErrorTraducido | null>(null)

  constructor() {
    this.cargar()
  }

  /** Primera página o la siguiente, con el cursor que devolvió el servidor. Mientras pide, el botón queda bloqueado. */
  protected cargar(): void {
    if (this.cargando()) return
    this.cargando.set(true)
    this.error.set(null)
    this.pedir(this.proximoAntesDeAnio()).subscribe({
      next: (pagina) => {
        this.ejercicios.update((previos) => [...previos, ...(pagina.items ?? [])])
        this.proximoAntesDeAnio.set(pagina.proximoAntesDeAnio ?? null)
        this.cargando.set(false)
      },
      error: (e: ErrorTraducido) => {
        this.error.set(e)
        this.cargando.set(false)
      },
    })
  }
}
