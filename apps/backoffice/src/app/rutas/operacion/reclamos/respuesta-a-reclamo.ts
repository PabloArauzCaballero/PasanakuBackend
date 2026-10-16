import { ChangeDetectionStrategy, Component, input, OnInit, output, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Campo } from '@aportaya/ui/campo/campo'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import type { ErrorTraducido } from '../../../nucleo/errores'
import {
  crearConsultarReclamo,
  crearResponderReclamo,
  RESPUESTA_MAXIMA,
  type EntradaRespuestaReclamo,
  type FichaDeReclamo,
  type ReclamoEnBandeja,
  type ResultadoReclamo,
  type SalidaRespuestaReclamo,
} from '../dominio/cu52-reclamos'
import { textosReclamos } from './textos'

const T = textosReclamos.respuesta
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/**
 * CU-52 paso 5 · responder y cerrar un reclamo (`responderReclamo`, RECLAMO_ATENDER). Trae la ficha
 * (`consultarReclamo`) para tener el texto delante; el formulario valida en el cliente lo mismo que el contrato
 * (resultado obligatorio, 1–4000 caracteres, devolución con forma UUID y obligatoria si es FAVORABLE con monto) y el
 * servidor vuelve a decidir. El confirmar se bloquea mientras envía, lo escrito se conserva ante cualquier fallo y el
 * éxito se anuncia recién con el 200.
 */
@Component({
  selector: 'ap-respuesta-a-reclamo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Campo, Dialogo, GrupoRadio],
  template: `
    <ap-dialogo [titulo]="T.titulo(reclamo().codigo)" [textoDeConfirmar]="T.confirmar(reclamo().codigo)" [abierto]="true" [cargando]="enviando()" [hayCambiosSinGuardar]="!!texto().trim() && !enviando()" (confirmar)="enviar()" (cancelar)="cerrar.emit()">
      @if (fichaError(); as e) {
        <ap-alerta tono="error">{{ e.estado === 403 ? T.fichaSinAcceso : e.mensaje }}</ap-alerta>
      } @else if (!ficha()) {
        <p role="status">{{ T.cargandoFicha }}</p>
      } @else {
        <h3>{{ T.queReclamo }}</h3>
        <p class="descripcion">{{ ficha()!.descripcion }}</p>
      }
      <p>{{ T.consecuencia }}</p>
      @if (aviso()) { <ap-alerta [tono]="sinConfirmar() ? 'aviso' : 'error'">{{ aviso() }}</ap-alerta> }
      <fieldset class="bloque" [disabled]="enviando()">
        <ap-grupo-radio [etiqueta]="T.resultadoEtiqueta" [opciones]="opciones" [(elegido)]="resultado" />
      </fieldset>
      @if (errorResultado()) { <p class="error-campo" role="alert">{{ errorResultado() }}</p> }
      <label for="respuesta-reclamo">{{ T.respuestaEtiqueta }}</label>
      <textarea id="respuesta-reclamo" rows="6" [value]="texto()" [disabled]="enviando()" [attr.aria-invalid]="!!errorTexto() || null" [attr.aria-describedby]="errorTexto() ? 'respuesta-reclamo-error' : null" (input)="texto.set($any($event.target).value)"></textarea>
      @if (errorTexto()) { <p id="respuesta-reclamo-error" class="error-campo" role="alert">{{ errorTexto() }}</p> }
      @if (resultado() === 'FAVORABLE') {
        <ap-campo [etiqueta]="T.devolucionEtiqueta" [ayuda]="T.devolucionAyuda" [error]="errorDevolucion()" [deshabilitado]="enviando()" [(valor)]="devolucion" />
      }
    </ap-dialogo>
  `,
  styles: `
    h3 { margin: 0 0 var(--s1); font-size: 1em; color: var(--text); }
    .descripcion { white-space: pre-wrap; color: var(--text); }
    .bloque { border: 0; margin: 0; padding: 0; min-width: 0; }
    label { display: block; margin-top: var(--s3); font-weight: 600; color: var(--text-2); }
    textarea { width: 100%; min-height: 8rem; padding: var(--s3); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); font: inherit; background: var(--field); color: var(--text); }
    .error-campo { margin: var(--s1) 0 0; color: var(--err-texto); font-size: .875rem; }
  `,
})
export class RespuestaAReclamo implements OnInit {
  readonly reclamo = input.required<ReclamoEnBandeja>()
  readonly respondido = output<SalidaRespuestaReclamo>()
  readonly cerrar = output<void>()
  protected readonly T = T
  private readonly consultar = crearConsultarReclamo()
  private readonly responder = crearResponderReclamo()
  protected readonly opciones: OpcionDeRadio[] = (Object.keys(T.resultados) as ResultadoReclamo[]).map((v) => ({ valor: v, texto: T.resultados[v] }))
  protected readonly ficha = signal<FichaDeReclamo | null>(null)
  protected readonly fichaError = signal<ErrorTraducido | null>(null)
  protected readonly resultado = signal<string | null>(null)
  protected readonly texto = signal('')
  protected readonly devolucion = signal('')
  protected readonly errorResultado = signal('')
  protected readonly errorTexto = signal('')
  protected readonly errorDevolucion = signal('')
  protected readonly enviando = signal(false)
  protected readonly aviso = signal('')
  protected readonly sinConfirmar = signal(false)

