import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Fecha } from '../fecha/fecha'

export type Etapa = { nombre: string; desdeIso?: string; descripcion: string }

/** Las etapas de cobranza en orden, con la actual marcada y las pasadas con su fecha. */
@Component({
  selector: 'ap-escalera-de-etapas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Fecha],
  template: `
    <ol [attr.aria-label]="lectura()">
      @for (e of etapas(); track e.nombre; let i = $index) {
        <li [class.pasada]="i < actual()" [class.actual]="i === actual()" [attr.aria-current]="i === actual() ? 'step' : null">
          <span class="marca" aria-hidden="true"></span>
          <div>
            <p class="nombre">{{ e.nombre }}</p>
            <p class="descripcion">{{ e.descripcion }}</p>
            @if (e.desdeIso) { <ap-fecha [iso]="e.desdeIso" /> }
          </div>
        </li>
      }
    </ol>
  `,
  styles: `
    ol { margin: 0; padding: 0; list-style: none; }
    li { position: relative; display: flex; gap: var(--s3); padding: 0 0 var(--s4) 0; color: var(--text-3); }
    li::before { content: ''; position: absolute; left: calc(var(--s3) - var(--borde-fino)); top: var(--s5); bottom: 0; width: var(--borde-desfase); background: var(--border); }
    li:last-child::before { display: none; }
    .marca { width: var(--s5); height: var(--s5); flex: none; border-radius: var(--r-pill); border: var(--borde-desfase) solid var(--border); background: var(--surface); }
    .pasada .marca { background: var(--ok); border-color: var(--ok); }
    .actual .marca { background: var(--verde-solido); border-color: var(--verde-solido); box-shadow: 0 0 0 var(--borde-foco) var(--brand-bg); }
    .actual { color: var(--text); }
    .pasada { color: var(--text-2); }
    p { margin: 0; }
    .nombre { font-weight: 600; }
    .descripcion { font-size: .9em; }
  `,
})
export class EscaleraDeEtapas {
  readonly etapas = input.required<Etapa[]>()
  readonly actual = input.required<number>()
  readonly lectura = computed(() => `Etapa ${this.actual() + 1} de ${this.etapas().length}: ${this.etapas()[this.actual()]?.nombre ?? ''}`)
}
