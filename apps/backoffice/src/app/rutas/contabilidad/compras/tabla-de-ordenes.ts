import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { DialogoDeConfirmacion } from '@aportaya/ui/dialogo-de-confirmacion/dialogo-de-confirmacion'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { Sesion } from '../../../nucleo/sesion'
import { textoDeImporte } from '../dominio/contrato-erp'
import { cargadorDeOrdenes, crearAprobacionDeOrden, motivoParaNoFacturar, puedeAprobarse, puedeFacturarse, type OrdenDeCompra } from '../dominio/cu102-compras'
import { crearListadoPorCursor, filtroInicialDeLaUrl, opcionesDe } from '../listado/listado-por-cursor'
import { TablaPorCursor } from '../listado/tabla-por-cursor'
import { textosContabilidad } from '../textos'

const t = textosContabilidad.compras
const centro = (o: OrdenDeCompra) => o.centroCostoNombre ?? t.sinCentroDeCosto

const COLUMNAS: Columna<OrdenDeCompra>[] = [
  { clave: 'numero', titulo: 'Número', mono: true },
  { clave: 'terceroRazonSocial', titulo: 'Proveedor' },
  { clave: 'centroCostoNombre', titulo: 'Centro de costo' },
  { clave: 'creadaEn', titulo: 'Creada' },
  { clave: 'monto', titulo: 'Monto', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'ordenCompraId', titulo: 'Facturable' },
]

const EXPORTABLES = [
  { clave: 'numero', titulo: 'Número' },
  { clave: 'proveedor', titulo: 'Proveedor', valor: (o: OrdenDeCompra) => o.terceroRazonSocial },
  { clave: 'centroCosto', titulo: 'Centro de costo', valor: centro },
  { clave: 'creadaEn', titulo: 'Creada' },
  { clave: 'monto', titulo: 'Monto', valor: (o: OrdenDeCompra) => textoDeImporte(o.monto) },
  { clave: 'estado', titulo: 'Estado', valor: (o: OrdenDeCompra) => t.estadosOrden[o.estado] },
]

/**
 * CU-102 · Órdenes de compra, listado por cursor del servidor (de la más reciente a la más antigua). El estado viaja
 * como `estados`; el buscador y el centro de costo filtran la página cargada.
 *
 * **Acción de fila: «Aprobar»**, solo sobre una orden en `BORRADOR` y con `CONTABILIDAD_ERP_COMPRAS`
 * (`aprobarOrdenDeCompra`, con Idempotency-Key). No hay Editar ni Eliminar: `erp.yaml` no los tiene. Una orden que no
 * se puede facturar lo dice con el motivo escrito, en vez de dejar que el `AP-CU102-03` sea la primera explicación.
 */
