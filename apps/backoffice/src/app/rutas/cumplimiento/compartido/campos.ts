import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'

let secuencia = 0

const ESTILOS = `
  :host { display: block; }
  label { display: block; margin-bottom: var(--s1); font-weight: 600; color: var(--text-2); }
  textarea, select, input { width: 100%; min-height: var(--area-tactil); padding: var(--s2) var(--s3); border: var(--borde-fino) solid var(--borde-control); border-radius: var(--r-md); background: var(--field); color: var(--text); font: inherit; }
  textarea { min-height: 8rem; resize: vertical; }
  textarea:focus-visible, select:focus-visible, input:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  :host(.con-error) textarea, :host(.con-error) select, :host(.con-error) input { border-color: var(--err); }
  .ayuda { margin: var(--s1) 0 0; color: var(--text-3); font-size: .9em; }
  .error { margin: var(--s1) 0 0; color: var(--err); font-size: .9em; }
  .contador { margin: var(--s1) 0 0; color: var(--text-3); font-size: .85em; font-variant-numeric: tabular-nums; }
`

/**
 * Área de texto con la misma anatomía que `ap-campo` de `@aportaya/ui` (etiqueta, ayuda, error
 * vinculado por `aria-describedby`): `@aportaya/ui` no tiene una, y este lote no toca `packages/`.
 */
@Component({
  selector: 'ap-area-de-texto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.con-error]': '!!error()' },
  template: `
    <label [for]="id">{{ etiqueta() }}</label>
    <textarea [id]="id" [value]="valor()" [attr.maxlength]="maximo() ?? null" [attr.aria-invalid]="error() ? 'true' : null" [attr.aria-describedby]="descriptor()" (input)="valor.set($any($event.target).value)"></textarea>
    @if (maximo()) { <p class="contador" [id]="id + '-contador'">{{ valor().trim().length }} / {{ maximo() }}</p> }
    @if (error()) { <p class="error" [id]="id + '-error'" role="alert">{{ error() }}</p> }
    @else if (ayuda()) { <p class="ayuda" [id]="id + '-ayuda'">{{ ayuda() }}</p> }
  `,
  styles: ESTILOS,
})
export class AreaDeTexto {
  readonly id = `ap-area-${++secuencia}`
  readonly etiqueta = input.required<string>()
  readonly valor = model('')
  readonly ayuda = input<string>()
  readonly error = input<string>()
  readonly maximo = input<number>()
  protected readonly descriptor = computed(() =>
    [this.error() ? `${this.id}-error` : this.ayuda() ? `${this.id}-ayuda` : null, this.maximo() ? `${this.id}-contador` : null].filter(Boolean).join(' ') || null,
  )
}

/** Fecha y hora local (`datetime-local`): `ap-campo` no admite ese tipo. La zona la fija el dominio (`aIsoLaPaz`). */
@Component({
  selector: 'ap-fecha-y-hora',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.con-error]': '!!error()' },
  template: `
    <label [for]="id">{{ etiqueta() }}</label>
    <input type="datetime-local" [id]="id" [value]="valor()" [attr.aria-invalid]="error() ? 'true' : null" [attr.aria-describedby]="error() ? id + '-error' : null" (input)="valor.set($any($event.target).value)" />
    @if (error()) { <p class="error" [id]="id + '-error'" role="alert">{{ error() }}</p> }
  `,
  styles: ESTILOS,
})
export class FechaYHora {
  readonly id = `ap-fecha-hora-${++secuencia}`
  readonly etiqueta = input.required<string>()
  readonly valor = model('')
  readonly error = input<string>()
}

export type Opcion = { valor: string; texto: string }

/** Lista desplegable nativa (teclado y lector de pantalla de serie), con la anatomía de `ap-campo`. */
@Component({
  selector: 'ap-lista-de-opciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.con-error]': '!!error()' },
  template: `
    <label [for]="id">{{ etiqueta() }}</label>
    <select [id]="id" [attr.aria-invalid]="error() ? 'true' : null" [attr.aria-describedby]="error() ? id + '-error' : ayuda() ? id + '-ayuda' : null" (change)="valor.set($any($event.target).value)">
      <option value="" [selected]="!valor()">Elegí una opción</option>
      @for (o of opciones(); track o.valor) { <option [value]="o.valor" [selected]="o.valor === valor()">{{ o.texto }}</option> }
    </select>
    @if (error()) { <p class="error" [id]="id + '-error'" role="alert">{{ error() }}</p> }
    @else if (ayuda()) { <p class="ayuda" [id]="id + '-ayuda'">{{ ayuda() }}</p> }
  `,
  styles: ESTILOS,
})
export class ListaDeOpciones {
  readonly id = `ap-lista-${++secuencia}`
  readonly etiqueta = input.required<string>()
  readonly opciones = input.required<readonly Opcion[]>()
  readonly valor = model('')
  readonly ayuda = input<string>()
  readonly error = input<string>()
}

/** Opciones de una lista a partir de un diccionario de textos del contrato. */
export const opcionesDe = (textos: Readonly<Record<string, string>>): Opcion[] => Object.entries(textos).map(([valor, texto]) => ({ valor, texto }))
