import { ChangeDetectionStrategy, Component, ElementRef, Injector, afterNextRender, computed, effect, inject, input, output, signal, untracked } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CampoContrasena } from '@aportaya/ui/campo-contrasena/campo-contrasena'
import { CampoOTP } from '@aportaya/ui/campo-otp/campo-otp'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { IndicadorDeClave } from '../compartido/indicador-de-clave'
import { LARGO_MINIMO_DE_CLAVE, requisitosDeClave } from '../dominio/clave'
import type { AlcanceDeInvitacion, SalidaConsultaInvitacion, SalidaInicioEnrolamientoTotp } from '../dominio/cuenta'
import type { ErrorDeCuenta } from '../dominio/errores-de-cuenta'
import { EnrolamientoTotp } from './enrolamiento-totp'

export type DatosDeInvitacion = { claveNueva?: string; claveActual?: string; codigo?: string }

/**
 * El formulario de una invitación vigente. Presentacional: lo que escribió la persona vive acá y **se conserva** ante cualquier
 * fallo; el contenedor le pasa el factor generado, el error del servidor (ya mapeado a su campo) y si está enviando.
 * Qué se pide sale de la consulta: clave actual (`requiereClaveActual`), clave nueva (alcance `ALTA` o `CLAVE`) y factor (`requiereFactor`).
 */
