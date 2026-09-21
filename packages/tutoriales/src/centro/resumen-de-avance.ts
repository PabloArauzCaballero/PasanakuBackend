import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Progreso } from '@aportaya/ui/progreso/progreso'
import { textosAyuda } from './textos'

/**
 * Cuánto llevás hecho, arriba de todo. Dice el número además de pintar la barra: una
 * barra sola no se puede leer en voz alta ni comparar de un día para otro.
 */
@Component({
  selector: 'ap-resumen-de-avance',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Progreso],
  template: `
    <h2>{{ t.avanceTitulo }}</h2>
    <p class="cifra">{{ t.avanceDe(hechos(), total()) }}</p>
    <ap-progreso [valor]="fraccion()" [etiqueta]="t.avanceEtiqueta" [tono]="fraccion() === 1 ? 'ok' : 'marca'" />
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: var(--s2); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--brand-bg); }
    h2 { margin: 0; font: var(--t-titulo-3); letter-spacing: var(--t-titulo-3-track); color: var(--brand-texto); }
    .cifra { margin: 0; color: var(--text-2); font: var(--t-cuerpo); }
  `,
})
export class ResumenDeAvance {
  protected readonly t = textosAyuda
  readonly hechos = input.required<number>()
  readonly total = input.required<number>()
  protected readonly fraccion = computed(() => (this.total() === 0 ? 0 : this.hechos() / this.total()))
}
