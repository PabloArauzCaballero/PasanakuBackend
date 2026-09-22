import { ChangeDetectionStrategy, Component } from '@angular/core'
import contenido from '../../../generado/contenido.json'
import { CabeceraDePagina } from '../../layout/cabecera-de-pagina'

/** Página de contenido: se prerenderiza y no carga ningún cliente de API. */
@Component({
  selector: 'ap-privacidad',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  imports: [CabeceraDePagina],
  template: `
    <main id="contenido">
      <ap-cabecera-de-pagina etiqueta="Privacidad" [titulo]="pagina.titulo" [bajada]="pagina.bajada" />
      <div class="contenedor contenedor--angosto cuerpo-pagina">
        <article class="prosa" [innerHTML]="pagina.html"></article>
        <p class="fecha">Actualizado el <time [attr.datetime]="pagina.actualizado">{{ pagina.actualizado }}</time></p>
      </div>
    </main>
  `,
})
export class Privacidad {
  protected readonly pagina = contenido.paginas['privacidad']!
}
