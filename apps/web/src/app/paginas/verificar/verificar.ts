import { CabeceraDePagina } from '../../layout/cabecera-de-pagina'
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import { Meta } from '@angular/platform-browser'
import { VerificadorDeCertificado } from '../../verificadores/verificador-de-certificado'

/**
 * CU-75 — `GET /verificar/:codigo`, ruta pública sin sesión. **No indexable**: es
 * información de una persona concreta (planes/14, regla 1). `noindex, nofollow` va
 * acá en la meta, y en la cabecera `X-Robots-Tag` por `app.routes.server.ts`.
 */
@Component({
  selector: 'ap-verificar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  imports: [CabeceraDePagina, VerificadorDeCertificado],
  template: `
    <main id="contenido">
      <ap-cabecera-de-pagina etiqueta="Verificación pública" titulo="Verificar un certificado de reputación" />
      <div class="contenedor contenedor--angosto cuerpo-pagina">
        <p class="texto-guia">Código: <strong>{{ codigo() }}</strong></p>
        <!-- hydrate on immediate, no on viewport: el verificador ES el contenido de esta
             página y está en la primera pantalla, así que diferirlo no ahorra nada. Y al
             hidratarse más tarde ya no existe la caché que el servidor transfiere —Angular
             la descarta cuando la app arranca—, así que el bloque volvía a pedir los datos,
             el DOM dejaba de coincidir (NG0502) y la página se quedaba para siempre en el
             esqueleto: nunca se veía el veredicto. -->
        @defer (hydrate on immediate) {
          <ap-verificador-de-certificado [codigo]="codigo()" />
        } @placeholder {
          <p role="status">Cargando el verificador…</p>
        }
      </div>
    </main>
  `,
})
export class Verificar {
  readonly codigo = input.required<string>()

  constructor() {
    inject(Meta).updateTag({ name: 'robots', content: 'noindex, nofollow' })
  }
}
