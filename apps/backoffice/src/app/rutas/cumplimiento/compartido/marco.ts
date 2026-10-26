import { ChangeDetectionStrategy, Component, effect, inject, input } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import type { NombreDeIcono } from '@aportaya/ui/icono/icono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'

/**
 * El marco común de las bandejas de cumplimiento: eyebrow de la sección, título con su ícono, propósito y acciones,
 * y el listado a todo el ancho. Ya no dibuja su propia navegación: las pantallas de la sección son las pestañas del
 * shell (`ap-pestanas-de-seccion`) y dos barras con los mismos nueve enlaces eran opciones de más (plan A1).
 */
@Component({
  selector: 'ap-marco-de-cumplimiento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Cumplimiento" [titulo]="titulo()" [descripcion]="proposito()" [icono]="icono()">
        <div acciones class="acciones"><ng-content select="[acciones]" /></div>
      </ap-cabecera-de-pagina>
      <ng-content />
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5) var(--s4); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
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
