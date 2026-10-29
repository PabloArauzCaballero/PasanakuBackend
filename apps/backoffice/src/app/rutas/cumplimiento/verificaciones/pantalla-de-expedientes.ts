import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { Fecha, formatearDia, formatearFecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { ListadoDeDatos } from '@aportaya/ui/listado/listado'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ExpedienteEnRevision } from 'clientes/angular/identidad'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Exportador } from '../../../nucleo/exportador'
import { cargaDe } from '../../sistemas/compartido/carga'
import { expedientesEnEstado, POR_DECIDIR, recortar, tonoDelExpediente, vencimientoDe } from '../dominio/cu02-expedientes'
import { documentoEnPalabras, estadoEnPalabras, fotosCompletas, fotosEnNumeros, OPCIONES_DE_ESTADO, tiempoEnCola, vencimientoEnPalabras } from '../dominio/cu02-presentacion'
import { textosVerificaciones } from './textos-verificaciones'

const T = textosVerificaciones.bandeja
const C = T.columnas
const documento = (e: ExpedienteEnRevision) => documentoEnPalabras(e.documento).texto
const correo = (e: ExpedienteEnRevision) => e.correo || T.sinCorreo
const fotos = (e: ExpedienteEnRevision) => (fotosCompletas(e) ? T.fotosCompletas : T.fotosIncompletas)
/**
 * Cinco columnas y la de acción: con siete, a 1440 px la tabla escondía fotos, estado y fecha detrás del
 * desplazamiento horizontal. El contacto va debajo del nombre y el vencimiento debajo del documento.
 */
const COLUMNAS: Columna<ExpedienteEnRevision>[] = [
  { clave: 'nombreCompleto', titulo: C.persona, ordenable: true },
  { clave: 'documento', titulo: C.documento, ordenable: true },
  { clave: 'fotos', titulo: C.fotos },
  { clave: 'estado', titulo: C.estado, ordenable: true },
  { clave: 'iniciadaEn', titulo: C.iniciada, ordenable: true },
]
/** El archivo lleva lo que la tabla muestra, en palabras (sin ids internos ni fotos). Ver A3 del 2026-10-26. */
const EXPORTABLES: ColumnaExportable<ExpedienteEnRevision>[] = [
  { clave: 'nombreCompleto', titulo: C.persona },
  { clave: 'telefono', titulo: C.telefono },
  { clave: 'correo', titulo: C.correo, valor: correo },
  { clave: 'documento', titulo: C.documento, valor: documento },
  { clave: 'fechaExpiracionDocumento', titulo: C.vencimiento, valor: (e) => (e.fechaExpiracionDocumento ? formatearDia(e.fechaExpiracionDocumento) : T.sinFecha) },
  { clave: 'documentoVigente', titulo: C.vigencia, valor: vencimientoEnPalabras },
  { clave: 'fotos', titulo: C.fotos, valor: fotosEnNumeros },
  { clave: 'estado', titulo: C.estado, valor: (e) => estadoEnPalabras(e.estado) },
  { clave: 'iniciadaEn', titulo: C.iniciada, valor: (e) => formatearFecha(e.iniciadaEn) },
  { clave: 'resueltaEn', titulo: C.resuelta, valor: (e) => (e.resueltaEn ? formatearFecha(e.resueltaEn) : T.sinResolver) },
  { clave: 'motivoRechazo', titulo: C.motivo, valor: (e) => e.motivoRechazo ?? '' },
]

/**
 * CU-02 · la cola de expedientes de identidad, **en tabla** con la anatomía de `ap-listado` (2026-10-28). El estado
 * viaja al servidor (`?estado=`); «Por decidir» pide sin filtro y recorta los dos estados que esperan a una persona.
 * Sobre lo cargado se busca, se filtra por vencimiento y fotos, se ordena, se pagina y se descarga lo filtrado tal
 * como se ve. La decisión se toma en la ficha (`verificaciones/:id`), con el expediente entero a la vista: decidir
 * desde una fila es decidir sin mirar.
 */
