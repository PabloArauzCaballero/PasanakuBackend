import { ChangeDetectionStrategy, Component, TemplateRef, computed, contentChild, inject, input, model } from '@angular/core'
import { NgTemplateOutlet } from '@angular/common'
import { BarraDeFiltros } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { Esqueleto } from '@aportaya/ui/esqueleto/esqueleto'
import { EstadoDeError } from '@aportaya/ui/estado-de-error/estado-de-error'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { ExportarListado } from '@aportaya/ui/exportar-listado/exportar-listado'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Exportador } from '../../../nucleo/exportador'
import type { Bandeja } from './bandeja-por-cursor'

const CLAVE_ACCIONES = '__acciones'

/**
 * La anatomía de `ap-listado` para una bandeja **paginada en el servidor por cursor** (la guía de franja lo pide así:
 * barra de filtros + exportar + tabla + paginación compuestos a mano). Cambia respecto de `ap-listado` en lo que el
 * cursor obliga: no hay total —el resumen dice cuántas filas trae la página y si hay más—, buscar y descargar operan
 * sobre la página cargada, y además resuelve el estado «sin acceso» (403), que en cumplimiento es frecuente.
 *
 * La celda se dibuja con `<ng-template #celda let-fila let-columna="columna">` y las acciones de fila con
 * `<ng-template #acciones let-fila>`, igual que en `ap-listado`.
 */
