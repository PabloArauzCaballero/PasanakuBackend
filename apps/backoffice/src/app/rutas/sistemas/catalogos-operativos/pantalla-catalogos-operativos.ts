import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Router } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { CampoBusqueda } from '@aportaya/ui/campo-busqueda/campo-busqueda'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import {
  catalogosOperativos,
  type CatalogoOperativoResumen,
} from '../dominio/catalogos-operativos'
import { textosSistemas } from '../textos'

type FilaCatalogo = CatalogoOperativoResumen & {
  conteoElementos?: string
  acciones?: string
}

@Component({
  selector: 'ap-pantalla-catalogos-operativos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    Boton,
    CabeceraDePagina,
    CampoBusqueda,
    ChipEstado,
    EstadoDePantalla,
    TablaDeDatos,
  ],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="baseDeDatos" />

      <div class="filtros" role="search" aria-label="Búsqueda de catálogos operativos">
        <div class="busqueda-contenedor">
          <ap-campo-busqueda [etiqueta]="t.buscar" [(valor)]="busqueda" />
        </div>
      </div>

      <ap-estado-de-pantalla
        [recurso]="recurso"
        [vacio]="esVacio"
        [mensajeVacio]="t.vacio"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="recurso.reload()"
      >
        @if (recurso.hasValue() && recurso.value(); as catalogos) {
          <ap-tabla-de-datos
            [titulo]="t.titulo"
            [columnas]="columnas"
            [filas]="filasFiltradas()"
          >
            <ng-template #celda let-fila let-columna="columna">
              @if (columna.clave === 'codigo') {
                <code class="codigo">{{ fila.codigo }}</code>
              } @else if (columna.clave === 'estado') {
                <ap-chip-estado [tono]="tonoEstado(fila.estado)">
                  {{ fila.estado }}
                </ap-chip-estado>
              } @else if (columna.clave === 'conteoElementos') {
                <span class="conteo">{{ fila.elementosActivos }} / {{ fila.totalElementos }}</span>
              } @else if (columna.clave === 'acciones') {
                <ap-boton
                  variante="fantasma"
                  (pulsado)="irADetalle(fila.codigo)"
                >
                  {{ t.verElementos }}
                </ap-boton>
              } @else {
                {{ fila[columna.clave] }}
              }
            </ng-template>
          </ap-tabla-de-datos>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { padding: 0; max-width: 72rem; display: flex; flex-direction: column; gap: var(--s5); }
    .filtros { display: flex; flex-wrap: wrap; align-items: flex-end; gap: var(--s3); }
    .busqueda-contenedor { flex: 1; min-width: 14rem; }
    .codigo { font-family: var(--f-mono); font-size: 0.85em; color: var(--text); background: var(--surface-2); padding: var(--s1) var(--s2); border-radius: var(--r-sm); }
    .conteo { font-variant-numeric: tabular-nums; font-weight: 500; }
  `,
})
export class PantallaCatalogosOperativos {
  private readonly router = inject(Router)

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosSistemas.catalogosOperativos
  readonly busqueda = signal('')

  protected readonly recurso = catalogosOperativos()

  protected readonly esVacio = (items: CatalogoOperativoResumen[]) => items.length === 0

  protected readonly filasFiltradas = computed<FilaCatalogo[]>(() => {
    const items = this.recurso.value()
    if (!items) return []
    const termino = this.busqueda().trim().toLowerCase()
    if (!termino) return items
    return items.filter(
      (c) =>
        c.codigo.toLowerCase().includes(termino) ||
        c.nombre.toLowerCase().includes(termino) ||
        c.dueno.toLowerCase().includes(termino) ||
        c.descripcion.toLowerCase().includes(termino),
    )
  })

  protected readonly columnas: Columna<FilaCatalogo>[] = [
    { clave: 'codigo', titulo: 'Código' },
    { clave: 'nombre', titulo: 'Catálogo', ordenable: true },
    { clave: 'dueno', titulo: 'Dueño', ordenable: true },
    { clave: 'version', titulo: 'Versión' },
    { clave: 'conteoElementos', titulo: 'Activos / Total' },
    { clave: 'vigenteDesde', titulo: 'Vigencia' },
    { clave: 'estado', titulo: 'Estado' },
    { clave: 'acciones', titulo: 'Acciones' },
  ]

  protected tonoEstado(estado: string): Tono {
    if (estado === 'VIGENTE') return 'ok'
    if (estado === 'EN_REVISION') return 'aviso'
    return 'neutro'
  }

  protected irADetalle(codigo: string): void {
    this.router.navigate(['/sistemas/catalogos-operativos', codigo])
  }
}
