import { ChangeDetectionStrategy, Component, input } from '@angular/core'

/** Superficie con borde. Con `titulo` es una `section` con encabezado; sin él, un `div`. */
@Component({
  selector: 'ap-tarjeta',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[attr.role]': 'titulo() ? "region" : null', '[attr.aria-label]': 'titulo() ?? null', '[class.relieve]': 'relieve()' },
  template: `
    @if (titulo()) { <div class="cabecera"><h3>{{ titulo() }}</h3><ng-content select="[accion]" /></div> }
    <ng-content />
  `,
  styles: `
    :host { display: block; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    :host(.relieve) { box-shadow: var(--sombra-2); }
    .cabecera { display: flex; align-items: center; justify-content: space-between; gap: var(--s3); margin-bottom: var(--s3); }
    h3 { font-size: 1.05em; color: var(--text); }
  `,
})
export class Tarjeta {
  readonly titulo = input<string>()
  readonly relieve = input(false)
}
