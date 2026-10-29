import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { ActivatedRoute } from '@angular/router'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Icono } from '@aportaya/ui/icono/icono'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import { Migas } from '@aportaya/ui/migas/migas'
import { SeccionClaveValor, type ClaveValor } from '@aportaya/ui/seccion-clave-valor/seccion-clave-valor'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { cargaDe, filasDe } from '../compartido/carga'
import { catalogoOperativo, elementosCatalogo, type CatalogoOperativoDetalle, type ElementoCatalogoOperativo } from '../dominio/catalogos-operativos'
import { textosSistemas } from '../textos'
import { ModalCambiarEstadoElemento } from './modal-cambiar-estado-elemento'

const estadoDe = (e: ElementoCatalogoOperativo) => (e.activo ? 'Activo' : 'Inactivo')

/**
 * Ficha de un catálogo operativo: cabecera y metadatos en una tarjeta y sus elementos como listado del kit. La única
 * operación del contrato sobre un elemento es activarlo o desactivarlo con motivo (PATCH `{activo, motivo}`, que deja
 * registro inmutable en bitácora): por eso la acción de fila es «Activar»/«Desactivar» y no hay Editar ni Eliminar.
 */
@Component({
  selector: 'ap-pantalla-catalogo-operativo-detalle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Boton, CabeceraDePagina, ChipEstado, EstadoDePantalla, Icono, ListadoDeDatos, Migas, ModalCambiarEstadoElemento, SeccionClaveValor],
  template: `
    <section class="pantalla">
      <ap-migas [migas]="[{ texto: t.titulo, ruta: '/sistemas/catalogos-operativos' }, { texto: codigo() ?? t.detalleTitulo }]" />
      <ap-estado-de-pantalla [recurso]="recursoCatalogo" [vacio]="esVacioCatalogo" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="recursoCatalogo.reload()">
        @if (recursoCatalogo.hasValue() && recursoCatalogo.value(); as c) {
          <ap-cabecera-de-pagina [eyebrow]="t.detalleTitulo" [titulo]="c.nombre" [descripcion]="c.descripcion">
            @if (carga().tipo !== 'sin-fuente') { <ap-boton acciones variante="fantasma" icono="recarga" (pulsado)="recargar()">Actualizar</ap-boton> }
          </ap-cabecera-de-pagina>
          <article class="ficha">
            <ap-chip-estado [tono]="c.estado === 'VIGENTE' ? 'ok' : c.estado === 'EN_REVISION' ? 'aviso' : 'neutro'">{{ c.estado }}</ap-chip-estado>
            <ap-seccion-clave-valor titulo="Metadatos del catálogo" [items]="metadatos(c)" />
          </article>
        }
      </ap-estado-de-pantalla>

      @if (recursoCatalogo.hasValue()) {
        <ap-listado [titulo]="t.elementosTitulo" [columnas]="columnas" [listado]="listado" [carga]="carga()" ayudaDeBusqueda="Busca por código, nombre o descripción del elemento." [exportables]="exportables" nombreDeRegistros="elementos" [textoVacio]="t.vacioElementos" explicacionVacio="El catálogo todavía no tiene elementos cargados." (reintentar)="recursoElementos.reload()">
          <ng-template #celda let-e let-col="columna">
            @switch (col.clave) {
              @case ('codigo') { <code>{{ e.codigo }}</code> }
              @case ('activo') { <ap-chip-estado [tono]="e.activo ? 'ok' : 'neutro'">{{ estado(e) }}</ap-chip-estado> }
              @default { {{ e[col.clave] }} }
            }
          </ng-template>
          <ng-template #acciones let-e>
            <ap-acciones-de-fila [fila]="e.nombre">
              <button type="button" apAccionDeFila [attr.aria-label]="(e.activo ? t.desactivar : t.activar) + ' ' + e.nombre" (click)="abrirModal(e)"><ap-icono [nombre]="e.activo ? 'pausa' : 'reproducir'" tamano="s4" />{{ e.activo ? t.desactivar : t.activar }}</button>
            </ap-acciones-de-fila>
          </ng-template>
        </ap-listado>
      }

      @if (elementoParaEditar(); as elem) {
        <ap-modal-cambiar-estado-elemento [catalogoCodigo]="codigo() ?? ''" [elemento]="elem" (cerrar)="cerrarModal()" (guardado)="alGuardar()" />
      }
    </section>
  `,
  styles: `
    :host { display: block; min-width: 0; }
    /* La tabla se desplaza dentro de su marco: las acciones van en una sola línea en vez de apilarse y estirar la fila. */
    ap-acciones-de-fila { flex-wrap: nowrap; }
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s5); min-width: 0; }
    /* ap-listado deja sus pistas en «auto»: sin esto la tabla ensancha la página en vez de desplazarse en su marco. */ ap-listado { grid-template-columns: minmax(0, 1fr); }
    .ficha { display: grid; gap: var(--s3); justify-items: start; padding: var(--s5); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); min-width: 0; }
    .ficha ap-seccion-clave-valor { justify-self: stretch; }
    code { padding: 0 var(--s1); border-radius: var(--r-sm); background: var(--surface-2); font: var(--t-ayuda); font-family: var(--f-mono); color: var(--text-2); overflow-wrap: anywhere; }
  `,
})
export class PantallaCatalogoOperativoDetalle {
  private readonly route = inject(ActivatedRoute)
  protected readonly t = textosSistemas.catalogosOperativos
  protected readonly codigo = signal<string | null>(this.route.snapshot.paramMap.get('codigo'))
  readonly elementoParaEditar = signal<ElementoCatalogoOperativo | null>(null)

