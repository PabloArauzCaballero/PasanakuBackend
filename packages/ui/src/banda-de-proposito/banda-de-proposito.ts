import { ChangeDetectionStrategy, Component, input } from '@angular/core'

/**
 * «Para qué sirve»: toda pantalla del backoffice lleva arriba una frase de negocio.
 * Una pantalla cuyo propósito no se puede escribir en una frase no debería existir
 * (regla §0.6 del flujo de pantallas).
 */
@Component({
  selector: 'ap-banda-de-proposito',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'note' },
  template: `<p>{{ texto() }}</p>`,
  styles: `
    :host { display: block; padding: var(--s3) var(--s5); background: var(--brand-bg); color: var(--brand-ink); border-bottom: var(--borde-fino) solid var(--border); }
    p { margin: 0; max-width: 70ch; }
  `,
})
export class BandaDeProposito {
  readonly texto = input.required<string>()
}