  ngOnInit(): void {
    this.consultar(this.reclamo().reclamoId).subscribe({ next: (f) => this.ficha.set(f), error: (e: ErrorTraducido) => this.fichaError.set(e) })
  }

  protected enviar(): void {
    if (this.enviando()) return
    const entrada = this.validar()
    if (!entrada) return
    this.enviando.set(true)
    this.aviso.set('')
    this.sinConfirmar.set(false)
    this.responder(this.reclamo().reclamoId, entrada).subscribe({
      next: (r) => {
        this.enviando.set(false)
        this.respondido.emit(r)
      },
      error: (e: ErrorTraducido) => this.alFallar(e),
    })
  }

  private validar(): EntradaRespuestaReclamo | null {
    const resultado = this.resultado() as ResultadoReclamo | null
    const texto = this.texto().trim()
    const devolucion = this.devolucion().trim().toLowerCase()
    const exigeDevolucion = resultado === 'FAVORABLE' && !!this.reclamo().montoReclamado
    this.errorResultado.set(resultado ? '' : T.resultadoRequerido)
    this.errorTexto.set(!texto ? T.respuestaRequerida : texto.length > RESPUESTA_MAXIMA ? T.respuestaLarga : '')
    this.errorDevolucion.set(
      resultado !== 'FAVORABLE' ? '' : devolucion && !UUID.test(devolucion) ? T.devolucionFormato : exigeDevolucion && !devolucion ? T.devolucionRequerida : '',
    )
    if (!resultado || this.errorTexto() || this.errorDevolucion()) return null
    // Entrada estricta: `devolucionId` viaja solo si es FAVORABLE y se escribió (el servidor rechaza campos de más).
    const base = { resultado, respuesta: texto } as EntradaRespuestaReclamo
    return resultado === 'FAVORABLE' && devolucion ? { ...base, devolucionId: devolucion } : base
  }

  /** Sin respuesta NO es «no pasó»: queda sin confirmar y el reintento sale con la misma clave. Todo lo escrito se conserva. */
  private alFallar(e: ErrorTraducido): void {
    this.enviando.set(false)
    const estado = e.estado ?? 0
    if (e.sinConexion || e.tiempoAgotado || estado === 0 || estado >= 500) {
      this.sinConfirmar.set(true)
      this.aviso.set(T.sinConfirmar)
      return
    }
    if (e.codigo === 'AP-CU52-03') return this.errorDevolucion.set(T.faltaReparacion)
    this.aviso.set(e.codigo === 'AP-CU52-06' ? T.yaCerrado : e.mensaje)
  }
}
