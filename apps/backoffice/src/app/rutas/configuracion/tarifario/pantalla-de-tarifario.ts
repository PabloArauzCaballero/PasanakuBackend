import { ChangeDetectionStrategy, Component } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import type { Tono } from '@aportaya/ui/tono/tono'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { AvisoDeListado } from '../aviso-de-listado'
import { cargadorDeTarifarios, type TarifarioEnLista } from '../dominio/cu34-tarifarios'
import { EstadoDeListado, NO_DISPONIBLE } from '../dominio/listado'
import { chipsDe, filtroDeEstadoDesdeUrl } from '../filtro-de-estado'
import { textosConfiguracion } from '../textos'

const COLUMNAS: ColumnaVirtual<TarifarioEnLista>[] = [
  { clave: 'codigo', titulo: 'Código' },
  { clave: 'version', titulo: 'Versión', numerica: true, ancho: '0 0 6rem' },
  { clave: 'nombre', titulo: 'Nombre' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'monedaBase', titulo: 'Moneda', ancho: '0 0 6rem' },
  { clave: 'vigenteDesde', titulo: 'Vigente desde' },
  { clave: 'vigenteHasta', titulo: 'Vigente hasta' },
  { clave: 'diasPreaviso', titulo: 'Preaviso (días)', numerica: true },
  { clave: 'simulacion', titulo: 'Última simulación' },
]

const TONO: Record<TarifarioEnLista['estado'], Tono> = { VIGENTE: 'ok', EN_PREAVISO: 'aviso', BORRADOR: 'info', SUSTITUIDO: 'neutro', ARCHIVADO: 'neutro' }

/**
 * CU-34 · Historial del tarifario, de solo lectura. Pagina por cursor del servidor (sin orden propio: el contrato ordena
 * por vigencia descendente). Un campo opcional ausente se dice «no disponible».
 */
@Component({
  selector: 'ap-pantalla-de-tarifario',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AvisoDeListado, BandaDeProposito, BarraDeFiltros, ChipEstado, Fecha, TablaDeDatosVirtualizada],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <section class="pantalla">
      <h1>{{ t.titulo }}</h1>
      <p class="nota">{{ comun.soloLectura }}</p>
      <ap-barra-de-filtros [etiqueta]="comun.estado" [incluirBusqueda]="false" [definiciones]="chips" />
      <ap-aviso-de-listado [error]="lectura.error()" [vacio]="lectura.vacio()" [conFiltro]="!!filtros()['estado']" [mensajeVacio]="t.vacio" (reintentar)="lectura.reintentar()" />
      @if (!lectura.error()) {
        <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="columnas" [cargador]="cargador" [ordenPermitido]="[]" [identidad]="identidad" [filtros]="filtros()">
          <ng-template #celda let-r let-columna="columna">
            @switch (columna.clave) {
              @case ('estado') { <ap-chip-estado [tono]="tonoDe(r)">{{ etiquetas[r.estado] }}</ap-chip-estado> }
              @case ('vigenteDesde') { <ap-fecha [iso]="r.vigenteDesde" /> }
              @case ('vigenteHasta') { @if (r.vigenteHasta) { <ap-fecha [iso]="r.vigenteHasta" /> } @else { {{ noDisponible }} } }
              @case ('simulacion') {
                @if (r.simulacion; as s) { {{ s.deltaIngresoEstimado }} ({{ t.deltaSinMoneda }}) · {{ s.usuariosImpactados }} {{ t.usuarios }} } @else { {{ noDisponible }} }
              }
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
export class PantallaDeTarifario {
  protected readonly t = textosConfiguracion.tarifario
  protected readonly comun = textosConfiguracion.comun
  protected readonly columnas = COLUMNAS
  protected readonly etiquetas: Record<string, string> = textosConfiguracion.tarifario.estados
  protected readonly noDisponible = NO_DISPONIBLE
  protected readonly chips = chipsDe(textosConfiguracion.tarifario.estados)
  protected readonly filtros = filtroDeEstadoDesdeUrl(Object.keys(textosConfiguracion.tarifario.estados))
  protected readonly lectura = new EstadoDeListado()
  protected readonly cargador = this.lectura.envolver(cargadorDeTarifarios())
  protected readonly identidad = (r: TarifarioEnLista) => r.tarifarioId
  protected tonoDe(r: TarifarioEnLista): Tono {
    return TONO[r.estado] ?? 'neutro'
  }
}
