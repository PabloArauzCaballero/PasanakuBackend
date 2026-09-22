import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { Meta } from '@angular/platform-browser'
import { Catalogo } from '@aportaya/ui/catalogo/catalogo'

/**
 * El catálogo vivo de `@aportaya/ui`, prerenderizado y **sin indexar** (robots + meta):
 * es para el equipo y para las capturas de la revisión lado a lado con Flutter (plan 22 §7).
 */
@Component({
  selector: 'ap-pagina-catalogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Catalogo],
  template: `
    <main>
      <h1>Catálogo de @aportaya/ui</h1>
      <p class="bajada">Cada pieza del sistema de diseño web, con sus variantes y estados. Es la misma lista que el catálogo de Flutter, para revisarlos lado a lado.</p>
      <ap-catalogo />
    </main>
  `,
  styles: `
    main { max-width: calc(var(--s7) * 24); margin: 0 auto; padding: var(--s6) var(--s5); }
    h1 { font-size: 1.75rem; margin-bottom: var(--s2); }
    .bajada { color: var(--text-2); max-width: 70ch; }
  `,
})
export class PaginaCatalogo {
  constructor() {
    inject(Meta).updateTag({ name: 'robots', content: 'noindex, nofollow' })
  }
}
