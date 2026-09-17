import { CabeceraDePagina } from '../../layout/cabecera-de-pagina'
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import { Meta } from '@angular/platform-browser'
import { VerificadorDeSorteo } from '../../verificadores/verificador-de-sorteo'

/** CU-61 — `GET /publico/sorteos/:id/verificacion`. No indexable: datos de un sorteo concreto. */
@Component({
  selector: 'ap-sorteo-verificacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  imports: [CabeceraDePagina, VerificadorDeSorteo],
  template: `
    <main id="contenido">
      <ap-cabecera-de-pagina etiqueta="Transparencia verificable" titulo="Verificar el sorteo de turnos" />
      <div class="contenedor contenedor--angosto cuerpo-pagina">
        <p class="texto-guia">Sorteo: <strong>{{ id() }}</strong></p>
        <!-- hydrate on immediate, no on viewport: el verificador ES el contenido de esta
             página y está en la primera pantalla, así que diferirlo no ahorra nada. Y al
             hidratarse más tarde ya no existe la caché que el servidor transfiere —Angular
             la descarta cuando la app arranca—, así que el bloque volvía a pedir los datos,
             el DOM dejaba de coincidir (NG0502) y la página se quedaba para siempre en el
             esqueleto: nunca se veía el veredicto. -->
        @defer (hydrate on immediate) {
          <ap-verificador-de-sorteo [sorteoId]="id()" />
        } @placeholder {
          <p role="status">Cargando el verificador…</p>
        }
      </div>
    </main>
  `,
})
export class SorteoVerificacion {
  readonly id = input.required<string>()

  constructor() {
    inject(Meta).updateTag({ name: 'robots', content: 'noindex, nofollow' })
  }
}
