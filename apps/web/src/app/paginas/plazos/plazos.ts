import { ChangeDetectionStrategy, Component } from '@angular/core'
import { CalculadoraDePlazo } from '../../verificadores/calculadora-de-plazo'

/** Ruta en servidor (consulta un servicio en vivo); el verificador se hidrata al verse. */
@Component({
  selector: 'ap-plazos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CalculadoraDePlazo],
  template: `
    <main>
      <h1>¿Cuándo vence un plazo de 5 días hábiles?</h1>
      <p>Los plazos de reclamo y de descargo se cuentan en días hábiles administrativos de Bolivia: los feriados no cuentan. Esta calculadora usa el mismo calendario que la plataforma.</p>
      @defer (hydrate on viewport) {
        <ap-calculadora-de-plazo />
      } @placeholder {
        <p role="status">Cargando la calculadora…</p>
      }
    </main>
  `,
  styles: `main { max-width: 70ch; margin: 0 auto; padding: var(--s6) var(--s5); }`,
})
export class Plazos {}
