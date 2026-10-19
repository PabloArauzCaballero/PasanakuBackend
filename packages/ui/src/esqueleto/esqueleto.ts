import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

export type FormaDeEsqueleto = 'lineas' | 'filas'

/**
 * Silueta de carga. `lineas`: tantas líneas como `lineas`, con anchos que no se repiten (texto).
 * `filas`: filas de tabla de alto fijo con un brillo que cruza (lo que se espera cuando lo que
 * viene es un listado). El brillo se mueve por `transform`, solo en el compositor, y con un
 * desfase por fila para que no latan todas a la vez. Se anuncia una sola vez.
 */
@Component({
  selector: 'ap-esqueleto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'status', 'aria-live': 'polite', '[attr.aria-label]': 'etiqueta()', '[class]': '"forma-" + forma()' },
  template: `
    @for (a of anchos(); track $index) {
      <span [style.width.%]="forma() === 'lineas' ? a : null" [style.animation-delay.ms]="$index * 60"><i></i></span>
    }
  `,
  styles: `
    :host { display: block; }
    span { position: relative; display: block; overflow: hidden; height: var(--s4); margin-bottom: var(--s3); border-radius: var(--r-sm); background: var(--esqueleto); animation: latir 1.4s ease-in-out infinite; }
    span:last-child { margin-bottom: 0; }
    /* Las filas ocupan el ancho del listado que reemplazan, aunque las aloje una caja flexible. */
    :host(.forma-filas) { width: 100%; }
    :host(.forma-filas) span { height: calc(var(--s6) + var(--s3)); margin-bottom: var(--s2); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); animation: none; }
    :host(.forma-filas) i { position: absolute; inset: 0 auto 0 0; width: 50%; background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--surface) 70%, transparent), transparent); animation: barrer 1.6s linear infinite; animation-delay: inherit; }
    @keyframes latir { 50% { opacity: .55; } }
    @keyframes barrer { from { transform: translateX(-100%); } to { transform: translateX(300%); } }
    @media (prefers-reduced-motion: reduce) { span, i { animation: none; } }
  `,
})
export class Esqueleto {
  readonly lineas = input(3)
  readonly etiqueta = input('Cargando')
  readonly forma = input<FormaDeEsqueleto>('lineas')
  readonly anchos = computed(() => Array.from({ length: this.lineas() }, (_, i) => [70, 45, 85, 60][i % 4]))
}
