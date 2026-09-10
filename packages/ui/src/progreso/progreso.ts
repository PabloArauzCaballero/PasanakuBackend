import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

/** Avance determinado de 0 a 1. Se lee como barra de progreso con su valor en porcentaje. */
@Component({
  selector: 'ap-progreso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    role: 'progressbar',
    '[attr.aria-label]': 'etiqueta()',
    '[attr.aria-valuemin]': '0',
    '[attr.aria-valuemax]': '100',
    '[attr.aria-valuenow]': 'porcentaje()',
    '[class]': '"tono-" + tono()',
  },
  template: `<span class="relleno" [style.width.%]="porcentaje()"></span>`,
  styles: `
    :host { display: block; height: var(--s2); border-radius: var(--r-pill); background: var(--surface-2); overflow: hidden; }
    .relleno { display: block; height: 100%; border-radius: inherit; background: var(--brand); transition: width .3s; }
    :host(.tono-accion) .relleno { background: var(--accent); }
    :host(.tono-ok) .relleno { background: var(--ok); }
    :host(.tono-aviso) .relleno { background: var(--warn); }
    :host(.tono-error) .relleno { background: var(--err); }
    @media (prefers-reduced-motion: reduce) { .relleno { transition: none; } }
  `,
})
export class Progreso {
  /** Fracción entre 0 y 1; se recorta al rango. */
  readonly valor = input.required<number>()
  readonly etiqueta = input.required<string>()
  readonly tono = input<'marca' | 'accion' | 'ok' | 'aviso' | 'error'>('marca')
  readonly porcentaje = computed(() => Math.round(Math.min(1, Math.max(0, this.valor())) * 100))
}
