import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import type { OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import type { ErrorTraducido } from '../../nucleo/errores'
import { claveDeIdempotencia } from '../../nucleo/idempotencia.interceptor'
import { senalDeRed } from '../../nucleo/senal-de-red'
import {
  crearDecidirDirecto,
  MOTIVO_DIRECTO_MAXIMO,
  MOTIVO_DIRECTO_MINIMO,
  type ResultadoDeAdmision,
  type SalidaDecisionDirecta,
} from './dominio/cu68-admision'
import { FormularioDeResolucionVista, type EnvioResolucion } from './formulario-de-resolucion-vista'
import { textosAdmisiones } from './textos'

/** El servidor exige propuesta del organizador antes de que backoffice decida (interruptor de admisión humana). */
const EXIGE_PROPUESTA = 'AP-CU68-15'

/**
 * Contenedor Smart de CU-68 · backoffice decide directo una solicitud SIN propuesta del organizador
 * (`decidirSolicitudDeIngreso`). Reusa la vista del formulario de resolución.
 *
 * Sin respuesta no es «no pasó»: se pasa a `sinConfirmar`, y «verificar» reenvía lo MISMO con la MISMA clave.
 * El servidor trata repetir la misma decisión como reintento y devuelve lo ya resuelto, así que no duplica.
 */
@Component({
  selector: 'ap-formulario-de-decision-directa',
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
      [errorDelServidor]="errorDelServidor()"
      [bloqueado]="bloqueado()"
      [enviando]="enviando()"
      [opciones]="opciones"
      [ayuda]="t.directoAyuda"
      [ayudaMotivo]="t.motivoDirectoAyuda"
      (enviar)="enviar()"
      (verificar)="reenviar()"
    />
  `,
})
export class FormularioDeDecisionDirecta {
  protected readonly t = textosAdmisiones
  readonly solicitudId = input.required<string>()
  /** Quedó registrada: quien la muestra actualiza el estado. */
  readonly decidida = output<SalidaDecisionDirecta>()
  /** El servidor pide propuesta del organizador antes. */
  readonly exigePropuesta = output<void>()

  private readonly decidir = crearDecidirDirecto()

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
  protected readonly sinRed = senalDeRed()
  protected readonly enviando = computed(() => this.envio() === 'enviando' || this.envio() === 'verificando')
  protected readonly bloqueado = computed(() => ['enviando', 'verificando', 'sinConfirmar'].includes(this.envio()))

  /** Una clave por contenido: se reutiliza al reintentar lo mismo y cambia si cambia algo. */
  private claveVigente: { huella: string; clave: string } | null = null
  private intento: { decision: ResultadoDeAdmision; motivo: string } | null = null

  protected enviar(): void {
    if (this.bloqueado()) return
    const resultado = this.decision() as ResultadoDeAdmision | null
    const motivo = this.motivo().trim()
    this.errorDecision.set(resultado ? '' : this.t.decisionRequerida)
    this.errorMotivo.set(
      motivo.length === 0
        ? this.t.motivoRequerido
        : motivo.length < MOTIVO_DIRECTO_MINIMO
          ? this.t.motivoDirectoCorto
          : motivo.length > MOTIVO_DIRECTO_MAXIMO
            ? this.t.motivoDirectoLargo
            : '',
    )
    if (!resultado || this.errorMotivo()) return
    const huella = [resultado, motivo, this.solicitudId()].join('|')
    if (this.claveVigente?.huella !== huella) this.claveVigente = { huella, clave: claveDeIdempotencia() }
    this.intento = { decision: resultado, motivo }
    this.mandar('enviando')
  }

  /** Reenvía exactamente lo último que se mandó, con la misma clave. */
  protected reenviar(): void {
    if (this.envio() === 'verificando' || !this.intento) return
    this.mandar('verificando')
  }

  private mandar(fase: 'enviando' | 'verificando'): void {
    const intento = this.intento
    const clave = this.claveVigente?.clave
    if (!intento || !clave) return
    this.envio.set(fase)
    this.errorDelServidor.set(null)
    this.decidir(this.solicitudId(), intento, clave).subscribe({
      next: (salida) => {
        this.envio.set('confirmado')
        this.claveVigente = null
        this.decidida.emit(salida)
      },
      error: (e: ErrorTraducido) => this.alFallar(e),
    })
  }

  private alFallar(e: ErrorTraducido): void {
    const estado = e.estado ?? 0
    if (e.sinConexion || estado === 0 || estado >= 500) {
      this.envio.set('sinConfirmar')
      return
    }
    if (e.codigo === EXIGE_PROPUESTA) {
      this.envio.set('inactivo')
      this.exigePropuesta.emit()
      return
    }
    this.envio.set('rechazado')
    this.errorDelServidor.set(e)
  }
}
