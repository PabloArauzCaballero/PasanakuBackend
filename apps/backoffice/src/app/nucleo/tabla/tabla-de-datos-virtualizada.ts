import { ScrollingModule, CdkVirtualScrollViewport } from '@angular/cdk/scrolling'
import {
  ChangeDetectionStrategy,
  Component,
  TemplateRef,
  ViewChild,
  computed,
  contentChild,
  effect,
  inject,
  input,
  model,
  signal,
  untracked,
} from '@angular/core'
import { DOCUMENT, NgTemplateOutlet } from '@angular/common'
import { Casilla } from '@aportaya/ui/casilla/casilla'; import { Icono } from '@aportaya/ui/icono/icono'; import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { Esqueleto } from '@aportaya/ui/esqueleto/esqueleto'
import { Girador } from '@aportaya/ui/girador/girador'
import type { CargadorDePagina, ColumnaVirtual, OrdenVirtual } from './tipos'
import { accionParaTecla, enfocarFilaPorId } from './navegacion-por-teclado'

/**
 * El organismo que van a usar las 64 pantallas de `F7`/`F8`: columnas configurables,
 * paginación **del servidor** (`cargador()` la trae; nunca pagina en memoria), orden con
 * lista blanca (pedir un campo fuera de `ordenPermitido()` se **rechaza visiblemente**,
 * no se ignora), selección múltiple y virtualización con el CDK. No se acopla a ningún
 * caso particular: no importa nada de `dominio/`, y la celda se proyecta por plantilla.
 */
@Component({
  selector: 'ap-tabla-de-datos-virtualizada',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ScrollingModule, Casilla, Icono, Paginacion, NgTemplateOutlet, Girador, Esqueleto],
  host: { class: 'ap-tdv' },
  template: `
    <div class="cabecera-tabla">
      <h2>{{ titulo() }}</h2>
      @if (cargando()) {
        <div class="indicador-carga" role="status" aria-live="polite">
          <ap-girador tamano="s4" etiqueta="Cargando" />
          <span class="cargando">Cargando…</span>
        </div>
      }
    </div>
    @if (error()) { <p role="alert" class="error">{{ error() }}</p> }

    <div class="marco" role="table" [attr.aria-label]="titulo()" [attr.aria-rowcount]="total()">
      <div class="fila fila-cabecera" role="row">
        @if (seleccionable()) {
          <div class="celda marca" role="columnheader"><ap-casilla [marcada]="todasMarcadas()" (marcadaChange)="marcarTodas($event)"><span class="oculto">Seleccionar todas las filas de la página</span></ap-casilla></div>
        }
        @for (c of columnas(); track c.clave) {
          <div class="celda" role="columnheader" [class.numerica]="c.numerica" [style.flex]="c.ancho ?? '1'" [attr.aria-sort]="ariaSort(c.clave)">
            @if (c.ordenable) {
              <button type="button" (click)="ordenarPor(c.clave)">
                {{ c.titulo }}
                @if (orden()?.clave === c.clave) { <ap-icono [nombre]="orden()!.sentido === 'asc' ? 'chevronAbajo' : 'chevronDerecha'" tamano="s4" /> }
              </button>
            } @else {
              {{ c.titulo }}
            }
          </div>
        }
      </div>
      <div class="contenedor-cuerpo">
        @if (cargando() && filas().length === 0) {
          <!-- Decorativo: dentro de role="table" solo caben filas. El aviso «Cargando…» de la cabecera ya lo anuncia. -->
          <div class="esqueleto-superpuesto" aria-hidden="true">
            <ap-esqueleto forma="filas" [lineas]="5" etiqueta="Cargando registros…" />
          </div>
        }
        <cdk-virtual-scroll-viewport [itemSize]="alturaDeFila()" class="viewport" [style.height.px]="alturaDeViewport()">
          <div
            *cdkVirtualFor="let f of filas(); let i = index; trackBy: identidadDeIndice"
            class="fila"
            role="row"
            [attr.aria-rowindex]="i + 2"
            [tabindex]="i === focoIndice() ? 0 : -1"
            [class.marcada]="elegidas().includes(identidad()(f))"
            [class.con-foco]="i === focoIndice()"
            [attr.data-fila-id]="identidad()(f)"
            (focus)="focoIndice.set(i)"
            (keydown)="onKeydown($event, i)"
          >
            @if (seleccionable()) {
              <div class="celda marca" role="cell"><ap-casilla [marcada]="elegidas().includes(identidad()(f))" (marcadaChange)="marcar(identidad()(f), $event)"><span class="oculto">Seleccionar fila</span></ap-casilla></div>
            }
            @for (c of columnas(); track c.clave) {
              <div class="celda" role="cell" [class.numerica]="c.numerica" [style.flex]="c.ancho ?? '1'" [attr.title]="celda() ? null : f[c.clave]">
                @if (celda()) {
                  <ng-container *ngTemplateOutlet="celda()!; context: { $implicit: f, columna: c }" />
                } @else {
                  {{ f[c.clave] }}
                }
              </div>
            }
          </div>
        </cdk-virtual-scroll-viewport>
      </div>
    </div>

    <ap-paginacion [(pagina)]="pagina" [totalDeFilas]="total()" [porPagina]="tamanoDePagina()" />
  `,
  styleUrl: './tabla-de-datos-virtualizada.css',
})
export class TablaDeDatosVirtualizada<T extends object> {
  readonly titulo = input.required<string>()
  readonly columnas = input.required<ColumnaVirtual<T>[]>()
  readonly cargador = input.required<CargadorDePagina<T>>()
  /** Campos que el servidor acepta para ordenar; pedir otro se rechaza, no se ignora. */
  readonly ordenPermitido = input.required<readonly string[]>()
  readonly identidad = input<(f: T) => string>((f) => String((f as { id?: unknown }).id ?? JSON.stringify(f)))
  readonly seleccionable = input(false)
  readonly tamanoDePagina = input(50)
  readonly alturaDeFila = input(48)
  readonly alturaDeViewport = input(480)
  /** Filtros ya resueltos por la pantalla (típicamente atados a la URL vía `BarraDeFiltros`). */
  readonly filtros = input<Readonly<Record<string, string>>>({})
  readonly pagina = model(1)
  readonly orden = model<OrdenVirtual | null>(null)
  readonly elegidas = model<string[]>([])
  readonly celda = contentChild<TemplateRef<{ $implicit: T; columna: ColumnaVirtual<T> }>>('celda')
  @ViewChild(CdkVirtualScrollViewport) private viewport?: CdkVirtualScrollViewport
  private readonly documento = inject(DOCUMENT)
  private readonly filasSig = signal<T[]>([])
  private readonly totalSig = signal(0)
  readonly cargando = signal(false)
  readonly error = signal<string | null>(null)
  readonly focoIndice = signal(0)
  readonly filas = computed(() => this.filasSig())
  readonly total = computed(() => this.totalSig())
  readonly todasMarcadas = computed(() => this.filas().length > 0 && this.filas().every((f) => this.elegidas().includes(this.identidad()(f))))
  identidadDeIndice = (_: number, f: T) => this.identidad()(f)

