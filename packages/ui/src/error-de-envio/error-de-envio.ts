import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Alerta } from '../alerta/alerta'

/** Lo que la capa de red entrega ya traducido (mismo contorno que `ErrorTraducido` de las apps). */
export type ErrorParaVista = {
  readonly mensaje: string
  readonly trazaId?: string
  readonly estado?: number
  readonly sinConexion?: boolean
  readonly tiempoAgotado?: boolean
}

/**
 * El error de una operación que MUEVE algo (cobrar, pagar, cerrar, resolver). Distingue dos cosas que la persona
 * necesita saber distinto: un rechazo del servidor (no pasó, corregí y probá) y un «no sabemos» (sin conexión, plazo
 * vencido, 5xx: pudo haber pasado, no la des por fallida). En el segundo caso aclara que repetir lo mismo no duplica.
 * Nunca muestra el texto crudo del backend; el código de seguimiento es lo que se le cita a soporte.
 */
@Component({
  selector: 'ap-error-de-envio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta],
  template: `
    @if (error(); as e) {
      <ap-alerta tono="error" [titulo]="sinConfirmar() ? 'No pudimos confirmar si se registró' : 'No se pudo completar'">
        {{ e.mensaje }}
        @if (sinConfirmar()) { <span class="nota"> Si repetís exactamente lo mismo, no se duplica.</span> }
        @if (e.trazaId) { <span class="traza">Código de seguimiento: {{ e.trazaId }}</span> }
      </ap-alerta>
    }
  `,
  styles: `
    :host { display: block; }
    :host(:not(:empty)) { margin-block: var(--s3); }
    .traza { display: block; margin-top: var(--s1); color: var(--text-2); font-size: .85em; }
  `,
})
export class ErrorDeEnvio {
  readonly error = input<ErrorParaVista | null>(null)
  /** Sin respuesta confiable: no hubo contacto, venció el plazo o el servidor falló. En esos casos pudo registrarse. */
  protected readonly sinConfirmar = computed(() => {
    const e = this.error()
    return !!e && (!!e.sinConexion || !!e.tiempoAgotado || (e.estado ?? 0) === 0 || (e.estado ?? 0) >= 500)
  })
}
