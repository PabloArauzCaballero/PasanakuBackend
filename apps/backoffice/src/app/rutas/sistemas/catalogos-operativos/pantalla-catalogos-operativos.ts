import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { Router } from '@angular/router'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { CifrasDeSistemas, contar, type Cifra } from '../compartido/cifras'
import { catalogosOperativos, type CatalogoOperativoResumen } from '../dominio/catalogos-operativos'
import { textosSistemas } from '../textos'
import { Fecha } from '@aportaya/ui/fecha/fecha'

type FilaCatalogo = CatalogoOperativoResumen & { conteoElementos?: string }
const ESTADO: Record<string, string> = { VIGENTE: 'Vigente', EN_REVISION: 'En revisión', OBSOLETO: 'Obsoleto' }
const TONO: Record<string, Tono> = { VIGENTE: 'ok', EN_REVISION: 'aviso', OBSOLETO: 'neutro' }

/**
 * Catálogos operativos (`GET /auditoria/catalogos`). La cabecera de un catálogo no se edita ni se borra por contrato;
 * la acción de fila es **Ver**, que lleva a la ficha, donde sí se activan o desactivan sus elementos.
 */
@Component({
  selector: 'ap-pantalla-catalogos-operativos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Boton, CabeceraDePagina, ChipEstado, CifrasDeSistemas, Fecha, ListadoDeDatos],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Sistemas · Auditoría" [titulo]="t.titulo" [descripcion]="t.proposito">
        @if (carga().tipo !== 'sin-fuente') { <ap-boton acciones variante="fantasma" icono="recarga" [deshabilitado]="recurso.isLoading()" (pulsado)="recurso.reload()">Actualizar</ap-boton> }
      </ap-cabecera-de-pagina>
      <ap-cifras-de-sistemas etiqueta="Resumen de catálogos" [cifras]="cifras()" [carga]="carga()" />
      <ap-listado [titulo]="t.titulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" [identidad]="identidad" [ayudaDeBusqueda]="t.buscar + ' o descripción.'" [exportables]="exportables" nombreDeRegistros="catálogos" [textoVacio]="t.vacio" explicacionVacio="Los catálogos los publica el servicio de auditoría; cuando existan, aparecen acá con su versión vigente." (reintentar)="recurso.reload()">
        <ng-template #celda let-c let-col="columna">
          @switch (col.clave) {
            @case ('codigo') { <code>{{ c.codigo }}</code> }
            @case ('estado') { <ap-chip-estado [tono]="tono(c)">{{ estado(c) }}</ap-chip-estado> }
            @case ('vigenteDesde') { @if (c.vigenteDesde) { <ap-fecha [iso]="c.vigenteDesde" [conHora]="false" /> } @else { — } }
            @case ('conteoElementos') { <span class="conteo">{{ c.elementosActivos }} / {{ c.totalElementos }}</span> }
            @default { {{ c[col.clave] }} }
          }
        </ng-template>
        <ng-template #acciones let-c>
          <ap-acciones-de-fila [fila]="c.nombre" [ver]="true" [textoVer]="t.verElementos" (verPulsado)="irADetalle(c.codigo)" />
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    /* La tabla se desplaza dentro de su marco: las acciones van en una sola línea en vez de apilarse y estirar la fila. */
    ap-acciones-de-fila { flex-wrap: nowrap; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }
    code { padding: 0 var(--s1); border-radius: var(--r-sm); background: var(--surface-2); font: var(--t-ayuda); font-family: var(--f-mono); color: var(--text-2); overflow-wrap: anywhere; }
    .conteo { font-variant-numeric: tabular-nums; font-weight: 600; }
  `,
})
export class PantallaCatalogosOperativos {
  private readonly router = inject(Router)

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.catalogosOperativos
  protected readonly recurso = catalogosOperativos()
  protected readonly carga = cargaDe(this.recurso)
  private readonly filas = filasDe<FilaCatalogo>(this.recurso)
  protected readonly identidad = (c: FilaCatalogo) => c.codigo
  protected readonly listado = crearListado<FilaCatalogo>({
    filas: this.filas,
    camposDeBusqueda: ['codigo', 'nombre', 'dueno', 'descripcion', 'version'],
    filtros: [
      { nombre: 'estado', etiqueta: 'Estado', valor: (c) => c.estado, texto: (v) => ESTADO[v] ?? v },
      { nombre: 'dueno', etiqueta: 'Dueño', valor: (c) => c.dueno },
    ],
    porPagina: 10,
    valorParaOrden: (c, clave) => (clave === 'conteoElementos' ? c.elementosActivos : (c as unknown as Record<string, unknown>)[clave]),
  })
  protected readonly cifras = computed<Cifra[]>(() => {
    const f = this.filas()
    return [
      { etiqueta: 'Catálogos', valor: contar(f), icono: 'lista' },
      { etiqueta: 'Vigentes', valor: contar(f, (c) => c.estado === 'VIGENTE'), icono: 'verificado', tono: 'ok' },
      { etiqueta: 'En revisión', valor: contar(f, (c) => c.estado === 'EN_REVISION'), icono: 'reloj', tono: 'aviso' },
      { etiqueta: 'Elementos activos', valor: String(f.reduce((s, c) => s + c.elementosActivos, 0)), icono: 'etiqueta', pista: `de ${f.reduce((s, c) => s + c.totalElementos, 0)} en total` },
    ]
  })
  protected readonly columnas: Columna<FilaCatalogo>[] = [
    { clave: 'codigo', titulo: 'Código', ordenable: true },
    { clave: 'nombre', titulo: 'Catálogo', ordenable: true },
    { clave: 'dueno', titulo: 'Dueño', ordenable: true },
    { clave: 'version', titulo: 'Versión' },
    { clave: 'conteoElementos', titulo: 'Activos / Total', numerica: true, ordenable: true },
    { clave: 'vigenteDesde', titulo: 'Vigencia', ordenable: true },
    { clave: 'estado', titulo: 'Estado', ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<FilaCatalogo>[] = [
    { clave: 'codigo', titulo: 'Código' },
    { clave: 'nombre', titulo: 'Catálogo' },
    { clave: 'dueno', titulo: 'Dueño' },
    { clave: 'version', titulo: 'Versión' },
    { clave: 'elementosActivos', titulo: 'Elementos activos' },
    { clave: 'totalElementos', titulo: 'Elementos totales' },
    { clave: 'vigenteDesde', titulo: 'Vigencia' },
    { clave: 'estado', titulo: 'Estado', valor: (c) => ESTADO[c.estado] ?? c.estado },
  ]

  protected estado(c: FilaCatalogo): string {
    return ESTADO[c.estado] ?? c.estado
  }
  protected tono(c: FilaCatalogo): Tono {
    return TONO[c.estado] ?? 'neutro'
  }
  protected irADetalle(codigo: string): void {
    void this.router.navigate(['/sistemas/catalogos-operativos', codigo])
  }
}
