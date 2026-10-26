import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { AccionesDeFila } from '@aportaya/ui/acciones-de-fila/acciones-de-fila'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Icono } from '@aportaya/ui/icono/icono'
import { Monto } from '@aportaya/ui/monto/monto'
import type { Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { Sesion } from '../../../nucleo/sesion'
import { textoDeImporte } from '../dominio/contrato-erp'
import { cargadorDeFacturas, motivoParaNoPagar, vencida, type FacturaDeProveedor } from '../dominio/cu103-facturas'
import { crearListadoPorCursor, filtroInicialDeLaUrl, opcionesDe } from '../listado/listado-por-cursor'
import { TablaPorCursor } from '../listado/tabla-por-cursor'
import { FichaDeFactura } from './ficha-de-factura'
import { textosContabilidad } from '../textos'

const t = textosContabilidad.compras
const hoy = () => new Date().toISOString()

const COLUMNAS: Columna<FacturaDeProveedor>[] = [
  { clave: 'numeroFactura', titulo: 'Número', mono: true },
  { clave: 'terceroRazonSocial', titulo: 'Proveedor' },
  { clave: 'fechaVencimiento', titulo: 'Vence' },
  { clave: 'monto', titulo: 'Monto', numerica: true },
  { clave: 'saldoPendiente', titulo: 'Saldo pendiente', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
]

const EXPORTABLES = [
  { clave: 'numeroFactura', titulo: 'Número' },
  { clave: 'proveedor', titulo: 'Proveedor', valor: (f: FacturaDeProveedor) => f.terceroRazonSocial },
  { clave: 'fechaEmision', titulo: 'Emitida' },
  { clave: 'fechaVencimiento', titulo: 'Vence' },
  { clave: 'monto', titulo: 'Monto', valor: (f: FacturaDeProveedor) => textoDeImporte(f.monto) },
  { clave: 'pagado', titulo: 'Pagado', valor: (f: FacturaDeProveedor) => textoDeImporte(f.montoPagado) },
  { clave: 'saldoPendiente', titulo: 'Saldo pendiente', valor: (f: FacturaDeProveedor) => textoDeImporte(f.saldoPendiente) },
  { clave: 'estado', titulo: 'Estado', valor: (f: FacturaDeProveedor) => t.estadosFactura[f.estado] },
]

/**
 * CU-103 · Cuentas por pagar, listado por cursor del servidor (de la emisión más reciente a la más antigua). El estado
 * viaja como `estados`; el buscador, el proveedor y «vencida» filtran la página cargada.
 *
 * **Registrar ≠ pagar.** La acción de fila «Registrar el pago» aparece solo si la sesión tiene `CONTABILIDAD_ERP_PAGAR`
 * y **no** fue esta cuenta la que aprobó la factura (`R-CTB-05`); si no, queda el motivo escrito. Una factura emitida
 * no se edita ni se borra (append-only, regla 91.1.3): no hay Editar ni Eliminar. Tras el pago, la lista se recarga.
 */
@Component({
  selector: 'ap-tabla-de-facturas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AccionesDeFila, Alerta, ChipEstado, Fecha, Icono, Monto, TablaPorCursor, FichaDeFactura],
  template: `
    @if (pagada(); as numero) { <ap-alerta tono="ok" titulo="Pago registrado">El pago de la factura {{ numero }} quedó registrado. La lista ya muestra el saldo nuevo.</ap-alerta> }
    <ap-tabla-por-cursor [titulo]="t.facturas" [columnas]="COLUMNAS" [listado]="listado" [identidad]="identidad" [exportables]="EXPORTABLES" nombreDeRegistros="facturas" [textoVacio]="t.enCeroFacturas" ayudaDeBusqueda="Busca por número o proveedor en la página cargada: el servidor no busca por texto.">
      <ng-template #celda let-f let-columna="columna">
        @switch (columna.clave) {
          @case ('fechaVencimiento') {
            <span class="vence"><ap-fecha [iso]="f.fechaVencimiento" [conHora]="false" />@if (esVencida(f)) { <ap-chip-estado tono="error" icono="reloj">Vencida</ap-chip-estado> }</span>
          }
          @case ('monto') { <ap-monto [monto]="f.monto.monto" [moneda]="f.monto.moneda" etiqueta="Monto de la factura" /> }
          @case ('saldoPendiente') { <ap-monto [monto]="f.saldoPendiente.monto" [moneda]="f.saldoPendiente.moneda" [etiqueta]="t.saldoPendiente" /> }
          @case ('estado') { <ap-chip-estado [tono]="f.estado === 'PAGADA' ? 'ok' : f.estado === 'ANULADA' ? 'error' : 'aviso'">{{ etiqueta(f) }}</ap-chip-estado> }
          @default { <span class="texto">{{ f[columna.clave] }}</span> }
        }
      </ng-template>
      <ng-template #acciones let-f>
        @if (motivo(f); as porQueNo) {
          <span class="motivo">{{ porQueNo }}</span>
        } @else {
          <ap-acciones-de-fila [fila]="'Factura ' + f.numeroFactura">
            <button type="button" apAccionDeFila [attr.aria-label]="t.pagar + ' de la factura ' + f.numeroFactura" (click)="elegida.set(f)"><ap-icono nombre="billetera" tamano="s4" /><span>{{ t.pagar }}</span></button>
          </ap-acciones-de-fila>
        }
      </ng-template>
    </ap-tabla-por-cursor>

    @if (elegida(); as f) {
      <ap-ficha-de-factura [factura]="f" (pagada)="alPagar(f)" (cerrada)="elegida.set(null)" />
    }
  `,
  styles: `
    /* Los textos largos se parten en la celda: así la tabla entra en su marco y la columna fija de acciones no tapa un importe. */
    .texto { display: inline-block; min-width: 8rem; white-space: normal; overflow-wrap: anywhere; }
    :host { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .vence { display: inline-flex; flex-wrap: wrap; align-items: center; gap: var(--s2); }
    .motivo { display: block; max-width: 13rem; color: var(--text-2); white-space: normal; font: var(--t-cuerpo-chico); }
  `,
})
export class TablaDeFacturas {
  protected readonly t = t
  protected readonly COLUMNAS = COLUMNAS
  protected readonly EXPORTABLES = EXPORTABLES
  protected readonly identidad = (f: FacturaDeProveedor) => f.facturaProveedorId
  protected readonly etiqueta = (f: FacturaDeProveedor) => t.estadosFactura[f.estado]
  protected readonly esVencida = (f: FacturaDeProveedor) => vencida(f, hoy())
  protected readonly elegida = signal<FacturaDeProveedor | null>(null)
  protected readonly pagada = signal<string | null>(null)
  private readonly sesion = inject(Sesion)
  protected readonly puedePagar = computed(() => this.sesion.puede('CONTABILIDAD_ERP_PAGAR'))
  protected readonly motivo = (f: FacturaDeProveedor): string | null => motivoParaNoPagar(f, this.puedePagar())
  protected readonly listado = crearListadoPorCursor<FacturaDeProveedor>({
    cargador: cargadorDeFacturas(),
    camposDeBusqueda: ['numeroFactura', 'terceroRazonSocial'],
    servidor: { nombre: 'estado', etiqueta: 'Estado de la factura', opciones: opcionesDe(t.estadosFactura), inicial: filtroInicialDeLaUrl(opcionesDe(t.estadosFactura)) },
    filtros: [
      { nombre: 'proveedor', etiqueta: 'Proveedor', valor: (f) => f.terceroRazonSocial },
      { nombre: 'vencimiento', etiqueta: 'Vencimiento', valor: (f) => (vencida(f, hoy()) ? 'VENCIDA' : 'AL_DIA'), opciones: [{ valor: 'VENCIDA', texto: 'Vencida con saldo' }, { valor: 'AL_DIA', texto: 'Al día' }] },
    ],
    textosDeError: textosContabilidad.erroresDeListado,
  })

  protected alPagar(f: FacturaDeProveedor): void {
    this.pagada.set(f.numeroFactura)
    this.listado.recargar()
  }
}
