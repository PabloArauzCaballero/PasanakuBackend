import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { Monto } from '@aportaya/ui/monto/monto'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { EstadoDeBandejaVista } from '../comun/estado-de-bandeja'
import { bandejaDeConciliaciones, esCero, type ConciliacionEnLista } from './dominio/cu50-conciliaciones'
import { FormularioDeConciliacion } from './formulario-de-conciliacion'
import { textosCustodia } from './textos'

const T = textosCustodia
const CLAVES = ['fecha', 'saldoEmitido', 'saldoCustodia', 'saldoEnTransito', 'diferencia', 'ratioCobertura', 'estado', 'descuadresAbiertos'] as const
const COLUMNAS: ColumnaVirtual<ConciliacionEnLista>[] = CLAVES.map((clave) => ({ clave, titulo: T.columnas[clave], numerica: clave.startsWith('saldo') || clave === 'diferencia' }))

/**
 * CU-50 · historial de conciliaciones de custodia (AUDITORIA_LEER) con las cifras enfrentadas por corte y, para
 * Tesorería (rol TESORERIA en el token, el mismo `@Permiso` del servidor), el formulario para registrar una nueva.
 * La diferencia se destaca con texto («Descuadre») además del tono: nunca solo por color (regla 95.4.6).
 */
@Component({
  selector: 'ap-pantalla-de-conciliaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, BandaDeProposito, ChipEstado, Monto, TablaDeDatosVirtualizada, EstadoDeBandejaVista, FormularioDeConciliacion],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <section class="pantalla">
      <h1>{{ t.titulo }}</h1>
      @if (puedeConciliar()) {
        <ap-formulario-de-conciliacion (registrada)="b.reintentar()" />
      } @else {
        <ap-alerta tono="info">{{ t.formulario.soloTesoreria }}</ap-alerta>
      }
      <ap-estado-de-bandeja [estado]="b.estado()" [error]="b.error()" [vacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [denegado]="t.denegado" (reintentar)="b.reintentar()" />
      @if (b.estado() !== 'denegado') {
        @for (v of [b.version()]; track v) {
          <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="COLUMNAS" [cargador]="b.cargador" [ordenPermitido]="[]" [identidad]="identidad" [tamanoDePagina]="25">
            <ng-template #celda let-r let-columna="columna">
              @switch (columna.clave) {
                @case ('fecha') { {{ dia(r.fecha) }} }
                @case ('diferencia') {
                  <span [class.descuadre]="!esCero(r.diferencia.monto)"><strong>{{ esCero(r.diferencia.monto) ? t.cuadra : t.descuadre }}</strong> <ap-monto [monto]="r.diferencia.monto" [moneda]="r.diferencia.moneda" [etiqueta]="t.columnas.diferencia" /></span>
                }
                @case ('ratioCobertura') { {{ r.ratioCobertura }} · {{ r.cumpleEncaje ? t.cumpleEncaje : t.noCumpleEncaje }} }
                @case ('estado') { <ap-chip-estado [tono]="r.estado === 'CUADRADA' ? 'ok' : r.estado === 'DESCUADRADA' ? 'error' : 'aviso'">{{ estadoDe(r) }}</ap-chip-estado> }
                @case ('descuadresAbiertos') { {{ r.descuadresAbiertos }} }
                @default { <ap-monto [monto]="r[columna.clave].monto" [moneda]="r[columna.clave].moneda" [etiqueta]="columna.titulo" /> }
              }
            </ng-template>
          </ap-tabla-de-datos-virtualizada>
        }
      }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 80rem; display: grid; gap: var(--s5); }
    h1 { margin: 0; }
    .descuadre { color: var(--err-texto); }
  `,
})
export class PantallaDeConciliaciones {
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly b = bandejaDeConciliaciones()
  protected readonly identidad = (r: ConciliacionEnLista) => r.conciliacionId
  protected readonly puedeConciliar = computed(() => this.sesion.puede('TESORERIA'))
  protected readonly esCero = esCero
  protected readonly dia = formatearDia

  protected estadoDe(r: ConciliacionEnLista): string {
    return T.estados[r.estado as keyof typeof T.estados]
  }
}
