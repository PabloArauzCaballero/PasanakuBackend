import { ChangeDetectionStrategy, Component } from '@angular/core'
import contenido from '../../../generado/contenido.json'
import { CabeceraDePagina } from '../../layout/cabecera-de-pagina'

/**
 * Contenido estático hoy — ver la nota técnica dentro del Markdown sobre por qué
 * todavía no es `RenderMode.Server` contra `servicios/cumplimiento` (hueco de
 * contrato, documentado también en `planes/informes/carril-W.md`).
 */
@Component({
  selector: 'ap-contrato-de-adhesion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  imports: [CabeceraDePagina],
  template: `
    <main id="contenido">
      <ap-cabecera-de-pagina etiqueta="Legal" [titulo]="pagina.titulo" [bajada]="pagina.bajada" />
      <div class="contenedor contenedor--angosto cuerpo-pagina">
        <article class="prosa" [innerHTML]="pagina.html"></article>
        <p class="fecha">Actualizado el <time [attr.datetime]="pagina.actualizado">{{ pagina.actualizado }}</time></p>
      </div>
    </main>
  `,
})
export class ContratoDeAdhesion {
  protected readonly pagina = contenido.paginas['contrato-de-adhesion']!
}
