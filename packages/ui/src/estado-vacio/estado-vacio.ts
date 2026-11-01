import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core'
import { Boton } from '../boton/boton'
import { Icono, NombreDeIcono } from '../icono/icono'
import type { MotivoVacio } from '../estado-de-pantalla/estado-de-pantalla'

/**
 * Vacío con **por qué** y **qué hacer**. Vacío por filtro ofrece quitar el filtro; vacío
 * por permiso dice a quién pedirlo; sin datos invita a la primera acción.
 */
@Component({
  selector: 'ap-estado-vacio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, Icono],
  host: { role: 'status' },
  template: `
    <span class="loseta" aria-hidden="true"><ap-icono [nombre]="iconoVisible()" tamano="s4" /></span>
    <p class="titulo">{{ titulo() }}</p>
    @if (porque()) { <p class="porque">{{ porque() }}</p> }
    @if (accion()) { <ap-boton [variante]="motivo() === 'porFiltro' ? 'fantasma' : 'secundario'" (pulsado)="actuar.emit()">{{ accion() }}</ap-boton> }
  `,
  styles: `
    :host { display: flex; flex-direction: column; align-items: center; gap: var(--s1); padding: var(--s5) var(--s4); text-align: center; border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    .loseta { display: inline-flex; align-items: center; justify-content: center; width: var(--s6); height: var(--s6); margin-bottom: var(--s2); border-radius: var(--r-pill); background: var(--surface-2); color: var(--text-3); }
    p { margin: 0; max-width: 52ch; text-wrap: pretty; }
    .titulo { font: var(--t-cuerpo-fuerte); color: var(--text); }
    .porque { font: var(--t-cuerpo-chico); color: var(--text-2); }
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
  /** Ícono por motivo (sin resultados, prohibido) salvo que quien aloja pase otro. */
  readonly icono = input<NombreDeIcono>()
  readonly iconoVisible = computed(() => this.icono() ?? ICONO[this.motivo()])
}

const PORQUE: Record<MotivoVacio, string> = {
  sinDatos: 'Todavía no hay nada acá.',
  porFiltro: 'Con estos filtros no aparece nada.',
  porPermiso: 'Tu rol no puede ver esta información.',
}
const ICONO: Record<MotivoVacio, NombreDeIcono> = { sinDatos: 'sinResultados', porFiltro: 'sinResultados', porPermiso: 'prohibido' }
const ACCION: Record<MotivoVacio, string | undefined> = {
  sinDatos: undefined,
  porFiltro: 'Quitar los filtros',
  porPermiso: undefined,
}