@Component({
  selector: 'ap-tabla-de-ordenes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Alerta, ChipEstado, DialogoDeConfirmacion, ErrorDeEnvio, Fecha, Icono, Monto, TablaPorCursor],
  template: `
    @if (aprobada(); as numero) { <ap-alerta tono="ok" titulo="Orden aprobada">La orden {{ numero }} quedó aprobada: ya se puede facturar contra ella.</ap-alerta> }
    <ap-error-de-envio [error]="errorDeEnvio()" />
    <ap-tabla-por-cursor [titulo]="t.ordenes" [columnas]="COLUMNAS" [listado]="listado" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="órdenes" [textoVacio]="t.enCeroOrdenes" ayudaDeBusqueda="Busca por número, proveedor o centro de costo en la página cargada: el servidor no busca por texto.">
      <ng-template #celda let-o let-columna="columna">
        @switch (columna.clave) {
          @case ('centroCostoNombre') { {{ centro(o) }} }
          @case ('creadaEn') { <ap-fecha [iso]="o.creadaEn" [conHora]="false" /> }
          @case ('monto') { <ap-monto [monto]="o.monto.monto" [moneda]="o.monto.moneda" etiqueta="Monto de la orden" /> }
          @case ('estado') { <ap-chip-estado [tono]="o.estado === 'CANCELADA' ? 'error' : o.estado === 'BORRADOR' ? 'neutro' : 'ok'">{{ etiqueta(o) }}</ap-chip-estado> }
          @case ('ordenCompraId') {
            @if (facturable(o)) { <span class="si">Sí</span> } @else { <span class="motivo">{{ motivo(o) }}</span> }
          }
          @default { {{ o[columna.clave] }} }
        }
      </ng-template>
      <ng-template #acciones let-o>
        @if (aprobable(o)) {
          <ap-acciones-de-fila [fila]="'Orden ' + o.numero">
            <button type="button" apAccionDeFila [disabled]="enviando() === o.ordenCompraId" [attr.aria-label]="'Aprobar la orden ' + o.numero" (click)="preguntar(o)"><ap-icono nombre="aprobar" tamano="s4" /><span>Aprobar</span></button>
          </ap-acciones-de-fila>
        } @else { <span class="motivo">Sin acciones</span> }
      </ng-template>
    </ap-tabla-por-cursor>
    @if (elegida(); as o) {
      <ap-dialogo-de-confirmacion [titulo]="'Aprobar la orden ' + o.numero" descripcion="Aprobar habilita facturar contra esta orden. No se deshace desde acá: una orden aprobada solo se cancela por su propio trámite." textoDeConfirmar="Aprobar la orden" [cargando]="enviando() !== null" [(abierto)]="abierto" (confirmar)="aprobar(o)" (cancelar)="elegida.set(null)" />
    }
  `,
  styles: `
    :host { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .motivo { color: var(--text-2); white-space: normal; font: var(--t-cuerpo-chico); }
    .si { color: var(--ok-texto); }
  `,
})
export class TablaDeOrdenes {
  protected readonly t = t
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly centro = centro
  protected readonly identidad = (o: OrdenDeCompra) => o.ordenCompraId
  protected readonly facturable = puedeFacturarse
  protected readonly motivo = motivoParaNoFacturar
  protected readonly etiqueta = (o: OrdenDeCompra) => t.estadosOrden[o.estado]
  private readonly sesion = inject(Sesion)
  private readonly enviarAprobacion = crearAprobacionDeOrden()
  protected readonly puedeAprobar = computed(() => this.sesion.puede('CONTABILIDAD_ERP_COMPRAS'))
  protected readonly aprobable = (o: OrdenDeCompra) => this.puedeAprobar() && puedeAprobarse(o)
  protected readonly elegida = signal<OrdenDeCompra | null>(null)
  protected readonly abierto = signal(false)
  protected readonly enviando = signal<string | null>(null)
  protected readonly aprobada = signal<string | null>(null)
  protected readonly errorDeEnvio = signal<ErrorTraducido | null>(null)
  protected readonly listado = crearListadoPorCursor<OrdenDeCompra>({
    cargador: cargadorDeOrdenes(),
    camposDeBusqueda: ['numero', 'terceroRazonSocial', centro],
    servidor: { nombre: 'estado', etiqueta: 'Estado de la orden', opciones: opcionesDe(t.estadosOrden), inicial: filtroInicialDeLaUrl(opcionesDe(t.estadosOrden)) },
    filtros: [{ nombre: 'centro', etiqueta: 'Centro de costo', valor: centro }],
    textosDeError: textosContabilidad.erroresDeListado,
  })

  protected preguntar(o: OrdenDeCompra): void {
    this.elegida.set(o)
    this.abierto.set(true)
  }

  protected aprobar(o: OrdenDeCompra): void {
    if (this.enviando()) return
    this.enviando.set(o.ordenCompraId)
    this.errorDeEnvio.set(null)
    this.aprobada.set(null)
    this.enviarAprobacion(o.ordenCompraId).subscribe({
      next: () => {
        this.enviando.set(null)
        this.abierto.set(false)
        this.elegida.set(null)
        this.aprobada.set(o.numero)
        this.listado.recargar()
      },
      error: (e: ErrorTraducido) => {
        this.enviando.set(null)
        this.abierto.set(false)
        this.elegida.set(null)
        this.errorDeEnvio.set(e)
      },
    })
  }
}
