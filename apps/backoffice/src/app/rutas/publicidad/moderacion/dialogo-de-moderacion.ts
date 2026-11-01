import { ChangeDetectionStrategy, Component, model, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { accionesDeModeracion, type PiezaCreativa } from '../dominio/cu112-moderacion'
import { traducirError } from '../dominio/lotes-por-cursor'
import { textosPublicidad } from '../textos'
import { EntradaRevisionDecisionEnum } from 'clientes/angular/publicidad/model/entradaRevision'

export type DecisionDeModeracion = 'APROBADA' | 'RECHAZADA'

/**
 * CU-112 · Confirmar la decisión sobre una pieza. El rechazo exige motivo; ante un error (p. ej. R-PUB-05, quien la
 * subió no la modera) el diálogo queda abierto con lo escrito y el mensaje del dominio. Solo emite `resuelta` tras el 2xx.
 */
@Component({
  selector: 'ap-dialogo-de-moderacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Campo, Dialogo, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="decision() === 'APROBADA' ? t.aprobar : t.rechazar" [textoDeConfirmar]="decision() === 'APROBADA' ? t.aprobar : t.rechazar" [destructivo]="decision() === 'RECHAZADA'" [(abierto)]="abierto" [cargando]="enviando()" (confirmar)="confirmar()" (cancelar)="abierto.set(false)">
      <p>{{ decision() === 'APROBADA' ? t.confirmarAprobar(titulo()) : t.confirmarRechazar(titulo()) }}</p>
      <p class="nota">{{ t.autorLoDecideElServidor }}</p>
      @if (decision() === 'RECHAZADA') {
        <ap-campo [etiqueta]="t.motivoDeRechazo" [(valor)]="motivo" [error]="intento() && !motivo().trim() ? t.faltaMotivo : undefined" />
      }
      <ap-error-de-envio [error]="error()" />
    </ap-dialogo>
  `,
  styles: `.nota { color: var(--text-2); font: var(--t-cuerpo-chico); }`,
})
export class DialogoDeModeracion {
  private readonly acciones = accionesDeModeracion()
  protected readonly t = textosPublicidad.moderacion
  readonly abierto = model(false)
  readonly resuelta = output<{ pieza: PiezaCreativa; decision: DecisionDeModeracion }>()
  protected readonly pieza = signal<PiezaCreativa | null>(null)
  protected readonly decision = signal<DecisionDeModeracion>('APROBADA')
  protected readonly motivo = signal('')
  protected readonly intento = signal(false)
  protected readonly enviando = signal(false)
  protected readonly error = signal<ErrorTraducido | null>(null)
  protected readonly titulo = () => this.pieza()?.titulo ?? ''

  abrir(pieza: PiezaCreativa, decision: DecisionDeModeracion): void {
    this.pieza.set(pieza)
    this.decision.set(decision)
    this.motivo.set('')
    this.intento.set(false)
    this.error.set(null)
    this.abierto.set(true)
  }

  async confirmar(): Promise<void> {
    const p = this.pieza()
    if (!p || this.enviando()) return
    const decision = this.decision()
    if (decision === 'RECHAZADA') {
      this.intento.set(true)
      if (!this.motivo().trim()) return
    }
    this.enviando.set(true)
    this.error.set(null)
    try {
      const enumerado = decision === 'APROBADA' ? EntradaRevisionDecisionEnum.Aprobada : EntradaRevisionDecisionEnum.Rechazada
      await this.acciones.moderar(p.piezaCreativaId, { decision: enumerado, ...(decision === 'RECHAZADA' ? { motivo: this.motivo().trim() } : {}) })
      this.abierto.set(false)
      this.resuelta.emit({ pieza: p, decision })
    } catch (e) {
      this.error.set(traducirError(e))
    } finally {
      this.enviando.set(false)
    }
  }
}
