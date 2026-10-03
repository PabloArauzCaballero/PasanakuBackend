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
    <button [type]="tipo()" [disabled]="deshabilitado() || cargando()" [attr.aria-busy]="cargando() || null" [attr.aria-pressed]="presionado() ?? null" (click)="pulsado.emit()">
      @if (cargando()) { <ap-girador etiqueta="Procesando" tamano="s4" /> }
      <span class="texto"><ng-content /></span>
    </button>
  `,
  styles: `
    :host { display: inline-block; --c: var(--verde-solido); --tinta: var(--sobre-verde-solido); }
    :host(.ancho), :host(.ancho) button { display: block; width: 100%; }
    button { position: relative; overflow: hidden; isolation: isolate; display: inline-flex; align-items: center; justify-content: center; gap: var(--s2); max-width: 100%; min-height: var(--area-tactil); padding: 0 var(--s5); border: var(--borde-fino) solid transparent; border-radius: var(--r-md); font: inherit; font-weight: 600; cursor: pointer; transition: transform var(--mov-micro) var(--curva-llega), box-shadow var(--mov-entrada) var(--curva-llega), background-color var(--mov-micro) ease-out, filter var(--mov-micro) ease-out; }
    .texto { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    button:disabled { cursor: not-allowed; }
    button:disabled:not([aria-busy]) { background: var(--surface-2); color: var(--text-3); border-color: var(--border); box-shadow: none; }
    :host(.t-sm) button { min-height: var(--area-tactil); padding: 0 var(--s3); font-size: .9em; }
    :host(.t-lg) button { min-height: calc(var(--s7) + var(--s2)); padding: 0 var(--s6); font-size: 1.05em; }

    /* Con relleno: degradado sutil del mismo color, filo de luz arriba y sombra en dos
       capas teñida con el color del botón. Al pasar el mouse se eleva; al apretar baja
       y la sombra se recoge. */
    :host(.v-primario), :host(.v-secundario), :host(.v-peligro) { --relleno: 1; }
    :host(.v-primario) { --c: var(--accent); --tinta: var(--accent-ink); }
    :host(.v-peligro) { --c: var(--rojo-solido); --tinta: var(--sobre-rojo-solido); }
    :host(.v-primario) button:is(:not(:disabled), [aria-busy]), :host(.v-secundario) button:is(:not(:disabled), [aria-busy]), :host(.v-peligro) button:is(:not(:disabled), [aria-busy]) {
      color: var(--tinta);
      background: linear-gradient(to bottom, color-mix(in oklab, var(--c), var(--white) 10%), color-mix(in oklab, var(--c), var(--ink) 10%));
      box-shadow: inset 0 var(--borde-fino) 0 color-mix(in oklab, var(--white) 28%, transparent), 0 var(--borde-desfase) var(--s1) color-mix(in oklab, var(--c) 30%, transparent), 0 var(--s2) calc(var(--s5) - var(--borde-desfase)) calc(-1 * var(--s1)) color-mix(in oklab, var(--c) 38%, transparent);
    }
    :host(.v-primario) button:hover:not(:disabled), :host(.v-secundario) button:hover:not(:disabled), :host(.v-peligro) button:hover:not(:disabled) {
      transform: translateY(calc(-1 * var(--borde-fino))); filter: saturate(1.06);
      box-shadow: inset 0 var(--borde-fino) 0 color-mix(in oklab, var(--white) 28%, transparent), 0 var(--borde-desfase) var(--s1) color-mix(in oklab, var(--c) 30%, transparent), 0 var(--s3) var(--s6) calc(-1 * var(--s1)) color-mix(in oklab, var(--c) 45%, transparent);
    }
    :host(.v-primario) button:active:not(:disabled), :host(.v-secundario) button:active:not(:disabled), :host(.v-peligro) button:active:not(:disabled) {
      transform: translateY(var(--borde-fino)) scale(.985);
      box-shadow: inset 0 var(--borde-fino) 0 color-mix(in oklab, var(--white) 18%, transparent), 0 var(--borde-fino) var(--borde-desfase) color-mix(in oklab, var(--c) 35%, transparent);
    }

    /* El primario tiene un brillo que lo cruza unas pocas veces, espaciado; cargando,
       cruza seguido. Con reducir movimiento lo apaga la regla global. */
    :host(.v-primario) button:not(:disabled)::after, button[aria-busy]::after {
      content: ''; position: absolute; inset: 0; z-index: -1; pointer-events: none;
      background: linear-gradient(110deg, transparent 35%, color-mix(in oklab, var(--white) 32%, transparent) 50%, transparent 65%);
      transform: translateX(-120%); animation: brillo var(--mov-brillo) var(--curva-llega) 600ms 4;
    }
    button[aria-busy]::after { animation: brillo calc(var(--mov-brillo) / 5) linear infinite; }
    @keyframes brillo { 0% { transform: translateX(-120%); } 20%, 100% { transform: translateX(120%); } }

    :host(.v-fantasma) button { background: transparent; color: var(--brand-texto); border-color: var(--brand); }
    :host(.v-fantasma) button:hover:not(:disabled) { background: var(--brand-bg); transform: translateY(calc(-1 * var(--borde-fino))); box-shadow: var(--sombra-1); }
    :host(.v-fantasma) button:active:not(:disabled) { transform: scale(.985); box-shadow: none; }
    /* Sin esto el fantasma deshabilitado se pintaba igual que el habilitado: la regla de
       la variante gana en especificidad a la de :disabled de arriba. */
    :host(.v-fantasma) button:disabled:not([aria-busy]), :host(.v-sobreVerde) button:disabled:not([aria-busy]) { background: transparent; color: var(--text-3); border-color: var(--border); box-shadow: none; }
    :host(.v-enlace) button { background: transparent; color: var(--brand-texto); padding: 0 var(--s2); text-decoration: underline; text-decoration-thickness: var(--borde-fino); text-underline-offset: .2em; transition: text-underline-offset var(--mov-micro) ease-out, color var(--mov-micro) ease-out; }
    :host(.v-enlace) button:hover:not(:disabled) { text-underline-offset: .32em; }
    :host(.v-enlace) button:disabled { background: transparent; border-color: transparent; color: var(--text-3); }
    :host(.v-sobreVerde) button { background: transparent; color: var(--sobre-verde-solido); border-color: var(--sobre-verde-solido); }
    :host(.v-sobreVerde) button:hover:not(:disabled) { background: color-mix(in oklab, var(--sobre-verde-solido) 12%, transparent); }
    :host(.v-sobreVerde) button:focus-visible { outline-color: var(--sobre-verde-solido); }
  `,
})
export class Boton {
  readonly variante = input<VarianteDeBoton>('secundario')
  readonly tamano = input<TamanoDeBoton>('base')
  readonly tipo = input<'button' | 'submit'>('button')
  readonly deshabilitado = input(false)
  readonly cargando = input(false)
  /** Botón de alternancia (un filtro, una pestaña de vista): anuncia cuál está activo sin depender del color. */
  readonly presionado = input<boolean | undefined>(undefined)
  readonly ancho = input(false)
  readonly pulsado = output<void>()
}