  /** Recarga con página/orden/filtros nuevos. `untracked` evita un segundo ciclo. */
  constructor() {
    effect(() => {
      const pedido = { pagina: this.pagina(), tamano: this.tamanoDePagina(), orden: this.orden(), filtros: this.filtros() }
      untracked(() => this.cargar(pedido))
    })
  }

  private async cargar(pedido: { pagina: number; tamano: number; orden: OrdenVirtual | null; filtros: Readonly<Record<string, string>> }): Promise<void> {
    this.cargando.set(true)
    try {
      const respuesta = await this.cargador()(pedido)
      this.filasSig.set(respuesta.filas)
      this.totalSig.set(respuesta.total)
      this.focoIndice.set(0)
    } finally {
      this.cargando.set(false)
    }
  }
  ariaSort(clave: string): 'ascending' | 'descending' | 'none' | null {
    const c = this.columnas().find((x) => x.clave === clave)
    if (!c?.ordenable) return null
    const o = this.orden()
    return o?.clave === clave ? (o.sentido === 'asc' ? 'ascending' : 'descending') : 'none'
  }
  ordenarPor(clave: string): void {
    if (!this.ordenPermitido().includes(clave)) {
      this.error.set(`No se puede ordenar por "${clave}": no está en la lista de campos permitidos.`)
      return
    }
    this.error.set(null)
    const o = this.orden()
    const nuevo: OrdenVirtual = { clave, sentido: o?.clave === clave && o.sentido === 'asc' ? 'desc' : 'asc' }
    this.orden.set(nuevo)
  }

  marcar(id: string, marcada: boolean): void {
    this.elegidas.set(marcada ? [...this.elegidas(), id] : this.elegidas().filter((x) => x !== id))
  }
  marcarTodas(marcada: boolean): void {
    this.elegidas.set(marcada ? this.filas().map((f) => this.identidad()(f)) : [])
  }

  /** Flechas mueven el foco, Inicio/Fin a los extremos, Espacio/Enter alterna selección. */
  onKeydown(evento: KeyboardEvent, indice: number): void {
    const accion = accionParaTecla(evento.key, indice, this.filas().length - 1, this.seleccionable())
    if (!accion) return
    evento.preventDefault()
    if (accion.tipo === 'mover') {
      this.moverFoco(accion.indice)
    } else {
      const fila = this.filas()[indice]
      if (fila) this.marcar(this.identidad()(fila), !this.elegidas().includes(this.identidad()(fila)))
    }
  }

  private moverFoco(indice: number): void {
    this.focoIndice.set(indice)
    const fila = this.filas()[indice]
    this.viewport?.scrollToIndex(indice)
    if (fila) enfocarFilaPorId(this.documento, this.identidad()(fila))
  }
}
