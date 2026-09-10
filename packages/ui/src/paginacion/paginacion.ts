import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'

/** Página actual de N, anterior y siguiente. Dice «Página 2 de 7» para todos, no solo con la vista. */
@Component({
  selector: 'ap-paginacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { role: 'navigation', 'aria-label': 'Paginación' },
  template: `
    <button type="button" [disabled]="pagina() <= 1" (click)="pagina.set(pagina() - 1)">Anterior</button>
    <span aria-live="polite">Página {{ pagina() }} de {{ total() }}</span>
    <button type="button" [disabled]="pagina() >= total()" (click)="pagina.set(pagina() + 1)">Siguiente</button>
  `,
  styles: `
    :host { display: flex; align-items: center; justify-content: center; gap: var(--s4); color: var(--text-2); }
    button { min-height: var(--area-tactil); padding: 0 var(--s4); border: var(--borde-fino) solid var(--brand); border-radius: var(--r-md); background: transparent; color: var(--brand-texto); font: inherit; font-weight: 600; cursor: pointer; }
    button:disabled { opacity: .5; cursor: not-allowed; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class Paginacion {
  readonly pagina = model(1)
  readonly totalDeFilas = input.required<number>()
  readonly porPagina = input(20)
  readonly total = computed(() => Math.max(1, Math.ceil(this.totalDeFilas() / this.porPagina())))
}
