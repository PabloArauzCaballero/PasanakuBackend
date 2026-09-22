import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Monto } from '../monto/monto'

export type LineaDeCobro = { concepto: string; monto: string; detalle?: string }

/**
 * Las líneas de un cobro y el total. **El total viene del servidor**: acá no se suma
 * nada, se muestra. Si las líneas no cierran, es un defecto del API, no de la vista.
 */
@Component({
  selector: 'ap-desglose-de-cobro',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Monto],
  template: `
    <table>
      <caption>{{ titulo() }}</caption>
      <tbody>
        @for (l of lineas(); track l.concepto) {
          <tr><th scope="row">{{ l.concepto }} @if (l.detalle) { <small>{{ l.detalle }}</small> }</th><td><ap-monto [monto]="l.monto" [moneda]="moneda()" [etiqueta]="l.concepto" /></td></tr>
        }
      </tbody>
      <tfoot><tr><th scope="row">{{ nombreDelTotal() }}</th><td><ap-monto [monto]="total()" [moneda]="moneda()" [etiqueta]="nombreDelTotal()" /></td></tr></tfoot>
    </table>
  `,
  styles: `
    table { width: 100%; border-collapse: collapse; color: var(--text); }
    caption { text-align: left; font-weight: 600; color: var(--text-2); padding-bottom: var(--s2); }
    th { text-align: left; font-weight: 400; padding: var(--s2) 0; }
    td { text-align: right; padding: var(--s2) 0; }
    tbody tr { border-bottom: var(--borde-fino) solid var(--border); }
    small { display: block; color: var(--text-3); font-size: .8em; }
    tfoot th, tfoot td { padding-top: var(--s3); font-weight: 700; font-size: 1.1em; color: var(--brand-texto); }
  `,
})
export class DesgloseDeCobro {
  readonly titulo = input('Desglose')
  readonly lineas = input.required<LineaDeCobro[]>()
  readonly total = input.required<string>()
  readonly moneda = input.required<string>()
  readonly nombreDelTotal = input('Total a recibir')
}
