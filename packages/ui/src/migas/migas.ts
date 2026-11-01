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
    /* Una sola línea: si no entra, se acortan las migas intermedias, nunca se parten en dos renglones. */
    ol { display: flex; flex-wrap: nowrap; align-items: center; gap: var(--s1); margin: 0; padding: 0; list-style: none; font: var(--t-cuerpo-chico); color: var(--text-3); white-space: nowrap; }
    li { display: inline-flex; align-items: center; gap: var(--s1); min-width: 0; }
    li a, li span { overflow: hidden; text-overflow: ellipsis; }
    li:last-child { flex: none; }
    a { color: inherit; text-decoration: none; border-radius: var(--r-sm); transition: color var(--mov-micro) ease-out; }
    /* El enlace mide el área táctil (44 px) sin engrosar la fila: el relleno vertical la completa y el margen negativo devuelve la altura de una línea. */
    li a { padding-block: calc((var(--area-tactil) - 1lh) / 2); margin-block: calc((1lh - var(--area-tactil)) / 2); }
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
