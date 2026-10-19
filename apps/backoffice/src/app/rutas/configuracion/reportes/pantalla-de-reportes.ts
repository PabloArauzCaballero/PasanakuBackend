import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { CargadorDePagina, ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { AvisoDeListado } from '../aviso-de-listado'
import { cargadorDeDefiniciones, cargadorDeEjecuciones, type DefinicionDeReporteEnLista, type EjecucionDeReporteEnLista } from '../dominio/cu58-reportes'
import { EstadoDeListado, NO_DISPONIBLE } from '../dominio/listado'
import { chipsDe, filtroDeEstadoDesdeUrl } from '../filtro-de-estado'
import { textosConfiguracion } from '../textos'

const COLUMNAS_CATALOGO: ColumnaVirtual<DefinicionDeReporteEnLista>[] = [
  { clave: 'nombre', titulo: 'Reporte' },
  { clave: 'tipo', titulo: 'Tipo' },
  { clave: 'descripcion', titulo: 'Descripción', ancho: '2' },
  { clave: 'permisoRequerido', titulo: 'Permiso para ejecutarlo' },
  { clave: 'contieneDatosSensibles', titulo: 'Sensibilidad' },
]

/** Sin quién lo pidió ni sus parámetros: el contrato no publica los parámetros y la persona no hace falta para auditar. */
const COLUMNAS_EJECUCIONES: ColumnaVirtual<EjecucionDeReporteEnLista>[] = [
  { clave: 'iniciadaEn', titulo: 'Iniciada' },
  { clave: 'definicionId', titulo: 'Reporte' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'filasGeneradas', titulo: 'Filas', numerica: true },
  { clave: 'duracionMs', titulo: 'Duración (ms)', numerica: true },
  { clave: 'hashResultado', titulo: 'Huella del resultado' },
  { clave: 'finalizadaEn', titulo: 'Finalizada' },
]

/**
 * CU-58 · Catálogo de reportes e historial de ejecuciones, de solo lectura. El nombre de cada ejecución se toma del
 * catálogo ya cargado; si su definición no está en lo cargado se muestra el identificador, no un nombre supuesto.
 */
@Component({
  selector: 'ap-pantalla-de-reportes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AvisoDeListado, CabeceraDePagina, BarraDeFiltros, ChipEstado, Fecha, TablaDeDatosVirtualizada],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="libro" />
      <p class="nota">{{ comun.soloLectura }}</p>

      <ap-aviso-de-listado [error]="catalogo.error()" [vacio]="catalogo.vacio()" [mensajeVacio]="t.definiciones.vacio" (reintentar)="catalogo.reintentar()" />
      @if (!catalogo.error()) {
        <ap-tabla-de-datos-virtualizada [titulo]="t.definiciones.titulo" [columnas]="columnasCatalogo" [cargador]="cargadorCatalogo" [ordenPermitido]="[]" [identidad]="idDefinicion" [alturaDeViewport]="320">
          <ng-template #celda let-r let-columna="columna">
            @switch (columna.clave) {
              @case ('contieneDatosSensibles') {
                <ap-chip-estado [tono]="r.contieneDatosSensibles ? 'aviso' : 'neutro'">{{ r.contieneDatosSensibles ? t.sensible : t.noSensible }}</ap-chip-estado>
              }
              @default { {{ r[columna.clave] ?? noDisponible }} }
            }
          </ng-template>
        </ap-tabla-de-datos-virtualizada>
      }

      <p class="nota">{{ t.ejecuciones.corte }}</p>
      <ap-barra-de-filtros [etiqueta]="comun.estado" [incluirBusqueda]="false" [definiciones]="chips" />
      <ap-aviso-de-listado [error]="historial.error()" [vacio]="historial.vacio()" [conFiltro]="!!filtros()['estado']" [mensajeVacio]="t.ejecuciones.vacio" (reintentar)="historial.reintentar()" />
      @if (!historial.error()) {
        <ap-tabla-de-datos-virtualizada [titulo]="t.ejecuciones.titulo" [columnas]="columnasEjecuciones" [cargador]="cargadorHistorial" [ordenPermitido]="[]" [identidad]="idEjecucion" [filtros]="filtros()">
          <ng-template #celda let-r let-columna="columna">
            @switch (columna.clave) {
              @case ('iniciadaEn') { <ap-fecha [iso]="r.iniciadaEn" /> }
              @case ('finalizadaEn') { @if (r.finalizadaEn) { <ap-fecha [iso]="r.finalizadaEn" /> } @else { {{ noDisponible }} } }
              @case ('definicionId') { {{ nombres()[r.definicionId] ?? r.definicionId }} }
              @case ('estado') { <ap-chip-estado>{{ etiquetas[r.estado] ?? r.estado }}</ap-chip-estado> }
              @default { {{ r[columna.clave] ?? noDisponible }} }
            }
          </ng-template>
        </ap-tabla-de-datos-virtualizada>
      }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; gap: var(--s4); }
    .nota { margin: 0; color: var(--text-2); }
  `,
})
export class PantallaDeReportes {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected readonly t = textosConfiguracion.reportes
  protected readonly comun = textosConfiguracion.comun
  protected readonly noDisponible = NO_DISPONIBLE
  protected readonly etiquetas: Record<string, string> = textosConfiguracion.reportes.estados
  protected readonly columnasCatalogo = COLUMNAS_CATALOGO
  protected readonly columnasEjecuciones = COLUMNAS_EJECUCIONES
  protected readonly chips = chipsDe(textosConfiguracion.reportes.estados)
  protected readonly filtros = filtroDeEstadoDesdeUrl(Object.keys(textosConfiguracion.reportes.estados))
  protected readonly catalogo = new EstadoDeListado()
  protected readonly historial = new EstadoDeListado()
  /** Nombre por definición, de las páginas del catálogo que ya se vieron. */
  protected readonly nombres = signal<Readonly<Record<string, string>>>({})
  protected readonly cargadorCatalogo = this.catalogo.envolver(this.recordandoNombres(cargadorDeDefiniciones()))
  protected readonly cargadorHistorial = this.historial.envolver(cargadorDeEjecuciones())
  protected readonly idDefinicion = (r: DefinicionDeReporteEnLista) => r.definicionId
  protected readonly idEjecucion = (r: EjecucionDeReporteEnLista) => r.ejecucionId

  private recordandoNombres(cargador: CargadorDePagina<DefinicionDeReporteEnLista>): CargadorDePagina<DefinicionDeReporteEnLista> {
    return async (pedido) => {
      const pagina = await cargador(pedido)
      this.nombres.update((n) => ({ ...n, ...Object.fromEntries(pagina.filas.map((d) => [d.definicionId, d.nombre])) }))
      return pagina
    }
  }
}
