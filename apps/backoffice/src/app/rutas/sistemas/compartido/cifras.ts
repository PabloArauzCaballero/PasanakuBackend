import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import type { NombreDeIcono } from '@aportaya/ui/icono/icono'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import type { Tono } from '@aportaya/ui/tono/tono'

/** Una cifra de la fila de arriba de una vista de sistemas. */
export type Cifra = { etiqueta: string; valor: string; icono: NombreDeIcono; tono?: Tono; pista?: string }

/**
 * La fila de cifras que encabeza cada vista de sistemas: una `ap-tarjeta-kpi` por cifra, en una grilla que pasa de
 * una columna en el celular a tantas como entren. Se calcula sobre todas las filas cargadas, no sobre lo filtrado.
 */
@Component({
  selector: 'ap-cifras-de-sistemas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TarjetaKPI],
  host: { role: 'group', '[attr.aria-label]': 'etiqueta()' },
  template: `
    @for (c of cifras(); track c.etiqueta) {
      <ap-tarjeta-kpi [etiqueta]="c.etiqueta" [valor]="c.valor" [icono]="c.icono" [tono]="c.tono ?? 'neutro'" [pista]="c.pista" />
    }
  `,
  styles: `:host { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 12rem), 1fr)); gap: var(--s3); } :host:empty { display: none; }`,
})
export class CifrasDeSistemas {
  readonly etiqueta = input('Resumen')
  readonly cifras = input.required<readonly Cifra[]>()
}

/** Cuántas filas cumplen la condición, como texto para una cifra. */
export function contar<T>(filas: readonly T[], condicion: (f: T) => boolean = () => true): string {
  return String(filas.filter(condicion).length)
}
