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
  imports: [VerificadorDeCertificado],
  template: `
    <main>
      <h1>Verificar un certificado de reputación</h1>
      <p class="bajada">Código: <strong>{{ codigo() }}</strong></p>
      @defer (hydrate on viewport) {
        <ap-verificador-de-certificado [codigo]="codigo()" />
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
export class Verificar {
  readonly codigo = input.required<string>()

  constructor() {
    inject(Meta).updateTag({ name: 'robots', content: 'noindex, nofollow' })
  }
}
