import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha, formatearDia } from '@aportaya/ui/fecha/fecha'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { EstadoDeBandejaVista } from '../comun/estado-de-bandeja'
import { ImporteSinMoneda } from '../comun/importe-sin-moneda'
import { bandejaDeCierresDiarios, type CierreDiarioEnLista } from './dominio/cu51-cierres-diarios'
import { FormularioDeCierre } from './formulario-de-cierre'
import { textosCierres } from './textos'

const T = textosCierres
const CLAVES = ['fecha', 'totalRecaudado', 'totalConciliado', 'totalExcepciones', 'cantidadPagos', 'cuadrado', 'cerradoEn'] as const
const COLUMNAS: ColumnaVirtual<CierreDiarioEnLista>[] = CLAVES.map((clave) => ({ clave, titulo: T.columnas[clave], numerica: clave.startsWith('total') || clave === 'cantidadPagos' }))

/**
 * CU-51 · historial de cierres diarios (AUDITORIA_LEER).
 */
@Component({
  selector: 'ap-pantalla-de-cierres',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, CabeceraDePagina, ChipEstado, Fecha, TablaDeDatosVirtualizada, EstadoDeBandejaVista, ImporteSinMoneda, FormularioDeCierre],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="calendario" />
      @if (puedeCerrar()) {
        <ap-formulario-de-cierre (cerrado)="b.reintentar()" />
      } @else {
        <ap-alerta tono="info">{{ t.formulario.soloContabilidad }}</ap-alerta>
      }
      <ap-estado-de-bandeja [estado]="b.estado()" [error]="b.error()" [vacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [denegado]="t.denegado" (reintentar)="b.reintentar()" />
      @if (b.estado() !== 'denegado' && b.estado() !== 'vacio') {
        @for (v of [b.version()]; track v) {
          <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="COLUMNAS" [cargador]="b.cargador" [ordenPermitido]="[]" [identidad]="identidad" [tamanoDePagina]="25">
            <ng-template #celda let-r let-columna="columna">
              @switch (columna.clave) {
                @case ('fecha') { {{ dia(r.fecha) }} }
                @case ('cantidadPagos') { {{ r.cantidadPagos }} }
                @case ('cuadrado') {
                  <ap-chip-estado [tono]="r.cuadrado ? 'ok' : 'aviso'">{{ r.cuadrado ? t.cuadrado : t.conObservaciones }}</ap-chip-estado>
                  @if (r.reabiertoEn) { <span class="nota">{{ t.reabierto }}</span> }
                }
                @case ('cerradoEn') { <ap-fecha [iso]="r.cerradoEn" /> }
                @default { <ap-importe-sin-moneda [monto]="r[columna.clave]" /> }
              }
            </ng-template>
          </ap-tabla-de-datos-virtualizada>
        }
      }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 80rem; display: grid; gap: var(--s5); }
    .nota { display: block; color: var(--text-2); font-size: .875rem; }
  `,
})
export class PantallaDeCierres {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  private readonly sesion = inject(Sesion)
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly b = bandejaDeCierresDiarios()
  protected readonly identidad = (r: CierreDiarioEnLista) => r.cierreId
  protected readonly puedeCerrar = computed(() => this.sesion.puede('CONTABILIDAD'))
  protected readonly dia = formatearDia
}
