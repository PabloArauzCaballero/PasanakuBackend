import { ChangeDetectionStrategy, Component, computed, effect, input, model, output, signal, untracked } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { GrupoRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import type { CasoEnBandeja, SalidaDecisionCasoLft } from 'clientes/angular/cumplimiento'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { AreaDeTexto, opcionesDe } from '../compartido/campos'
import { traducirError } from '../compartido/errores-de-cumplimiento'
import { accionesDeCasos } from '../dominio/cu44-bandejas'
import {
  CAMPO_DE_CODIGO_CU44,
  cuerpoDeDecision,
  DecisionDeCaso,
  MAX_CONCLUSION,
  validarDecision,
  type CampoDeDecision,
  type Decision,
} from '../dominio/cu44-validacion'
import { sinErrores, type Errores } from '../dominio/validacion'
import { textosLft } from '../textos-lft'

/**
 * CU-44 pasos 5 a 8 · la decisión del oficial (`decidirCasoLft`). El revisor es la sesión
 * —no hay campo para elegirlo— y la decisión nunca se comunica al titular: esta pantalla no
 * ofrece «avisar al cliente» (deber de reserva LFT). Nada dice «decidido» antes del 2xx.
 */
@Component({
  selector: 'ap-decidir-caso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo, Campo, AreaDeTexto, GrupoRadio, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="t.titulo(caso()?.codigo ?? '')" [textoDeConfirmar]="t.confirmar" [abierto]="abierto()" (abiertoChange)="abierto.set($event)" [cargando]="enviando()" (confirmar)="enviar()" (cancelar)="abierto.set(false)">
      <div class="formulario">
        <p>{{ t.consecuencia }}</p>
        <ap-grupo-radio [etiqueta]="t.decision" [opciones]="OPCIONES" [elegido]="decision()" (elegidoChange)="decision.set($any($event))" />
        @if (errores().decision) { <p class="error" role="alert">{{ errores().decision }}</p> }
        <ap-area-de-texto [etiqueta]="t.conclusion" [(valor)]="conclusion" [ayuda]="t.conclusionAyuda" [error]="errores().conclusion" [maximo]="MAX_CONCLUSION" />
        @if (reporta()) {
          <ap-campo [etiqueta]="t.ros" [(valor)]="reporteSospechosoId" [ayuda]="t.rosAyuda" [error]="errores().reporteSospechosoId" autocompletar="off" />
        }
        <ap-error-de-envio [error]="errorGeneral()" />
      </div>
    </ap-dialogo>
  `,
  styles: `
    .formulario { display: grid; gap: var(--s3); }
    p { margin: 0; }
    .error { color: var(--err); }
  `,
})
export class DecidirCaso {
  private readonly acciones = accionesDeCasos()
  protected readonly t = textosLft.decision
  protected readonly OPCIONES = opcionesDe(textosLft.decisiones)
  protected readonly MAX_CONCLUSION = MAX_CONCLUSION

  readonly abierto = model(false)
  readonly caso = input<CasoEnBandeja | null>(null)
  /** Lo que ya se escribió en la ficha del caso: se ofrece como punto de partida de la conclusión, no la pisa. */
  readonly conclusionInicial = input('')
  readonly decidio = output<SalidaDecisionCasoLft>()

  constructor() {
    effect(() => {
      if (this.abierto() && this.conclusionInicial() && !untracked(this.conclusion)) this.conclusion.set(this.conclusionInicial())
    })
  }

  protected readonly decision = signal<Decision | null>(null)
  protected readonly conclusion = signal('')
  protected readonly reporteSospechosoId = signal('')
  protected readonly reporta = computed(() => this.decision() === DecisionDeCaso.Reportar)
  protected readonly enviando = signal(false)
  protected readonly errores = signal<Errores<CampoDeDecision>>({})
  protected readonly errorGeneral = signal<ErrorTraducido | null>(null)

  async enviar(): Promise<void> {
    const caso = this.caso()
    if (this.enviando() || !caso) return
    const decision = this.decision()
    const errores = validarDecision(decision, this.conclusion(), this.reporteSospechosoId())
    this.errores.set(errores)
    this.errorGeneral.set(null)
    if (!sinErrores(errores) || !decision) return
    this.enviando.set(true)
    try {
      const salida = await this.acciones.decidir(caso.casoId, cuerpoDeDecision(decision, this.conclusion(), this.reporteSospechosoId()))
      this.decision.set(null)
      this.conclusion.set('')
      this.reporteSospechosoId.set('')
      this.abierto.set(false)
      this.decidio.emit(salida)
    } catch (e) {
      const error = traducirError(e)
      const campo = error.codigo ? (CAMPO_DE_CODIGO_CU44[error.codigo] as CampoDeDecision | undefined) : undefined
      if (campo === 'conclusion' || campo === 'reporteSospechosoId') this.errores.set({ [campo]: error.mensaje })
      else this.errorGeneral.set(error)
    } finally {
      this.enviando.set(false)
    }
  }
}
