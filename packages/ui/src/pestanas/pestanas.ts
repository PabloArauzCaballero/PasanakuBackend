import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'

export type Pestana = { valor: string; texto: string; cuenta?: number }

/**
 * Pestañas WAI-ARIA con flechas, Inicio y Fin. El panel lo dibuja quien las usa, con `aria-labelledby`.
 * `segmentada`: la variante de ficha del portal de Atlas (fondo suave, la activa en blanco). Quien
 * quiera el estado en la URL (`?vista=`) enlaza `elegida` con la ruta: esta pieza no conoce el router.
 */
@Component({
  selector: 'ap-pestanas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'tablist', '[attr.aria-label]': 'etiqueta()', '[class.segmentada]': 'estilo() === "segmentada"' },
  template: `
    @for (p of pestanas(); track p.valor; let i = $index) {
      <button type="button" role="tab" [id]="'pestana-' + p.valor" [attr.aria-selected]="elegida() === p.valor" [attr.aria-controls]="'panel-' + p.valor" [tabindex]="elegida() === p.valor ? 0 : -1" (click)="elegida.set(p.valor)" (keydown)="teclas($event, i)">
        {{ p.texto }} @if (p.cuenta !== undefined) { <span class="cuenta">{{ p.cuenta }}</span> }
      </button>
    }
  `,
  styles: `
    :host { display: flex; gap: var(--s2); border-bottom: var(--borde-fino) solid var(--border); overflow-x: auto; }
    button { display: inline-flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3); border: 0; border-bottom: var(--borde-foco) solid transparent; margin-bottom: calc(var(--borde-fino) * -1); background: transparent; color: var(--text-2); font: inherit; font-weight: 600; cursor: pointer; white-space: nowrap; }
    button[aria-selected="true"] { color: var(--brand-texto); border-bottom-color: var(--brand); }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: calc(var(--borde-desfase) * -1); }
    .cuenta { padding: 0 var(--s2); border-radius: var(--r-pill); background: var(--surface-2); color: var(--text-2); font-size: .8em; }
    /* Segmentada envuelve: a 360 px la activa quedaba recortada («Opera») y las últimas pestañas, inalcanzables y sin pista. */
    :host(.segmentada) { flex-wrap: wrap; overflow-x: visible; gap: var(--s1); padding: var(--s1); border: var(--borde-fino) solid var(--border); border-bottom-width: var(--borde-fino); border-radius: var(--r-md); background: var(--surface-2); }
    :host(.segmentada) button { min-height: calc(var(--area-tactil) - var(--s2)); margin: 0; border: 0; border-radius: var(--r-sm); color: var(--text-3); transition: background-color var(--mov-micro) ease-out, color var(--mov-micro) ease-out; }
    /* El hover se distingue de la activa: sin fondo claro ni sombra, solo un velo de la tinta; la activa es la única «tarjeta». */
    :host(.segmentada) button:hover:not([aria-selected="true"]) { color: var(--text); background: color-mix(in srgb, var(--text) 7%, transparent); }
    :host(.segmentada) button[aria-selected="true"] { color: var(--text); background: var(--surface); box-shadow: var(--sombra-1), inset 0 0 0 var(--borde-fino) var(--border); }
    button[aria-selected="true"] .cuenta { background: var(--brand-bg); color: var(--brand-ink); }
  `,
})
export class Pestanas {
  readonly etiqueta = input.required<string>()
  readonly pestanas = input.required<Pestana[]>()
  readonly elegida = model<string>('')
  readonly estilo = input<'linea' | 'segmentada'>('linea')
  teclas(e: KeyboardEvent, i: number): void {
    const n = this.pestanas().length
    const salto = e.key === 'ArrowRight' ? 1 : e.key === 'ArrowLeft' ? -1 : 0
    if (salto === 0 && e.key !== 'Home' && e.key !== 'End') return
    e.preventDefault()
    const j = e.key === 'Home' ? 0 : e.key === 'End' ? n - 1 : (i + salto + n) % n
    this.elegida.set(this.pestanas()[j]!.valor)
    ;(e.currentTarget as HTMLElement).parentElement?.querySelectorAll<HTMLElement>('button')[j]?.focus()
  }
}
