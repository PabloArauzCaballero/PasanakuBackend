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
        @defer (hydrate on viewport) {
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
