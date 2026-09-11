import { ChangeDetectionStrategy, Component } from '@angular/core'
import contenido from '../../../generado/contenido.json'

/** Página de contenido: se prerenderiza y no carga ningún cliente de API. */
@Component({
  selector: 'ap-transparencia',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  template: `
    <main>
      <h1>{{ pagina.titulo }}</h1>
      <p class="bajada">{{ pagina.bajada }}</p>
      <article [innerHTML]="pagina.html"></article>
      <p class="fecha">Actualizado el <time [attr.datetime]="pagina.actualizado">{{ pagina.actualizado }}</time></p>
    </main>
  `,
  styles: `
    main { max-width: 70ch; margin: 0 auto; padding: var(--s6) var(--s5); }
    h1 { font-size: 2.25rem; margin-bottom: var(--s3); }
    .bajada { color: var(--text-2); font-size: 1.125rem; }
    .fecha { color: var(--text-3); margin-top: var(--s6); }
  `,
})
export class Transparencia {
  protected readonly pagina = contenido.paginas['transparencia']!
}
