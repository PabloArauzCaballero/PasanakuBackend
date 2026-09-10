import { ChangeDetectionStrategy, Component, input } from '@angular/core'

/** Un bloque del catálogo: nombre de la pieza y una grilla con sus variantes. */
@Component({
  selector: 'ap-seccion-de-catalogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[id]': 'ancla()' },
  template: `<h2>{{ nombre() }}</h2><div class="grilla"><ng-content /></div>`,
  styles: `
    :host { display: block; padding: var(--s5) 0; border-bottom: var(--borde-fino) solid var(--border); }
    h2 { font-size: 1.1em; color: var(--text-2); margin-bottom: var(--s3); }
    .grilla { display: flex; flex-wrap: wrap; align-items: flex-start; gap: var(--s4); }
    .grilla > * { max-width: 100%; }
  `,
})
export class SeccionDeCatalogo {
  readonly nombre = input.required<string>()
  readonly ancla = input.required<string>()
}
