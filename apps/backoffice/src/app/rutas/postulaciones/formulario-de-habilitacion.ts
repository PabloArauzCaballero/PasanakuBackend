import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
import { Boton } from '@aportaya/ui/boton/boton'
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import type { ErrorTraducido } from '../../nucleo/errores'
import {
  crearResolver,
  crearRevisar,
  decisionesDe,
  entradaDeResolucion,
  entradaDeRevision,
  MOTIVO_MAXIMO,
  type Accion,
  type DecisionDeHabilitacion,
} from './dominio/cu90-postulaciones'
import { textosPostulaciones } from './textos'

type Envio = 'inactivo' | 'enviando' | 'sinConfirmar' | 'rechazado'

/**
 * Contenedor de CU-90 · resolver (APROBAR/RECHAZAR) o revisar (CONFIRMAR/REVOCAR) una postulación. Ofrece la acción que
 * el servidor declaró en `acciones` y manda `revisionEsperada`: si otra persona decidió mientras tanto, el servidor
 * responde AP-CU90-08 y quien muestra el expediente lo vuelve a pedir.
 *
 * Sin respuesta (sin red, 5xx) no es «no pasó»: la clave la gobierna el contenido (`contextoIdempotente`), así que
 * reintentar lo MISMO reusa la clave y el servidor devuelve la decisión original (`nueva: false`) en vez de duplicarla.
 */
@Component({
  selector: 'ap-formulario-de-habilitacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, AreaDeTexto, Boton, GrupoRadio],
  template: `
    <section [attr.aria-labelledby]="'titulo-' + accion()">
      <h2 [id]="'titulo-' + accion()">{{ accion() === 'RESOLVER' ? t.resolverTitulo : t.revisarTitulo }}</h2>
      <p class="ayuda">{{ accion() === 'RESOLVER' ? t.resolverAyuda : t.revisarAyuda }}</p>
      @if (envio() === 'sinConfirmar') {
        <ap-alerta tono="aviso">{{ sinConfirmar }}</ap-alerta>
      }
      @if (error(); as e) {
        <ap-alerta tono="error">{{ e.mensaje }}</ap-alerta>
      }
      <form (submit)="$event.preventDefault(); enviar()" novalidate>
        <fieldset class="bloque" [disabled]="enviando()">
          <ap-grupo-radio [etiqueta]="t.decisionEtiqueta" [opciones]="opciones()" [(elegido)]="decision" />
        </fieldset>
        @if (errorDecision()) {
          <p class="error-campo" role="alert">{{ errorDecision() }}</p>
        }
        <ap-area-de-texto [etiqueta]="t.motivoEtiqueta" [ayuda]="t.motivoAyuda" [error]="errorMotivo()" [maximo]="maximo" [deshabilitado]="enviando()" [(valor)]="motivo" />
        <div class="acciones">
          <ap-boton variante="primario" tipo="submit" [cargando]="enviando()" [deshabilitado]="enviando()">{{ enviando() ? t.enviando : t.enviar }}</ap-boton>
        </div>
      </form>
    </section>
  `,
  styles: `
    section { display: flex; flex-direction: column; gap: var(--s3); }
    h2 { margin: 0; font-size: 1.1rem; }
    .ayuda { margin: 0; color: var(--text-2); }
    form { display: flex; flex-direction: column; gap: var(--s4); }
    .bloque { border: 0; margin: 0; padding: 0; min-width: 0; }
    .error-campo { margin: 0; color: var(--err-texto); font-size: .875rem; }
    .acciones { display: flex; gap: var(--s2); flex-wrap: wrap; }
  `,
})
export class FormularioDeHabilitacion {
  protected readonly t = textosPostulaciones
  protected readonly maximo = MOTIVO_MAXIMO
  protected readonly sinConfirmar =
    'No pudimos confirmar si tu decisión quedó registrada. Volvé a enviarla igual: el sistema reconoce que es la misma y no la duplica.'

  readonly solicitudId = input.required<string>()
  readonly accion = input.required<Accion>()
  readonly revision = input.required<number>()
  /** Quedó registrada (o ya lo estaba): quien muestra el expediente lo vuelve a pedir. */
  readonly registrada = output<DecisionDeHabilitacion>()
  /** El servidor dice que el expediente cambió: hay que volver a pedirlo. */
  readonly desactualizada = output<void>()

  private readonly resolver = crearResolver()
  private readonly revisar = crearRevisar()

  protected readonly opciones = computed<OpcionDeRadio[]>(() => decisionesDe(this.accion()).map((d) => ({ valor: d, texto: this.t.decisiones[d] ?? d })))
  protected readonly decision = signal<string | null>(null)
  protected readonly motivo = signal('')
  protected readonly errorDecision = signal('')
  protected readonly errorMotivo = signal('')
  protected readonly envio = signal<Envio>('inactivo')
  protected readonly error = signal<ErrorTraducido | null>(null)
  protected readonly enviando = computed(() => this.envio() === 'enviando')

  protected enviar(): void {
    if (this.enviando()) return
    const decision = this.decision()
    const motivo = this.motivo().trim()
    const validas = decisionesDe(this.accion()) as string[]
    this.errorDecision.set(decision && validas.includes(decision) ? '' : this.t.decisionRequerida)
    this.errorMotivo.set(motivo.length === 0 ? this.t.motivoRequerido : motivo.length > MOTIVO_MAXIMO ? this.t.motivoLargo : '')
    if (this.errorDecision() || this.errorMotivo() || !decision) return

    this.envio.set('enviando')
    this.error.set(null)
    const id = this.solicitudId()
    const pedido =
      this.accion() === 'RESOLVER'
        ? this.resolver(id, entradaDeResolucion(decision as 'APROBAR' | 'RECHAZAR', motivo, this.revision()))
        : this.revisar(id, entradaDeRevision(decision as 'CONFIRMAR' | 'REVOCAR', motivo, this.revision()))
    pedido.subscribe({
      next: (d) => {
        this.envio.set('inactivo')
        this.decision.set(null)
        this.motivo.set('')
        this.registrada.emit(d)
      },
      error: (e: ErrorTraducido) => {
        const estado = e.estado ?? 0
        if (e.sinConexion || estado === 0 || estado >= 500) {
          this.envio.set('sinConfirmar')
          return
        }
        this.envio.set('rechazado')
        this.error.set(e)
        if (e.codigo === 'AP-CU90-03' || e.codigo === 'AP-CU90-08') this.desactualizada.emit()
      },
    })
  }
}
