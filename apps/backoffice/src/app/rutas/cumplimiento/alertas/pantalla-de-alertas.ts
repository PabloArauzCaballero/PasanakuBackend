import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { PanelDeFactores } from '@aportaya/ui/panel-de-factores/panel-de-factores'
import { factoresDeEstimaciones, factoresDeHechos, type AlertaDeRiesgo } from '../dominio/cu97-alertas'
import { textosCumplimiento } from '../textos'

/**
 * CU-97 · Anticipar el riesgo con alertas tempranas. Una alerta es una razón para
 * ACOMPAÑAR, nunca una condena: la pantalla separa, en dos `PanelDeFactores`
 * distintos, los hechos ya registrados de las estimaciones del modelo, y nunca los
 * mezcla en un solo veredicto de "probablemente incumplirá".
 */
@Component({
  selector: 'ap-pantalla-de-alertas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, PanelDeFactores],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <p class="aviso">{{ t.aviso }}</p>

      <section aria-labelledby="hechos">
        <h2 id="hechos">{{ t.hechos }}</h2>
        <ap-panel-de-factores [veredicto]="t.hechos + ': ' + t.veredicto" [factores]="factoresDeHechos(alerta())" />
      </section>

      <section aria-labelledby="estimaciones">
        <h2 id="estimaciones">{{ t.estimaciones }}</h2>
        <ap-panel-de-factores [veredicto]="t.estimaciones + ': ' + t.veredicto" [factores]="factoresDeEstimaciones(alerta())" />
      </section>
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 40rem; display: flex; flex-direction: column; gap: var(--s5); }
    .aviso { color: var(--text-2); }
    h2 { font-size: 1em; color: var(--text-2); margin-bottom: var(--s2); }
  `,
})
export class PantallaDeAlertas {
  protected readonly t = textosCumplimiento.alertas
  protected readonly factoresDeHechos = factoresDeHechos
  protected readonly factoresDeEstimaciones = factoresDeEstimaciones
  readonly alerta = input.required<AlertaDeRiesgo>()
}
