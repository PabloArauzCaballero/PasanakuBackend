import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { DecisionDeVerificacionDecisionEnum } from 'clientes/angular/identidad'
import { textosCumplimiento } from '../textos'

export type Decision = { decision: DecisionDeVerificacionDecisionEnum; motivo?: string }

/**
 * Motivo, error y botones de UN expediente. El motivo vive en esta instancia: con un
 * solo `signal` para toda la cola, lo escrito en una tarjeta aparecía en todas y
 * habilitaba «Rechazar» en cualquiera — un rechazo podía salir con el motivo pensado
 * para otra persona.
 */
@Component({
  selector: 'ap-decision-de-expediente',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Campo],
  template: `
    @if (!completo()) {
      <p class="aviso">{{ t.expedienteIncompleto }}</p>
    }
    <ap-campo [etiqueta]="t.motivo" [ayuda]="t.motivoAyuda" [(valor)]="motivo" [id]="'motivo-' + verificacionId()" />
    @if (error(); as e) {
      <ap-alerta tono="error">{{ e }}</ap-alerta>
    }
    <div class="acciones">
      <ap-boton
        variante="primario"
        [deshabilitado]="!completo() || ocupado()"
        [cargando]="enCurso() === decisiones.Aprobar"
        (pulsado)="decidir.emit({ decision: decisiones.Aprobar })"
      >
        {{ t.aprobar }}
      </ap-boton>
      <ap-boton
        variante="peligro"
        [deshabilitado]="!motivo().trim() || ocupado()"
        [cargando]="enCurso() === decisiones.Rechazar"
        (pulsado)="decidir.emit({ decision: decisiones.Rechazar, motivo: motivo().trim() })"
      >
        {{ t.rechazar }}
      </ap-boton>
    </div>
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: var(--s3); }
    .aviso { margin: 0; color: var(--aviso-texto); font-size: .875rem; }
    .acciones { display: flex; gap: var(--s2); flex-wrap: wrap; }
  `,
})
export class DecisionDeExpediente {
  protected readonly t = textosCumplimiento.verificaciones
  protected readonly decisiones = DecisionDeVerificacionDecisionEnum

  readonly verificacionId = input.required<string>()
  /** Sin las cinco fotos no se aprueba: aprobar a ciegas es no revisar. */
  readonly completo = input.required<boolean>()
  /** Hay una decisión en vuelo (de este u otro expediente): no se dispara otra. */
  readonly ocupado = input(false)
  /** La decisión de ESTE expediente que está en vuelo, para mostrar su botón cargando. */
  readonly enCurso = input<DecisionDeVerificacionDecisionEnum | null>(null)
  /** Si la última decisión falló, se dice acá — callarlo dejaba al operador creyendo que decidió. */
  readonly error = input<string>()
  /** El motivo viaja solo con el rechazo: un aprobado no lleva «motivo de rechazo». */
  readonly decidir = output<Decision>()

  protected readonly motivo = signal('')
}
