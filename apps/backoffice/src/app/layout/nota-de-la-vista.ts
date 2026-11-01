import { ChangeDetectionStrategy, Component, effect, inject, input, signal, untracked } from '@angular/core'
import { Icono } from '@aportaya/ui/icono/icono'
import { UrlActual } from './url-actual'

/**
 * «Sobre esta pantalla»: las notas de la vista detrás de un botón ⓘ en la cabecera fija, en un panel que flota.
 * Antes eran una franja plegada arriba de cada pantalla que repetía la descripción de la cabecera y empujaba el
 * contenido 70 px hacia abajo (2026-10-27). Al cambiar de pantalla el panel se cierra; `Escape` también lo cierra.
 */
@Component({
  selector: 'ap-nota-de-la-vista',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { '(keydown.escape)': 'abierta.set(false)' },
  template: `
    <button type="button" [attr.aria-expanded]="abierta()" aria-controls="nota-de-la-vista" aria-label="Sobre esta pantalla" title="Sobre esta pantalla" (click)="abierta.set(!abierta())">
      <ap-icono nombre="info" tamano="s4" />
    </button>
    @if (abierta()) {
      <div id="nota-de-la-vista" class="nota" role="note"><p>{{ texto() }}</p></div>
    }
  `,
  styles: `
    :host { position: relative; flex: none; display: inline-flex; }
    button { display: inline-flex; align-items: center; justify-content: center; width: var(--area-tactil); height: var(--area-tactil); border: 0; border-radius: var(--r-pill); background: transparent; color: var(--text-3); cursor: pointer; transition: color var(--mov-micro) ease-out, background-color var(--mov-micro) ease-out; }
    button:hover, button[aria-expanded='true'] { color: var(--brand-texto); background: var(--surface-2); }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .nota { position: absolute; top: calc(100% + var(--s2)); left: calc(var(--s7) * -4); z-index: 20; width: min(36rem, calc(100vw - var(--s6))); padding: var(--s3) var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-3); }
    .nota p { margin: 0; font: var(--t-cuerpo-chico); color: var(--text-2); }
  `,
})
export class NotaDeLaVista {
  readonly texto = input.required<string>()
  readonly abierta = signal(false)
  private readonly url = inject(UrlActual).url

  constructor() {
    effect(() => {
      this.url()
      untracked(() => this.abierta.set(false))
    })
  }
}
