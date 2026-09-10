import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Boton } from '../boton/boton'
import { Monto } from '../monto/monto'

/**
 * Encabezado verde con el saldo. «Recargar» es el único naranja; «Retirar» se pinta
 * **sobre verde** (variante propia, contraste medido) y se apaga si no hay saldo.
 */
@Component({
  selector: 'ap-tarjeta-saldo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, Monto],
  host: { role: 'region', '[attr.aria-label]': 'etiqueta()' },
  template: `
    <p class="etiqueta">{{ etiqueta() }}</p>
    <p class="saldo"><ap-monto [monto]="saldo()" [moneda]="moneda()" [etiqueta]="etiqueta()" /></p>
    @if (retenido()) { <p class="retenido">Retenido: <ap-monto [monto]="retenido()!" [moneda]="moneda()" etiqueta="Retenido" /></p> }
    <div class="acciones">
      <ap-boton variante="primario" (pulsado)="recargar.emit()">Recargar</ap-boton>
      <ap-boton variante="sobreVerde" [deshabilitado]="saldo() === '0.00'" (pulsado)="retirar.emit()">Retirar</ap-boton>
    </div>
  `,
  styles: `
    :host { display: block; padding: var(--s5); border-radius: var(--r-lg); background: var(--verde-solido); color: var(--sobre-verde-solido); }
    p { margin: 0; }
    .etiqueta { opacity: .85; font-weight: 600; }
    .saldo { margin-top: var(--s1); font-size: 2em; font-weight: 600; }
    .retenido { margin-top: var(--s1); opacity: .85; font-size: .9em; }
    .acciones { display: flex; flex-wrap: wrap; gap: var(--s3); margin-top: var(--s4); }
  `,
})
export class TarjetaSaldo {
  readonly etiqueta = input('Saldo disponible')
  readonly saldo = input.required<string>()
  readonly moneda = input.required<string>()
  readonly retenido = input<string>()
  readonly recargar = output<void>()
  readonly retirar = output<void>()
}
