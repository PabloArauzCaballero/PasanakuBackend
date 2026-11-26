import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Icono } from '@aportaya/ui/icono/icono'
import type { Requisito } from '../dominio/clave'

/**
 * Los requisitos de la clave que la pantalla puede comprobar. Cada uno dice su estado **en texto** («cumplido» /
 * «pendiente»), no solo con el color ni con el ícono. Es una guía: la política la decide el servidor.
 */
@Component({
  selector: 'ap-indicador-de-clave',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  template: `
    <ul [attr.aria-label]="etiqueta()">
      @for (r of requisitos(); track r.id) {
        <li [class.cumplido]="r.cumplido">
          <ap-icono [nombre]="r.cumplido ? 'verificado' : 'reloj'" tamano="s4" />
          <span>{{ r.texto }}<span class="estado"> · {{ r.cumplido ? 'cumplido' : 'pendiente' }}</span></span>
        </li>
      }
    </ul>
  `,
  styles: `
    ul { margin: 0; padding: 0; list-style: none; display: grid; gap: var(--s1); }
    li { display: flex; align-items: center; gap: var(--s2); font: var(--t-cuerpo-chico); color: var(--text-2); }
    li.cumplido { color: var(--ok-texto); }
    .estado { color: var(--text-3); }
    li.cumplido .estado { color: inherit; }
  `,
})
export class IndicadorDeClave {
  readonly requisitos = input.required<readonly Requisito[]>()
  readonly etiqueta = input('Requisitos de la clave')
}
