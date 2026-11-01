import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'
import { Icono } from '../icono/icono'
import { Tooltip } from '../tooltip/tooltip'

let secuencia = 0

/**
 * Campo de texto con etiqueta, ayuda y error. El error **sustituye** a la ayuda y se
 * anuncia (`aria-describedby` + `aria-invalid`). Foco con halo; éxito con borde ok.
 */
@Component({
  selector: 'ap-campo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono, Tooltip],
  host: { '[class.con-error]': '!!error()', '[class.con-exito]': 'exito()' },
  template: `
    <span class="rotulo">
      <label [for]="id">{{ etiqueta() }}</label>
      @if (tooltip()) { <ap-tooltip [texto]="tooltip()!"><button type="button" class="ayuda-boton" [attr.aria-label]="'Ayuda sobre ' + etiqueta()"><ap-icono nombre="info" tamano="s4" /></button></ap-tooltip> }
    </span>
    <div class="caja">
      @if (prefijo()) { <span class="prefijo" aria-hidden="true">{{ prefijo() }}</span> }
      <input
        [id]="id"
        [type]="tipo()"
        [value]="valor()"
        [placeholder]="marcador() ?? ''"
        [disabled]="deshabilitado()"
        [attr.inputmode]="modoDeEntrada() ?? null"
        [attr.autocomplete]="autocompletar() ?? null"
        [attr.aria-invalid]="error() ? 'true' : null"
        [attr.aria-describedby]="descriptor()"
        (input)="valor.set(entrada($event))"
      />
      <ng-content select="[sufijo]" />
    </div>
    @if (error()) { <p class="error" [id]="id + '-error'" role="alert">{{ error() }}</p> }
    @else if (ayuda()) { <p class="ayuda" [id]="id + '-ayuda'">{{ ayuda() }}</p> }
  `,
  styles: `
    :host { display: block; }
    .rotulo { display: flex; align-items: center; gap: var(--s1); margin-bottom: var(--s1); }
    label { display: block; font-weight: 600; color: var(--text-2); }
    .ayuda-boton { display: inline-flex; align-items: center; justify-content: center; width: var(--area-tactil); height: var(--area-tactil); margin: calc((var(--s5) - var(--area-tactil)) / 2); padding: 0; border: 0; border-radius: var(--r-pill); background: transparent; color: var(--text-3); cursor: help; }
    /* El botón mide el área táctil (44 px); el margen negativo conserva el hueco de 24 px y el anillo de foco rodea el mismo círculo de siempre. */
    .ayuda-boton:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: calc(var(--borde-desfase) - (var(--area-tactil) - var(--s5)) / 2); }
    /* Posicionada: pinta (y recibe el clic) por encima de los 10 px que el botón de ayuda, ya de 44 px, le solapa. */
    .caja { position: relative; display: flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--borde-control); border-radius: var(--r-md); background: var(--field); color: var(--text); }
    .caja:focus-within { border-color: var(--brand); box-shadow: 0 0 0 var(--borde-foco) var(--g300); }
    :host(.con-error) .caja { border-color: var(--err); }
    :host(.con-exito) .caja { border-color: var(--ok); }
    /* width: 0 además de min-width: 0: sin él, el ancho intrínseco del input (~20 caracteres) entra en el mínimo del
       renglón y a 360 px el campo con botón desborda la pantalla. flex: 1 lo vuelve a estirar. */
    input { flex: 1; width: 0; min-width: 0; border: 0; background: transparent; color: inherit; font: inherit; outline: none; }
    input::placeholder { color: var(--text-3); }
    input:disabled { opacity: .5; }
    .prefijo { color: var(--text-3); font-family: var(--font-d); }
    p { margin: var(--s1) 0 0; font-size: .85em; }
    .ayuda { color: var(--text-3); }
    .error { color: var(--err-texto); }
  `,
})
export class Campo {
  readonly id = `ap-campo-${++secuencia}`
  readonly etiqueta = input.required<string>()
  readonly valor = model('')
  readonly tipo = input<'text' | 'email' | 'tel' | 'password' | 'search' | 'date' | 'number'>('text')
  readonly marcador = input<string>()
  readonly ayuda = input<string>()
  /** Ayuda ampliada por hover/foco; lo imprescindible va en `ayuda`, que siempre se ve. */
  readonly tooltip = input<string>()
  readonly error = input<string>()
  readonly exito = input(false)
  readonly prefijo = input<string>()
  readonly deshabilitado = input(false)
  readonly modoDeEntrada = input<string>()
  readonly autocompletar = input<string>()
  readonly descriptor = computed(() => (this.error() ? `${this.id}-error` : this.ayuda() ? `${this.id}-ayuda` : null))
  entrada(e: Event): string {
    return (e.target as HTMLInputElement).value
  }
}
