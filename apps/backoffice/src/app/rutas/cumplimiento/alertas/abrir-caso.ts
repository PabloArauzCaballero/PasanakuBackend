import { ChangeDetectionStrategy, Component, input, model, output, signal } from '@angular/core'
import { Campo } from '@aportaya/ui/campo/campo'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { SalidaCasoLft } from 'clientes/angular/cumplimiento'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { AreaDeTexto } from '../compartido/campos'
import { traducirError } from '../compartido/errores-de-cumplimiento'
import { accionesDeCasos } from '../dominio/cu44-bandejas'
import { CAMPO_DE_CODIGO_CU44, cuerpoDeApertura, MAX_RESUMEN, validarApertura, type CampoDeApertura } from '../dominio/cu44-validacion'
import { sinErrores, type Errores } from '../dominio/validacion'
import { textosLft } from '../textos-lft'

/**
 * CU-44 paso 4 · agrupar alertas en un caso (`abrirCasoLft`). El botón de confirmar queda
 * bloqueado mientras se envía (`ap-dialogo [cargando]`), y la pantalla no dice «abierto»
 * hasta que el servidor responde 2xx.
 */
@Component({
  selector: 'ap-abrir-caso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialogo, Campo, AreaDeTexto, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="t.titulo" [textoDeConfirmar]="t.confirmar" [abierto]="abierto()" (abiertoChange)="abierto.set($event)" [cargando]="enviando()" (confirmar)="enviar()" (cancelar)="abierto.set(false)">
      <div class="formulario">
        <p>{{ t.alertas(alertaIds().length) }}</p>
        @if (errores().alertas) { <p class="error" role="alert">{{ errores().alertas }}</p> }
        <p>{{ t.consecuencia }}</p>
        <ap-campo [etiqueta]="t.analista" [(valor)]="analistaId" [ayuda]="t.analistaAyuda" [error]="errores().analistaId" autocompletar="off" />
        <ap-area-de-texto [etiqueta]="t.resumen" [(valor)]="resumen" [ayuda]="t.resumenAyuda" [error]="errores().resumen" [maximo]="MAX_RESUMEN" />
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
export class AbrirCaso {
  private readonly acciones = accionesDeCasos()
  protected readonly t = textosLft.apertura
  protected readonly MAX_RESUMEN = MAX_RESUMEN

  readonly abierto = model(false)
  readonly alertaIds = input.required<readonly string[]>()
  readonly abrio = output<SalidaCasoLft>()

  protected readonly analistaId = signal('')
  protected readonly resumen = signal('')
  protected readonly enviando = signal(false)
  protected readonly errores = signal<Errores<CampoDeApertura>>({})
  protected readonly errorGeneral = signal<ErrorTraducido | null>(null)

  async enviar(): Promise<void> {
    if (this.enviando()) return
    const errores = validarApertura(this.alertaIds(), this.analistaId(), this.resumen())
    this.errores.set(errores)
    this.errorGeneral.set(null)
    if (!sinErrores(errores)) return
    this.enviando.set(true)
    try {
      const salida = await this.acciones.abrir(cuerpoDeApertura(this.alertaIds(), this.analistaId(), this.resumen()))
      this.analistaId.set('')
      this.resumen.set('')
      this.abierto.set(false)
      this.abrio.emit(salida)
    } catch (e) {
      const error = traducirError(e)
      const campo = error.codigo ? (CAMPO_DE_CODIGO_CU44[error.codigo] as CampoDeApertura | undefined) : undefined
      if (campo === 'alertas') this.errores.set({ alertas: error.mensaje })
      else this.errorGeneral.set(error)
    } finally {
      this.enviando.set(false)
    }
  }
}
