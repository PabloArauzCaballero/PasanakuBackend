import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import type { ExpedienteEnRevision } from 'clientes/angular/identidad'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { crearResolver, esPendiente, vencimientoDe } from '../dominio/cu02-expedientes'
import { estadoEnPalabras, fotosCompletas, impedimentosParaAprobar } from '../dominio/cu02-presentacion'
import { textosCumplimiento } from '../textos'
import { DecisionDeExpediente, type Decision } from './decision-de-expediente'
import { textosVerificaciones } from './textos-verificaciones'

/**
 * La pestaña «Decisión»: si el expediente espera, lo que impide aprobar (en palabras, el mismo orden que el servidor),
 * el motivo y los dos botones; si ya está resuelto, el resultado y por qué no se vuelve a decidir. El envío lleva una
 * clave de idempotencia nueva por intento (`crearResolver`); un error se muestra entero (`ap-error-de-envio` distingue
 * «no pasó» de «no sabemos») y callarlo dejaría a quien revisa creyendo que decidió.
 */
@Component({
  selector: 'ap-vista-decision-del-expediente',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, DecisionDeExpediente, ErrorDeEnvio],
  template: `
    @let e = expediente();
    <h2>{{ t.titulo }}</h2>
    @if (hecha()) { <ap-alerta tono="ok">{{ t.hecha }}</ap-alerta> }
    @if (pendiente()) {
      <p class="lead">{{ t.pendiente }}</p>
      @if (impedimentos().length) {
        <ap-alerta tono="aviso" [titulo]="t.impedimentos">
          <ul>@for (m of impedimentos(); track m) { <li>{{ m }}</li> }</ul>
        </ap-alerta>
      } @else {
        <ap-alerta tono="info">{{ t.sinImpedimentos }}</ap-alerta>
      }
      <ap-decision-de-expediente [conAvisos]="false" [verificacionId]="e.verificacionId" [completo]="completo()" [vencimiento]="vencimiento()" [ocupado]="enCurso() !== null" [enCurso]="enCurso()" (decidir)="resolver($event)" />
      <ap-error-de-envio [error]="error()" />
    } @else {
      <ap-alerta [tono]="e.estado === 'APROBADA' ? 'ok' : 'error'" [titulo]="t.resultado + ': ' + estado(e.estado)">
        @if (e.motivoRechazo) { <p>{{ motivo }}: {{ e.motivoRechazo }}</p> }
        <p>{{ t.yaResuelto }}</p>
      </ap-alerta>
    }
  `,
  styles: `
    :host { display: grid; gap: var(--s4); max-width: 60rem; min-width: 0; }
    h2 { margin: 0; font: var(--t-titulo-3); color: var(--text); }
    .lead { margin: 0; font: var(--t-cuerpo); color: var(--text-2); }
    ul { margin: 0; padding-left: var(--s4); display: grid; gap: var(--s1); }
    p { margin: 0; }
  `,
})
export class VistaDecisionDelExpediente {
  readonly expediente = input.required<ExpedienteEnRevision>()
  /** El servidor ya resolvió: quien aloja la ficha recarga el expediente. */
  readonly resuelto = output<void>()
  protected readonly t = textosVerificaciones.ficha.decision
  protected readonly motivo = textosCumplimiento.verificaciones.motivo
  protected readonly estado = estadoEnPalabras
  protected readonly pendiente = computed(() => esPendiente(this.expediente()))
  protected readonly completo = computed(() => fotosCompletas(this.expediente()))
  protected readonly vencimiento = computed(() => vencimientoDe(this.expediente()))
  protected readonly impedimentos = computed(() => impedimentosParaAprobar(this.expediente()))
  protected readonly enCurso = signal<Decision['decision'] | null>(null)
  protected readonly error = signal<ErrorTraducido | null>(null)
  protected readonly hecha = signal(false)
  private readonly enviar = crearResolver()

  protected resolver({ decision, motivo }: Decision): void {
    if (this.enCurso()) return
    this.enCurso.set(decision)
    this.error.set(null)
    this.enviar(this.expediente().verificacionId, { decision, motivo }).subscribe({
      next: () => {
        this.enCurso.set(null)
        this.hecha.set(true)
        this.resuelto.emit()
      },
      error: (err: ErrorTraducido) => {
        this.enCurso.set(null)
        this.error.set({ ...err, mensaje: err?.mensaje ?? textosCumplimiento.verificaciones.decisionFallo })
      },
    })
  }
}
