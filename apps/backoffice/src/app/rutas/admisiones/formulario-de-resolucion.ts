import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import type { OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import type { ErrorTraducido } from '../../nucleo/errores'
import { claveDeIdempotencia } from '../../nucleo/idempotencia.interceptor'
import { senalDeRed } from '../../nucleo/senal-de-red'
import {
  crearConsultarHistorial,
  crearResolver,
  esMiResolucion,
  MOTIVO_MAXIMO,
  resolucionDe,
  type DecisionDeIngreso,
  type ResultadoDeAdmision,
} from './dominio/cu68-admision'
import { FormularioDeResolucionVista, type EnvioResolucion } from './formulario-de-resolucion-vista'
import { textosAdmisiones } from './textos'

/**
 * Contenedor Smart de CU-68 · La resolución humana de una solicitud de ingreso (`resolverAdmision`).
 * Orquesta la clave de idempotencia por contenido (resultado + motivo + propuesta + revisión),
 * la resiliencia ante estados «sin confirmar», la consulta al historial y la gestión de red,
 * delegando la presentación visual al componente presentacional puro `FormularioDeResolucionVista`.
 */
@Component({
  selector: 'ap-formulario-de-resolucion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormularioDeResolucionVista],
  template: `
    <ap-formulario-de-resolucion-vista
      [(decision)]="decision"
      [(motivo)]="motivo"
      [errorDecision]="errorDecision()"
      [errorMotivo]="errorMotivo()"
      [envio]="envio()"
      [sinRed]="sinRed()"
      [avisoDeCambio]="avisoDeCambio()"
      [otraPersonaResolvio]="otraPersonaResolvio()"
      [errorDelServidor]="errorDelServidor()"
      [bloqueado]="bloqueado()"
      [enviando]="enviando()"
      [opciones]="opciones"
      (enviar)="enviar()"
      (verificar)="verificar()"
    />
  `,
})
export class FormularioDeResolucion {
  protected readonly t = textosAdmisiones
  readonly solicitudId = input.required<string>()
  readonly propuesta = input.required<DecisionDeIngreso>()
  readonly revision = input.required<number>()
  /** Quedó registrada, o el servidor dice que la solicitud cambió: quien muestra el historial lo vuelve a pedir. */
  readonly registrada = output<void>()
  readonly desactualizada = output<void>()

  private readonly consultarHistorial = crearConsultarHistorial()
  private readonly enviarResolucion = crearResolver()

  protected readonly opciones: OpcionDeRadio[] = [
    { valor: 'ACEPTAR', texto: this.t.aceptar },
    { valor: 'RECHAZAR', texto: this.t.rechazar },
  ]
  protected readonly decision = signal<string | null>(null)
  protected readonly motivo = signal('')
  protected readonly errorDecision = signal('')
  protected readonly errorMotivo = signal('')
  protected readonly envio = signal<EnvioResolucion>('inactivo')
  protected readonly errorDelServidor = signal<ErrorTraducido | null>(null)
  protected readonly otraPersonaResolvio = signal(false)
  protected readonly avisoDeCambio = signal(false)
  protected readonly sinRed = senalDeRed()
  protected readonly enviando = computed(() => this.envio() === 'enviando')
  protected readonly bloqueado = computed(() => ['enviando', 'verificando', 'sinConfirmar'].includes(this.envio()))

  /**
   * Lo que se mandó la última vez. La verificación compara contra esto y no contra el formulario:
   * lo que se ve en pantalla puede haber cambiado, lo enviado no.
   */
  private intento: { propuestaId: string; decision: ResultadoDeAdmision; motivo: string } | null = null
  /** Una clave por contenido: se reutiliza al reintentar lo mismo y cambia si cambia algo. */
  private claveVigente: { huella: string; clave: string } | null = null

  protected enviar(): void {
    if (this.bloqueado()) return
    const propuesta = this.propuesta()
    const resultado = this.decision() as ResultadoDeAdmision | null
    const motivo = this.motivo().trim()
    this.errorDecision.set(resultado ? '' : this.t.decisionRequerida)
    this.errorMotivo.set(motivo.length === 0 ? this.t.motivoRequerido : motivo.length > MOTIVO_MAXIMO ? this.t.motivoLargo : '')
    if (!resultado || this.errorMotivo()) return

    const revisionEsperada = this.revision()
    const huella = [resultado, motivo, propuesta.id, revisionEsperada].join('|')
    if (this.claveVigente?.huella !== huella) this.claveVigente = { huella, clave: claveDeIdempotencia() }
    const clave = this.claveVigente.clave

    this.intento = { propuestaId: propuesta.id, decision: resultado, motivo }
    this.envio.set('enviando')
    this.errorDelServidor.set(null)
    this.otraPersonaResolvio.set(false)
    this.avisoDeCambio.set(false)
    this.enviarResolucion(this.solicitudId(), { decision: resultado, motivo, revisionEsperada, propuestaId: propuesta.id }, clave).subscribe({
      next: () => this.confirmar(),
      error: (e: ErrorTraducido) => this.alFallar(e),
    })
  }

  private confirmar(): void {
    this.envio.set('confirmado')
    this.motivo.set('')
    this.decision.set(null)
    this.claveVigente = null
    this.registrada.emit()
  }

  /**
   * Sin respuesta (sin red, tiempo agotado, 5xx) NO es «no pasó»: pudo registrarse en el servidor.
   * Se pasa a `sinConfirmar` y se obliga a consultar antes de reenviar.
   */
  private alFallar(e: ErrorTraducido): void {
    const estado = e.estado ?? 0
    if (e.sinConexion || estado === 0 || estado >= 500) {
      this.envio.set('sinConfirmar')
      return
    }
    this.envio.set('rechazado')
    this.errorDelServidor.set(e)
    this.avisoDeCambio.set(true)
    this.desactualizada.emit()
  }

  /** Consulta el historial y decide qué pasó con el envío sin confirmar. No manda nada nuevo. */
  protected verificar(): void {
    if (this.envio() === 'verificando') return
    const intento = this.intento
    this.envio.set('verificando')
    this.consultarHistorial(this.solicitudId()).subscribe({
      next: (historial) => {
        if (intento && esMiResolucion(historial, intento.propuestaId, intento.decision, intento.motivo)) this.confirmar()
        else if (resolucionDe(historial)) {
          this.envio.set('rechazado')
          this.otraPersonaResolvio.set(true)
          this.desactualizada.emit()
        } else this.envio.set('noRegistrada')
      },
      error: () => this.envio.set('sinConfirmar'),
    })
  }
}
