import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Icono } from '../icono/icono'

export type Factor = { nombre: string; peso: 'aFavor' | 'enContra' | 'neutro'; explicacion: string }

/**
 * Por qué el veredicto fue el que fue, en palabras. Cada factor dice si sumó, restó o
 * no pesó, y por qué. Un veredicto sin factores es un «no» sin explicación.
 */
@Component({
  selector: 'ap-panel-de-factores',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { role: 'region', '[attr.aria-label]': '"Factores del veredicto: " + veredicto()' },
  template: `
    <p class="veredicto">{{ veredicto() }}</p>
    <ul>
      @for (f of factores(); track f.nombre) {
        <li [class]="f.peso">
          <ap-icono [nombre]="f.peso === 'aFavor' ? 'verificado' : f.peso === 'enContra' ? 'alerta' : 'info'" [etiqueta]="NOMBRE[f.peso]" tamano="s5" />
          <div><p class="nombre">{{ f.nombre }}</p><p class="explicacion">{{ f.explicacion }}</p></div>
        </li>
      }
    </ul>
  `,
  styles: `
    :host { display: block; padding: var(--s4); border-radius: var(--r-lg); background: var(--surface-2); }
    .veredicto { margin: 0 0 var(--s3); font-family: var(--font-d); font-size: 1.1em; color: var(--text); }
    ul { margin: 0; padding: 0; list-style: none; display: flex; flex-direction: column; gap: var(--s3); }
    li { display: flex; gap: var(--s3); }
    .aFavor ap-icono { color: var(--ok-texto); } .enContra ap-icono { color: var(--err-texto); } .neutro ap-icono { color: var(--text-3); }
    p { margin: 0; }
    .nombre { font-weight: 600; color: var(--text); }
    .explicacion { color: var(--text-2); font-size: .9em; }
  `,
})
export class PanelDeFactores {
  readonly NOMBRE = { aFavor: 'A favor', enContra: 'En contra', neutro: 'No pesó' }
  readonly veredicto = input.required<string>()
  readonly factores = input.required<Factor[]>()
}
