import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
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
import { textosAdmisiones } from './textos'

/** Lo que se sabe del envío. `sinConfirmar` es el estado que NO se puede tratar como un fallo. */
type Envio = 'inactivo' | 'enviando' | 'confirmado' | 'sinConfirmar' | 'verificando' | 'noRegistrada' | 'rechazado'

/**
 * La resolución humana de una solicitud de ingreso (CU-68, `resolverAdmision`). Una resolución no se
 * duplica ni se pierde: la clave de idempotencia nace con el contenido (resultado + motivo + propuesta +
 * revisión) y se reutiliza al reintentar lo mismo. Sin respuesta (sin red, tiempo agotado, 5xx) el estado
 * es «sin confirmar», no «falló»: no se reenvía a ciegas, se consulta el historial primero. Lo escrito se
 * conserva ante cualquier fallo y el formulario queda bloqueado mientras el envío está en curso o sin confirmar.
 */
@Component({
  selector: 'ap-formulario-de-resolucion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, Campo, GrupoRadio],
  template: `
    @if (envio() === 'confirmado') {
      <ap-alerta tono="ok">{{ t.confirmada }}</ap-alerta>
    } @else {
      <section aria-labelledby="titulo-resolver">
        <h2 id="titulo-resolver">{{ t.resolverTitulo }}</h2>
        <p class="ayuda">{{ t.resolverAyuda }}</p>

        @if (sinRed()) {
          <ap-alerta tono="aviso">{{ t.sinConexion }}</ap-alerta>
        }
        @if (avisoDeCambio()) {
          <ap-alerta tono="aviso">{{ t.cambio }}</ap-alerta>
        }
        @if (envio() === 'sinConfirmar' || envio() === 'verificando') {
          <ap-alerta tono="aviso" [titulo]="t.sinConfirmar">
            {{ t.sinConfirmarAyuda }}
            <div accion class="acciones">
              <ap-boton variante="primario" [cargando]="envio() === 'verificando'" [deshabilitado]="envio() === 'verificando'" (pulsado)="verificar()">
                {{ envio() === 'verificando' ? t.verificando : t.verificar }}
              </ap-boton>
            </div>
          </ap-alerta>
        }
        @if (envio() === 'noRegistrada') {
          <ap-alerta tono="aviso">{{ t.noRegistrada }}</ap-alerta>
        }
        @if (otraPersonaResolvio()) {
          <ap-alerta tono="error">{{ t.otraResolucion }}</ap-alerta>
        }
        @if (errorDelServidor(); as e) {
          <ap-alerta tono="error">{{ e.mensaje }}</ap-alerta>
        }

        <form (submit)="$event.preventDefault(); enviar()" novalidate>
          <fieldset class="bloque" [disabled]="bloqueado()">
            <ap-grupo-radio [etiqueta]="t.decisionEtiqueta" [opciones]="opciones" [(elegido)]="decision" />
          </fieldset>
          @if (errorDecision()) {
            <p class="error-campo" role="alert">{{ errorDecision() }}</p>
          }
          <ap-campo [etiqueta]="t.motivoEtiqueta" [ayuda]="t.motivoAyuda" [error]="errorMotivo()" [deshabilitado]="bloqueado()" [(valor)]="motivo" />
          <div class="acciones">
            <ap-boton variante="primario" tipo="submit" [cargando]="enviando()" [deshabilitado]="bloqueado()">
              {{ envio() === 'noRegistrada' ? t.reintentar : enviando() ? t.resolviendo : t.resolver }}
            </ap-boton>
          </div>
        </form>
      </section>
    }
  `,
  styles: `
    section { display: flex; flex-direction: column; gap: var(--s3); }
    section > * { margin: 0; }
    h2 { font-size: 1.1rem; }
    .ayuda { color: var(--text-2); }
    form { display: flex; flex-direction: column; gap: var(--s4); }
    .bloque { border: 0; margin: 0; padding: 0; min-width: 0; }
    .error-campo { margin: 0; color: var(--err-texto); font-size: .875rem; }
    .acciones { display: flex; gap: var(--s2); flex-wrap: wrap; margin-top: var(--s2); }
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
  protected readonly envio = signal<Envio>('inactivo')
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
