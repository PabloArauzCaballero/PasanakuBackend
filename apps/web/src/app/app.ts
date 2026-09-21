import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { RouterOutlet } from '@angular/router'
import { AnfitrionDeTutorial } from '@aportaya/tutoriales/anfitrion-de-tutorial'
import { EncendidoDeTutorial } from '@aportaya/tutoriales/encendido'
import { Cabecera } from './layout/cabecera'
import { Pie } from './layout/pie'
import { ServicioMeta } from './seo/servicio-meta'

/** El sitio: la barra y el pie de la landing alrededor de cada página. */
@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, Cabecera, Pie, AnfitrionDeTutorial],
  template: `
    <ap-cabecera />
    <router-outlet />
    <ap-pie />
    <!--
      La guía va dentro de un \`@defer\`: el velo y el globo son kilobytes que nadie
      necesita para leer una página de contenido, y acá el presupuesto del primer
      pintado es lo que más cuida el sitio. Con la guía apagada no se trae nada.
    -->
    @defer (when tutorial.activo()) {
      <ap-anfitrion-de-tutorial />
    }
  `,
})
export class App {
  protected readonly tutorial = inject(EncendidoDeTutorial)
  private readonly servicioMeta = inject(ServicioMeta)

  constructor() {
    this.servicioMeta.observar()
  }
}
