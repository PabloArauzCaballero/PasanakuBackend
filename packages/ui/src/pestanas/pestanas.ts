import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'

export type Pestana = { valor: string; texto: string; cuenta?: number }

/** Pestañas WAI-ARIA con flechas. El panel lo dibuja quien las usa, con `aria-labelledby`. */
@Component({
  selector: 'ap-pestanas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'tablist', '[attr.aria-label]': 'etiqueta()' },
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
    button[aria-selected="true"] .cuenta { background: var(--g100); color: var(--brand-ink); }
  `,
})
export class Pestanas {
  readonly etiqueta = input.required<string>()
  readonly pestanas = input.required<Pestana[]>()
  readonly elegida = model<string>('')
  teclas(e: KeyboardEvent, i: number): void {
    const n = this.pestanas().length
    const salto = e.key === 'ArrowRight' ? 1 : e.key === 'ArrowLeft' ? -1 : 0
    if (salto === 0) return
    e.preventDefault()
    const j = (i + salto + n) % n
    this.elegida.set(this.pestanas()[j]!.valor)
    ;(e.currentTarget as HTMLElement).parentElement?.querySelectorAll<HTMLElement>('button')[j]?.focus()
  }
}
