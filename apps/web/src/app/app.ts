import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { RouterOutlet } from '@angular/router'
import { Cabecera } from './layout/cabecera'
import { Pie } from './layout/pie'
import { ServicioMeta } from './seo/servicio-meta'

/** El sitio: la barra y el pie de la landing alrededor de cada página. */
@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, Cabecera, Pie],
  template: `
    <ap-cabecera />
    <router-outlet />
    <ap-pie />
  `,
})
export class App {
  private readonly servicioMeta = inject(ServicioMeta)

  constructor() {
    this.servicioMeta.observar()
  }
}
