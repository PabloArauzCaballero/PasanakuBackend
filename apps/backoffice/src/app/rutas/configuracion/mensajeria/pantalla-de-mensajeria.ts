import { ChangeDetectionStrategy, Component } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { AvisoDeListado } from '../aviso-de-listado'
import { cargadorDeEnvios, cargadorDePlantillas, type EnvioEnLista, type PlantillaEnLista } from '../dominio/cu80-cu83-mensajeria'
import { EstadoDeListado, NO_DISPONIBLE } from '../dominio/listado'
import { chipsDe, filtroDeEstadoDesdeUrl } from '../filtro-de-estado'
import { textosConfiguracion } from '../textos'

const COLUMNAS_PLANTILLAS: ColumnaVirtual<PlantillaEnLista>[] = [
  { clave: 'codigo', titulo: 'Código' },
  { clave: 'evento', titulo: 'Evento' },
  { clave: 'canal', titulo: 'Canal' },
  { clave: 'estadoAprobacion', titulo: 'Aprobación' },
  { clave: 'activa', titulo: 'Activa' },
  { clave: 'version', titulo: 'Versión', numerica: true },
  { clave: 'idioma', titulo: 'Idioma' },
  { clave: 'vigenteDesde', titulo: 'Vigente desde' },
  { clave: 'vigenteHasta', titulo: 'Vigente hasta' },
]

/** Sin destinatario, contenido ni identificador del proveedor: el contrato no los trae y esta tabla no los pide. */
const COLUMNAS_ENVIOS: ColumnaVirtual<EnvioEnLista>[] = [
  { clave: 'encoladoEn', titulo: 'Encolado' },
  { clave: 'canal', titulo: 'Canal' },
  { clave: 'proveedor', titulo: 'Proveedor' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'intentos', titulo: 'Intentos', numerica: true },
  { clave: 'costo', titulo: 'Costo', numerica: true },
  { clave: 'codigoError', titulo: 'Código de error' },
  { clave: 'entregadoEn', titulo: 'Entregado' },
]

/**
 * CU-80 / CU-83 · Mensajería de solo lectura: qué plantilla rige cada mensaje y la auditoría de envíos. El filtro de
 * estado de la URL aplica a los envíos (es lo que se audita: fallidos, expirados).
 */
@Component({
  selector: 'ap-pantalla-de-mensajeria',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AvisoDeListado, BandaDeProposito, BarraDeFiltros, ChipEstado, Fecha, TablaDeDatosVirtualizada],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <section class="pantalla">
      <h1>{{ t.titulo }}</h1>
      <p class="nota">{{ comun.soloLectura }}</p>

      <ap-aviso-de-listado [error]="plantillas.error()" [vacio]="plantillas.vacio()" [mensajeVacio]="t.plantillas.vacio" (reintentar)="plantillas.reintentar()" />
      @if (!plantillas.error()) {
        <ap-tabla-de-datos-virtualizada [titulo]="t.plantillas.titulo" [columnas]="columnasPlantillas" [cargador]="cargadorPlantillas" [ordenPermitido]="[]" [identidad]="idPlantilla" [alturaDeViewport]="320">
          <ng-template #celda let-r let-columna="columna">
            @switch (columna.clave) {
              @case ('estadoAprobacion') { <ap-chip-estado>{{ etiqueta(t.estadosPlantilla, r.estadoAprobacion) }}</ap-chip-estado> }
              @case ('activa') { {{ r.activa ? t.activa : t.inactiva }} }
              @case ('vigenteDesde') { <ap-fecha [iso]="r.vigenteDesde" /> }
              @case ('vigenteHasta') { @if (r.vigenteHasta) { <ap-fecha [iso]="r.vigenteHasta" /> } @else { {{ noDisponible }} } }
              @default { {{ r[columna.clave] ?? noDisponible }} }
            }
          </ng-template>
        </ap-tabla-de-datos-virtualizada>
      }

      <ap-barra-de-filtros [etiqueta]="comun.estado" [incluirBusqueda]="false" [definiciones]="chipsEnvios" />
      <ap-aviso-de-listado [error]="envios.error()" [vacio]="envios.vacio()" [conFiltro]="!!filtros()['estado']" [mensajeVacio]="t.envios.vacio" (reintentar)="envios.reintentar()" />
      @if (!envios.error()) {
        <ap-tabla-de-datos-virtualizada [titulo]="t.envios.titulo" [columnas]="columnasEnvios" [cargador]="cargadorEnvios" [ordenPermitido]="[]" [identidad]="idEnvio" [filtros]="filtros()">
          <ng-template #celda let-r let-columna="columna">
            @switch (columna.clave) {
              @case ('estado') { <ap-chip-estado>{{ etiqueta(t.estadosEnvio, r.estado) }}</ap-chip-estado> }
              @case ('intentos') { {{ r.intentos }} / {{ r.maxIntentos }} }
              @case ('costo') { {{ r.costo }} {{ r.moneda }} }
              @case ('encoladoEn') { <ap-fecha [iso]="r.encoladoEn" /> }
              @case ('entregadoEn') { @if (r.entregadoEn) { <ap-fecha [iso]="r.entregadoEn" /> } @else { {{ noDisponible }} } }
              @default { {{ r[columna.clave] ?? noDisponible }} }
            }
          </ng-template>
        </ap-tabla-de-datos-virtualizada>
      }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; gap: var(--s4); }
    h1, .nota { margin: 0; }
    .nota { color: var(--text-2); }
  `,
})
export class PantallaDeMensajeria {
  protected readonly t = textosConfiguracion.mensajeria
  protected readonly comun = textosConfiguracion.comun
  protected readonly noDisponible = NO_DISPONIBLE
  protected readonly columnasPlantillas = COLUMNAS_PLANTILLAS
  protected readonly columnasEnvios = COLUMNAS_ENVIOS
  protected readonly chipsEnvios = chipsDe(textosConfiguracion.mensajeria.estadosEnvio)
  protected readonly filtros = filtroDeEstadoDesdeUrl(Object.keys(textosConfiguracion.mensajeria.estadosEnvio))
  protected readonly plantillas = new EstadoDeListado()
  protected readonly envios = new EstadoDeListado()
  protected readonly cargadorPlantillas = this.plantillas.envolver(cargadorDePlantillas())
  protected readonly cargadorEnvios = this.envios.envolver(cargadorDeEnvios())
  protected readonly idPlantilla = (r: PlantillaEnLista) => r.versionId
  protected readonly idEnvio = (r: EnvioEnLista) => r.envioId

  /** Un estado que el contrato agregue después se muestra con su código, no se oculta. */
  protected etiqueta(etiquetas: Readonly<Record<string, string>>, codigo: string): string {
    return etiquetas[codigo] ?? codigo
  }
}
