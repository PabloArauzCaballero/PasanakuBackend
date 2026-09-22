import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Icono, NombreDeIcono } from '../icono/icono'
import { coloresDe, Tono } from '../tono/tono'

/**
 * Un estado con nombre. Un color de estado en una superficie chica lleva **relleno y
 * borde** (D-12), y el punto o ícono acompaña al texto: nunca es solo color.
 */
@Component({
  selector: 'ap-chip-estado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { '[style.--fondo]': 'colores().fondo', '[style.--frente]': 'colores().frente' },
  template: `
    @if (icono()) { <ap-icono [nombre]="icono()!" tamano="s4" /> } @else { <span class="punto" aria-hidden="true"></span> }
    <span class="texto"><ng-content /></span>
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: var(--s2); max-width: 100%; min-height: var(--s5); padding: 0 var(--s3); border-radius: var(--r-pill); background: var(--fondo); color: var(--frente); border: var(--borde-fino) solid color-mix(in srgb, var(--frente) 35%, transparent); font-size: .85em; font-weight: 600; white-space: nowrap; }
    .punto { width: var(--s2); height: var(--s2); border-radius: var(--r-pill); background: currentColor; flex: none; }
    .texto { overflow: hidden; text-overflow: ellipsis; }
  `,
})
export class ChipEstado {
  readonly tono = input<Tono>('neutro')
  readonly icono = input<NombreDeIcono>()
  readonly colores = computed(() => coloresDe(this.tono()))
}
