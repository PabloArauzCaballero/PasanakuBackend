import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Fecha } from '@aportaya/ui/fecha/fecha'

export type EventoDeEvidencia = { id: string; cuando: string; actor: string; descripcion: string; trazaId?: string }

/**
 * Bitácora, movimientos y trazas de un caso — reclamos, disputas y descargos la
 * reutilizan tal cual. Es una cronología simple con semántica de `<ol>`: cada evento es
 * un hecho ordenado en el tiempo, del más nuevo al más viejo.
 *
 * **Hueco declarado:** la ficha F6 pide componerlo con `LineaDeTiempo` de `@aportaya/ui`,
 * pero ese organismo no existe en `packages/ui/src` (relevado el 2026-09-11: no hay
 * `linea-de-tiempo` ni equivalente entre los organismos ya congelados por TF.2/TF.3).
 * `packages/ui` es de solo lectura para este carril, así que no se crea acá — queda
 * como hueco para quien posea `packages/ui` (carril `F1-W` o quien lo suceda). Mientras
 * tanto este panel usa una lista semántica propia, con los mismos tokens del sistema de
 * diseño, para no bloquear a `F7`/`F8`.
 */
@Component({
  selector: 'ap-panel-de-evidencia',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Fecha],
  host: { role: 'region', '[attr.aria-label]': 'titulo()' },
  template: `
    <h3>{{ titulo() }}</h3>
    @if (eventos().length === 0) {
      <p class="vacio">{{ mensajeVacio() }}</p>
    } @else {
      <ol>
        @for (e of eventos(); track e.id) {
          <li>
            <p class="cuando"><ap-fecha [iso]="e.cuando" /> · {{ e.actor }}</p>
            <p class="descripcion">{{ e.descripcion }}</p>
            @if (e.trazaId) { <p class="traza">Traza: {{ e.trazaId }}</p> }
          </li>
        }
      </ol>
    }
  `,
  styles: `
    :host { display: block; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    h3 { font-size: 1.05em; color: var(--text); margin: 0 0 var(--s3); }
    .vacio { color: var(--text-2); margin: 0; }
    ol { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--s3); }
    li { padding-left: var(--s4); border-left: calc(var(--borde-fino) * 2) solid var(--border); }
    .cuando { margin: 0 0 var(--s1); font-size: .85em; }
    .descripcion { margin: 0; color: var(--text); }
    .traza { margin: var(--s1) 0 0; color: var(--text-3); font-size: .8em; }
  `,
})
export class PanelDeEvidencia {
  readonly titulo = input('Evidencia del caso')
  readonly eventos = input.required<EventoDeEvidencia[]>()
  readonly mensajeVacio = input('Todavía no hay eventos registrados en este caso.')
}