@Component({
  selector: 'ap-formulario-de-invitacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CampoContrasena, CampoOTP, EnrolamientoTotp, Fecha, IndicadorDeClave],
  template: `
    <p class="vence">Esta invitación vence el <ap-fecha [iso]="consulta().venceEn" /> y se puede usar una sola vez.</p>
    @if (alertaGeneral(); as a) { <ap-alerta tono="error">{{ a }}</ap-alerta> }
    <form (submit)="$event.preventDefault(); enviar()" novalidate>
      @if (consulta().requiereClaveActual) {
        <ap-campo-contrasena etiqueta="Clave actual" autocompletar="current-password" [(valor)]="claveActual" [error]="errorClaveActual()" />
      }
      @if (pideClaveNueva()) {
        <ap-campo-contrasena etiqueta="Clave nueva" autocompletar="new-password" [ayuda]="'Mínimo ' + largoMinimo + ' caracteres.'" [(valor)]="claveNueva" [error]="errorClaveNueva()" />
        <ap-campo-contrasena etiqueta="Repetí la clave nueva" autocompletar="new-password" [(valor)]="repeticion" [error]="errorRepeticion()" />
        <ap-indicador-de-clave [requisitos]="requisitos()" />
      }
      @if (consulta().requiereFactor) {
        <ap-enrolamiento-totp [factor]="factor()" [cargando]="generandoFactor()" [error]="errorFactor()" (iniciar)="iniciarFactor.emit()" />
        @if (factor()) { <ap-campo-otp etiqueta="Código de 6 dígitos de tu aplicación" [(valor)]="codigo" [error]="errorCodigo()" /> }
      }
      <ap-boton variante="primario" tipo="submit" icono="candado" [ancho]="true" [cargando]="enviando()" [deshabilitado]="enviando()">{{ textoDeEnviar() }}</ap-boton>
    </form>
  `,
  styles: `
    :host { display: grid; gap: var(--s4); }
    form { display: grid; gap: var(--s4); }
    .vence { margin: 0; font: var(--t-cuerpo-chico); color: var(--text-2); }
  `,
})
export class FormularioDeInvitacion {
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef)
  private readonly injector = inject(Injector)
  readonly consulta = input.required<SalidaConsultaInvitacion>()
  readonly factor = input<SalidaInicioEnrolamientoTotp | null>(null)
  readonly generandoFactor = input(false)
  readonly enviando = input(false)
  readonly errorDelServidor = input<ErrorDeCuenta | null>(null)
  readonly iniciarFactor = output<void>()
  readonly completar = output<DatosDeInvitacion>()
  /** La persona tocó algo: el error del servidor deja de ser actual. */
  readonly cambio = output<void>()

  protected readonly largoMinimo = LARGO_MINIMO_DE_CLAVE
  protected readonly claveActual = signal('')
  protected readonly claveNueva = signal('')
  protected readonly repeticion = signal('')
  protected readonly codigo = signal('')
  private readonly local = signal<Record<string, string>>({})
  private omitirCambio = false

  protected readonly pideClaveNueva = computed(() => this.consulta().alcance !== 'FACTOR')
  protected readonly requisitos = computed(() => requisitosDeClave(this.claveNueva(), this.repeticion()))
  protected readonly textoDeEnviar = computed(() => ({ ALTA: 'Activar mi cuenta', CLAVE: 'Cambiar mi clave', FACTOR: 'Registrar mi autenticador' })[this.consulta().alcance as AlcanceDeInvitacion])
  private readonly delServidor = (campo: string) => (this.errorDelServidor()?.campo === campo ? this.errorDelServidor()!.mensaje : undefined)
  protected readonly errorClaveActual = computed(() => this.local()['claveActual'] ?? this.delServidor('claveActual'))
  protected readonly errorClaveNueva = computed(() => this.local()['claveNueva'] ?? this.delServidor('claveNueva'))
  protected readonly errorRepeticion = computed(() => this.local()['repeticion'])
  protected readonly errorCodigo = computed(() => this.local()['codigo'] ?? this.delServidor('codigo'))
  protected readonly errorFactor = computed(() => this.local()['factor'])
  /** El error del servidor que no es de ningún campo (red, 5xx, permiso) va arriba, en una alerta. */
  protected readonly alertaGeneral = computed(() => (this.errorDelServidor()?.campo ? null : (this.errorDelServidor()?.mensaje ?? null)))

  constructor() {
    effect(() => {
      this.claveActual(); this.claveNueva(); this.repeticion(); this.codigo()
      untracked(() => {
        if (this.omitirCambio) return void (this.omitirCambio = false)
        this.local.set({})
        this.cambio.emit()
      })
    })
    effect(() => {
      if (this.errorDelServidor()?.campo) untracked(() => this.enfocarPrimerError())
    })
  }

  protected enviar(): void {
    if (this.enviando()) return
    const e: Record<string, string> = {}
    if (this.consulta().requiereClaveActual && !this.claveActual()) e['claveActual'] = 'Escribí tu clave actual.'
    if (this.pideClaveNueva()) {
      if (this.claveNueva().length < LARGO_MINIMO_DE_CLAVE) e['claveNueva'] = `La clave nueva tiene al menos ${LARGO_MINIMO_DE_CLAVE} caracteres.`
      if (this.repeticion() !== this.claveNueva()) e['repeticion'] = 'Las dos claves no coinciden. Volvé a escribir la repetición.'
    }
    if (this.consulta().requiereFactor) {
      if (!this.factor()) e['factor'] = 'Generá tu código QR y registralo en tu aplicación antes de seguir.'
      else if (!/^\d{6}$/.test(this.codigo())) e['codigo'] = 'El código tiene 6 dígitos: completalo para seguir.'
    }
    this.local.set(e)
    if (Object.keys(e).length) return this.enfocarPrimerError()
    this.completar.emit({
      ...(this.consulta().requiereClaveActual ? { claveActual: this.claveActual() } : {}),
      ...(this.pideClaveNueva() ? { claveNueva: this.claveNueva() } : {}),
      ...(this.consulta().requiereFactor ? { codigo: this.codigo() } : {}),
    })
  }

  /** Limpia el código tras un rechazo (lo escrito antes —claves— se conserva) sin descartar el error que acaba de llegar. */
  limpiarCodigo(): void {
    if (!this.codigo()) return
    this.omitirCambio = true
    this.codigo.set('')
  }

  private enfocarPrimerError(): void {
    afterNextRender(() => this.el.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"], fieldset[aria-describedby] button')?.focus(), { injector: this.injector })
  }
}
