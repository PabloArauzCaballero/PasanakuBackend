import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Monto } from '../monto/monto'

/**
 * Un número con nombre. Si es dinero va por `ap-monto`; la variación dice **qué pasó**
 * («12 más que ayer»), no una flecha.
 */
@Component({
  selector: 'ap-tarjeta-kpi',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Monto],
  host: { role: 'group', '[attr.aria-label]': 'etiqueta()' },
  template: `
    <p class="etiqueta">{{ etiqueta() }}</p>
    <p class="valor">
      @if (moneda()) { <ap-monto [monto]="valor()" [moneda]="moneda()!" [etiqueta]="etiqueta()" /> } @else { {{ valor() }} }
    </p>
    @if (variacion()) { <p class="variacion">{{ variacion() }}</p> }
  `,
  styles: `
    :host { display: block; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    p { margin: 0; }
    .etiqueta { color: var(--text-2); font-weight: 600; font-size: .9em; }
    .valor { margin-top: var(--s1); font-family: var(--font-d); font-size: 1.6em; font-weight: 600; color: var(--text); font-variant-numeric: tabular-nums; }
    .variacion { margin-top: var(--s1); color: var(--text-3); font-size: .85em; }
  `,
})
export class TarjetaKPI {
  readonly etiqueta = input.required<string>()
  readonly valor = input.required<string>()
  readonly moneda = input<string>()
  readonly variacion = input<string>()
}
