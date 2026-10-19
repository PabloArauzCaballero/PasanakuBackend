import { ChangeDetectionStrategy, Component, input } from '@angular/core'

/**
 * El símbolo de AportaYa (`landing/assets/img/simbolo.svg`): las patas de la apacheta en
 * verde y el vértice del «Ya» en naranja. Los trazos son los del original; el color sale
 * de los tokens para que se lea en fondo claro y en el panel oscuro (`sobreOscuro`).
 * Decorativo: el nombre de la marca siempre va escrito al lado.
 */
@Component({
  selector: 'ap-simbolo-de-marca',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { 'aria-hidden': 'true', '[class.sobre-oscuro]': 'sobreOscuro()' },
  template: `
    <svg viewBox="0 0 200 200" fill="none" stroke-linecap="round">
      <path class="pata" d="M92 30 C74 66 52 108 40 168" stroke-width="13" />
      <path class="pata" d="M108 30 C126 66 148 108 160 168" stroke-width="13" />
      <path class="pata" d="M46 158 C86 140 114 140 154 158" stroke-width="12" />
      <path class="vertice" d="M100 58 C84 92 68 130 62 170" stroke-width="12" />
      <path class="vertice" d="M100 58 C116 92 132 130 138 170" stroke-width="12" />
      <path class="vertice" d="M70 150 C100 134 100 134 130 150" stroke-width="10" />
    </svg>
  `,
  styles: `
    :host { display: inline-flex; width: var(--tamano-simbolo, 2.5rem); height: var(--tamano-simbolo, 2.5rem); flex: none; }
    svg { width: 100%; height: 100%; }
    .pata { stroke: var(--g600); }
    .vertice { stroke: var(--o500); }
    :host(.sobre-oscuro) .pata { stroke: var(--g200); }
    :host(.sobre-oscuro) .vertice { stroke: var(--o300); }
  `,
})
export class SimboloDeMarca {
  readonly sobreOscuro = input(false)
}
