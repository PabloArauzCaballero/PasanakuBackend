import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

/** Silueta de carga: tantas líneas como `lineas`, con anchos que no se repiten. Se anuncia una sola vez. */
@Component({
  selector: 'ap-esqueleto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'status', '[attr.aria-label]': 'etiqueta()' },
  template: `@for (a of anchos(); track $index) { <span [style.width.%]="a"></span> }`,
  styles: `
    :host { display: block; }
    span { display: block; height: var(--s4); margin-bottom: var(--s3); border-radius: var(--r-sm); background: var(--surface-2); animation: latir 1.4s ease-in-out infinite; }
    span:last-child { margin-bottom: 0; }
    @keyframes latir { 50% { opacity: .55; } }
    @media (prefers-reduced-motion: reduce) { span { animation: none; } }
  `,
})
export class Esqueleto {
  readonly lineas = input(3)
  readonly etiqueta = input('Cargando')
  readonly anchos = computed(() => Array.from({ length: this.lineas() }, (_, i) => [70, 45, 85, 60][i % 4]))
}
