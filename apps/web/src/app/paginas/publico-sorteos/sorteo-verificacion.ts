import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import { Meta } from '@angular/platform-browser'
import { VerificadorDeSorteo } from '../../verificadores/verificador-de-sorteo'

/** CU-61 — `GET /publico/sorteos/:id/verificacion`. No indexable: datos de un sorteo concreto. */
@Component({
  selector: 'ap-sorteo-verificacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  imports: [VerificadorDeSorteo],
  template: `
    <main>
      <h1>Verificar el sorteo de turnos</h1>
      <p class="bajada">Sorteo: <strong>{{ id() }}</strong></p>
      @defer (hydrate on viewport) {
        <ap-verificador-de-sorteo [sorteoId]="id()" />
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
export class SorteoVerificacion {
  readonly id = input.required<string>()

  constructor() {
    inject(Meta).updateTag({ name: 'robots', content: 'noindex, nofollow' })
  }
}
