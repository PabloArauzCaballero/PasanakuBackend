import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { DialogoDeActo } from '../compartido/dialogo-de-acto'
import { actualizadorUsuarioInterno } from '../dominio/usuarios-internos'
import type { ErrorDeGobierno } from '../dominio/errores-de-gobierno'
import { errorDeFechaDeNacimiento } from '../dominio/fecha-de-nacimiento'

/**
 * GU-17 (A8): completa la fecha de nacimiento de un operador «sin dato» (PATCH `/usuarios-internos/{id}` con
 * `fechaNacimiento`), con motivo y segundo factor. El servidor solo la acepta si la persona no tiene una, es real y
 * de alguien de 18 años o más; su rechazo vuelve al campo (`AP-CU08-13/14/15`) y lo escrito se conserva.
 */
@Component({
  selector: 'ap-modal-completar-fecha',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Campo, DialogoDeActo],
  template: `
    <ap-dialogo-de-acto titulo="Completar la fecha de nacimiento" textoDeConfirmar="Guardar la fecha" etiquetaDeMotivo="Por qué se completa" [minimoDeMotivo]="10" [maximoDeMotivo]="200" [valido]="esValido()" [accion]="accion" [abierto]="true" (abiertoChange)="!$event && cerrar.emit()" [(error)]="error" (completado)="guardado.emit()">
      <ap-campo etiqueta="Fecha de nacimiento" tipo="date" [(valor)]="fecha" autocompletar="bday" [error]="errorDeFecha()" ayuda="Se guarda una sola vez: después no se reescribe." />
    </ap-dialogo-de-acto>
  `,
})
export class ModalCompletarFecha {
  readonly usuarioId = input.required<string>()
  readonly cerrar = output<void>()
  readonly guardado = output<void>()

  private readonly actualizar = actualizadorUsuarioInterno()
  protected readonly fecha = signal('')
  protected readonly error = signal<ErrorDeGobierno | null>(null)
  protected readonly errorDeFecha = computed(() => (this.error()?.campo === 'fechaNacimiento' ? this.error()!.mensaje : errorDeFechaDeNacimiento(this.fecha())))

  protected esValido(): boolean {
    return this.fecha() !== '' && !this.errorDeFecha()
  }

  protected readonly accion = (evidenciaMfa: string, motivo: string) =>
    this.actualizar(this.usuarioId(), { fechaNacimiento: this.fecha(), motivo, evidenciaMfa })
}
