import { ChangeDetectionStrategy, Component, TemplateRef, computed, contentChild, input, output } from '@angular/core'
import { NgTemplateOutlet } from '@angular/common'
import { BarraDeFiltros } from '../barra-de-filtros/barra-de-filtros'
import { Esqueleto } from '../esqueleto/esqueleto'
import { EstadoDeError } from '../estado-de-error/estado-de-error'
import { EstadoVacio } from '../estado-vacio/estado-vacio'
import { ExportarListado } from '../exportar-listado/exportar-listado'
import type { ColumnaExportable } from '../exportar-listado/exportar'
import { Paginacion } from '../paginacion/paginacion'
import { TablaDeDatos, type Columna, type Densidad } from '../tabla-de-datos/tabla-de-datos'
import type { Listado } from './crear-listado'

/** La columna que agrega `#acciones`: no choca con un campo real porque ningún contrato usa este nombre. */
const CLAVE_ACCIONES = '__acciones'

/** De dónde están las filas: el listado no sabe de red, solo de este estado. `lista` = las filas ya están en `listado`. */
export type CargaDeListado = { tipo: 'cargando' } | { tipo: 'lista' } | { tipo: 'error'; mensaje: string; idPeticion?: string }

/**
 * **El listado del backoffice, una sola anatomía para todas las vistas** (2026-10-26):
 *
 * 1. barra de filtros — buscador multicampo + un selector por criterio + «Limpiar»;
 * 2. resumen «n de m registros», acciones del listado (proyectadas con `[accionesDeListado]`) y
 *    «Descargar CSV / JSON» de **todo lo filtrado**;
 * 3. la tabla con la página visible, orden por columna y la columna de acciones de fila;
 * 4. paginación: filas por página, ir a la página N, Anterior y Siguiente.
 *
 * Resuelve los estados: cargando (esqueleto), error (con reintento y su identificador de petición), vacío sin datos
 * (con su explicación y, si se pasa, la primera acción) y vacío por filtro (con «Quitar filtros»). La celda se
 * dibuja con `<ng-template #celda let-fila let-columna="columna">` igual que en `ap-tabla-de-datos`; las acciones de
 * fila, con `<ng-template #acciones let-fila>` (típicamente un `ap-acciones-de-fila`): con esa plantilla el listado
 * agrega al final la columna «Acciones», fija a la derecha, y la deja fuera de la exportación.
 */
