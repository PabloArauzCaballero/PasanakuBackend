import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core'

/**
 * Una ayuda corta que aparece al pasar el mouse o al enfocar lo que envuelve, y se va con
 * `Escape`. El contenido proyectado es el disparador (un botón, un ícono de ayuda); el globo
 * lleva `role="tooltip"` y queda vinculado por `aria-describedby`, así el lector de pantalla lo
 * lee con el control. Se posiciona debajo con CSS puro: no mide nada ni abre portales.
 *
 * Un tooltip nunca es el único lugar donde vive una instrucción imprescindible: en el celular
 * no hay hover. Lo que hay que saber va en la ayuda del campo; esto amplía.
 */
@Component({
  selector: 'ap-tooltip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(mouseenter)': 'mostrar()',
    '(mouseleave)': 'ocultar()',
    '(focusin)': 'mostrar()',
    '(focusout)': 'ocultar()',
    '(keydown.escape)': 'cerrarConEscape()',
    '[class.visible]': 'visible()',
  },
  template: `
    <span class="disparador" [attr.aria-describedby]="id"><ng-content /></span>
    <span class="globo" role="tooltip" [id]="id">{{ texto() }}</span>
  `,
  styles: `
    :host { position: relative; display: inline-flex; }
    .disparador { display: inline-flex; }
    .globo { position: absolute; top: calc(100% + var(--s1)); left: 50%; z-index: 20; transform: translateX(-50%) translateY(calc(var(--s1) * -1)); width: max-content; max-width: min(18rem, 80vw); padding: var(--s2) var(--s3); border-radius: var(--r-md); background: var(--ink); color: var(--white); font: var(--t-ayuda); line-height: 1.4; box-shadow: var(--sombra-2); display: none; opacity: 0; pointer-events: none; transition: opacity var(--mov-micro) ease-out, transform var(--mov-micro) var(--curva-llega); }
    /* Oculto sale del flujo (display: none): un globo invisible pero presente ensanchaba la página en el celular. */
    @starting-style { :host(.visible) .globo { opacity: 0; transform: translateX(-50%) translateY(calc(var(--s1) * -1)); } }
    :host(.visible) .globo { display: block; opacity: 1; transform: translateX(-50%) translateY(0); }
  `,
})
export class Tooltip {
  private static secuencia = 0
  readonly id = `ap-tooltip-${++Tooltip.secuencia}`
  readonly texto = input.required<string>()
  readonly visible = signal(false)
  private descartado = false

  mostrar(): void {
    if (!this.descartado) this.visible.set(true)
  }
  ocultar(): void {
    this.visible.set(false)
    this.descartado = false
  }
  /** Escape lo cierra y lo deja cerrado hasta que el puntero o el foco se vayan y vuelvan. */
  cerrarConEscape(): void {
    this.descartado = true
    this.visible.set(false)
  }
}
