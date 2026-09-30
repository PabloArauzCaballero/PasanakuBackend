import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core'
import { Boton } from '../boton/boton'
import type { MotivoVacio } from '../estado-de-pantalla/estado-de-pantalla'

/**
 * Vacío con **por qué** y **qué hacer**. Vacío por filtro ofrece quitar el filtro; vacío
 * por permiso dice a quién pedirlo; sin datos invita a la primera acción.
 */
@Component({
  selector: 'ap-estado-vacio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton],
  host: { role: 'status' },
  template: `
    <p class="titulo">{{ titulo() }}</p>
    @if (porque()) { <p class="porque">{{ porque() }}</p> }
    @if (accion()) { <ap-boton [variante]="motivo() === 'porFiltro' ? 'fantasma' : 'secundario'" (pulsado)="actuar.emit()">{{ accion() }}</ap-boton> }
  `,
  styles: `
    :host { display: flex; flex-direction: column; align-items: center; gap: var(--s2); padding: var(--s6) var(--s4); text-align: center; }
    p { margin: 0; max-width: 40ch; }
    .titulo { font-weight: 600; color: var(--text); }
    .porque { color: var(--text-2); }
    ap-boton { margin-top: var(--s2); }
  `,
})
export class EstadoVacio {
  readonly motivo = input<MotivoVacio>('sinDatos')
  readonly titulo = input.required<string>()
  /** Qué hacer; si no se pasa, se deduce del motivo. */
  readonly accionPropia = input<string>()
  readonly explicacion = input<string>()
  readonly actuar = output<void>()
  /** `undefined` deduce el porqué del motivo; `''` explícito lo suprime a propósito
   * (lo usa `EstadoDePantalla` para no duplicar el mensaje que ya pasó como título). */
  readonly porque = computed(() => (this.explicacion() !== undefined ? this.explicacion() : PORQUE[this.motivo()]))
  readonly accion = computed(() => this.accionPropia() ?? ACCION[this.motivo()])
}

const PORQUE: Record<MotivoVacio, string> = {
  sinDatos: 'Todavía no hay nada acá.',
  porFiltro: 'Con estos filtros no aparece nada.',
  porPermiso: 'Tu rol no puede ver esta información.',
}
const ACCION: Record<MotivoVacio, string | undefined> = {
  sinDatos: undefined,
  porFiltro: 'Quitar los filtros',
  porPermiso: undefined,
}
