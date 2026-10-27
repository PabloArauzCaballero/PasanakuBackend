import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { formatearMonto } from '@aportaya/ui/monto/formatear'

/**
 * Un importe que el contrato entrega SIN moneda (ambigüedad A-7 del carril A: `reclamo_cliente`, `cierre_diario` y
 * `reverso_transaccion` no la modelan). No se supone BOB: se agrupa con el mismo formateador de `Monto` y se dice que
 * la moneda no está registrada, en texto (no solo por estilo).
 */
@Component({
  selector: 'ap-importe-sin-moneda',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="cifra">{{ cifra() }}</span> <span class="nota">· moneda no registrada</span>`,
  styles: `
    .cifra { white-space: nowrap; font-family: var(--font-d); font-variant-numeric: tabular-nums; }
    .nota { color: var(--text-2); font-size: .875rem; }
  `,
})
export class ImporteSinMoneda {
  readonly monto = input.required<string>()
  readonly cifra = computed(() => importeSinMoneda(this.monto()))
}

/** «-1240.50» → «-1.240,50»: la cadena del contrato agrupada, sin convertirla a número ni inventarle prefijo. */
export function importeSinMoneda(monto: string): string {
  return formatearMonto({ monto, moneda: '' }).replace(/^(-?) /, '$1')
}
