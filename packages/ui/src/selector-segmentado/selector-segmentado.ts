import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'

export type Segmento = { valor: string; texto: string }

/**
 * Dos a cuatro vistas equivalentes (lista/calendario, QR/código). Es un `radiogroup`
 * con flechas; **el elegido lleva relleno de marca** (regla 3) y área táctil completa.
 */
@Component({
  selector: 'ap-selector-segmentado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'radiogroup', '[attr.aria-label]': 'etiqueta()' },
  template: `
    @for (s of segmentos(); track s.valor; let i = $index) {
      <button type="button" role="radio" [attr.aria-checked]="elegido() === s.valor" [tabindex]="elegido() === s.valor ? 0 : -1" (click)="elegido.set(s.valor)" (keydown)="teclas($event, i)">{{ s.texto }}</button>
    }
  `,
  styles: `
    :host { display: inline-flex; padding: var(--s1); border-radius: var(--r-md); background: var(--surface-2); gap: var(--s1); max-width: 100%; }
    button { flex: 1; min-height: var(--area-tactil); padding: 0 var(--s4); border: 0; border-radius: var(--r-sm); background: transparent; color: var(--text-2); font: inherit; font-weight: 600; cursor: pointer; white-space: nowrap; }
    button[aria-checked="true"] { background: var(--verde-solido); color: var(--sobre-verde-solido); }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class SelectorSegmentado {
  readonly etiqueta = input.required<string>()
  readonly segmentos = input.required<Segmento[]>()
  readonly elegido = model<string>('')
  teclas(e: KeyboardEvent, i: number): void {
    const n = this.segmentos().length
    const salto = e.key === 'ArrowRight' || e.key === 'ArrowDown' ? 1 : e.key === 'ArrowLeft' || e.key === 'ArrowUp' ? -1 : 0
    if (salto === 0) return
    e.preventDefault()
    const destino = this.segmentos()[(i + salto + n) % n]!
    this.elegido.set(destino.valor)
    const botones = (e.currentTarget as HTMLElement).parentElement?.querySelectorAll<HTMLElement>('button')
    botones?.[(i + salto + n) % n]?.focus()
  }
}
