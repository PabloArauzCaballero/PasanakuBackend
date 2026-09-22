import { ChangeDetectionStrategy, Component } from '@angular/core'
import contenido from '../../../generado/contenido.json'
import { CabeceraDePagina } from '../../layout/cabecera-de-pagina'
import { EnlaceALaApp } from '../../layout/enlace-a-la-app'

/** Página de contenido: se prerenderiza y no carga ningún cliente de API. */
@Component({
  selector: 'ap-descargar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  imports: [CabeceraDePagina, EnlaceALaApp],
  template: `
    <main id="contenido">
      <ap-cabecera-de-pagina etiqueta="La app" [titulo]="pagina.titulo" [bajada]="pagina.bajada" />
      <div class="contenedor contenedor--angosto cuerpo-pagina">
        <div class="acciones acciones--descarga"><ap-enlace-a-la-app /></div>
        <article class="prosa" [innerHTML]="pagina.html"></article>
        <p class="fecha">Actualizado el <time [attr.datetime]="pagina.actualizado">{{ pagina.actualizado }}</time></p>
      </div>
    </main>
  `,
})
export class Descargar {
  protected readonly pagina = contenido.paginas['descargar']!
}
