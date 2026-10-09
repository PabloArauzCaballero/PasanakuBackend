import { DatePipe } from '@angular/common'
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import type { DecisionDeIngreso } from './dominio/cu68-admision'
import { textosAdmisiones } from './textos'

/**
 * Lo que ya se decidió sobre una solicitud, en orden de revisión, y la evidencia del algoritmo.
 * Presentacional: no pide nada ni decide nada. La evidencia es una RECOMENDACIÓN y se rotula así:
 * nadie entra ni queda afuera por ella.
 */
@Component({
  selector: 'ap-historial-de-admision',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, ChipEstado, Alerta],
  template: `
    @if (ultima(); as u) {
      <section class="evidencia" aria-labelledby="titulo-evidencia">
        <h2 id="titulo-evidencia">{{ t.evidencia }}</h2>
        <p class="evidencia__texto">{{ u.evidenciaAlgoritmo }}</p>
        <p class="evidencia__aviso">{{ t.evidenciaAviso }}</p>
      </section>
    }
    <section aria-labelledby="titulo-historial">
      <h2 id="titulo-historial">{{ t.historial }}</h2>
      <ol class="decisiones">
        @for (d of decisiones(); track d.id) {
          <li>
            <header>
              <h3>{{ d.fase === 'RESOLUCION' ? t.faseResolucion : t.fasePropuesta }}</h3>
              <ap-chip-estado [tono]="d.decision === 'ACEPTAR' ? 'ok' : 'error'">{{ d.decision === 'ACEPTAR' ? t.aceptar : t.rechazar }}</ap-chip-estado>
            </header>
            <p class="motivo">{{ d.motivo }}</p>
            <p class="meta">
              {{ t.revision }} {{ d.revision }} · {{ t.decidio }} {{ d.fase === 'RESOLUCION' ? t.porBackoffice : t.porAdministrador }} ·
              <time [attr.datetime]="d.ocurridaEn">{{ d.ocurridaEn | date: 'dd/MM/yyyy HH:mm' : '-0400' }} (La Paz)</time>
            </p>
            @if (d.fase === 'RESOLUCION' && d.decision === 'ACEPTAR') {
              <ap-alerta tono="info">{{ t.reservaCupos }}</ap-alerta>
            }
          </li>
        }
      </ol>
    </section>
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: var(--s5); }
    h2 { margin: 0 0 var(--s3); font-size: 1.1rem; }
    h3 { margin: 0; font-size: 1rem; }
    .evidencia { padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sh-1); }
    .evidencia h2 { margin-bottom: var(--s2); }
    .evidencia p { margin: 0; }
    .evidencia__aviso { margin-top: var(--s2); color: var(--text-2); font-size: .875rem; }
    .decisiones { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--s3); }
    .decisiones li { padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); display: flex; flex-direction: column; gap: var(--s2); }
    header { display: flex; justify-content: space-between; align-items: center; gap: var(--s3); flex-wrap: wrap; }
    .motivo { margin: 0; color: var(--text); overflow-wrap: anywhere; }
    .meta { margin: 0; color: var(--text-2); font-size: .875rem; }
  `,
})
export class HistorialDeAdmision {
  protected readonly t = textosAdmisiones
  readonly decisiones = input.required<readonly DecisionDeIngreso[]>()
  protected readonly ultima = computed(() => this.decisiones().at(-1) ?? null)
}