@Component({
  selector: 'ap-pantalla-de-expedientes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, BarraDeFiltros, CabeceraDePagina, ChipEstado, Fecha, Icono, ListadoDeDatos, RouterLink],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Cumplimiento · CU-02" [titulo]="t.titulo" [descripcion]="t.descripcion" />
      <ap-barra-de-filtros data-tutorial-id="cumplimiento-filtros" etiqueta="Filtros de expedientes" [etiquetaDeBusqueda]="t.buscar" [ayudaDeBusqueda]="t.ayudaBusqueda" [busqueda]="listado.busqueda()" [selectores]="selectores()" textoLimpiar="Limpiar filtros" (busquedaChange)="listado.buscar($event)" (selectorChange)="alFiltrar($event)" (limpiar)="limpiar()" />
      <ap-listado data-tutorial-id="cumplimiento-cola" [titulo]="t.titulo" [columnas]="COLUMNAS" [listado]="listado" [carga]="carga()" [conFiltros]="false" [identidad]="porId" [exportable]="exportador.puedeExportar('expedientes-de-identidad')" nombreDeExportacion="expedientes de identidad" [exportables]="EXPORTABLES" nombreDeRegistros="expedientes" [textoVacio]="vacio()" [explicacionVacio]="t.explicacionVacio" textoAcciones="Acción" (reintentar)="cola.reload()">
        <ng-template #celda let-e let-columna="columna">
          @switch (columna.clave) {
            @case ('nombreCompleto') {
              <span class="apilado">
                <a class="persona" [routerLink]="e.verificacionId"><ap-icono nombre="persona" tamano="s4" />{{ e.nombreCompleto }}</a>
                <span class="tenue">{{ e.telefono }} · {{ correo(e) }}</span>
              </span>
            }
            @case ('documento') {
              <span class="apilado">
                <span [class.tenue]="!e.documento">{{ documento(e) }}</span>
                <span class="vence">
                  @if (e.fechaExpiracionDocumento) { <span class="tenue">Vence {{ dia(e.fechaExpiracionDocumento) }}</span> }
                  <ap-chip-estado [tono]="vence(e) === 'vigente' ? 'ok' : 'aviso'">{{ vence(e) === 'sin-fecha' ? t.sinFecha : vigencia(e) }}</ap-chip-estado>
                </span>
              </span>
            }
            @case ('fotos') { <ap-chip-estado [tono]="completas(e) ? 'ok' : 'aviso'" [icono]="completas(e) ? 'imagen' : 'advertencia'">{{ enNumeros(e) }}</ap-chip-estado> }
            @case ('estado') { <ap-chip-estado [tono]="tono(e)">{{ estado(e.estado) }}</ap-chip-estado> }
            @case ('iniciadaEn') { <span class="apilado"><ap-fecha [iso]="e.iniciadaEn" [conHora]="false" /><span class="tenue">{{ enCola(e) }}</span></span> }
          }
        </ng-template>
        <ng-template #acciones let-e>
          <ap-acciones-de-fila [fila]="'expediente de ' + e.nombreCompleto">
            <a apAccionDeFila [routerLink]="e.verificacionId" [attr.aria-label]="(pendiente(e) ? t.revisar : t.ver) + ' el expediente de ' + e.nombreCompleto"><ap-icono [nombre]="pendiente(e) ? 'aprobar' : 'ojo'" tamano="s4" /><span>{{ pendiente(e) ? t.revisar : t.ver }}</span></a>
          </ap-acciones-de-fila>
        </ng-template>
      </ap-listado>
    </section>
  `,
  styles: `
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .persona { display: inline-flex; align-items: center; gap: var(--s2); color: var(--text); font-weight: 600; text-decoration: none; }
    .persona:hover { color: var(--brand-texto); text-decoration: underline; }
    .persona:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .apilado { display: inline-grid; gap: var(--s1); justify-items: start; }
    .tenue { color: var(--text-3); font: var(--t-cuerpo-chico); }
    .vence { display: inline-flex; flex-wrap: wrap; align-items: center; gap: var(--s2); }
    a[apAccionDeFila] { text-decoration: none; }
  `,
})
export class PantallaDeExpedientes {
  /** Datos de identidad sin enmascarar: la descarga en el navegador solo con `exportar:expedientes-de-identidad` (regla 90.2). */
  protected readonly exportador = inject(Exportador)
  constructor() {
    inject(ExplicadorDeVista).fijar(textosVerificaciones.notaDeVista)
  }
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly porId = (e: ExpedienteEnRevision) => e.verificacionId
  protected readonly documento = documento
  protected readonly dia = formatearDia
  protected readonly correo = correo
  protected readonly estado = estadoEnPalabras
  protected readonly vigencia = vencimientoEnPalabras
  protected readonly vence = vencimientoDe
  protected readonly completas = fotosCompletas
  protected readonly enNumeros = fotosEnNumeros
  protected readonly tono = tonoDelExpediente
  /** Referencia para ordenar el trabajo, con el reloj de quien mira; la fecha de arriba es la del servidor. */
  protected readonly enCola = (e: ExpedienteEnRevision) => (e.resueltaEn ? 'Tardó ' : 'Hace ') + tiempoEnCola(e, new Date()).toLowerCase()
  protected readonly pendiente = (e: ExpedienteEnRevision) => recortar([e], POR_DECIDIR).length > 0
  /** La cola de trabajo primero y por omisión; después los del contrato, no una lista paralela. */
  protected readonly estadoPedido = signal<string>(POR_DECIDIR)
  protected readonly cola = expedientesEnEstado(this.estadoPedido)
  protected readonly carga = cargaDe(this.cola)
  /** `POR_DECIDIR` se recorta acá porque el backend filtra por UN estado y los que esperan una persona son dos. */
  private readonly recortados = computed(() => recortar(this.cola.hasValue() ? (this.cola.value() ?? []) : [], this.estadoPedido()))
  protected readonly listado = crearListado<ExpedienteEnRevision>({
    filas: this.recortados,
    camposDeBusqueda: ['nombreCompleto', 'telefono', 'correo', documento, (e) => estadoEnPalabras(e.estado)],
    filtros: [
      { nombre: 'vencimiento', etiqueta: C.vigencia, valor: vencimientoEnPalabras },
      { nombre: 'fotos', etiqueta: C.fotos, valor: fotos },
    ],
    ordenInicial: { clave: 'iniciadaEn', sentido: 'asc' },
    porPagina: 10,
    opcionesPorPagina: [5, 10, 20, 50],
  })
  private readonly selectorDeEstado = computed<Selector>(() => ({
    nombre: 'estado',
    etiqueta: C.estado,
    textoTodos: T.porDecidir,
    valor: this.estadoPedido() === POR_DECIDIR ? '' : this.estadoPedido(),
    opciones: OPCIONES_DE_ESTADO,
  }))
  protected readonly selectores = computed(() => [this.selectorDeEstado(), ...this.listado.selectores()])
  protected readonly vacio = computed(() => (this.estadoPedido() === POR_DECIDIR ? T.sinPendientes : T.sinEnEstado(estadoEnPalabras(this.estadoPedido()))))

  protected alFiltrar({ nombre, valor }: { nombre: string; valor: string }): void {
    if (nombre === 'estado') {
      // Otro estado es otra consulta: seguir en la página N de la anterior muestra una lista que nadie pidió.
      this.estadoPedido.set(valor || POR_DECIDIR)
      this.listado.irAPagina(1)
    } else this.listado.filtrar(nombre, valor)
  }

  protected limpiar(): void {
    this.listado.limpiar()
    this.estadoPedido.set(POR_DECIDIR)
  }
}
