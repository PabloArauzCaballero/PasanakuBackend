import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core'
import { Router, RouterLink } from '@angular/router'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { ExportarListado } from '@aportaya/ui/exportar-listado/exportar-listado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Sesion } from '../../../nucleo/sesion'
import type { FilaDeTabla } from './dominio'
import { textosCatalogo } from './textos'

const t = textosCatalogo

/**
 * La página de tablas del catálogo, **paginada en el servidor** (la búsqueda y los filtros viajan en la consulta). Como
 * no se baja el catálogo entero, «Descargar CSV / JSON» baja **la página filtrada que se ve**, y el botón lo dice con su
 * cuenta de filas; para más, se sube «Filas por página». Cada fila ofrece «Ver ficha» y, con `CATALOGO_DATOS_EDITAR`,
 * «Editar» (la ficha de gobierno de CU-130, `PUT …/gobierno`). No hay eliminar: el catálogo describe tablas que existen.
 */
@Component({
  selector: 'ap-lista-de-tablas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, ChipEstado, ExportarListado, Paginacion, RouterLink, TablaDeDatos],
  template: `
    <div class="barra">
      <p class="resumen">{{ t.notaDeDescarga }}</p>
      <ap-exportar-listado [nombre]="'catalogo de datos pagina ' + pagina()" [columnas]="exportables" [filas]="filas()" />
    </div>
    <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [estado]="{ tipo: 'lista', filas: filas() }" [identidad]="porId" densidad="compacta" [cabecera]="true" [textoRegistros]="t.registros" [totalDeRegistros]="total()" [plantillaDeCelda]="celda">
      <ng-template #celda let-f let-columna="columna">
        @switch (columna.clave) {
          @case ('tabla') {
            <a class="nombre" [routerLink]="['/tablero/catalogo-de-datos', f.esquema, f.tabla]">{{ f.tabla }}</a>
            <span class="que-es">{{ f.queEs }}</span>
          }
          @case ('esquema') {
            <code>{{ f.esquema }}</code>
            @if (f.infraestructura) { <span class="que-es">{{ t.infraestructura }}</span> }
          }
          @case ('estado') { <ap-chip-estado [tono]="f.tonoEstado">{{ f.estado }}</ap-chip-estado> }
          @case ('revision') { <ap-chip-estado [tono]="f.tonoRevision">{{ f.revision }}</ap-chip-estado> }
          @case ('sensible') { <ap-chip-estado [tono]="f.tonoSensible">{{ f.sensible }}</ap-chip-estado> }
          @case ('acciones') { <ap-acciones-de-fila [fila]="f.id" [ver]="true" textoVer="Ver ficha" [editar]="puedeEditar()" (verPulsado)="ir(f)" (editarPulsado)="ir(f, 'ficha')" /> }
          @default { {{ f[columna.clave] }} }
        }
      </ng-template>
    </ap-tabla-de-datos>
    <ap-paginacion etiqueta="Paginación de tablas" [pagina]="pagina()" (paginaChange)="paginaChange.emit($event)" [totalDeFilas]="total()" [porPagina]="porPagina()" (porPaginaChange)="porPaginaChange.emit($event)" [opcionesPorPagina]="opciones()" nombreDeRegistros="tablas" />
  `,
  styles: `
    :host { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .barra { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s3); }
    .resumen { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
    .nombre { display: block; color: var(--text); font-family: var(--mono); font-size: .92em; font-weight: 600; text-decoration: none; overflow-wrap: anywhere; }
    .nombre:hover { color: var(--brand-texto); text-decoration: underline; }
    .nombre:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .que-es { display: block; margin-top: var(--s1); min-width: 24ch; max-width: 40ch; color: var(--text-2); font: var(--t-cuerpo-chico); white-space: normal; }
    code { font-family: var(--mono); font-size: .92em; }
  `,
})
export class ListaDeTablas {
  private readonly router = inject(Router)
  private readonly sesion = inject(Sesion)
  protected readonly t = t
  readonly filas = input.required<FilaDeTabla[]>()
  readonly pagina = input.required<number>()
  readonly porPagina = input.required<number>()
  readonly total = input.required<number>()
  readonly opciones = input<readonly number[]>([10, 20, 50])
  readonly paginaChange = output<number>()
  readonly porPaginaChange = output<number>()
  protected readonly puedeEditar = computed(() => this.sesion.puede('CATALOGO_DATOS_EDITAR'))
  protected readonly porId = (f: FilaDeTabla) => f.id
  // Lo que se viene a mirar va primero (como en el portal de Atlas): la tabla con lo que es, su estado y su revisión.
  protected readonly columnas: Columna<FilaDeTabla>[] = [
    { clave: 'tabla', titulo: t.columnasLista.tabla },
    { clave: 'esquema', titulo: t.columnasLista.esquema },
    { clave: 'estado', titulo: t.columnasLista.estado },
    { clave: 'revision', titulo: t.columnasLista.revision },
    { clave: 'sensible', titulo: t.columnasLista.sensible },
    { clave: 'responsable', titulo: t.columnasLista.responsable },
    { clave: 'columnas', titulo: t.columnasLista.columnas, numerica: true },
    { clave: 'acciones' as keyof FilaDeTabla & string, titulo: 'Acciones', fijaDerecha: true },
  ]
  protected readonly exportables: ColumnaExportable<FilaDeTabla>[] = [
    { clave: 'esquema', titulo: t.columnasLista.esquema },
    { clave: 'tabla', titulo: t.columnasLista.tabla },
    { clave: 'queEs', titulo: 'Qué es' },
    { clave: 'servicio', titulo: 'Servicio' },
    { clave: 'estado', titulo: t.columnasLista.estado },
    { clave: 'revision', titulo: t.columnasLista.revision },
    { clave: 'sensible', titulo: t.columnasLista.sensible },
    { clave: 'responsable', titulo: t.columnasLista.responsable },
    { clave: 'columnas', titulo: t.columnasLista.columnas },
  ]

  protected ir(f: FilaDeTabla, destino?: 'ficha'): void {
    void this.router.navigate(['/tablero/catalogo-de-datos', f.esquema, f.tabla, ...(destino ? [destino] : [])])
  }
}
