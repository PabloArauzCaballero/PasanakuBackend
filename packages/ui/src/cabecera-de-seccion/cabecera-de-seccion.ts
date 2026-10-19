import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Icono, NombreDeIcono } from '../icono/icono'

/**
 * El encabezado de un bloque dentro de una pantalla: `h2` con ícono en el acento y una línea de
 * descripción. Las acciones del bloque (un botón, un filtro) se proyectan con `acciones`.
 */
@Component({
  selector: 'ap-cabecera-de-seccion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  template: `
    <div class="textos">
      <h2>
        @if (icono()) { <ap-icono [nombre]="icono()!" tamano="s4" /> }
        {{ titulo() }}
      </h2>
      @if (descripcion()) { <p>{{ descripcion() }}</p> }
    </div>
    <div class="acciones"><ng-content select="[acciones]" /></div>
  `,
  styles: `
    :host { display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between; gap: var(--s2) var(--s3); }
    .textos { min-width: 0; }
    h2 { display: flex; align-items: center; gap: var(--s2); margin: 0; font: var(--t-titulo-3); letter-spacing: var(--t-titulo-3-track); color: var(--text); }
    ap-icono { color: var(--brand-texto); flex: none; }
    p { margin: var(--s1) 0 0; font: var(--t-cuerpo-chico); color: var(--text-3); max-width: 70ch; }
    .acciones { display: flex; flex-wrap: wrap; gap: var(--s2); }
    .acciones:empty { display: none; }
  `,
})
export class CabeceraDeSeccion {
  readonly titulo = input.required<string>()
  readonly descripcion = input<string>()
  readonly icono = input<NombreDeIcono>()
}
