import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import { Meta } from '@angular/platform-browser'
import { VerificadorDeCadena } from '../../verificadores/verificador-de-cadena'

/** CU-72/CU-73 — `GET /publico/grupos/:codigo/verificacion`. No indexable: datos de un grupo concreto. */
@Component({
  selector: 'ap-grupo-transparencia',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  imports: [VerificadorDeCadena],
  template: `
    <main>
      <h1>Cadena de transparencia del grupo</h1>
      <p class="bajada">Código de grupo: <strong>{{ codigo() }}</strong></p>
      @defer (hydrate on viewport) {
        <ap-verificador-de-cadena [codigoGrupo]="codigo()" />
      } @placeholder {
        <p role="status">Cargando el verificador…</p>
      }
    </main>
  `,
  styles: `
    main { max-width: 70ch; margin: 0 auto; padding: var(--s6) var(--s5); }
    h1 { font-size: 1.75rem; margin-bottom: var(--s3); }
    .bajada { color: var(--text-2); margin-bottom: var(--s5); }
  `,
})
export class GrupoTransparencia {
  readonly codigo = input.required<string>()

  constructor() {
    inject(Meta).updateTag({ name: 'robots', content: 'noindex, nofollow' })
  }
}
