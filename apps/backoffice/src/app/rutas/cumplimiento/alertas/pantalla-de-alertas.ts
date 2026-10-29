import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { PanelDeFactores } from '@aportaya/ui/panel-de-factores/panel-de-factores'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { factoresDeEstimaciones, factoresDeHechos, type AlertaDeRiesgo } from '../dominio/cu97-alertas'
import { textosCumplimiento } from '../textos'

/**
 * CU-97 · Anticipar el riesgo con alertas tempranas.
 */
@Component({
  selector: 'ap-pantalla-de-alertas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, PanelDeFactores],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" />
      <p class="aviso">{{ t.aviso }}</p>

      <section aria-labelledby="hechos">
        <h2 id="hechos">{{ t.hechos }}</h2>
        <ap-panel-de-factores [veredicto]="t.hechos + ': ' + t.veredicto" [factores]="factoresDeHechos(alerta())" />
      </section>

      <section aria-labelledby="estimaciones">
        <h2 id="estimaciones">{{ t.estimaciones }}</h2>
        <ap-panel-de-factores [veredicto]="t.estimaciones + ': ' + t.veredicto" [factores]="factoresDeEstimaciones(alerta())" />
      </section>
    </section>
  `,
  styles: `
    .pantalla { max-width: 40rem; display: flex; flex-direction: column; gap: var(--s5); }
    .aviso { color: var(--text-2); }
    h2 { font-size: 1em; color: var(--text-2); margin-bottom: var(--s2); }
  `,
})
export class PantallaDeAlertas {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  protected readonly t = textosCumplimiento.alertas
  protected readonly factoresDeHechos = factoresDeHechos
  protected readonly factoresDeEstimaciones = factoresDeEstimaciones
  readonly alerta = input.required<AlertaDeRiesgo>()
}
