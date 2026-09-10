import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'
import { Icono, NombreDeIcono } from '../icono/icono'

/**
 * Un chip que se elige. **Elegido = relleno de marca**, no un cambio sutil de fondo
 * (regla 3 de la maqueta). Es un botón con `aria-pressed`, área táctil completa.
 */
@Component({
  selector: 'ap-chip-elegible',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  template: `
    <button type="button" [attr.aria-pressed]="elegido()" [disabled]="deshabilitado()" (click)="elegido.set(!elegido())">
      @if (icono()) { <ap-icono [nombre]="icono()!" tamano="s4" /> }
      <span class="texto"><ng-content /></span>
    </button>
  `,
  styles: `
    :host { display: inline-block; max-width: 100%; }
    button { display: inline-flex; align-items: center; gap: var(--s2); max-width: 100%; min-height: var(--area-tactil); padding: 0 var(--s4); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-pill); background: var(--surface); color: var(--text); font: inherit; cursor: pointer; }
    button[aria-pressed="true"] { background: var(--verde-solido); color: var(--sobre-verde-solido); border-color: var(--verde-solido); font-weight: 600; }
    button:disabled { opacity: .5; cursor: not-allowed; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .texto { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `,
})
export class ChipElegible {
  readonly elegido = model(false)
  readonly icono = input<NombreDeIcono>()
  readonly deshabilitado = input(false)
}
