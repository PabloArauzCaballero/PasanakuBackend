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
        @defer (hydrate on viewport) {
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
