import { ChangeDetectionStrategy, Component, computed, input, model, output, TemplateRef, contentChild } from '@angular/core'
import { NgTemplateOutlet } from '@angular/common'
import { Casilla } from '../casilla/casilla'
import { Icono } from '../icono/icono'

export type Columna<T> = { clave: keyof T & string; titulo: string; ordenable?: boolean; numerica?: boolean }
export type Orden = { clave: string; sentido: 'asc' | 'desc' }

/**
 * Tabla del backoffice: orden por columna (`aria-sort`), selección múltiple opcional y
 * celda por plantilla. Pagina y filtra quien la usa; ella solo muestra la página.
 */
@Component({
  selector: 'ap-tabla-de-datos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Casilla, Icono, NgTemplateOutlet],
  template: `
    <div class="marco">
      <table>
        <caption>{{ titulo() }}</caption>
        <thead>
          <tr>
            @if (seleccionable()) { <th scope="col" class="marca"><ap-casilla [marcada]="todasMarcadas()" (marcadaChange)="marcarTodas($event)"><span class="oculto">Seleccionar todas</span></ap-casilla></th> }
            @for (c of columnas(); track c.clave) {
              <th scope="col" [class.numerica]="c.numerica" [attr.aria-sort]="ariaSort(c.clave)">
                @if (c.ordenable) {
                  <button type="button" (click)="ordenarPor(c.clave)">{{ c.titulo }} @if (orden()?.clave === c.clave) { <ap-icono [nombre]="orden()!.sentido === 'asc' ? 'chevronAbajo' : 'chevronDerecha'" tamano="s4" /> }</button>
                } @else { {{ c.titulo }} }
              </th>
            }
          </tr>
        </thead>
        <tbody>
          @for (f of filas(); track identidad()(f)) {
            <tr [class.marcada]="elegidas().includes(identidad()(f))">
              @if (seleccionable()) { <td class="marca"><ap-casilla [marcada]="elegidas().includes(identidad()(f))" (marcadaChange)="marcar(identidad()(f), $event)"><span class="oculto">Seleccionar fila</span></ap-casilla></td> }
              @for (c of columnas(); track c.clave) {
                <td [class.numerica]="c.numerica">
                  @if (celda()) { <ng-container *ngTemplateOutlet="celda()!; context: { $implicit: f, columna: c }" /> } @else { {{ f[c.clave] }} }
                </td>
              }
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: `
    .marco { overflow-x: auto; border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    table { width: 100%; border-collapse: collapse; color: var(--text); }
    caption { text-align: left; padding: var(--s3) var(--s4); font-weight: 600; color: var(--text-2); }
    th, td { padding: var(--s3) var(--s4); text-align: left; border-top: var(--borde-fino) solid var(--border); white-space: nowrap; }
    th { color: var(--text-2); font-size: .9em; background: var(--surface-2); }
    .numerica { text-align: right; font-variant-numeric: tabular-nums; }
    .marca { width: var(--s7); }
    tr.marcada td { background: var(--g100); }
    button { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); margin: calc(var(--s2) * -1) 0; padding: 0; border: 0; background: transparent; color: inherit; font: inherit; font-weight: 600; cursor: pointer; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); border-radius: var(--r-sm); }
    .oculto { position: absolute; width: var(--borde-fino); height: var(--borde-fino); overflow: hidden; clip: rect(0 0 0 0); }
  `,
})
export class TablaDeDatos<T extends object> {
  readonly titulo = input.required<string>()
  readonly columnas = input.required<Columna<T>[]>()
  readonly filas = input.required<T[]>()
  readonly identidad = input<(f: T) => string>((f) => String((f as { id?: unknown }).id ?? JSON.stringify(f)))
  readonly seleccionable = input(false)
  readonly elegidas = model<string[]>([])
  readonly orden = model<Orden | null>(null)
  readonly ordenado = output<Orden>()
  readonly celda = contentChild<TemplateRef<{ $implicit: T; columna: Columna<T> }>>('celda')
  readonly todasMarcadas = computed(() => this.filas().length > 0 && this.filas().every((f) => this.elegidas().includes(this.identidad()(f))))

  ariaSort(clave: string): 'ascending' | 'descending' | 'none' | null {
    const c = this.columnas().find((x) => x.clave === clave)
    if (!c?.ordenable) return null
    const o = this.orden()
    return o?.clave === clave ? (o.sentido === 'asc' ? 'ascending' : 'descending') : 'none'
  }
  ordenarPor(clave: string): void {
    const o = this.orden()
    const nuevo: Orden = { clave, sentido: o?.clave === clave && o.sentido === 'asc' ? 'desc' : 'asc' }
    this.orden.set(nuevo)
    this.ordenado.emit(nuevo)
  }
  marcar(id: string, marcada: boolean): void {
    this.elegidas.set(marcada ? [...this.elegidas(), id] : this.elegidas().filter((x) => x !== id))
  }
  marcarTodas(marcada: boolean): void {
    this.elegidas.set(marcada ? this.filas().map((f) => this.identidad()(f)) : [])
  }
}
