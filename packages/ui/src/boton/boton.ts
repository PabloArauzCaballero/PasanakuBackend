import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Girador } from '../girador/girador'
import { Icono, type NombreDeIcono } from '../icono/icono'

export type VarianteDeBoton = 'primario' | 'secundario' | 'fantasma' | 'peligro' | 'enlace' | 'sobreVerde'
export type TamanoDeBoton = 'sm' | 'base' | 'lg'

/**
 * El único botón. **El color de acción (`--accent`) = acción** y hay uno por pantalla: el
 * primario. Los demás son verde sólido, fantasma o peligro; todos planos. Cargando: mantiene el ancho, muestra el girador y
 * queda deshabilitado; el texto se acorta antes que desbordar.
 */
@Component({
  selector: 'ap-boton',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Girador, Icono],
  host: { '[class]': '"v-" + variante() + " t-" + tamano()', '[class.ancho]': 'ancho()' },
  template: `
    <button [type]="tipo()" [disabled]="deshabilitado() || cargando()" [attr.aria-busy]="cargando() || null" [attr.aria-pressed]="presionado() ?? null" (click)="pulsado.emit()">
      @if (cargando()) { <ap-girador etiqueta="Procesando" tamano="s4" /> } @else if (icono()) { <ap-icono [nombre]="icono()!" tamano="s4" /> }
      <span class="texto"><ng-content /></span>
    </button>
  `,
  styles: `
    :host { display: inline-block; --c: var(--verde-solido); --tinta: var(--sobre-verde-solido); }
    :host(.ancho), :host(.ancho) button { display: block; width: 100%; }
    button { position: relative; overflow: hidden; isolation: isolate; display: inline-flex; align-items: center; justify-content: center; gap: var(--s2); max-width: 100%; min-height: var(--area-tactil); padding: 0 var(--s5); border: var(--borde-fino) solid transparent; border-radius: var(--r-md); font: inherit; font-weight: 600; cursor: pointer; transition: transform var(--mov-micro) var(--curva-llega), box-shadow var(--mov-entrada) var(--curva-llega), background-color var(--mov-micro) ease-out, filter var(--mov-micro) ease-out; }
    /* Por omisión el texto se acorta antes que desbordar; quien aloja el botón en un espacio angosto puede
       pedir que se parta en líneas con --boton-texto-ajuste: normal (el diálogo lo hace). */
    .texto { overflow: hidden; text-overflow: ellipsis; white-space: var(--boton-texto-ajuste, nowrap); }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    button:disabled { cursor: not-allowed; }
    button:disabled:not([aria-busy]) { background: var(--surface-2); color: var(--text-3); border-color: var(--border); box-shadow: none; }
    :host(.t-sm) button { min-height: var(--area-tactil); padding: 0 var(--s3); font-size: .9em; }
    :host(.t-lg) button { min-height: calc(var(--s7) + var(--s2)); padding: 0 var(--s6); font-size: 1.05em; }

    /* Con relleno: color plano y una sombra neutra mínima, la identidad institucional que
       ya tiene la app (2026-10-15-visual-bancario). Sin degradé, sin brillo que cruza y
       sin sombra teñida: al pasar el mouse se oscurece apenas; al apretar, baja. */
    :host(.v-primario), :host(.v-secundario), :host(.v-peligro) { --relleno: 1; }
    :host(.v-primario) { --c: var(--accent); --tinta: var(--accent-ink); }
    :host(.v-peligro) { --c: var(--rojo-solido); --tinta: var(--sobre-rojo-solido); }
    :host(.v-primario) button:is(:not(:disabled), [aria-busy]), :host(.v-secundario) button:is(:not(:disabled), [aria-busy]), :host(.v-peligro) button:is(:not(:disabled), [aria-busy]) {
      color: var(--tinta); background: var(--c); box-shadow: var(--sombra-1);
    }
    :host(.v-primario) button:hover:not(:disabled), :host(.v-secundario) button:hover:not(:disabled), :host(.v-peligro) button:hover:not(:disabled) {
      background: color-mix(in oklab, var(--c), var(--ink) 12%);
    }
    :host(.v-primario) button:active:not(:disabled), :host(.v-secundario) button:active:not(:disabled), :host(.v-peligro) button:active:not(:disabled) {
      transform: scale(.985); box-shadow: none;
    }

    :host(.v-fantasma) button { background: transparent; color: var(--brand-texto); border-color: var(--brand); }
    :host(.v-fantasma) button:hover:not(:disabled) { background: var(--brand-bg); }
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
  /** Ícono decorativo antes del texto (ícono + nombre, nunca ícono solo): mientras carga lo reemplaza el girador. */
  readonly icono = input<NombreDeIcono>()
  readonly pulsado = output<void>()
}
