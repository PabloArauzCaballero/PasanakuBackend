import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Monto } from '@aportaya/ui/monto/monto'
import { textosCustodia } from './textos'

export interface DineroVm {
  monto: string
  moneda: string
}

/**
 * View Model desacoplado de la API: cualquier entidad que tenga estos campos
 * (sea `SalidaConciliacionCustodia` o `ConciliacionEnLista`) puede mostrarse con este recibo.
 */
export interface ReciboConciliacionVm {
  conciliacionId: string
  saldoEmitido: DineroVm
  ratioCobertura: string
  cumpleEncaje: boolean
  diferencia: DineroVm
  estado: string
}

/**
 * Presentacional puro (Dumb): muestra el recibo/comprobante de una conciliación.
 * Sin dependencias de red ni servicios; reusable tanto al confirmar una nueva
 * conciliación como al inspeccionar el historial.
 */
@Component({
  selector: 'ap-recibo-de-conciliacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Monto],
  template: `
    <ap-alerta [tono]="recibo().estado === 'CUADRADA' ? 'ok' : 'aviso'" [titulo]="titulo()">
      <dl class="recibo">
        <dt>{{ t.formulario.reciboId }}</dt>
        <dd class="id">{{ recibo().conciliacionId }}</dd>

        <dt>{{ t.columnas.estado }}</dt>
        <dd>{{ estadoTexto() }}</dd>

        <dt>{{ t.columnas.saldoEmitido }}</dt>
        <dd><ap-monto [monto]="recibo().saldoEmitido.monto" [moneda]="recibo().saldoEmitido.moneda" /></dd>

        <dt>{{ t.columnas.diferencia }}</dt>
        <dd>
          <strong>{{ recibo().estado === 'CUADRADA' ? t.cuadra : t.descuadre }}:</strong>
          <ap-monto [monto]="recibo().diferencia.monto" [moneda]="recibo().diferencia.moneda" />
        </dd>

        <dt>{{ t.columnas.ratioCobertura }}</dt>
        <dd>{{ recibo().ratioCobertura }} · {{ recibo().cumpleEncaje ? t.cumpleEncaje : t.noCumpleEncaje }}</dd>
      </dl>
    </ap-alerta>
    @if (permitirOtro()) {
      <div class="acciones"><ap-boton icono="mas" (pulsado)="otra.emit()">{{ t.formulario.otra }}</ap-boton></div>
    }
  `,
  styles: `
    :host { display: grid; gap: var(--s3); }
    .recibo { display: grid; grid-template-columns: auto 1fr; gap: var(--s1) var(--s3); margin: 0; }
    dd { margin: 0; }
    .id { overflow-wrap: anywhere; }
    .acciones { margin-top: var(--s1); }
  `,
})
export class ReciboDeConciliacion {
  readonly recibo = input.required<ReciboConciliacionVm>()
  readonly titulo = input<string>(textosCustodia.formulario.recibo)
  readonly permitirOtro = input<boolean>(true)
  readonly otra = output<void>()

  protected readonly t = textosCustodia

  protected estadoTexto(): string {
    const estado = this.recibo().estado
    return this.t.estados[estado as keyof typeof this.t.estados] ?? estado
  }
}
