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
 * La anatomía de `ap-listado` (barra de filtros · resumen + acciones + descarga · tabla · paginación) para las
 * bandejas que paginan **en el servidor por cursor** (`crearListadoPorCursor`). Mismos estados: cargando, error con
 * reintento y código de seguimiento, sin acceso (403), vacío sin datos y vacío por filtro con «Quitar filtros».
 * La paginación sigue visible aunque la búsqueda vacíe la página: la coincidencia puede estar en la siguiente.
 */
@Component({
  selector: 'ap-listado-por-cursor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, Esqueleto, EstadoDeError, EstadoVacio, ExportarListado, NgTemplateOutlet, Paginacion, TablaDeDatos],
  template: `
    @if (fuente().estado() === 'denegado') {
      <ap-estado-vacio motivo="porPermiso" titulo="No tenés acceso a esta bandeja." [explicacion]="textoDenegado()" />
    } @else {
      <ap-barra-de-filtros [etiqueta]="'Filtros de ' + titulo()" [ayudaDeBusqueda]="ayudaDeBusqueda()" [busqueda]="fuente().local.busqueda()" [selectores]="fuente().selectores()" textoLimpiar="Limpiar filtros" (busquedaChange)="fuente().buscar($event)" (selectorChange)="fuente().filtrar($event.nombre, $event.valor)" (limpiar)="fuente().limpiar()" />
      <div class="barra">
        <p class="resumen" aria-live="polite">{{ resumen() }}</p>
        <div class="acciones"><ng-content select="[accionesDeListado]" />
          @if (fuente().estado() !== 'error') { <ap-exportar-listado [nombre]="titulo()" [columnas]="exportables()" [filas]="fuente().local.filtradas()" [deshabilitado]="fuente().estado() !== 'lista'" /> }
        </div>
      </div>
      <ng-template #combinada let-f let-c="columna">
        @if (c.clave === CLAVE_ACCIONES) { <ng-container *ngTemplateOutlet="acciones()!; context: { $implicit: f }" /> }
        @else { <ng-container *ngTemplateOutlet="celda(); context: { $implicit: f, columna: c }" /> }
      </ng-template>
      @switch (fuente().estado()) {
        @case ('cargando') { <ap-esqueleto [lineas]="6" [etiqueta]="'Cargando ' + titulo().toLowerCase()" /> }
        @case ('error') { <ap-estado-de-error [titulo]="titulo() + ': no se pudo cargar la información.'" [mensaje]="fuente().error()?.mensaje" [trazaId]="fuente().error()?.trazaId" (reintentar)="fuente().recargar()" /> }
        @default {
          @if (fuente().local.total() === 0) {
            @if (fuente().hayCriterios()) {
              <ap-estado-vacio motivo="porFiltro" titulo="Ningún registro de esta página coincide con la búsqueda o los filtros." accionPropia="Quitar filtros" (actuar)="fuente().limpiar()" />
            } @else if (fuente().pagina() === 1) {
              <ap-estado-vacio [titulo]="textoVacio()" [explicacion]="explicacionVacio()" />
            } @else {
              <ap-estado-vacio titulo="Esta página ya no trae registros." explicacion="Volvé a la página anterior." />
            }
          } @else {
            <ap-tabla-de-datos [titulo]="titulo()" [tituloVisible]="false" [columnas]="columnasVisibles()" [filas]="fuente().local.visibles()" [identidad]="identidad()" [plantillaDeCelda]="combinada" />
          }
        }
      }
      @if (fuente().estado() === 'lista' && (fuente().pagina() > 1 || fuente().totalConocido() > 0)) {
        <ap-paginacion [etiqueta]="'Paginación de ' + titulo()" [pagina]="fuente().pagina()" (paginaChange)="fuente().irAPagina($event)" [totalDeFilas]="fuente().totalConocido()" [porPagina]="fuente().porPagina()" (porPaginaChange)="fuente().cambiarPorPagina($event)" [opcionesPorPagina]="fuente().opcionesPorPagina" [conRegistros]="false" />
      }
    }
  `,
  styles: `
    :host { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .barra { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s3); }
    .barra:has(.resumen:empty):not(:has(button)) { display: none; }
    .resumen { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
    .acciones { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s2); }
  `,
})
export class ListadoPorCursorVista<T extends object> {
  readonly titulo = input.required<string>()
  readonly columnas = input.required<Columna<T>[]>()
  readonly fuente = input.required<ListadoPorCursor<T>>()
  readonly exportables = input.required<readonly ColumnaExportable<T>[]>()
  readonly identidad = input.required<(f: T) => string>()
  readonly ayudaDeBusqueda = input<string>()
  readonly textoVacio = input('Todavía no hay registros.')
  readonly explicacionVacio = input<string>()
  readonly textoDenegado = input('Esta bandeja es de quien tiene el permiso para verla.')
  readonly nombreDeRegistros = input('registros')
  readonly nombreDeRegistro = input('registro')
  readonly celda = contentChild.required<TemplateRef<{ $implicit: T; columna: Columna<T> }>>('celda')
  readonly acciones = contentChild<TemplateRef<{ $implicit: T }>>('acciones')
  protected readonly CLAVE_ACCIONES = CLAVE_ACCIONES
  readonly columnasVisibles = computed<Columna<T>[]>(() =>
    this.acciones() ? [...this.columnas(), { clave: CLAVE_ACCIONES as keyof T & string, titulo: 'Acciones', fijaDerecha: true }] : this.columnas(),
  )
  /** El total del servidor no existe: el resumen habla de la página, para que nadie lea «20» como «todos». */
  readonly resumen = computed(() => {
    const f = this.fuente()
    if (f.estado() !== 'lista') return ''
    const l = f.local
    const nombre = (n: number) => (n === 1 ? this.nombreDeRegistro() : this.nombreDeRegistros())
    const base = l.hayCriterios() ? `${l.total()} de ${l.filas().length} ${nombre(l.filas().length)}` : `${l.total()} ${nombre(l.total())}`
    return `${base} en la página ${f.pagina()}`
  })
}
