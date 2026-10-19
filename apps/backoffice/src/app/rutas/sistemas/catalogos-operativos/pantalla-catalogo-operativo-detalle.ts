import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { ActivatedRoute, RouterLink } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { CampoBusqueda } from '@aportaya/ui/campo-busqueda/campo-busqueda'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { ListaDeOpciones, type Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { Tono } from '@aportaya/ui/tono/tono'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import {
  catalogoOperativo,
  elementosCatalogo,
  type CatalogoOperativoDetalle,
  type ElementoCatalogoOperativo,
} from '../dominio/catalogos-operativos'
import { textosSistemas } from '../textos'
import { ModalCambiarEstadoElemento } from './modal-cambiar-estado-elemento'

type FilaElemento = ElementoCatalogoOperativo & { acciones?: string }

@Component({
  selector: 'ap-pantalla-catalogo-operativo-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, CampoBusqueda, ChipEstado, EstadoDePantalla, ListaDeOpciones, ModalCambiarEstadoElemento, RouterLink, TablaDeDatos],
  template: `
    <section class="pantalla">
      <div class="navegacion-superior">
        <a routerLink="/sistemas/catalogos-operativos" class="enlace-volver">{{ t.volver }}</a>
      </div>

      <ap-estado-de-pantalla [recurso]="recursoCatalogo" [vacio]="esVacioCatalogo" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="recursoCatalogo.reload()">
        @if (recursoCatalogo.hasValue() && recursoCatalogo.value(); as c) {
          <header class="cabecera-ficha">
            <div>
              <ap-cabecera-de-pagina [titulo]="c.nombre" [descripcion]="c.descripcion" icono="baseDeDatos" />
              <p class="metadatos">
                <span>Dueño: <strong>{{ c.dueno }}</strong></span> ·
                <span>Versión: <strong>{{ c.version }}</strong></span> ·
                <span>Tabla: <code>{{ c.tablaFisica }}</code></span> ·
                <span>Norma: <strong>{{ c.baseNormativa }}</strong></span>
              </p>
            </div>
            <div class="acciones-cabecera">
              <ap-chip-estado [tono]="tonoEstado(c.estado)">{{ c.estado }}</ap-chip-estado>
            </div>
          </header>

          <section class="seccion-elementos">
            <h2>{{ t.elementosTitulo }}</h2>

            <div class="filtros" role="search" aria-label="Filtros de elementos del catálogo">
              <ap-lista-de-opciones [compacta]="true" [etiqueta]="t.filtrarEstado" [opciones]="opcionesEstado" [(valor)]="filtroEstadoStr" textoVacio="Todos los estados" />
              <div class="busqueda-contenedor">
                <ap-campo-busqueda [etiqueta]="t.buscar" [(valor)]="busqueda" />
              </div>
            </div>

            <ap-estado-de-pantalla [recurso]="recursoElementos" [vacio]="esVacioElementos" [mensajeVacio]="t.vacioElementos" [etiquetaDeCarga]="t.cargandoElementos" (reintentar)="recursoElementos.reload()">
              @if (recursoElementos.hasValue()) {
                <ap-tabla-de-datos [titulo]="t.elementosTitulo" [columnas]="columnas" [filas]="filasFiltradas()">
                  <ng-template #celda let-fila let-columna="columna">
                    @if (columna.clave === 'codigo') {
                      <code class="codigo">{{ fila.codigo }}</code>
                    } @else if (columna.clave === 'activo') {
                      <ap-chip-estado [tono]="fila.activo ? 'ok' : 'neutro'">
                        {{ fila.activo ? 'Activo' : 'Inactivo' }}
                      </ap-chip-estado>
                    } @else if (columna.clave === 'acciones') {
                      <ap-boton variante="secundario" (pulsado)="abrirModal(fila)">
                        {{ fila.activo ? t.desactivar : t.activar }}
                      </ap-boton>
                    } @else {
                      {{ fila[columna.clave] }}
                    }
                  </ng-template>
                </ap-tabla-de-datos>
              }
            </ap-estado-de-pantalla>
          </section>
        }
      </ap-estado-de-pantalla>

      @if (elementoParaEditar(); as elem) {
        <ap-modal-cambiar-estado-elemento [catalogoCodigo]="codigo() ?? ''" [elemento]="elem" (cerrar)="cerrarModal()" (guardado)="alGuardar()" />
      }
    </section>
  `,
  styles: `
    .pantalla { padding: 0; max-width: 72rem; display: flex; flex-direction: column; gap: var(--s5); }
    .navegacion-superior { margin-bottom: var(--s2); }
    .enlace-volver { color: var(--text-2); text-decoration: none; font-size: 0.875rem; }
    .enlace-volver:hover { color: var(--text); }
    .cabecera-ficha { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--s4); }
    .metadatos { margin: var(--s1) 0 0; color: var(--text-2); font-size: 0.875rem; display: flex; flex-wrap: wrap; gap: var(--s2); }
    .seccion-elementos { display: flex; flex-direction: column; gap: var(--s3); margin-top: var(--s3); }
    .seccion-elementos h2 { margin: 0; font-size: 1.15rem; font-weight: 600; color: var(--text); }
    .filtros { display: flex; flex-wrap: wrap; align-items: flex-end; gap: var(--s3); }
    .busqueda-contenedor { flex: 1; min-width: 14rem; }
    .codigo { font-family: var(--f-mono); font-size: 0.85em; color: var(--text); background: var(--surface-2); padding: var(--s1) var(--s2); border-radius: var(--r-sm); }
  `,
})
export class PantallaCatalogoOperativoDetalle {
  private readonly route = inject(ActivatedRoute)
  protected readonly t = textosSistemas.catalogosOperativos
  protected readonly codigo = signal<string | null>(this.route.snapshot.paramMap.get('codigo'))
  readonly filtroEstadoStr = signal('')
  readonly busqueda = signal('')
  readonly elementoParaEditar = signal<ElementoCatalogoOperativo | null>(null)

  protected readonly filtroActivo = computed<boolean | null>(() => {
    const val = this.filtroEstadoStr()
    return val === 'ACTIVO' ? true : val === 'INACTIVO' ? false : null
  })

  protected readonly recursoCatalogo = catalogoOperativo(this.codigo)
  protected readonly recursoElementos = elementosCatalogo(this.codigo, this.filtroActivo)

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly esVacioCatalogo = (c: CatalogoOperativoDetalle | undefined) => !c
  protected readonly esVacioElementos = (items: ElementoCatalogoOperativo[]) => items.length === 0

  protected readonly opcionesEstado: readonly Opcion[] = [
    { valor: '', texto: 'Todos los estados' },
    { valor: 'ACTIVO', texto: 'Solo activos' },
    { valor: 'INACTIVO', texto: 'Solo inactivos' },
  ]

  protected readonly filasFiltradas = computed<FilaElemento[]>(() => {
    const items = this.recursoElementos.value()
    if (!items) return []
    const termino = this.busqueda().trim().toLowerCase()
    if (!termino) return items
    return items.filter(
      (e) =>
        e.codigo.toLowerCase().includes(termino) ||
        e.nombre.toLowerCase().includes(termino) ||
        e.descripcion.toLowerCase().includes(termino),
    )
  })

  protected readonly columnas: Columna<FilaElemento>[] = [
    { clave: 'orden', titulo: '#' },
    { clave: 'codigo', titulo: 'Código' },
    { clave: 'nombre', titulo: 'Elemento', ordenable: true },
    { clave: 'descripcion', titulo: 'Descripción' },
    { clave: 'activo', titulo: 'Estado' },
    { clave: 'acciones', titulo: 'Acción' },
  ]

  protected tonoEstado(estado: string): Tono {
    return estado === 'VIGENTE' ? 'ok' : estado === 'EN_REVISION' ? 'aviso' : 'neutro'
  }

  protected abrirModal(elemento: ElementoCatalogoOperativo): void {
    this.elementoParaEditar.set(elemento)
  }

  protected cerrarModal(): void {
    this.elementoParaEditar.set(null)
  }

  protected alGuardar(): void {
    this.cerrarModal()
    this.recursoElementos.reload()
    this.recursoCatalogo.reload()
  }
}
