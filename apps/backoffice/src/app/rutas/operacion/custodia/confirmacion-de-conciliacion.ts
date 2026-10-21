import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { Monto } from '@aportaya/ui/monto/monto'
import { textosCustodia } from './textos'

export interface ConfirmacionConciliacionVm {
  cuentaCustodiaId: string
  fecha: string
  saldoCustodia: { monto: string; moneda: string }
  saldoEnTransito: { monto: string; moneda: string }
}

/**
 * Presentacional puro (Dumb): diálogo modal que detalla la consecuencia exacta
 * y los saldos antes de registrar la conciliación.
 */
@Component({
  selector: 'ap-confirmacion-de-conciliacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo, Monto],
  template: `
    @if (pendiente(); as p) {
      <ap-dialogo
        [titulo]="t.formulario.titulo"
        [textoDeConfirmar]="t.formulario.confirmar"
        [abierto]="true"
        [cargando]="cargando()"
        (confirmar)="confirmar.emit(p)"
        (cancelar)="cancelar.emit()"
      >
        <p>{{ t.formulario.consecuencia(p.cuentaCustodiaId, dia(p.fecha)) }}</p>
        <p>
          {{ t.formulario.extracto }}:
          <ap-monto [monto]="p.saldoCustodia.monto" [moneda]="p.saldoCustodia.moneda" />
          · {{ t.formulario.transito }}:
          <ap-monto [monto]="p.saldoEnTransito.monto" [moneda]="p.saldoEnTransito.moneda" />
        </p>
      </ap-dialogo>
    }
  `,
})
export class ConfirmacionDeConciliacion {
  readonly pendiente = input<ConfirmacionConciliacionVm | null>(null)
  readonly cargando = input<boolean>(false)

  readonly confirmar = output<ConfirmacionConciliacionVm>()
  readonly cancelar = output<void>()

  protected readonly t = textosCustodia
  protected readonly dia = formatearDia
}
