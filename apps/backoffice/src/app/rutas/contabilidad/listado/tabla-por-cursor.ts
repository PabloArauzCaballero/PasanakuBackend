import { ChangeDetectionStrategy, Component, TemplateRef, computed, contentChild, input } from '@angular/core'
import { NgTemplateOutlet } from '@angular/common'
import { BarraDeFiltros } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { Esqueleto } from '@aportaya/ui/esqueleto/esqueleto'
import { EstadoDeError } from '@aportaya/ui/estado-de-error/estado-de-error'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { ExportarListado } from '@aportaya/ui/exportar-listado/exportar-listado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ListadoPorCursor } from './listado-por-cursor'

const CLAVE_ACCIONES = '__acciones'

/**
 * **`ap-listado` para colecciones paginadas por cursor en el servidor** (la guía de franja lo pide así: barra de filtros
 * + exportar + tabla + paginación compuestas a mano con la misma anatomía). El estado lo trae `crearListadoPorCursor`;
 * esto solo dibuja. Diferencias con `ap-listado`, todas por el contrato:
 *
 * - el resumen y la exportación hablan de **esta página** (el servidor no da total ni busca por texto);
 * - la paginación no muestra un total de registros que nadie dio: «Página N de M» crece a medida que el servidor dice
 *   que hay más;
 * - no se ofrece ordenar por columna: ordenar una sola página mentiría sobre el resto.
 */
@Component({
  selector: 'ap-tabla-por-cursor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, Esqueleto, EstadoDeError, EstadoVacio, ExportarListado, NgTemplateOutlet, Paginacion, TablaDeDatos],
  template: `
    <ap-barra-de-filtros [etiqueta]="'Filtros de ' + titulo()" [ayudaDeBusqueda]="ayudaDeBusqueda()" [busqueda]="listado().local.busqueda()" [selectores]="listado().selectores()" textoLimpiar="Limpiar filtros" (busquedaChange)="listado().buscar($event)" (selectorChange)="listado().filtrar($event.nombre, $event.valor)" (limpiar)="listado().limpiar()" />
    <div class="barra">
      <p class="resumen" aria-live="polite">{{ resumen() }}</p>
      <div class="acciones">
        <ng-content select="[accionesDeListado]" />
        <ap-exportar-listado [nombre]="titulo()" [columnas]="columnasExportables()" [filas]="listado().visibles()" [deshabilitado]="listado().carga().tipo !== 'lista'" />
      </div>
    </div>
    <ng-template #combinada let-f let-c="columna">
      @if (c.clave === CLAVE_ACCIONES) { <ng-container *ngTemplateOutlet="acciones()!; context: { $implicit: f }" /> }
      @else { <ng-container *ngTemplateOutlet="celda()!; context: { $implicit: f, columna: c }" /> }
    </ng-template>
    @switch (listado().carga().tipo) {
      @case ('cargando') { <ap-esqueleto [lineas]="6" [etiqueta]="'Cargando ' + titulo().toLowerCase()" /> }
      @case ('error') { <ap-estado-de-error [titulo]="'No se pudo cargar ' + titulo().toLowerCase() + '.'" [mensaje]="mensajeDeError()" [trazaId]="idDePeticion()" (reintentar)="listado().recargar()" /> }
      @default {
        @if (listado().visibles().length === 0) {
          @if (listado().hayCriterios()) {
            <ap-estado-vacio motivo="porFiltro" [titulo]="textoSinCoincidencias()" accionPropia="Quitar filtros" (actuar)="listado().limpiar()" />
          } @else {
            <ap-estado-vacio [titulo]="textoVacio()" [explicacion]="explicacionVacio()" />
          }
        } @else {
          <ap-tabla-de-datos [titulo]="titulo()" [columnas]="columnasVisibles()" [filas]="listado().visibles()" [identidad]="identidad()" [plantillaDeCelda]="combinada" />
        }
        @if (listado().filas().length > 0 || listado().pagina() > 1) {
          <ap-paginacion [etiqueta]="'Paginación de ' + titulo()" [pagina]="listado().pagina()" (paginaChange)="listado().irAPagina($event)" [totalDeFilas]="listado().total()" [porPagina]="listado().porPagina()" (porPaginaChange)="listado().cambiarPorPagina($event)" [opcionesPorPagina]="listado().opcionesPorPagina" [conRegistros]="false" />
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
export class TablaPorCursor<T extends object> {
  readonly titulo = input.required<string>()
  readonly columnas = input.required<Columna<T>[]>()
  readonly listado = input.required<ListadoPorCursor<T>>()
  readonly identidad = input.required<(f: T) => string>()
  readonly exportables = input.required<readonly ColumnaExportable<T>[]>()
  readonly ayudaDeBusqueda = input<string>()
  readonly nombreDeRegistros = input('registros')
  readonly textoVacio = input('Todavía no hay registros.')
  readonly explicacionVacio = input<string>()
  readonly textoSinCoincidencias = input('Ningún registro de esta página coincide con la búsqueda o los filtros.')
  readonly celda = contentChild.required<TemplateRef<{ $implicit: T; columna: Columna<T> }>>('celda')
  readonly acciones = contentChild<TemplateRef<{ $implicit: T }>>('acciones')
  protected readonly CLAVE_ACCIONES = CLAVE_ACCIONES

  readonly columnasVisibles = computed<Columna<T>[]>(() =>
    this.acciones() ? [...this.columnas(), { clave: CLAVE_ACCIONES as keyof T & string, titulo: 'Acciones', fijaDerecha: true }] : this.columnas(),
  )
  readonly columnasExportables = computed(() => this.exportables())
  readonly resumen = computed(() => {
    const l = this.listado()
    if (l.carga().tipo !== 'lista') return ''
    const n = l.visibles().length
    const enPagina = l.filas().length
    return l.local.hayCriterios() ? `${n} de ${enPagina} ${this.nombreDeRegistros()} en esta página` : `${enPagina} ${this.nombreDeRegistros()} en la página ${l.pagina()}`
  })
  readonly mensajeDeError = computed(() => {
    const c = this.listado().carga()
    return c.tipo === 'error' ? c.mensaje : undefined
  })
  readonly idDePeticion = computed(() => {
    const c = this.listado().carga()
    return c.tipo === 'error' ? c.idPeticion : undefined
  })
}
