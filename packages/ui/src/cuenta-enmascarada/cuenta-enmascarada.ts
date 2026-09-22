import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

/** Cuenta bancaria mostrando solo los últimos cuatro. El lector oye «terminada en 1234». */
@Component({
  selector: 'ap-cuenta-enmascarada',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[attr.aria-label]': 'lectura()' },
  template: `<span aria-hidden="true">@if (banco()) { {{ banco() }} · }{{ texto() }}</span>`,
  styles: `:host { font-family: var(--font-d); font-variant-numeric: tabular-nums; color: var(--text); }`,
})
export class CuentaEnmascarada {
  readonly numero = input.required<string>()
  readonly banco = input<string>()
  readonly texto = computed(() => enmascarar(this.numero()))
  readonly lectura = computed(() => `${this.banco() ?? 'Cuenta'} terminada en ${this.texto().slice(-4)}`)
}

export function enmascarar(numero: string): string {
  const limpio = numero.replace(/\s/g, '')
  return `•••• ${limpio.slice(-4)}`
}