@Component({
  selector: 'ap-listado-de-bandeja',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, Esqueleto, EstadoDeError, EstadoVacio, ExportarListado, NgTemplateOutlet, Paginacion, TablaDeDatos],
  template: `
    @let b = bandeja();
    @let c = b.carga();
    @if (c.tipo !== 'denegado') {
      <ap-barra-de-filtros [etiqueta]="'Filtros de ' + titulo()" [ayudaDeBusqueda]="ayudaDeBusqueda()" [busqueda]="b.busqueda()" [selectores]="b.selectores()" textoLimpiar="Limpiar filtros" (busquedaChange)="b.buscar($event)" (selectorChange)="b.filtrar($event.nombre, $event.valor)" (limpiar)="b.limpiar()" />
      <div class="barra">
        <p class="resumen" aria-live="polite">{{ resumen() }}</p>
        <div class="acciones"><ng-content select="[accionesDeListado]" />
          @if (puedeExportar()) { <ap-exportar-listado [nombre]="titulo() + ' pagina ' + b.pagina()" [columnas]="columnasExportables()" [filas]="b.filtradas()" [deshabilitado]="c.tipo !== 'lista'" /> }
        </div>
      </div>
    }
    <ng-template #combinada let-f let-col="columna">
      @if (col.clave === CLAVE_ACCIONES) { <ng-container *ngTemplateOutlet="acciones()!; context: { $implicit: f }" /> }
      @else if (celda(); as p) { <ng-container *ngTemplateOutlet="p; context: { $implicit: f, columna: col }" /> }
      @else { {{ crudo(f, col.clave) }} }
    </ng-template>
    @switch (c.tipo) {
      @case ('cargando') { <ap-esqueleto [lineas]="6" [etiqueta]="'Cargando ' + titulo()" /> }
      @case ('denegado') {
        <ap-estado-vacio motivo="porPermiso" titulo="No tenés acceso a esta bandeja." explicacion="Tu rol no puede ver esta información. Si la necesitás, pedí el permiso a quien administra los accesos." />
      }
      @case ('error') {
        <ap-estado-de-error [titulo]="'No se pudo cargar la bandeja «' + titulo() + '».'" [mensaje]="c.error.mensaje" [trazaId]="c.error.trazaId" (reintentar)="b.recargar()" />
      }
      @default {
        @if (b.filtradas().length === 0) {
          @if (b.hayCriterios()) {
            <ap-estado-vacio motivo="porFiltro" titulo="Ningún registro coincide con la búsqueda o los filtros." accionPropia="Quitar filtros" (actuar)="b.limpiar()" />
          } @else {
            <ap-estado-vacio [titulo]="textoVacio()" [explicacion]="explicacionVacio()" />
          }
        } @else {
          <ap-tabla-de-datos [titulo]="titulo()" [columnas]="columnasVisibles()" [filas]="b.filtradas()" [identidad]="identidad()" [seleccionable]="seleccionable()" [(elegidas)]="elegidas" [plantillaDeCelda]="combinada" />
        }
        @if (b.filas().length > 0 || b.pagina() > 1) {
          <ap-paginacion [etiqueta]="'Paginación de ' + titulo()" [pagina]="b.pagina()" (paginaChange)="b.irAPagina($event)" [totalDeFilas]="b.total()" [porPagina]="b.porPagina()" (porPaginaChange)="b.cambiarPorPagina($event)" [opcionesPorPagina]="b.opcionesPorPagina" [conRegistros]="false" />
        }
      }
    }
  `,
  styles: `
    :host { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .barra { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s3); }
    .resumen { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
    .acciones { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s2); }
  `,
})
export class ListadoDeBandeja<T extends object> {
  private readonly exportador = inject(Exportador)
  readonly titulo = input.required<string>()
  /**
   * Datos sujetos a reserva (regla 90.2): la descarga en el navegador no deja huella ni tope, así que solo aparece con el
   * permiso de exportación del recurso (`exportar:<recurso>`, el de CU-58). Sin recurso no hay descarga.
   */
  readonly recursoDeExportacion = input<string>()
  readonly puedeExportar = computed(() => !!this.recursoDeExportacion() && this.exportador.puedeExportar(this.recursoDeExportacion()!))
  readonly columnas = input.required<Columna<T>[]>()
  readonly bandeja = input.required<Bandeja<T>>()
  readonly identidad = input.required<(f: T) => string>()
  /** Dónde busca, en palabras. Siempre aclara que es en la página cargada: el contrato no admite búsqueda. */
  readonly ayudaDeBusqueda = input<string>()
  /** Las columnas del archivo, con los valores como se ven (lo enmascarado sale enmascarado). */
  readonly exportables = input<readonly ColumnaExportable<T>[]>()
  readonly nombreDeRegistros = input('registros')
  readonly textoVacio = input('Todavía no hay registros.')
  readonly explicacionVacio = input<string>()
  readonly seleccionable = input(false)
  readonly elegidas = model<string[]>([])
  readonly celda = contentChild<TemplateRef<{ $implicit: T; columna: Columna<T> }>>('celda')
  readonly acciones = contentChild<TemplateRef<{ $implicit: T }>>('acciones')
  protected readonly CLAVE_ACCIONES = CLAVE_ACCIONES

  readonly columnasVisibles = computed<Columna<T>[]>(() =>
    this.acciones() ? [...this.columnas(), { clave: CLAVE_ACCIONES as keyof T & string, titulo: 'Acciones', fijaDerecha: true }] : this.columnas(),
  )
  readonly columnasExportables = computed<readonly ColumnaExportable<T>[]>(
    () => this.exportables() ?? this.columnas().map((c) => ({ clave: c.clave, titulo: c.titulo })),
  )

  /** «25 alertas en la página 2 · hay más»: sin total del servidor, se dice lo que se sabe. */
  readonly resumen = computed(() => {
    const b = this.bandeja()
    if (b.carga().tipo !== 'lista') return ''
    const n = b.filtradas().length
    const base = b.busqueda() ? `${n} de ${b.filas().length} ${this.nombreDeRegistros()}` : `${n} ${this.nombreDeRegistros()}`
    return `${base} en la página ${b.pagina()}${b.hayMas() ? ' · hay más' : ''}`
  })

  protected crudo(f: T, clave: string): unknown {
    return (f as Record<string, unknown>)[clave]
  }
}
