import { ChangeDetectionStrategy, Component, computed, input, model, output, TemplateRef, contentChild } from '@angular/core'
import { NgTemplateOutlet } from '@angular/common'
import { Casilla } from '../casilla/casilla'
import { Icono } from '../icono/icono'

export type Columna<T> = { clave: keyof T & string; titulo: string; ordenable?: boolean; numerica?: boolean }
export type Orden = { clave: string; sentido: 'asc' | 'desc' }
export type Densidad = 'confortable' | 'compacta'

/**
 * Estado de una colección paginada, provisto por quien consume la tabla (regla: "pagina y
 * filtra quien la usa; ella solo muestra la página" — la tabla nunca hace `fetch`).
 *
 * **Contrato provisorio (regla 65 / ambigüedad Q-J1 del carril PR7).** El tipo de estado
 * "real" del repo (`ViewState`) lo publica Leo en PR8 y todavía no está en `dev`. Mientras
 * tanto este es el doble en tres niveles declarado, contra el que cierra este carril:
 *
 * - **correcto** — `{ tipo: 'lista', filas }` con una colección de filas válidas.
 * - **límite** — `{ tipo: 'lista', filas: [] }` (colección vacía), `{ tipo: 'lista', filas: [f] }`
 *   (una sola fila) y `{ tipo: 'lista', filas, obsoleta: true }` (la respuesta vigente se marcó
 *   vieja porque ya se pidió una más nueva: la tabla la sigue mostrando tal cual, nunca la
 *   reemplaza ni resucita una selección que el consumidor ya descartó).
 * - **inválido** — `{ tipo: 'error', idPeticion, motivo, mensaje }`, con `motivo` distinguiendo
 *   un error sin permiso, un recurso no encontrado y un error genérico con su propio mensaje.
 *
 * **Microtarea de adopción diferida (declarada, no resuelta acá):** cuando `ViewState<T>` esté
 * en `dev`, se agrega un adaptador `ViewState<T[]> → EstadoColeccion<T>` en el punto donde cada
 * consumidor arma este valor; la plantilla del organismo no cambia porque ya consume la unión
 * discriminada por `tipo`, no una forma concreta de `ViewState`.
 */
export type EstadoColeccion<T> =
  | { tipo: 'cargando' }
  | { tipo: 'lista'; filas: T[]; obsoleta?: boolean }
  | { tipo: 'error'; idPeticion: string; motivo: 'sin-permiso' | 'no-encontrado' | 'desconocido'; mensaje: string }

/**
 * El organismo canónico de tabla de datos (único; ver `tabla-de-datos-virtualizada` en
 * `apps/backoffice/nucleo/tabla` como excepción documentada — PR7 §H4.S1.M3): identidad de
 * fila por función pura tipada, columnas tipadas, selección, orden por columna (`aria-sort`)
 * y, opcionalmente, el estado de una colección paginada por el consumidor (`estado()`).
 *
 * Pagina y filtra quien la usa; ella solo muestra la página — la paginación (por índice, con
 * `ap-paginacion`) se compone **al lado** de este organismo en la plantilla del consumidor, no
 * dentro de él, igual que ya hace `tabla-de-datos-virtualizada`. Ninguna condición de acá
 * depende del nombre de una pantalla: las diferencias de apariencia se resuelven con
 * `densidad()` (variante semántica) y con tokens, nunca con un caso especial por consumidor.
 *
 * `estado()` es aditivo y opcional: si no se pasa (el caso de los ocho consumidores previos
 * de `apps/backoffice/rutas/sistemas/*`), el comportamiento es idéntico al de antes de esta
 * extensión — `filas()` se usa tal cual, sin regiones de carga/error/vacío nuevas.
 */
