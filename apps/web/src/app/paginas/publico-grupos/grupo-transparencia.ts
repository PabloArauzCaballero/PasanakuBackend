import { CabeceraDePagina } from '../../layout/cabecera-de-pagina'
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import { Meta } from '@angular/platform-browser'
import { VerificadorDeCadena } from '../../verificadores/verificador-de-cadena'

/** CU-72/CU-73 — `GET /publico/grupos/:codigo/verificacion`. No indexable: datos de un grupo concreto. */
@Component({
  selector: 'ap-grupo-transparencia',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  imports: [CabeceraDePagina, VerificadorDeCadena],
  template: `
    <main id="contenido">
      <ap-cabecera-de-pagina etiqueta="Transparencia verificable" titulo="Cadena de transparencia del grupo" />
      <div class="contenedor contenedor--angosto cuerpo-pagina">
        <p class="texto-guia">Código de grupo: <strong>{{ codigo() }}</strong></p>
        <!-- hydrate on immediate, no on viewport: el verificador ES el contenido de esta
             página y está en la primera pantalla, así que diferirlo no ahorra nada. Y al
             hidratarse más tarde ya no existe la caché que el servidor transfiere —Angular
             la descarta cuando la app arranca—, así que el bloque volvía a pedir los datos,
             el DOM dejaba de coincidir (NG0502) y la página se quedaba para siempre en el
             esqueleto: nunca se veía el veredicto. -->
        @defer (hydrate on immediate) {
          <ap-verificador-de-cadena [codigoGrupo]="codigo()" />
        } @placeholder {
          <p role="status">Cargando el verificador…</p>
        }
      </div>
    </main>
  `,
})
export class GrupoTransparencia {
  readonly codigo = input.required<string>()

  constructor() {
    inject(Meta).updateTag({ name: 'robots', content: 'noindex, nofollow' })
  }
}
