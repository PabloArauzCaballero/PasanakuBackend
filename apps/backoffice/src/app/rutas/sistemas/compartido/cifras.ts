import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import type { NombreDeIcono } from '@aportaya/ui/icono/icono'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import type { CargaDeListado } from '@aportaya/ui/listado/listado'
import type { Tono } from '@aportaya/ui/tono/tono'

/** Una cifra de la fila de arriba de una vista de sistemas. */
export type Cifra = { etiqueta: string; valor: string; icono: NombreDeIcono; tono?: Tono; pista?: string }

/**
 * La fila de cifras que encabeza cada vista de sistemas: una `ap-tarjeta-kpi` por cifra, en una grilla que pasa de
 * una columna en el celular a tantas como entren. Se calcula sobre todas las filas cargadas, no sobre lo filtrado.
 *
 * Mientras no hay filas (cargando o con error) cada cifra dice «—», no «0»: con la carga caída, «0 caídos» afirmaba
 * algo que nadie midió (2026-10-27).
 */
@Component({
  selector: 'ap-cifras-de-sistemas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TarjetaKPI],
  host: { role: 'group', '[attr.aria-label]': 'etiqueta()' },
  template: `
    <!-- Sin fuente no hay nada que contar: ni ceros ni guiones, la pantalla ya lo explica abajo. -->
    @if (carga()?.tipo !== 'sin-fuente') {
      @for (c of cifras(); track c.etiqueta) {
        @if (conDatos()) {
          <ap-tarjeta-kpi [etiqueta]="c.etiqueta" [valor]="c.valor" [icono]="c.icono" [tono]="c.tono ?? 'neutro'" [pista]="c.pista" />
        } @else {
          <ap-tarjeta-kpi [etiqueta]="c.etiqueta" valor="—" [icono]="c.icono" tono="neutro" [pista]="carga()?.tipo === 'error' ? 'Sin datos' : 'Cargando…'" />
        }
      }
    }
  `,
  styles: `:host { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 9rem), 1fr)); gap: var(--s3); } :host:empty { display: none; }`,
})
export class CifrasDeSistemas {
  readonly etiqueta = input('Resumen')
  readonly cifras = input.required<readonly Cifra[]>()
  /** El estado de la carga de la pantalla; sin él, las cifras se muestran tal cual (consumidores previos). */
  readonly carga = input<CargaDeListado>()
  protected readonly conDatos = computed(() => (this.carga()?.tipo ?? 'lista') === 'lista')
}

/** Cuántas filas cumplen la condición, como texto para una cifra. */
export function contar<T>(filas: readonly T[], condicion: (f: T) => boolean = () => true): string {
  return String(filas.filter(condicion).length)
}
