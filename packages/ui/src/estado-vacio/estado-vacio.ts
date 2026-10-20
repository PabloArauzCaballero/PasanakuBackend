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
    <span class="loseta" aria-hidden="true"><ap-icono [nombre]="iconoVisible()" tamano="s5" /></span>
    <p class="titulo">{{ titulo() }}</p>
    @if (porque()) { <p class="porque">{{ porque() }}</p> }
    @if (accion()) { <ap-boton [variante]="motivo() === 'porFiltro' ? 'fantasma' : 'secundario'" (pulsado)="actuar.emit()">{{ accion() }}</ap-boton> }
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--s2);
      padding: var(--s6) var(--s4);
      text-align: center;
      border: var(--borde-fino) dashed color-mix(in srgb, var(--borde-control) 80%, transparent);
      border-radius: var(--r-xl);
      background: color-mix(in srgb, var(--surface) 95%, transparent);
      box-shadow: var(--sombra-1);
    }
    .loseta {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: calc(var(--s6) + var(--s3));
      height: calc(var(--s6) + var(--s3));
      margin-bottom: var(--s2);
      border: var(--borde-fino) solid var(--border);
      border-radius: var(--r-pill);
      background: linear-gradient(135deg, var(--surface-2), var(--surface));
      color: var(--brand-texto);
      box-shadow: var(--sombra-2);
    }
    p { margin: 0; max-width: 48ch; }
    .titulo { font-size: 1.05rem; font-weight: 600; color: var(--text); letter-spacing: -0.01em; }
    .porque { font: var(--t-cuerpo-chico); color: var(--text-2); line-height: 1.5; }
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
