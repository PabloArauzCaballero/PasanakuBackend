import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { formatearMonto, montoParaLectura } from './formatear'

/**
 * El único lugar de la web donde se dibuja dinero: `tabular-nums`, prefijo `Bs`, coma
 * decimal → `Bs 1.240,00`. Recibe el importe como cadena del contrato y no lo convierte.
 */
@Component({
  selector: 'ap-monto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[attr.aria-label]': 'lectura()', class: 'ap-monto' },
  template: `<span aria-hidden="true">{{ texto() }}</span>`,
  styles: `
    :host { font-family: var(--font-d); font-variant-numeric: tabular-nums; white-space: nowrap; }
    :host(.negativo) { color: var(--err-texto); }
  `,
})
export class Monto {
  readonly monto = input.required<string>()
  readonly moneda = input.required<string>()
  /** Lo que el lector dice antes de la cifra («Saldo disponible»). */
  readonly etiqueta = input<string>()
  readonly texto = computed(() => formatearMonto({ monto: this.monto(), moneda: this.moneda() }))
  readonly lectura = computed(() => montoParaLectura({ monto: this.monto(), moneda: this.moneda() }, this.etiqueta()))
}
