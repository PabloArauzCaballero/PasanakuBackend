import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Icono } from '../icono/icono'
import { coloresDe, Tono } from '../tono/tono'

const ICONOS = { ok: 'verificado', aviso: 'alerta', error: 'alerta', info: 'info', neutro: 'info' } as const

/** Aviso en línea. Dice qué pasó y qué hacer; el ícono acompaña al color. `error` se anuncia como alerta. */
@Component({
  selector: 'ap-alerta',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { '[attr.role]': 'tono() === "error" ? "alert" : "status"', '[style.--fondo]': 'colores().fondo', '[style.--frente]': 'colores().frente' },
  template: `
    <ap-icono [nombre]="icono()" tamano="s5" />
    <div class="cuerpo">
      @if (titulo()) { <p class="titulo">{{ titulo() }}</p> }
      <p class="texto"><ng-content /></p>
      <ng-content select="[accion]" />
    </div>
  `,
  styles: `
    :host { display: flex; gap: var(--s3); padding: var(--s3) var(--s4); border-radius: var(--r-md); background: var(--fondo); color: var(--text); border-left: var(--s1) solid var(--frente); }
    ap-icono { color: var(--frente); margin-top: var(--borde-desfase); }
    .cuerpo { flex: 1; min-width: 0; }
    p { margin: 0; }
    .titulo { font-weight: 600; margin-bottom: var(--s1); }
  `,
})
export class Alerta {
  readonly tono = input<Tono>('info')
  readonly titulo = input<string>()
  readonly colores = computed(() => coloresDe(this.tono()))
  readonly icono = computed(() => ICONOS[this.tono()])
}
