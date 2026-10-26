import { ChangeDetectionStrategy, Component, computed, input, model, output, TemplateRef, contentChild } from '@angular/core'
import { NgTemplateOutlet } from '@angular/common'
import { BotonCopiar } from '../boton-copiar/boton-copiar'
import { Casilla } from '../casilla/casilla'
import { Esqueleto } from '../esqueleto/esqueleto'
import { Girador } from '../girador/girador'
import { Icono } from '../icono/icono'

import type { Columna, Densidad, EstadoColeccion, Orden } from './tabla-de-datos-tipos'
export type { Columna, Densidad, EstadoColeccion, Orden } from './tabla-de-datos-tipos'

/**
 * El organismo canónico de tabla de datos (único; ver `tabla-de-datos-virtualizada` en
 * `apps/backoffice/nucleo/tabla` como excepción documentada — PR7 §H4.S1.M3): identidad de
 * fila por función pura tipada, columnas tipadas, selección, orden por columna (`aria-sort`)
 * estados de carga con girador y esqueleto y, opcionalmente, el estado de una colección paginada por el consumidor (`estado()`).
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
  imports: [BotonCopiar, Casilla, Icono, NgTemplateOutlet, Girador, Esqueleto],
  host: { '[class.densidad-compacta]': "densidad() === 'compacta'" },
  template: `
    <div class="marco" [class.con-cabecera]="cabecera()">
      @if (cabecera()) {
        <div class="cabecera-tabla">
          <span class="registros">{{ textoRegistros() }}</span>
          <span class="cuenta"><ap-boton-copiar [valor]="comoTexto" etiqueta="Copiar la tabla" /><span class="total">{{ totalDeRegistros() ?? filasVisibles().length }}</span></span>
        </div>
      }
      @if (estado(); as e) {
        @if (e.tipo === 'cargando') {
          <div class="mensaje-estado cargando-contenedor" role="status" aria-live="polite">
            <div class="aviso-cargando">
              <ap-girador tamano="s4" etiqueta="Cargando datos" />
              <span>{{ textoCargando() }}</span>
            </div>
            <ap-esqueleto forma="filas" [lineas]="5" etiqueta="Cargando filas" />
          </div>
        } @else if (e.tipo === 'error') {
          <p class="mensaje-estado mensaje-error" role="alert">{{ e.mensaje }}</p>
        } @else if (e.tipo === 'lista' && e.filas.length === 0) {
          <p class="mensaje-estado">{{ textoVacio() }}</p>
        }
        @if (e.tipo === 'lista' && e.obsoleta) {
          <div class="mensaje-obsoleta" role="status" aria-live="polite">
            <ap-girador tamano="s4" etiqueta="Actualizando" />
            <span>{{ textoActualizando() }}</span>
          </div>
        }
      }
      @if (!ocultarTabla()) {
        <div class="desplazable" [class.acotada]="altoMaximo()" [attr.role]="altoMaximo() ? 'region' : null" [attr.aria-label]="altoMaximo() ? titulo() : null" [attr.tabindex]="altoMaximo() ? 0 : null">
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
                <th scope="col" role="columnheader" [class.numerica]="c.numerica" [class.fija]="c.fijaDerecha" [attr.aria-sort]="ariaSort(c.clave)">
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
                  <td role="cell" [class.numerica]="c.numerica" [class.fija]="c.fijaDerecha" [class.mono]="c.mono">
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
        </div>
      }
    </div>
  `,
  styleUrl: './tabla-de-datos.css',
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
  /** Anatomía del portal de Atlas: cabecera «Registros · n» con copiar, y alto acotado con desplazamiento propio. */
  readonly cabecera = input(false)
  readonly textoRegistros = input('Registros')
  /** El total del servidor cuando la página es una parte; sin él, cuenta las filas visibles. */
  readonly totalDeRegistros = input<number>()
  readonly altoMaximo = input(false)
  /** La tabla como texto tabulado (encabezados + valores crudos), para el botón de copiar. */
  readonly comoTexto = (): string =>
    [this.columnas().map((c) => c.titulo), ...this.filasVisibles().map((f) => this.columnas().map((c) => String(f[c.clave] ?? '')))].map((l) => l.join('\t')).join('\n')

  readonly filasVisibles = computed<T[]>(() => {
    const e = this.estado()
    if (e === null) return this.filas()
    return e.tipo === 'lista' ? e.filas : []
  })
  readonly ocultarTabla = computed(() => {
    const e = this.estado()
    return e !== null && (e.tipo === 'cargando' || e.tipo === 'error')
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
