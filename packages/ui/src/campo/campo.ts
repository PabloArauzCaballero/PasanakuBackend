import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'

let secuencia = 0

/**
 * Campo de texto con etiqueta, ayuda y error. El error **sustituye** a la ayuda y se
 * anuncia (`aria-describedby` + `aria-invalid`). Foco con halo; éxito con borde ok.
 */
@Component({
  selector: 'ap-campo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.con-error]': '!!error()', '[class.con-exito]': 'exito()' },
  template: `
    <label [for]="id">{{ etiqueta() }}</label>
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
    label { display: block; margin-bottom: var(--s1); font-weight: 600; color: var(--text-2); }
    .caja { display: flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); background: var(--field); color: var(--text); }
    .caja:focus-within { border-color: var(--brand); box-shadow: 0 0 0 var(--borde-foco) var(--g300); }
    :host(.con-error) .caja { border-color: var(--err); }
    :host(.con-exito) .caja { border-color: var(--ok); }
    input { flex: 1; min-width: 0; border: 0; background: transparent; color: inherit; font: inherit; outline: none; }
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
