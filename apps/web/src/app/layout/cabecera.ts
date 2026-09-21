import { ChangeDetectionStrategy, Component } from '@angular/core'
import { RouterLink, RouterLinkActive } from '@angular/router'
import { LanzadorDeTutorial } from '@aportaya/tutoriales/lanzador-de-tutorial'
import { Marca } from './marca'

/** La barra del sitio: la de la landing, con rutas reales en vez de anclas de una sola página. */
@Component({
  selector: 'ap-cabecera',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Marca, RouterLink, RouterLinkActive, LanzadorDeTutorial],
  host: { style: 'display: contents' },
  template: `
    <a class="saltar" href="#contenido">Saltar al contenido</a>
    <header class="barra">
      <div class="barra-in">
        <ap-marca />
        <nav aria-label="Principal" data-tutorial-id="sitio-menu">
          @for (enlace of enlaces; track enlace.ruta) {
            <a [routerLink]="enlace.ruta" routerLinkActive #activo="routerLinkActive" [attr.aria-current]="activo.isActive ? 'page' : null">{{ enlace.texto }}</a>
          }
        </nav>
        <div class="acciones" data-tutorial-id="sitio-empezar">
          <!-- Se trae cuando el navegador está libre: es ayuda, no contenido. -->
          @defer (on idle) {
            <ap-lanzador-de-tutorial data-tutorial-id="sitio-guia" />
          }
          <a class="boton boton--principal boton--sm" routerLink="/descargar">Crear mi grupo</a>
        </div>
      </div>
    </header>
  `,
})
export class Cabecera {
  protected readonly enlaces = [
    { ruta: '/como-funciona', texto: 'Cómo funciona' },
    { ruta: '/tarifas', texto: 'Tarifas' },
    { ruta: '/transparencia', texto: 'Transparencia' },
    { ruta: '/seguridad', texto: 'Seguridad' },
    { ruta: '/preguntas', texto: 'Preguntas' },
    { ruta: '/tutoriales', texto: 'Guía' },
  ]
}
