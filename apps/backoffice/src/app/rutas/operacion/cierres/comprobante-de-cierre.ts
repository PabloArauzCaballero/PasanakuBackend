import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { textosCierres } from './textos'

export interface ComprobanteCierreVm {
  cierreId: string
  fecha: string
  cuadrado: boolean
  yaExistia?: boolean
  excepcionesAbiertas: number
  motivoDelDescuadre?: string | null
}

/**
 * Presentacional puro (Dumb): muestra el comprobante de un cierre diario.
 * Desacoplado de la red, reutilizable para confirmaciones o para inspección histórica.
 */
@Component({
  selector: 'ap-comprobante-de-cierre',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton],
  template: `
    <ap-alerta [tono]="comprobante().cuadrado ? 'ok' : 'aviso'" [titulo]="t.formulario.comprobante">
      @if (comprobante().yaExistia) {
        <p>{{ t.formulario.yaExistia }}</p>
      }
      <dl class="comprobante">
        <dt>{{ t.formulario.cierreId }}</dt>
        <dd class="id">{{ comprobante().cierreId }}</dd>

        <dt>{{ t.columnas.fecha }}</dt>
        <dd>{{ dia(comprobante().fecha) }}</dd>

        <dt>{{ t.columnas.cuadrado }}</dt>
        <dd>{{ comprobante().cuadrado ? t.cuadrado : t.conObservaciones }}</dd>

        <dt>{{ t.formulario.excepcionesAbiertas }}</dt>
        <dd>{{ comprobante().excepcionesAbiertas }}</dd>

        @if (comprobante().motivoDelDescuadre) {
          <dt>{{ t.formulario.motivo }}</dt>
          <dd>{{ comprobante().motivoDelDescuadre }}</dd>
        }
      </dl>
    </ap-alerta>
    @if (permitirOtro()) {
      <div class="acciones">
        <ap-boton (pulsado)="otro.emit()">{{ t.formulario.otro }}</ap-boton>
      </div>
    }
  `,
  styles: `
    :host { display: grid; gap: var(--s3); }
    .comprobante { display: grid; grid-template-columns: auto 1fr; gap: var(--s1) var(--s3); margin: 0; }
    dd { margin: 0; }
    .id { overflow-wrap: anywhere; }
    .acciones { margin-top: var(--s1); }
  `,
})
export class ComprobanteDeCierre {
  readonly comprobante = input.required<ComprobanteCierreVm>()
  readonly permitirOtro = input<boolean>(true)
  readonly otro = output<void>()

  protected readonly t = textosCierres
  protected readonly dia = formatearDia
}
