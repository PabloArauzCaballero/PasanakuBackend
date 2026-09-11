import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Girador } from '../girador/girador'

export type VarianteDeBoton = 'primario' | 'secundario' | 'fantasma' | 'peligro' | 'enlace' | 'sobreVerde'
export type TamanoDeBoton = 'sm' | 'base' | 'lg'

/**
 * El único botón. **Naranja = acción** y hay uno por pantalla: el primario. Los demás
 * son verdes, fantasma o peligro. Cargando: mantiene el ancho, muestra el girador y
 * queda deshabilitado; el texto se acorta antes que desbordar.
 */
@Component({
  selector: 'ap-boton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Girador],
  host: { '[class]': '"v-" + variante() + " t-" + tamano()', '[class.ancho]': 'ancho()' },
  template: `
    <button [type]="tipo()" [disabled]="deshabilitado() || cargando()" [attr.aria-busy]="cargando() || null" (click)="pulsado.emit()">
      @if (cargando()) { <ap-girador etiqueta="Procesando" tamano="s4" /> }
      <span class="texto"><ng-content /></span>
    </button>
  `,
  styles: `
    :host { display: inline-block; }
    :host(.ancho), :host(.ancho) button { display: block; width: 100%; }
    button { display: inline-flex; align-items: center; justify-content: center; gap: var(--s2); max-width: 100%; min-height: var(--area-tactil); padding: 0 var(--s5); border: var(--borde-fino) solid transparent; border-radius: var(--r-md); font: inherit; font-weight: 600; cursor: pointer; transition: background-color .15s, transform .05s; }
    .texto { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    button:active:not(:disabled) { transform: translateY(var(--borde-fino)); }
    button:disabled { opacity: .5; cursor: not-allowed; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    :host(.t-sm) button { min-height: var(--area-tactil); padding: 0 var(--s3); font-size: .9em; }
    :host(.t-lg) button { min-height: calc(var(--s7) + var(--s2)); padding: 0 var(--s6); font-size: 1.05em; }
    :host(.v-primario) button { background: var(--accent); color: var(--accent-ink); }
    :host(.v-primario) button:hover:not(:disabled) { background: var(--o700); color: var(--white); }
    :host(.v-secundario) button { background: var(--verde-solido); color: var(--sobre-verde-solido); }
    :host(.v-secundario) button:hover:not(:disabled) { background: var(--brand-ink); }
    :host(.v-fantasma) button { background: transparent; color: var(--brand-texto); border-color: var(--brand); }
    :host(.v-fantasma) button:hover:not(:disabled) { background: var(--brand-bg); }
    :host(.v-peligro) button { background: var(--rojo-solido); color: var(--sobre-rojo-solido); }
    :host(.v-enlace) button { background: transparent; color: var(--brand-texto); padding: 0 var(--s2); text-decoration: underline; text-underline-offset: .2em; }
    :host(.v-sobreVerde) button { background: transparent; color: var(--sobre-verde-solido); border-color: var(--sobre-verde-solido); }
    :host(.v-sobreVerde) button:focus-visible { outline-color: var(--sobre-verde-solido); }
  `,
})
export class Boton {
  readonly variante = input<VarianteDeBoton>('secundario')
  readonly tamano = input<TamanoDeBoton>('base')
  readonly tipo = input<'button' | 'submit'>('button')
  readonly deshabilitado = input(false)
  readonly cargando = input(false)
  readonly ancho = input(false)
  readonly pulsado = output<void>()
}
