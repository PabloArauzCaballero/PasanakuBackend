import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { formatearDia } from '@aportaya/ui/fecha/fecha'
import { textosCierres } from './textos'

/**
 * Presentacional puro (Dumb): modal de confirmación previa al cierre diario.
 */
@Component({
  selector: 'ap-confirmacion-de-cierre',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo],
  template: `
    @if (pendiente(); as p) {
      <ap-dialogo
        [titulo]="t.formulario.titulo"
        [textoDeConfirmar]="t.formulario.confirmar + ' ' + dia(p)"
        [abierto]="true"
        [cargando]="cargando()"
        (confirmar)="confirmar.emit(p)"
        (cancelar)="cancelar.emit()"
      >
        <p>{{ t.formulario.consecuencia(dia(p)) }}</p>
      </ap-dialogo>
    }
  `,
})
export class ConfirmacionDeCierre {
  readonly pendiente = input<string | null>(null)
  readonly cargando = input<boolean>(false)

  readonly confirmar = output<string>()
  readonly cancelar = output<void>()

  protected readonly t = textosCierres
  protected readonly dia = formatearDia
}
