import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

/** Actividad indeterminada. Siempre se anuncia (`role=status`); respeta *reduced motion*. */
@Component({
  selector: 'ap-girador',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'status', '[attr.aria-label]': 'etiqueta()', '[style.--tamano]': 'tamanoCss()' },
  template: ``,
  styles: `
    :host { display: inline-block; width: var(--tamano); height: var(--tamano); border: var(--borde-desfase) solid var(--border); border-top-color: currentColor; border-radius: var(--r-pill); animation: girar .8s linear infinite; }
    @keyframes girar { to { transform: rotate(360deg); } }
    @media (prefers-reduced-motion: reduce) { :host { animation: none; border-top-color: var(--border); border-right-color: currentColor; } }
  `,
})
export class Girador {
  readonly etiqueta = input('Cargando')
  readonly tamano = input<'s4' | 's5' | 's6'>('s5')
  readonly tamanoCss = computed(() => `var(--${this.tamano()})`)
}
