import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'
import { ChipElegible } from '../chip-elegible/chip-elegible'
import type { NombreDeIcono } from '../icono/icono'

export type Filtro = { valor: string; texto: string; icono?: NombreDeIcono }

/**
 * Filtros de varias líneas, con ícono por chip y **sin «otros»**: cada tipo tiene su chip
 * o no se filtra. Selección múltiple; vacío = todos.
 */
@Component({
  selector: 'ap-chips-de-filtro',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipElegible],
  host: { role: 'group', '[attr.aria-label]': 'etiqueta()' },
  template: `
    @for (f of filtros(); track f.valor) {
      <ap-chip-elegible [icono]="f.icono" [elegido]="elegidos().includes(f.valor)" (elegidoChange)="alternar(f.valor, $event)">{{ f.texto }}</ap-chip-elegible>
    }
  `,
  styles: `:host { display: flex; flex-wrap: wrap; gap: var(--s2); }`,
})
export class ChipsDeFiltro {
  readonly etiqueta = input('Filtrar')
  readonly filtros = input.required<Filtro[]>()
  readonly elegidos = model<string[]>([])
  alternar(valor: string, elegido: boolean): void {
    const sin = this.elegidos().filter((v) => v !== valor)
    this.elegidos.set(elegido ? [...sin, valor] : sin)
  }
}
