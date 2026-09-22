import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import { CampoMonto } from '@aportaya/ui/campo-monto/campo-monto'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { confirmarDescarteSiSucio } from '@aportaya/ui/dialogo/politica-de-descarte'
import { GrupoRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import { Monto } from '@aportaya/ui/monto/monto'
import { cobroExcedeElSaldo, crearCobro, type CuentaPorCobrar, type FormaDeCobro } from '../dominio/cu104-cobros'
import { textosContabilidad } from '../textos'

/**
 * CU-104 · El paso de Tesorería sobre una cuenta por cobrar. El excedente no se acredita
 * a ninguna otra cuenta (flujo alternativo de CU-104), así que un cobro mayor al saldo se
 * bloquea acá antes de pedirlo, y el `AP-CU104-02` del backend queda de respaldo.
 */
@Component({
  selector: 'ap-ficha-de-cobro',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CampoMonto, Dialogo, GrupoRadio, Monto],
  template: `
    <ap-dialogo
      [titulo]="t.cobrar + ' · ' + cuenta().origenTipo"
      [textoDeConfirmar]="t.cobrar + ' de ' + cuenta().terceroRazonSocial"
      [cargando]="enviando()"
      [abierto]="abierto()"
      [puedeDescartar]="puedeDescartar"
      (abiertoChange)="cambiarApertura($event)"
      (confirmar)="confirmar()"
      (cancelar)="cambiarApertura(false)"
    >
      <p>{{ t.confirmarCobro(cuenta().origenTipo) }}</p>
      <p class="saldo">
        {{ t.saldoPendiente }}:
        <ap-monto [monto]="cuenta().saldoPendiente.monto" [moneda]="cuenta().saldoPendiente.moneda" [etiqueta]="t.saldoPendiente" />
      </p>
      <ap-campo-monto [etiqueta]="'Monto cobrado'" [(valor)]="monto" [error]="errorDeMonto()" />
      <ap-grupo-radio nombre="forma-de-cobro" etiqueta="Forma de cobro" [opciones]="formas" [(elegido)]="forma" />
    </ap-dialogo>
  `,
  styles: `
    .saldo { display: flex; align-items: center; gap: var(--s2); color: var(--text-2); }
  `,
})
export class FichaDeCobro {
  protected readonly t = textosContabilidad.cobros
  readonly cuenta = input.required<CuentaPorCobrar>()
  readonly cerrada = output<void>()
  protected readonly abierto = signal(true)
  protected readonly enviando = signal(false)
  protected readonly monto = signal('')
  protected readonly forma = signal<string>('TRANSFERENCIA')
  protected readonly formas = [
    { valor: 'TRANSFERENCIA', texto: 'Transferencia' },
    { valor: 'QR', texto: 'QR' },
    { valor: 'TARJETA', texto: 'Tarjeta' },
    { valor: 'EFECTIVO', texto: 'Efectivo' },
  ]
  private readonly enviarCobro = crearCobro()

  /** Borrador sucio: se tocó el monto o se cambió la forma de cobro del valor inicial. */
  protected readonly sucio = computed(() => this.monto().trim() !== '' || this.forma() !== 'TRANSFERENCIA')
  protected readonly puedeDescartar = confirmarDescarteSiSucio(this.sucio)

  protected readonly errorDeMonto = computed(() =>
    this.monto() && cobroExcedeElSaldo(this.cuenta(), this.monto()) ? 'El cobro no puede exceder el saldo pendiente de la cuenta.' : undefined,
  )

  protected cambiarApertura(abierto: boolean): void {
    this.abierto.set(abierto)
    if (!abierto) this.cerrada.emit()
  }

  protected confirmar(): void {
    if (this.errorDeMonto() || !this.monto()) return
    this.enviando.set(true)
    this.enviarCobro(this.cuenta().cuentaPorCobrarId, { monto: this.monto(), formaCobro: this.forma() as FormaDeCobro }).subscribe({
      next: () => {
        this.enviando.set(false)
        this.cambiarApertura(false)
      },
      error: () => this.enviando.set(false),
    })
  }
}
