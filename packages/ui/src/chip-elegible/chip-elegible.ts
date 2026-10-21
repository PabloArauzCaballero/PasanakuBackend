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
    button {
      display: inline-flex;
      align-items: center;
      gap: var(--s2);
      max-width: 100%;
      min-height: var(--area-tactil);
      padding: 0 var(--s4);
      border: var(--borde-fino) solid var(--borde-control);
      border-radius: var(--r-pill);
      background: var(--surface);
      color: var(--text);
      font: inherit;
      font-size: 0.875rem;
      font-weight: 500;
      cursor: pointer;
      transition: background-color var(--mov-micro) cubic-bezier(0.16, 1, 0.3, 1),
                  border-color var(--mov-micro) cubic-bezier(0.16, 1, 0.3, 1),
                  color var(--mov-micro) cubic-bezier(0.16, 1, 0.3, 1),
                  box-shadow var(--mov-micro) cubic-bezier(0.16, 1, 0.3, 1),
                  transform var(--mov-micro) cubic-bezier(0.16, 1, 0.3, 1);
    }
    button:hover:not(:disabled):not([aria-pressed="true"]) {
      background: var(--surface-2);
      border-color: var(--g400);
      transform: translateY(-0.0625rem);
      box-shadow: var(--sombra-2);
    }
    button[aria-pressed="true"] {
      background: var(--verde-solido);
      color: var(--sobre-verde-solido);
      border-color: var(--verde-solido);
      font-weight: 600;
      box-shadow: var(--sombra-2);
    }
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
