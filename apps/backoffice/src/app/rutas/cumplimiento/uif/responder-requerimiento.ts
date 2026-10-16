import { ChangeDetectionStrategy, Component, input, model, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { RequerimientoEnLista, SalidaRequerimientoDeAutoridad } from 'clientes/angular/cumplimiento'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { traducirError } from '../compartido/errores-de-cumplimiento'
import type { Recibo } from '../dominio/cu43-reportes'
import { accionesDeRequerimientos, validarRespuesta } from '../dominio/cu45-requerimientos'
import { textosUif } from '../textos-uif'

/** CU-45 · responder (`responderRequerimientoDeAutoridad`). El plazo y el oficio son los registrados: no se editan. */
@Component({
  selector: 'ap-responder-requerimiento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo, Campo, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="t.titulo(requerimiento()?.numeroOficio ?? '')" [textoDeConfirmar]="t.confirmar" [abierto]="abierto()" (abiertoChange)="abierto.set($event)" [cargando]="enviando()" (confirmar)="enviar()" (cancelar)="abierto.set(false)">
      <div class="formulario">
        <p>{{ t.consecuencia }}</p>
        <ap-campo [etiqueta]="t.enlace" [(valor)]="respuestaUrl" [error]="error()" autocompletar="off" />
        <ap-error-de-envio [error]="errorGeneral()" />
      </div>
    </ap-dialogo>
  `,
  styles: `.formulario { display: grid; gap: var(--s3); } p { margin: 0; }`,
})
export class ResponderRequerimiento {
  private readonly acciones = accionesDeRequerimientos()
  protected readonly t = textosUif.respuesta

  readonly abierto = model(false)
  readonly requerimiento = input<RequerimientoEnLista | null>(null)
  readonly respondio = output<Recibo<SalidaRequerimientoDeAutoridad>>()

  protected readonly respuestaUrl = signal('')
  protected readonly enviando = signal(false)
  protected readonly error = signal<string | undefined>(undefined)
  protected readonly errorGeneral = signal<ErrorTraducido | null>(null)

  async enviar(): Promise<void> {
    const requerimiento = this.requerimiento()
    if (this.enviando() || !requerimiento) return
    const error = validarRespuesta(this.respuestaUrl())
    this.error.set(error)
    this.errorGeneral.set(null)
    if (error) return
    this.enviando.set(true)
    try {
      const recibo = await this.acciones.responder(requerimiento.requerimientoId, { respuestaUrl: this.respuestaUrl().trim() })
      this.respuestaUrl.set('')
      this.abierto.set(false)
      this.respondio.emit(recibo)
    } catch (e) {
      this.errorGeneral.set(traducirError(e))
    } finally {
      this.enviando.set(false)
    }
  }
}
