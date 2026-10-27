import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'
import { Icono } from '../icono/icono'

/**
 * «Página 2 de 7 · 134 registros», filas por página, ir a una página y Anterior/Siguiente, como
 * en el portal de Atlas. Dice la página para todos, no solo con la vista. Cambiar las filas por
 * página vuelve a la primera: seguir en la página 6 de otro tamaño muestra otra cosa.
 */
@Component({
  selector: 'ap-paginacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { role: 'navigation', '[attr.aria-label]': 'etiqueta()' },
  template: `
    <span class="resumen" aria-live="polite">Página {{ pagina() }} de {{ total() }}@if (conRegistros()) { · {{ totalDeFilas() }} {{ nombreDeRegistros() }}}</span>
    <div class="controles">
      @if (opcionesPorPagina().length > 1) {
        <label class="por-pagina">
          <span>Filas por página</span>
          <select [value]="porPagina()" (change)="cambiarTamano($any($event.target).value)">
            @for (n of opcionesPorPagina(); track n) { <option [value]="n" [selected]="n === porPagina()">{{ n }}</option> }
          </select>
        </label>
      }
      @if (total() > 2) {
        <form class="ir-a" (submit)="$event.preventDefault(); irA($any(destino).value)">
          <label><span>Ir a</span><input #destino type="number" inputmode="numeric" min="1" [max]="total()" [value]="pagina()" aria-label="Ir a la página" /></label>
        </form>
      }
      <div class="pasos">
        <button type="button" [disabled]="pagina() <= 1" (click)="pagina.set(pagina() - 1)"><ap-icono nombre="chevronIzquierda" tamano="s4" />Anterior</button>
        <button type="button" [disabled]="pagina() >= total()" (click)="pagina.set(pagina() + 1)">Siguiente<ap-icono nombre="chevronDerecha" tamano="s4" /></button>
      </div>
    </div>
  `,
  styles: `
    :host { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s3) var(--s4); color: var(--text-2); font: var(--t-cuerpo-chico); }
    .controles { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s3) var(--s4); }
    label { display: inline-flex; align-items: center; gap: var(--s2); }
    select, input { min-height: var(--area-tactil); padding: 0 var(--s2); border: var(--borde-fino) solid var(--borde-control); border-radius: var(--r-md); background: var(--field); color: var(--text); font: inherit; }
    input { width: calc(var(--s7) + var(--s3)); text-align: center; }
    select:focus-visible, input:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .pasos { display: flex; gap: var(--s2); }
    button { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); color: var(--text); font: inherit; font-weight: 600; cursor: pointer; transition: background-color var(--mov-micro) ease-out; }
    button:hover:not(:disabled) { background: var(--surface-2); }
    button:disabled { opacity: .5; cursor: not-allowed; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class Paginacion {
  /** Dos paginaciones en una misma página necesitan nombres distintos (landmark-unique). */
  readonly etiqueta = input('Paginación')
  readonly pagina = model(1)
  readonly totalDeFilas = input.required<number>()
  readonly porPagina = model(20)
  /** Con una sola opción no se ofrece cambiar el tamaño (el caso de quien pasaba `porPagina` fijo). */
  readonly opcionesPorPagina = input<readonly number[]>([])
  readonly conRegistros = input(true)
  readonly nombreDeRegistros = input('registros')
  readonly total = computed(() => Math.max(1, Math.ceil(this.totalDeFilas() / this.porPagina())))

  cambiarTamano(valor: string): void {
    const n = Number(valor)
    if (!Number.isFinite(n) || n <= 0) return
    this.porPagina.set(n)
    this.pagina.set(1)
  }

  /** Fuera de rango se acota; con basura no se mueve. */
  irA(valor: string): void {
    const pedida = Math.trunc(Number(valor))
    if (!Number.isFinite(pedida)) return
    this.pagina.set(Math.min(Math.max(pedida, 1), this.total()))
  }
}