@Component({
  selector: 'ap-listado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, Esqueleto, EstadoDeError, EstadoVacio, ExportarListado, NgTemplateOutlet, Paginacion, TablaDeDatos],
  template: `
    @if (conFiltros()) {
      <ap-barra-de-filtros [etiqueta]="'Filtros de ' + titulo()" [etiquetaDeBusqueda]="etiquetaDeBusqueda()" [ayudaDeBusqueda]="ayudaDeBusqueda()" [busqueda]="listado().busqueda()" [selectores]="listado().selectores()" textoLimpiar="Limpiar filtros" (busquedaChange)="listado().buscar($event)" (selectorChange)="listado().filtrar($event.nombre, $event.valor)" (limpiar)="listado().limpiar()" />
    }
    <div class="barra">
      <p class="resumen" aria-live="polite">{{ resumen() }}</p>
      <div class="acciones"><ng-content select="[accionesDeListado]" />
        @if (exportable()) {
          <ap-exportar-listado [nombre]="nombreDeExportacion() ?? titulo()" [columnas]="columnasExportables()" [filas]="listado().filtradas()" [conJson]="conJson()" [deshabilitado]="carga().tipo !== 'lista'" />
        }
      </div>
    </div>
    <ng-template #combinada let-f let-c="columna">
      @if (c.clave === CLAVE_ACCIONES) { <ng-container *ngTemplateOutlet="acciones()!; context: { $implicit: f }" /> }
      @else if (celda(); as p) { <ng-container *ngTemplateOutlet="p; context: { $implicit: f, columna: c }" /> }
      @else { {{ crudo(f, c.clave) }} }
    </ng-template>
    @switch (carga().tipo) {
      @case ('cargando') { <ap-esqueleto [lineas]="6" [etiqueta]="'Cargando ' + titulo()" /> }
      @case ('error') { <ap-estado-de-error [titulo]="'No se pudo cargar ' + titulo().toLowerCase() + '.'" [mensaje]="mensajeDeError()" [trazaId]="idDePeticion()" (reintentar)="reintentar.emit()" /> }
      @default {
        @if (listado().total() === 0) {
          @if (listado().hayCriterios()) {
            <ap-estado-vacio motivo="porFiltro" titulo="Ningún registro coincide con la búsqueda o los filtros." accionPropia="Quitar filtros" (actuar)="listado().limpiar()" />
          } @else {
            <ap-estado-vacio [titulo]="textoVacio()" [explicacion]="explicacionVacio()" [accionPropia]="accionVacio() ?? ''" (actuar)="primeraAccion.emit()" />
          }
        } @else {
          <ap-tabla-de-datos [titulo]="titulo()" [columnas]="columnasVisibles()" [filas]="listado().visibles()" [identidad]="identidad()" [densidad]="densidad()" [orden]="listado().orden()" (ordenado)="listado().orden.set($event)" [plantillaDeCelda]="combinada" />
          <ap-paginacion [etiqueta]="'Paginación de ' + titulo()" [pagina]="listado().pagina()" (paginaChange)="listado().irAPagina($event)" [totalDeFilas]="listado().total()" [porPagina]="listado().porPagina()" (porPaginaChange)="listado().cambiarPorPagina($event)" [opcionesPorPagina]="listado().opcionesPorPagina" [nombreDeRegistros]="nombreDeRegistros()" />
        }
      }
    }
  `,
  styles: `
    :host { display: grid; gap: var(--s4); min-width: 0; }
    .barra { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s3); }
    .resumen { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
    .acciones { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s2); }
  `,
})
export class ListadoDeDatos<T extends object> {
  readonly titulo = input.required<string>()
  readonly columnas = input.required<Columna<T>[]>()
  readonly listado = input.required<Listado<T>>()
  readonly carga = input<CargaDeListado>({ tipo: 'lista' })
  readonly identidad = input<(f: T) => string>((f) => String((f as { id?: unknown }).id ?? JSON.stringify(f)))
  readonly densidad = input<Densidad>('confortable')
  readonly conFiltros = input(true)
  readonly etiquetaDeBusqueda = input('Buscar')
  /** Dónde busca el buscador, en palabras: «Busca por nombre, documento o código». */
  readonly ayudaDeBusqueda = input<string>()
  readonly exportable = input(true)
  readonly conJson = input(true)
  readonly nombreDeExportacion = input<string>()
  /** Las columnas del archivo con los valores como se ven. Sin ellas: las de la tabla (sin la de acciones). */
  readonly exportables = input<readonly ColumnaExportable<T>[]>()
  readonly nombreDeRegistros = input('registros')
  readonly textoVacio = input('Todavía no hay registros.')
  readonly explicacionVacio = input<string>()
  /** La primera acción cuando no hay datos («Registrar el primero»); sin ella, no se ofrece ninguna. */
  readonly accionVacio = input<string>()
  readonly reintentar = output<void>()
  readonly primeraAccion = output<void>()
  readonly celda = contentChild<TemplateRef<{ $implicit: T; columna: Columna<T> }>>('celda')
  readonly acciones = contentChild<TemplateRef<{ $implicit: T }>>('acciones')
  readonly textoAcciones = input('Acciones')
  protected readonly CLAVE_ACCIONES = CLAVE_ACCIONES
  readonly columnasVisibles = computed<Columna<T>[]>(() =>
    this.acciones() ? [...this.columnas(), { clave: CLAVE_ACCIONES as keyof T & string, titulo: this.textoAcciones(), fijaDerecha: true }] : this.columnas(),
  )

  readonly columnasExportables = computed<readonly ColumnaExportable<T>[]>(
    () => this.exportables() ?? this.columnas().filter((c) => c.clave !== CLAVE_ACCIONES).map((c) => ({ clave: c.clave, titulo: c.titulo })),
  )
  protected crudo(f: T, clave: string): unknown {
    return (f as Record<string, unknown>)[clave]
  }
  readonly resumen = computed(() => {
    if (this.carga().tipo !== 'lista') return ''
    const l = this.listado()
    return l.hayCriterios() ? `${l.total()} de ${l.filas().length} ${this.nombreDeRegistros()}` : `${l.total()} ${this.nombreDeRegistros()}`
  })
  readonly mensajeDeError = computed(() => {
    const c = this.carga()
    return c.tipo === 'error' ? c.mensaje : undefined
  })
  readonly idDePeticion = computed(() => {
    const c = this.carga()
    return c.tipo === 'error' ? c.idPeticion : undefined
  })
}
