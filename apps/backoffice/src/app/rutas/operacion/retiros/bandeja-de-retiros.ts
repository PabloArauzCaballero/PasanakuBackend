import { ChangeDetectionStrategy, Component, inject } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Monto } from '@aportaya/ui/monto/monto'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { EstadoDeBandejaVista } from '../comun/estado-de-bandeja'
import { bandejaDeRetiros, type RetiroEnRevision } from './dominio/cu11-retiros-en-revision'
import { textosRetiros } from './textos'

const T = textosRetiros.bandeja
const COLUMNAS: ColumnaVirtual<RetiroEnRevision>[] = [
  { clave: 'solicitadaEn', titulo: T.columnas.solicitadaEn },
  { clave: 'solicitado', titulo: T.columnas.solicitado, numerica: true },
  { clave: 'costo', titulo: T.columnas.costo, numerica: true },
  { clave: 'neto', titulo: T.columnas.neto, numerica: true },
  { clave: 'mfaVerificado', titulo: T.columnas.mfaVerificado },
  { clave: 'ventanaEnfriamientoHasta', titulo: T.columnas.ventanaEnfriamientoHasta },
  { clave: 'ordenRetiroId', titulo: T.columnas.ordenRetiroId, ancho: '1.4' },
]

/**
 * CU-11 · bandeja de retiros en revisión.
 */
@Component({
  selector: 'ap-bandeja-de-retiros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, Fecha, Monto, TablaDeDatosVirtualizada, EstadoDeBandejaVista],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="retiro" />
      <ap-estado-de-bandeja [estado]="b.estado()" [error]="b.error()" [vacio]="t.vacio" [explicacionVacio]="t.explicacionVacio" [denegado]="t.denegado" (reintentar)="b.reintentar()" />
      @if (b.estado() !== 'denegado' && b.estado() !== 'vacio') {
        @for (v of [b.version()]; track v) {
          <ap-tabla-de-datos-virtualizada [titulo]="t.titulo" [columnas]="COLUMNAS" [cargador]="b.cargador" [ordenPermitido]="[]" [identidad]="identidad" [tamanoDePagina]="25">
            <ng-template #celda let-r let-columna="columna">
              @switch (columna.clave) {
                @case ('solicitadaEn') { <ap-fecha [iso]="r.solicitadaEn" /> }
                @case ('solicitado') { <ap-monto [monto]="r.solicitado.monto" [moneda]="r.solicitado.moneda" [etiqueta]="t.columnas.solicitado" /> }
                @case ('costo') { <ap-monto [monto]="r.costo.monto" [moneda]="r.costo.moneda" [etiqueta]="t.columnas.costo" /> }
                @case ('neto') { <ap-monto [monto]="r.neto.monto" [moneda]="r.neto.moneda" [etiqueta]="t.columnas.neto" /> }
                @case ('mfaVerificado') { {{ r.mfaVerificado ? t.conSegundoFactor : t.sinSegundoFactor }} }
                @case ('ventanaEnfriamientoHasta') {
                  @if (r.ventanaEnfriamientoHasta) { <ap-fecha [iso]="r.ventanaEnfriamientoHasta" /> } @else { {{ t.sinEnfriamiento }} }
                }
                @case ('ordenRetiroId') {
                  @if (r.puedeDecidir) { <ap-boton variante="primario" (pulsado)="abrir(r)">{{ t.decidir }}</ap-boton> }
                  @else { <span class="propia">{{ t.propia }}</span> }
                }
              }
            </ng-template>
          </ap-tabla-de-datos-virtualizada>
        }
      }
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 72rem; display: grid; gap: var(--s4); }
    .propia { color: var(--text-2); white-space: normal; }
  `,
})
export class BandejaDeRetiros {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  protected readonly t = T
  protected readonly COLUMNAS = COLUMNAS
  protected readonly b = bandejaDeRetiros()
  protected readonly identidad = (r: RetiroEnRevision) => r.ordenRetiroId
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)

  /** La ficha recibe la fila por el estado de la navegación: el contrato no publica una consulta de la orden. */
  protected abrir(r: RetiroEnRevision): void {
    if (!r.puedeDecidir) return
    void this.router.navigate([r.ordenRetiroId], { relativeTo: this.ruta, state: { retiro: r } })
  }
}
