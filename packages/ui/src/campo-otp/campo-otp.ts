import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core'

/**
 * Código de un solo uso en celdas. Un único `input` real (`one-time-code`, numérico):
 * las celdas son dibujo. Al completar, emite `completado`.
 */
@Component({
  selector: 'ap-campo-otp',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.con-error]': '!!error()' },
  template: `
    <label class="oculto" [for]="id">{{ etiqueta() }}</label>
    <div class="celdas" aria-hidden="true">
      @for (c of celdas(); track $index) { <span class="celda" [class.activa]="$index === valor().length">{{ c }}</span> }
    </div>
    <input [id]="id" type="text" inputmode="numeric" autocomplete="one-time-code" [attr.maxlength]="largo()" [value]="valor()" [attr.aria-invalid]="error() ? 'true' : null" [attr.aria-describedby]="error() ? id + '-error' : null" (input)="alEscribir($event)" />
    @if (error()) { <p [id]="id + '-error'" role="alert">{{ error() }}</p> }
  `,
  styles: `
    :host { position: relative; display: block; width: 100%; max-width: calc(var(--s7) * 6); }
    .oculto { position: absolute; width: var(--borde-fino); height: var(--borde-fino); overflow: hidden; clip: rect(0 0 0 0); }
    .celdas { display: flex; gap: var(--s2); }
    .celda { display: inline-flex; align-items: center; justify-content: center; flex: 1; min-height: calc(var(--s7) + var(--s2)); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); background: var(--field); font-family: var(--font-d); font-size: 1.4em; color: var(--text); }
    :host(:focus-within) .celda.activa { border-color: var(--brand); box-shadow: 0 0 0 var(--borde-foco) var(--g300); }
    :host(.con-error) .celda { border-color: var(--err); }
    input { position: absolute; inset: 0; width: 100%; opacity: 0; font-size: 1em; }
    p { margin: var(--s1) 0 0; font-size: .85em; color: var(--err-texto); }
  `,
})
export class CampoOTP {
  private static secuencia = 0
  readonly id = `ap-otp-${++CampoOTP.secuencia}`
  readonly etiqueta = input('Código de verificación')
  readonly largo = input(6)
  readonly valor = model('')
  readonly error = input<string>()
  readonly completado = output<string>()
  readonly celdas = computed(() => Array.from({ length: this.largo() }, (_, i) => this.valor()[i] ?? ''))
  alEscribir(e: Event): void {
    const limpio = (e.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, this.largo())
    ;(e.target as HTMLInputElement).value = limpio
    this.valor.set(limpio)
    if (limpio.length === this.largo()) this.completado.emit(limpio)
  }
}
