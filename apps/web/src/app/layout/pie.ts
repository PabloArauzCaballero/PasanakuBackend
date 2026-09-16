import { ChangeDetectionStrategy, Component } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Marca } from './marca'

/** El pie de la landing, con las páginas legales que el sitio sí tiene. */
@Component({
  selector: 'ap-pie',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Marca, RouterLink],
  host: { style: 'display: contents' },
  template: `
    <footer class="pie">
      <div class="contenedor">
        <div class="pie-cols">
          <div>
            <ap-marca />
            <p class="tagline">El pasanaku, en tu bolsillo. Tu plata, tu grupo, tu confianza.</p>
          </div>
          @for (columna of columnas; track columna.titulo) {
            <div>
              <h2 class="pie-titulo">{{ columna.titulo }}</h2>
              <ul>
                @for (enlace of columna.enlaces; track enlace.ruta) {
                  <li><a [routerLink]="enlace.ruta">{{ enlace.texto }}</a></li>
                }
              </ul>
            </div>
          }
        </div>
        <div class="pie-abajo">
          <span>© 2026 AportaYa · Hecho en Bolivia</span>
          <span>Solicitud de licencia en trámite ante la ASFI. Todavía no opera con dinero del público.</span>
        </div>
      </div>
    </footer>
  `,
})
export class Pie {
  protected readonly columnas = [
    { titulo: 'Producto', enlaces: [
      { ruta: '/como-funciona', texto: 'Cómo funciona' },
      { ruta: '/tarifas', texto: 'Tarifas' },
      { ruta: '/preguntas', texto: 'Preguntas frecuentes' },
      { ruta: '/descargar', texto: 'Descargar la app' },
    ] },
    { titulo: 'Confianza', enlaces: [
      { ruta: '/transparencia', texto: 'Transparencia verificable' },
      { ruta: '/seguridad', texto: 'Seguridad' },
      { ruta: '/plazos', texto: 'Plazos' },
      { ruta: '/reclamos', texto: 'Reclamos' },
    ] },
    { titulo: 'Legal', enlaces: [
      { ruta: '/privacidad', texto: 'Privacidad' },
      { ruta: '/contrato-de-adhesion', texto: 'Contrato de adhesión' },
      { ruta: '/legal/estado-regulatorio', texto: 'Estado regulatorio' },
    ] },
  ]
}