@Component({
  selector: 'ap-tabla-de-datos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Casilla, Icono, NgTemplateOutlet],
  host: { '[class.densidad-compacta]': "densidad() === 'compacta'" },
  template: `
    <div class="marco">
      @if (estado(); as e) {
        @if (e.tipo === 'cargando') {
          <p class="mensaje-estado" role="status" aria-live="polite">{{ textoCargando() }}</p>
        } @else if (e.tipo === 'error') {
          <p class="mensaje-estado mensaje-error" role="alert">{{ e.mensaje }}</p>
        } @else if (e.tipo === 'lista' && e.filas.length === 0) {
          <p class="mensaje-estado">{{ textoVacio() }}</p>
        }
        @if (e.tipo === 'lista' && e.obsoleta) {
          <p class="mensaje-obsoleta" role="status" aria-live="polite">{{ textoActualizando() }}</p>
        }
      }
      @if (!ocultarTabla()) {
        <table role="table" [attr.aria-label]="titulo()">
          <caption>{{ titulo() }}</caption>
          <thead role="rowgroup">
            <tr role="row">
              @if (seleccionable()) {
                <th scope="col" role="columnheader" class="marca">
                  <ap-casilla [marcada]="todasMarcadas()" (marcadaChange)="marcarTodas($event)"><span class="oculto">Seleccionar todas</span></ap-casilla>
                </th>
              }
              @for (c of columnas(); track c.clave) {
                <th scope="col" role="columnheader" [class.numerica]="c.numerica" [attr.aria-sort]="ariaSort(c.clave)">
                  @if (c.ordenable) {
                    <button type="button" (click)="ordenarPor(c.clave)">
                      {{ c.titulo }}
                      @if (orden()?.clave === c.clave) {
                        <ap-icono [nombre]="orden()!.sentido === 'asc' ? 'chevronAbajo' : 'chevronDerecha'" tamano="s4" />
                        <span class="oculto">, orden {{ orden()!.sentido === 'asc' ? 'ascendente' : 'descendente' }}</span>
                      }
                    </button>
                  } @else {
                    {{ c.titulo }}
                  }
                </th>
              }
            </tr>
          </thead>
          <tbody role="rowgroup">
            @for (f of filasVisibles(); track identidad()(f)) {
              <tr role="row" [class.marcada]="elegidas().includes(identidad()(f))">
                @if (seleccionable()) {
                  <td role="cell" class="marca">
                    <ap-casilla [marcada]="elegidas().includes(identidad()(f))" (marcadaChange)="marcar(identidad()(f), $event)"><span class="oculto">Seleccionar fila</span></ap-casilla>
                  </td>
                }
                @for (c of columnas(); track c.clave) {
                  <td role="cell" [class.numerica]="c.numerica">
                    <span class="etiqueta-movil" aria-hidden="true">{{ c.titulo }}</span>
                    @if (celda()) {
                      <ng-container *ngTemplateOutlet="celda()!; context: { $implicit: f, columna: c }" />
                    } @else {
                      {{ f[c.clave] }}
                    }
                  </td>
                }
              </tr>
            }
          </tbody>
        </table>
      }
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
    tr.marcada td { background: var(--brand-bg); }
    button { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); margin: calc(var(--s2) * -1) 0; padding: 0; border: 0; background: transparent; color: inherit; font: inherit; font-weight: 600; cursor: pointer; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); border-radius: var(--r-sm); }
    .oculto { position: absolute; width: var(--borde-fino); height: var(--borde-fino); overflow: hidden; clip: rect(0 0 0 0); }
    .mensaje-estado { margin: 0; padding: var(--s4); color: var(--text-2); }
    .mensaje-error { color: var(--rojo-texto, var(--text)); }
    .mensaje-obsoleta { margin: 0; padding: 0 var(--s4) var(--s3); color: var(--text-3, var(--text-2)); font-size: .85em; }
    .etiqueta-movil { display: none; }

    /* Densidad: variante semántica, no un nombre de pantalla (H3.S1.M3). */
    :host(.densidad-compacta) th, :host(.densidad-compacta) td { padding: var(--s2) var(--s3); }

    /* Colapso a tarjetas en móvil estrecho: ninguna columna ni acción del escritorio se
     * pierde, solo se reordenan como pares etiqueta/valor dentro de una tarjeta por fila.
     * Los role explícitos de arriba sostienen la semántica de tabla aunque el layout deje
     * de ser table visualmente (WAI-ARIA Authoring Practices, "responsive tables"). */
    @media (max-width: 640px) {
      .marco { overflow-x: visible; border: 0; background: transparent; }
      table, thead, tbody, tr, th, td { display: block; width: 100%; }
      thead tr { position: absolute; width: var(--borde-fino); height: var(--borde-fino); overflow: hidden; clip: rect(0 0 0 0); }
      tbody tr { margin-bottom: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); padding: var(--s2) var(--s4); }
      tbody tr:last-child { margin-bottom: 0; }
      tbody tr.marcada { background: var(--brand-bg); }
      td { display: flex; align-items: baseline; justify-content: space-between; gap: var(--s3); border-top: var(--borde-fino) solid var(--border); white-space: normal; text-align: right; }
      td:first-child { border-top: 0; }
      td.numerica { text-align: right; }
      td.marca { justify-content: flex-start; }
      .etiqueta-movil { display: inline-block; flex: none; font-weight: 600; color: var(--text-2); text-align: left; }
    }
  `,
})
export class TablaDeDatos<T extends object> {
  readonly titulo = input.required<string>()
  readonly columnas = input.required<Columna<T>[]>()
  /** Filas visibles cuando no se usa `estado()`. Opcional para no romper a quien ya pasaba `filas` a secas. */
  readonly filas = input<T[]>([])
  readonly identidad = input<(f: T) => string>((f) => String((f as { id?: unknown }).id ?? JSON.stringify(f)))
  readonly seleccionable = input(false)
  readonly densidad = input<Densidad>('confortable')
  readonly elegidas = model<string[]>([])
  readonly orden = model<Orden | null>(null)
  readonly ordenado = output<Orden>()
  readonly celda = contentChild<TemplateRef<{ $implicit: T; columna: Columna<T> }>>('celda')

  /** Estado de colección opcional (regla 65 / Q-J1). `null` conserva el comportamiento previo a esta extensión. */
  readonly estado = input<EstadoColeccion<T> | null>(null)
  readonly textoVacio = input('Sin resultados.')
  readonly textoCargando = input('Cargando…')
  readonly textoActualizando = input('Actualizando…')

  readonly filasVisibles = computed<T[]>(() => {
    const e = this.estado()
    if (e === null) return this.filas()
    return e.tipo === 'lista' ? e.filas : []
  })
  readonly ocultarTabla = computed(() => {
    const e = this.estado()
    return e !== null && e.tipo === 'cargando'
  })
  readonly todasMarcadas = computed(
    () => this.filasVisibles().length > 0 && this.filasVisibles().every((f) => this.elegidas().includes(this.identidad()(f))),
  )

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
    this.elegidas.set(marcada ? this.filasVisibles().map((f) => this.identidad()(f)) : [])
  }
}
