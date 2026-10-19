import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'
import { Icono } from '../icono/icono'
import { Tooltip } from '../tooltip/tooltip'

/**
 * Área de texto con la misma anatomía que `ap-campo`: etiqueta (con tooltip opcional), ayuda o
 * error vinculados por `aria-describedby`, y un contador cuando hay máximo. El error sustituye a
 * la ayuda y se anuncia.
 */
@Component({
  selector: 'ap-area-de-texto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono, Tooltip],
  host: { '[class.con-error]': '!!error()' },
  template: `
    <span class="rotulo">
      <label [for]="id">{{ etiqueta() }}</label>
      @if (tooltip()) { <ap-tooltip [texto]="tooltip()!"><button type="button" class="ayuda-boton" [attr.aria-label]="'Ayuda sobre ' + etiqueta()"><ap-icono nombre="info" tamano="s4" /></button></ap-tooltip> }
    </span>
    <textarea [id]="id" [value]="valor()" [placeholder]="marcador() ?? ''" [disabled]="deshabilitado()" [attr.maxlength]="maximo() ?? null" [attr.aria-invalid]="error() ? 'true' : null" [attr.aria-describedby]="descriptor()" (input)="valor.set($any($event.target).value)"></textarea>
    @if (maximo()) { <p class="contador" [id]="id + '-contador'">{{ valor().trim().length }} / {{ maximo() }}</p> }
    @if (error()) { <p class="error" [id]="id + '-error'" role="alert">{{ error() }}</p> }
    @else if (ayuda()) { <p class="ayuda" [id]="id + '-ayuda'">{{ ayuda() }}</p> }
  `,
  styles: `
    :host { display: block; }
    .rotulo { display: flex; align-items: center; gap: var(--s1); margin-bottom: var(--s1); }
    label { font: var(--t-campo-etiqueta); color: var(--text-2); }
    .ayuda-boton { display: inline-flex; align-items: center; justify-content: center; width: var(--s5); height: var(--s5); padding: 0; border: 0; border-radius: var(--r-pill); background: transparent; color: var(--text-3); cursor: help; }
    .ayuda-boton:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    textarea { width: 100%; min-height: calc(var(--s7) * 2 + var(--s6)); padding: var(--s2) var(--s3); border: var(--borde-fino) solid var(--borde-control); border-radius: var(--r-md); background: var(--field); color: var(--text); font: inherit; resize: vertical; }
    textarea::placeholder { color: var(--text-3); }
    textarea:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    textarea:disabled { background: var(--surface-2); color: var(--text-3); }
    :host(.con-error) textarea { border-color: var(--err); }
    p { margin: var(--s1) 0 0; font: var(--t-ayuda); }
    .ayuda, .contador { color: var(--text-3); } .contador { font-variant-numeric: tabular-nums; }
    .error { color: var(--err-texto); }
  `,
})
export class AreaDeTexto {
  private static secuencia = 0
  readonly id = `ap-area-${++AreaDeTexto.secuencia}`
  readonly etiqueta = input.required<string>()
  readonly valor = model('')
  readonly marcador = input<string>()
  readonly ayuda = input<string>()
  readonly tooltip = input<string>()
  readonly error = input<string>()
  readonly maximo = input<number>()
  readonly deshabilitado = input(false)
  protected readonly descriptor = computed(() =>
    [this.error() ? `${this.id}-error` : this.ayuda() ? `${this.id}-ayuda` : null, this.maximo() ? `${this.id}-contador` : null].filter(Boolean).join(' ') || null,
  )
}
