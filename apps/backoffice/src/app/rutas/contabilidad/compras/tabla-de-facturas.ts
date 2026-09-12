import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { Monto } from '@aportaya/ui/monto/monto'
import { Sesion } from '../../../nucleo/sesion'
import { TablaDeDatosVirtualizada } from '../../../nucleo/tabla/tabla-de-datos-virtualizada'
import type { ColumnaVirtual } from '../../../nucleo/tabla/tipos'
import { cargadorDeFacturas, motivoParaNoPagar, type FacturaDeProveedor } from '../dominio/cu103-facturas'
import { FichaDeFactura } from './ficha-de-factura'
import { textosContabilidad } from '../textos'

const COLUMNAS: ColumnaVirtual<FacturaDeProveedor>[] = [
  { clave: 'numeroFactura', titulo: 'Número', ordenable: true },
  { clave: 'terceroRazonSocial', titulo: 'Proveedor', ordenable: true },
  { clave: 'fechaVencimiento', titulo: 'Vence', ordenable: true },
  { clave: 'monto', titulo: 'Monto', numerica: true, ordenable: true },
  { clave: 'saldoPendiente', titulo: 'Saldo pendiente', numerica: true },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'facturaProveedorId', titulo: 'Pago' },
]

/**
 * CU-103 · Cuentas por pagar, sobre `TablaDeDatosVirtualizada` (la del shell), ordenada
 * por vencimiento desde el `cargador`: la que vence antes va primero.
 *
 * **Registrar ≠ pagar.** El botón de pago aparece solo si la sesión tiene
 * `CONTABILIDAD_ERP_PAGAR` y **no** fue esta cuenta la que aprobó la factura
 * (`R-CTB-05`). Cuando no, en su lugar queda el motivo escrito: un operador nunca ve los
 * dos botones del control de cuatro ojos, ni un botón mudo sin explicación.
 */
@Component({
  selector: 'ap-tabla-de-facturas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, ChipEstado, Fecha, Monto, TablaDeDatosVirtualizada, FichaDeFactura],
  template: `
    <section aria-labelledby="facturas">
      <h2 id="facturas">{{ t.facturas }}</h2>
      <ap-tabla-de-datos-virtualizada
        [titulo]="t.facturas"
        [columnas]="COLUMNAS"
        [cargador]="cargador"
        [ordenPermitido]="['numeroFactura', 'terceroRazonSocial', 'fechaVencimiento', 'monto']"
        [identidad]="identidad"
      >
        <ng-template #celda let-f let-columna="columna">
          @switch (columna.clave) {
            @case ('fechaVencimiento') {
              <ap-fecha [iso]="f.fechaVencimiento" [conHora]="false" />
            }
            @case ('monto') {
              <ap-monto [monto]="f.monto.monto" [moneda]="f.monto.moneda" etiqueta="Monto de la factura" />
            }
            @case ('saldoPendiente') {
              <ap-monto [monto]="f.saldoPendiente.monto" [moneda]="f.saldoPendiente.moneda" [etiqueta]="t.saldoPendiente" />
            }
            @case ('estado') {
              <ap-chip-estado [tono]="f.estado === 'PAGADA' ? 'ok' : f.estado === 'ANULADA' ? 'error' : 'aviso'">{{ etiqueta(f) }}</ap-chip-estado>
            }
            @case ('facturaProveedorId') {
              @if (motivo(f); as porQueNo) {
                <span class="motivo">{{ porQueNo }}</span>
              } @else {
                <ap-boton (pulsado)="elegir(f)">{{ t.pagar }}</ap-boton>
              }
            }
            @default {
              {{ f[columna.clave] }}
            }
          }
        </ng-template>
      </ap-tabla-de-datos-virtualizada>
    </section>

    @if (elegida(); as f) {
      <ap-ficha-de-factura [factura]="f" (cerrada)="elegida.set(null)" />
    }
  `,
  styles: `
    section { display: grid; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.05em; }
    .motivo { color: var(--text-2); white-space: normal; }
  `,
})
export class TablaDeFacturas {
  protected readonly t = textosContabilidad.compras
  protected readonly COLUMNAS = COLUMNAS
  protected readonly cargador = cargadorDeFacturas()
  protected readonly identidad = (f: FacturaDeProveedor) => f.facturaProveedorId
  protected readonly etiqueta = (f: FacturaDeProveedor) => this.t.estadosFactura[f.estado]
  protected readonly elegida = signal<FacturaDeProveedor | null>(null)
  private readonly sesion = inject(Sesion)
  protected readonly puedePagar = computed(() => this.sesion.puede('CONTABILIDAD_ERP_PAGAR'))

  protected motivo = (f: FacturaDeProveedor): string | null => motivoParaNoPagar(f, this.puedePagar())

  protected elegir(f: FacturaDeProveedor): void {
    this.elegida.set(f)
  }
}
