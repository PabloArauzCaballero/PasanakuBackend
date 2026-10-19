import { ChangeDetectionStrategy, Component, effect, inject, input } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import type { NombreDeIcono } from '@aportaya/ui/icono/icono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { NavegacionDeCumplimiento } from './navegacion'

/** El marco común de las bandejas de cumplimiento: propósito, navegación de la sección, título y acciones. */
@Component({
  selector: 'ap-marco-de-cumplimiento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, NavegacionDeCumplimiento],
  template: `
    <section class="pantalla">
      <ap-navegacion-de-cumplimiento />
      <ap-cabecera-de-pagina [titulo]="titulo()" [descripcion]="proposito()" [icono]="icono()">
        <div acciones class="acciones"><ng-content select="[acciones]" /></div>
      </ap-cabecera-de-pagina>
      <ng-content />
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 72rem; display: grid; gap: var(--s4); min-width: 0; }
    .acciones { display: flex; flex-wrap: wrap; gap: var(--s2); }
  `,
})
export class MarcoDeCumplimiento {
  readonly titulo = input.required<string>()
  readonly proposito = input.required<string>()
  readonly icono = input<NombreDeIcono>('candado')
  private readonly explicador = inject(ExplicadorDeVista)

  constructor() {
    effect(() => {
      this.explicador.fijar(this.proposito())
    })
  }
}
