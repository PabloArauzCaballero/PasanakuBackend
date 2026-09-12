import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import { CampoMonto } from '@aportaya/ui/campo-monto/campo-monto'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { GrupoRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import { Monto } from '@aportaya/ui/monto/monto'
import { crearPagoDeFactura, pagoExcedeElSaldo, type FacturaDeProveedor, type FormaDePago } from '../dominio/cu103-facturas'
import { textosContabilidad } from '../textos'

/**
 * `FichaFactura` de CU-103: el paso de Tesorería. Confirma el pago con el número de la
 * factura delante, porque el pago es append-only —corregirlo exige un movimiento
 * inverso, no un `UPDATE`— y porque `Dialogo` es el patrón de doble confirmación del
 * backoffice.
 *
 * El monto no puede exceder el saldo pendiente (`AP-CU103-04`): la ficha lo bloquea
 * antes de pedirlo. El importe se escribe con `CampoMonto` y se muestra con `Monto`:
 * este archivo no formatea dinero.
 */
@Component({
  selector: 'ap-ficha-de-factura',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CampoMonto, Dialogo, GrupoRadio, Monto],
  template: `
    <ap-dialogo
      [titulo]="t.pagar + ' · ' + factura().numeroFactura"
      [textoDeConfirmar]="t.pagar + ' de ' + factura().numeroFactura"
      [cargando]="enviando()"
      [abierto]="abierto()"
      (abiertoChange)="cambiarApertura($event)"
      (confirmar)="confirmar()"
      (cancelar)="cambiarApertura(false)"
    >
      <p>{{ t.confirmarPago(factura().numeroFactura) }}</p>
      <p class="saldo">
        {{ t.saldoPendiente }}:
        <ap-monto [monto]="factura().saldoPendiente.monto" [moneda]="factura().saldoPendiente.moneda" [etiqueta]="t.saldoPendiente" />
      </p>
      <ap-campo-monto [etiqueta]="'Monto a pagar'" [(valor)]="monto" [error]="errorDeMonto()" />
      <ap-grupo-radio nombre="forma-de-pago" etiqueta="Forma de pago" [opciones]="formas" [(elegido)]="forma" />
    </ap-dialogo>
  `,
  styles: `
    .saldo { display: flex; align-items: center; gap: var(--s2); color: var(--text-2); }
  `,
})
export class FichaDeFactura {
  protected readonly t = textosContabilidad.compras
  readonly factura = input.required<FacturaDeProveedor>()
  readonly cerrada = output<void>()
  protected readonly abierto = signal(true)
  protected readonly enviando = signal(false)
  protected readonly monto = signal('')
  protected readonly forma = signal<string>('TRANSFERENCIA')
  protected readonly formas = [
    { valor: 'TRANSFERENCIA', texto: 'Transferencia' },
    { valor: 'CHEQUE', texto: 'Cheque' },
    { valor: 'EFECTIVO', texto: 'Efectivo' },
    { valor: 'QR', texto: 'QR' },
  ]
  private readonly enviarPago = crearPagoDeFactura()

  protected readonly errorDeMonto = computed(() =>
    this.monto() && pagoExcedeElSaldo(this.factura(), this.monto()) ? 'El pago no puede exceder el saldo pendiente de la factura.' : undefined,
  )

  protected cambiarApertura(abierto: boolean): void {
    this.abierto.set(abierto)
    if (!abierto) this.cerrada.emit()
  }

  protected confirmar(): void {
    if (this.errorDeMonto() || !this.monto()) return
    this.enviando.set(true)
    this.enviarPago(this.factura().facturaProveedorId, {
      monto: this.monto(),
      moneda: this.factura().saldoPendiente.moneda,
      formaPago: this.forma() as FormaDePago,
    }).subscribe({
      next: () => {
        this.enviando.set(false)
        this.cambiarApertura(false)
      },
      error: () => this.enviando.set(false),
    })
  }
}
