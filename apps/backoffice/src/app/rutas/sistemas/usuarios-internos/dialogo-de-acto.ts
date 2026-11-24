import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, signal, untracked } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { CampoOTP } from '@aportaya/ui/campo-otp/campo-otp'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { of, switchMap, tap, type Observable } from 'rxjs'
import { esRechazoDefinitivo, traducirErrorDeGobierno, type ErrorDeGobierno } from '../dominio/errores-de-gobierno'
import { esCodigoValido, LARGO_DEL_CODIGO, pasoDeSeguridad } from '../dominio/paso-de-seguridad'

/**
 * El marco de todo acto de gobierno sobre el personal interno (GU-11, decisión de Pablo 2026-11-24): un diálogo con lo
 * propio del acto (proyectado), el **efecto** dicho en claro, el **motivo** y el **código del segundo factor**. Al
 * confirmar abre el desafío, presenta el código, y con la evidencia llama a `accion`. Resuelve lo que cada diálogo
 * repetiría: sin doble envío, lo escrito se conserva ante un fallo, el error va al campo que le corresponde
 * (`motivo`, `codigo`) o, si es de un campo propio del acto (`rol`, `ambito`), se lo entrega a quien proyecta por
 * `error`, y nunca se muestra el texto crudo del servidor. Un fallo sin respuesta (no se sabe si se registró) conserva
 * la evidencia: reintentar manda lo mismo y la clave de idempotencia evita duplicar.
 */
@Component({
  selector: 'ap-dialogo-de-acto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, AreaDeTexto, CampoOTP, Dialogo, ErrorDeEnvio],
  template: `
    <ap-dialogo [titulo]="titulo()" [textoDeConfirmar]="enviando() ? 'Confirmando…' : textoDeConfirmar()" [destructivo]="destructivo()" [cargando]="enviando()" [confirmarDeshabilitado]="!puedeEnviar() || enviando()" [puedeDescartar]="puedeCerrar" [(abierto)]="abierto" (confirmar)="enviar()" (cancelar)="abierto.set(false)">
      <ng-content />
      @if (efecto(); as texto) { <ap-alerta tono="aviso" titulo="Qué va a pasar">{{ texto }}</ap-alerta> }
      <div class="campos">
        <ap-area-de-texto [etiqueta]="etiquetaDeMotivo()" [(valor)]="motivo" [maximo]="maximoDeMotivo()" [deshabilitado]="enviando()" [error]="errorDeCampo('motivo')" [ayuda]="'Queda en la auditoría. Mínimo ' + minimoDeMotivo() + ' caracteres.'" />
        <div class="otp">
          <span class="rotulo" aria-hidden="true">Confirmá con tu segundo factor</span>
          <ap-campo-otp [etiqueta]="'Código de ' + largo + ' dígitos de tu aplicación de autenticación'" [largo]="largo" [(valor)]="codigo" [error]="errorDeCampo('codigo')" />
          <span class="ayuda">Abrí tu aplicación de autenticación y escribí el código de {{ largo }} dígitos. Sirve una sola vez.</span>
        </div>
      </div>
      <ap-error-de-envio [error]="errorGeneral()" />
    </ap-dialogo>
  `,
  styles: `
    .campos { display: grid; gap: var(--s4); margin-top: var(--s3); }
    .otp { display: grid; gap: var(--s1); }
    .rotulo { font-weight: 600; color: var(--text-2); }
    .ayuda { font: var(--t-ayuda); color: var(--text-3); }
  `,
})
export class DialogoDeActo {
  readonly titulo = input.required<string>()
  readonly textoDeConfirmar = input.required<string>()
  readonly destructivo = input(false)
  /** Lo que el acto provoca, dicho antes de confirmar (p. ej. «revoca todos sus roles»). */
  readonly efecto = input<string>()
  readonly etiquetaDeMotivo = input('Motivo')
  readonly minimoDeMotivo = input(10)
  readonly maximoDeMotivo = input(200)
  /** Validez de los campos propios del acto (proyectados). */
  readonly valido = input(true)
  /** Lo que se hace con la evidencia del segundo factor y el motivo ya escrito. */
  readonly accion = input.required<(evidencia: string, motivo: string) => Observable<unknown>>()
  readonly abierto = model(false)
  /** El último rechazo, para que quien proyecta señale sus campos (`rol`, `ambito`, `vigencia`). */
  readonly error = model<ErrorDeGobierno | null>(null)
  readonly completado = output<unknown>()

  protected readonly largo = LARGO_DEL_CODIGO
  private readonly obtenerEvidencia = pasoDeSeguridad()
  protected readonly motivo = signal('')
  protected readonly codigo = signal('')
  protected readonly enviando = signal(false)
  private evidenciaPendiente: string | null = null
  protected readonly puedeCerrar = () => !this.enviando()

  protected readonly puedeEnviar = computed(() => this.valido() && this.motivo().trim().length >= this.minimoDeMotivo() && (this.evidenciaPendienteVisible() || esCodigoValido(this.codigo())))
  private readonly evidenciaPendienteVisible = signal(false)

  protected readonly errorGeneral = computed(() => {
    const e = this.error()
    return e && !['motivo', 'codigo', 'rol', 'ambito', 'vigencia'].includes(e.campo ?? '') ? e : null
  })
  protected errorDeCampo(campo: 'motivo' | 'codigo'): string | undefined {
    const e = this.error()
    return e?.campo === campo ? e.mensaje : undefined
  }

  constructor() {
    // Cada vez que se abre, parte limpio: lo escrito de un intento anterior no es de este.
    effect(() => {
      if (!this.abierto()) return
      untracked(() => {
        this.motivo.set('')
        this.codigo.set('')
        this.error.set(null)
        this.enviando.set(false)
        this.evidenciaPendiente = null
        this.evidenciaPendienteVisible.set(false)
      })
    })
  }

  protected enviar(): void {
    if (!this.puedeEnviar() || this.enviando()) return
    this.enviando.set(true)
    this.error.set(null)
    const motivo = this.motivo().trim()
    const evidencia$ = this.evidenciaPendiente ? of(this.evidenciaPendiente) : this.obtenerEvidencia(this.codigo())
    evidencia$
      .pipe(
        tap((e) => {
          this.evidenciaPendiente = e
        }),
        switchMap((e) => this.accion()(e, motivo)),
      )
      .subscribe({
        next: (resultado) => {
          this.enviando.set(false)
          this.evidenciaPendiente = null
          this.abierto.set(false)
          this.completado.emit(resultado)
        },
        error: (e: unknown) => {
          const traducido = traducirErrorDeGobierno(e, 'No se pudo completar el cambio. Probá de nuevo.')
          this.enviando.set(false)
          // Un rechazo del servidor consume la evidencia; sin respuesta no se sabe, y reintentar lo mismo no duplica.
          if (esRechazoDefinitivo(traducido)) {
            this.evidenciaPendiente = null
            this.codigo.set('')
          }
          this.evidenciaPendienteVisible.set(this.evidenciaPendiente !== null)
          this.error.set(traducido)
        },
      })
  }
}
