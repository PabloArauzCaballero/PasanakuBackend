import { ChangeDetectionStrategy, Component, input } from '@angular/core'

/** Marca de «hay algo nuevo». Sin texto no dice nada al lector: por eso `etiqueta` es obligatoria. */
@Component({
  selector: 'ap-punto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'status', '[attr.aria-label]': 'etiqueta()', '[class.accion]': 'tono() === "accion"' },
  template: ``,
  styles: `
    :host { display: inline-block; width: var(--s2); height: var(--s2); border-radius: var(--r-pill); background: var(--err); box-shadow: 0 0 0 var(--borde-desfase) var(--surface); }
    :host(.accion) { background: var(--accent); }
  `,
})
export class Punto {
  readonly etiqueta = input.required<string>()
  readonly tono = input<'alerta' | 'accion'>('alerta')
}
