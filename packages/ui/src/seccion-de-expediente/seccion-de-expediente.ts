import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Fecha } from '../fecha/fecha'

export type Dato = { nombre: string; valor: string; esFecha?: boolean }

/**
 * Un bloque del expediente: título, quién y cuándo, y una lista de datos con nombre.
 * Verificación, incumplimientos, disputas y reclamos se leen con la misma forma.
 */
@Component({
  selector: 'ap-seccion-de-expediente',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Fecha],
  host: { role: 'region', '[attr.aria-label]': 'titulo()' },
  template: `
    <div class="cabecera">
      <h3>{{ titulo() }}</h3>
      @if (actualizadoIso()) { <p class="cuando">@if (actor()) { {{ actor() }} · }<ap-fecha [iso]="actualizadoIso()!" /></p> }
    </div>
    <dl>
      @for (d of datos(); track d.nombre) {
        <div><dt>{{ d.nombre }}</dt><dd>@if (d.esFecha) { <ap-fecha [iso]="d.valor" /> } @else { {{ d.valor }} }</dd></div>
      }
    </dl>
    <ng-content />
  `,
  styles: `
    :host { display: block; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    .cabecera { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: var(--s2); margin-bottom: var(--s3); }
    h3 { font-size: 1.05em; color: var(--text); }
    .cuando { margin: 0; color: var(--text-3); font-size: .85em; }
    dl { display: grid; grid-template-columns: repeat(auto-fill, minmax(calc(var(--s7) * 4), 1fr)); gap: var(--s3) var(--s4); margin: 0; }
    dt { color: var(--text-3); font-size: .85em; }
    dd { margin: 0; color: var(--text); overflow-wrap: anywhere; }
  `,
})
export class SeccionDeExpediente {
  readonly titulo = input.required<string>()
  readonly datos = input.required<Dato[]>()
  readonly actor = input<string>()
  readonly actualizadoIso = input<string>()
}
