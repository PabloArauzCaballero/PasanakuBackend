import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Monto } from '../monto/monto'

/** Entradas, salidas y neto de un período. Tres cifras, todas por `ap-monto`. */
@Component({
  selector: 'ap-resumen-de-periodo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Monto],
  host: { role: 'group', '[attr.aria-label]': '"Resumen de " + periodo()' },
  template: `
    <p class="periodo">{{ periodo() }}</p>
    <dl>
      <div><dt>Entradas</dt><dd><ap-monto [monto]="entradas()" [moneda]="moneda()" etiqueta="Entradas" /></dd></div>
      <div><dt>Salidas</dt><dd><ap-monto [monto]="salidas()" [moneda]="moneda()" etiqueta="Salidas" /></dd></div>
      <div class="neto"><dt>Neto</dt><dd><ap-monto [monto]="neto()" [moneda]="moneda()" etiqueta="Neto" /></dd></div>
    </dl>
  `,
  styles: `
    :host { display: block; padding: var(--s4); border-radius: var(--r-lg); background: var(--surface-2); }
    .periodo { margin: 0 0 var(--s2); font-weight: 600; color: var(--text-2); }
    dl { display: flex; flex-wrap: wrap; gap: var(--s4); margin: 0; }
    div { flex: 1; min-width: calc(var(--s7) * 2); }
    dt { font-size: .85em; color: var(--text-3); }
    dd { margin: 0; color: var(--text); font-weight: 600; }
    .neto dd { color: var(--brand-texto); }
  `,
})
export class ResumenDePeriodo {
  readonly periodo = input.required<string>()
  readonly entradas = input.required<string>()
  readonly salidas = input.required<string>()
  readonly neto = input.required<string>()
  readonly moneda = input.required<string>()
}
