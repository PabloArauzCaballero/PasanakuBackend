import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Icono } from '../icono/icono'

export type Miga = { texto: string; ruta?: string }

/**
 * Migas de pan: «Inicio › Sección › Vista». Las migas con `ruta` son enlaces; la última es la
 * página actual (`aria-current="page"`). `@aportaya/ui` no depende del router: el enlace lleva
 * `href` para el lector de pantalla y el botón derecho, y emite `navegar` para que quien aloja
 * las migas navegue sin recargar.
 */
@Component({
  selector: 'ap-migas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { role: 'navigation', 'aria-label': 'Migas de pan' },
  template: `
    <ol>
      @for (m of migas(); track $index; let ultima = $last) {
        <li>
          @if (m.ruta && !ultima) {
            <a [href]="m.ruta" (click)="irA($event, m.ruta)">{{ m.texto }}</a>
          } @else {
            <span [attr.aria-current]="ultima ? 'page' : null">{{ m.texto }}</span>
          }
          @if (!ultima) { <ap-icono nombre="chevronDerecha" tamano="s4" class="separador" /> }
        </li>
      }
    </ol>
  `,
  styles: `
    :host { display: block; }
    ol { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s1); margin: 0; padding: 0; list-style: none; font: var(--t-ayuda); color: var(--text-3); }
    li { display: inline-flex; align-items: center; gap: var(--s1); min-width: 0; }
    a { color: inherit; text-decoration: none; border-radius: var(--r-sm); transition: color var(--mov-micro) ease-out; }
    a:hover { color: var(--text); text-decoration: underline; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    [aria-current='page'] { color: var(--text); font-weight: 600; }
    .separador { color: var(--border); flex: none; }
  `,
})
export class Migas {
  readonly migas = input.required<Miga[]>()
  readonly navegar = output<string>()

  irA(evento: Event, ruta: string): void {
    evento.preventDefault()
    this.navegar.emit(ruta)
  }
}
