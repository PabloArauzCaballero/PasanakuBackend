import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'
import { Tooltip } from '../tooltip/tooltip'
import { Icono } from '../icono/icono'

export type Opcion = { valor: string; texto: string; descripcion?: string }

/**
 * Lista desplegable nativa (teclado y lector de pantalla de serie) con la anatomía de `ap-campo`:
 * etiqueta, ayuda o error vinculados por `aria-describedby`, y un tooltip opcional al lado de la
 * etiqueta (el portal de Atlas exige una ayuda por campo). En un filtro, la opción vacía dice
 * «Todos»; en un formulario, «Elegí una opción». `compacta` es la variante para barras de filtros.
 */
@Component({
  selector: 'ap-lista-de-opciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Tooltip, Icono],
  host: { '[class.con-error]': '!!error()', '[class.compacta]': 'compacta()' },
  template: `
    <span class="etiqueta">
      <label [for]="id">{{ etiqueta() }}</label>
      @if (tooltip()) {
        <ap-tooltip [texto]="tooltip()!"><button type="button" class="ayuda-boton" [attr.aria-label]="'Ayuda sobre ' + etiqueta()"><ap-icono nombre="info" tamano="s4" /></button></ap-tooltip>
      }
    </span>
    <select [id]="id" [disabled]="deshabilitado()" [attr.aria-invalid]="error() ? 'true' : null" [attr.aria-describedby]="descriptor()" (change)="valor.set($any($event.target).value)">
      <option value="" [selected]="!valor()">{{ textoVacio() }}</option>
      @for (o of opciones(); track o.valor) { <option [value]="o.valor" [selected]="o.valor === valor()" [title]="o.descripcion ?? ''">{{ o.texto }}</option> }
    </select>
    @if (error()) { <p class="error" [id]="id + '-error'" role="alert">{{ error() }}</p> }
    @else if (ayuda()) { <p class="ayuda" [id]="id + '-ayuda'">{{ ayuda() }}</p> }
  `,
  styles: `
    :host { display: block; min-width: 0; }
    .etiqueta { display: flex; align-items: center; gap: var(--s1); margin-bottom: var(--s1); }
    label { font: var(--t-campo-etiqueta); color: var(--text-2); }
    .ayuda-boton { display: inline-flex; align-items: center; justify-content: center; width: var(--s5); height: var(--s5); padding: 0; border: 0; border-radius: var(--r-pill); background: transparent; color: var(--text-3); cursor: help; }
    .ayuda-boton:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    select { width: 100%; min-height: var(--area-tactil); padding: var(--s2) var(--s3); border: var(--borde-fino) solid var(--borde-control); border-radius: var(--r-md); background: var(--field); color: var(--text); font: inherit; }
    select:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    select:disabled { background: var(--surface-2); color: var(--text-3); cursor: not-allowed; }
    :host(.con-error) select { border-color: var(--err); }
    :host(.compacta) label { font: var(--t-ayuda); color: var(--text-3); }
    .ayuda { margin: var(--s1) 0 0; font: var(--t-ayuda); color: var(--text-3); }
    .error { margin: var(--s1) 0 0; font: var(--t-ayuda); color: var(--err-texto); }
  `,
})
export class ListaDeOpciones {
  private static secuencia = 0
  readonly id = `ap-lista-${++ListaDeOpciones.secuencia}`
  readonly etiqueta = input.required<string>()
  readonly opciones = input.required<readonly Opcion[]>()
  readonly valor = model('')
  readonly textoVacio = input('Elegí una opción')
  readonly ayuda = input<string>()
  readonly tooltip = input<string>()
  readonly error = input<string>()
  readonly deshabilitado = input(false)
  readonly compacta = input(false)
  protected readonly descriptor = computed(() => (this.error() ? `${this.id}-error` : this.ayuda() ? `${this.id}-ayuda` : null))
}