  protected readonly recursoCatalogo = catalogoOperativo(this.codigo)
  // El contrato acepta `activo` y `termino`, pero devuelve la lista sin paginar: se pide entera y el kit filtra.
  protected readonly recursoElementos = elementosCatalogo(this.codigo, signal<boolean | null>(null))
  protected readonly carga = cargaDe(this.recursoElementos)
  private readonly filas = filasDe<ElementoCatalogoOperativo>(this.recursoElementos)
  protected readonly estado = estadoDe

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly esVacioCatalogo = (c: CatalogoOperativoDetalle | undefined) => !c
  protected readonly listado = crearListado<ElementoCatalogoOperativo>({
    filas: this.filas,
    camposDeBusqueda: ['codigo', 'nombre', 'descripcion'],
    filtros: [{ nombre: 'activo', etiqueta: 'Estado', valor: estadoDe }],
    porPagina: 10,
    ordenInicial: { clave: 'orden', sentido: 'asc' },
  })
  protected readonly columnas: Columna<ElementoCatalogoOperativo>[] = [
    { clave: 'orden', titulo: '#', numerica: true, ordenable: true },
    { clave: 'codigo', titulo: 'Código', ordenable: true },
    { clave: 'nombre', titulo: 'Elemento', ordenable: true },
    { clave: 'descripcion', titulo: 'Descripción' },
    { clave: 'activo', titulo: 'Estado', ordenable: true },
  ]
  protected readonly exportables: ColumnaExportable<ElementoCatalogoOperativo>[] = [
    { clave: 'orden', titulo: 'Orden' },
    { clave: 'codigo', titulo: 'Código' },
    { clave: 'nombre', titulo: 'Elemento' },
    { clave: 'descripcion', titulo: 'Descripción' },
    { clave: 'activo', titulo: 'Estado', valor: estadoDe },
    { clave: 'actualizadoEn', titulo: 'Actualizado el' },
  ]

  protected metadatos(c: CatalogoOperativoDetalle): ClaveValor[] {
    return [
      { etiqueta: 'Código', valor: c.codigo, mono: true },
      { etiqueta: 'Dueño', valor: c.dueno },
      { etiqueta: 'Versión', valor: c.version },
      { etiqueta: 'Vigente desde', valor: c.vigenteDesde },
      { etiqueta: 'Tabla física', valor: c.tablaFisica, mono: true },
      { etiqueta: 'Base normativa', valor: c.baseNormativa },
      { etiqueta: 'Elementos activos', valor: `${c.elementosActivos} de ${c.totalElementos}` },
    ]
  }

  protected recargar(): void {
    this.recursoCatalogo.reload()
    this.recursoElementos.reload()
  }

  protected abrirModal(elemento: ElementoCatalogoOperativo): void {
    this.elementoParaEditar.set(elemento)
  }

  protected cerrarModal(): void {
    this.elementoParaEditar.set(null)
  }

  protected alGuardar(): void {
    this.cerrarModal()
    this.recargar()
  }
}
