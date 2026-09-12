import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute } from '@angular/router'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { cargadorDeOrdenes, motivoParaNoFacturar, puedeFacturarse, type OrdenDeCompra } from '../dominio/cu102-compras'
import { textosContabilidad } from '../textos'

const COLUMNAS: ColumnaVirtual<OrdenDeCompra>[] = [
  { clave: 'numero', titulo: 'Número', ordenable: true },
  { clave: 'terceroRazonSocial', titulo: 'Proveedor', ordenable: true },
  { clave: 'centroCostoNombre', titulo: 'Centro de costo' },
  { clave: 'descripcion', titulo: 'Descripción', ancho: '2' },
  { clave: 'monto', titulo: 'Monto', numerica: true, ordenable: true },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'ordenCompraId', titulo: 'Facturable' },
]

/**
 * CU-102 · Órdenes de compra, sobre `TablaDeDatosVirtualizada` (la del shell). El filtro
 * por estado vive en la `querystring` vía `BarraDeFiltros`, así que el enlace de una
 * bandeja filtrada se puede pegar en un expediente.
 *
 * Una orden en `BORRADOR` no se puede facturar: la columna lo dice con el motivo escrito,
 * en vez de dejar que el `AP-CU102-03` del backend sea la primera explicación.
 */
@Component({
  selector: 'ap-tabla-de-ordenes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, ChipEstado, Monto, TablaDeDatosVirtualizada],
  template: `
    <section aria-labelledby="ordenes">
      <h2 id="ordenes">{{ t.ordenes }}</h2>
      <ap-barra-de-filtros etiqueta="Estado de la orden" [incluirBusqueda]="false" [definiciones]="filtros" />
      <ap-tabla-de-datos-virtualizada
        [titulo]="t.ordenes"
        [columnas]="COLUMNAS"
        [cargador]="cargador"
        [ordenPermitido]="['numero', 'terceroRazonSocial', 'monto']"
        [identidad]="identidad"
        [filtros]="filtrosActivos()"
      >
        <ng-template #celda let-o let-columna="columna">
          @switch (columna.clave) {
            @case ('monto') {
              <ap-monto [monto]="o.monto.monto" [moneda]="o.monto.moneda" etiqueta="Monto de la orden" />
            }
            @case ('estado') {
              <ap-chip-estado [tono]="o.estado === 'CANCELADA' ? 'error' : o.estado === 'BORRADOR' ? 'neutro' : 'ok'">{{ etiqueta(o) }}</ap-chip-estado>
            }
            @case ('ordenCompraId') {
              @if (facturable(o)) {
                <span class="si">Sí</span>
              } @else {
                <span class="motivo">{{ motivo(o) }}</span>
              }
            }
            @default {
              {{ o[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </section>
  `,
  styles: `
    section { display: grid; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.05em; }
    .motivo { color: var(--text-2); white-space: normal; }
    .si { color: var(--ok-texto); }
  `,
})
export class TablaDeOrdenes {
  protected readonly t = textosContabilidad.compras
  protected readonly COLUMNAS = COLUMNAS
  protected readonly cargador = cargadorDeOrdenes()
  protected readonly identidad = (o: OrdenDeCompra) => o.ordenCompraId
  protected readonly facturable = puedeFacturarse
  protected readonly motivo = motivoParaNoFacturar
  protected readonly etiqueta = (o: OrdenDeCompra) => this.t.estadosOrden[o.estado]
  protected readonly filtros: Filtro[] = [
    { valor: 'BORRADOR', texto: 'Borrador' },
    { valor: 'APROBADA', texto: 'Aprobada' },
    { valor: 'RECIBIDA', texto: 'Recibida' },
    { valor: 'CANCELADA', texto: 'Cancelada' },
  ]
  private readonly ruta = inject(ActivatedRoute)
  private readonly queryParamMap = toSignal(this.ruta.queryParamMap, { requireSync: false })
  protected readonly filtrosActivos = computed<Record<string, string>>(() => {
    const elegido = this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean)[0]
    const filtros: Record<string, string> = {}
    if (elegido) filtros['estado'] = elegido
    return filtros
  })
}
