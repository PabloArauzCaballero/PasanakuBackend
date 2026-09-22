import { CabeceraDePagina } from '../../layout/cabecera-de-pagina'
import { ChangeDetectionStrategy, Component } from '@angular/core'
import { CalculadoraDePlazo } from '../../verificadores/calculadora-de-plazo'

/** Ruta en servidor (consulta un servicio en vivo); el verificador se hidrata al verse. */
@Component({
  selector: 'ap-plazos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, CalculadoraDePlazo],
  template: `
    <main id="contenido">
      <ap-cabecera-de-pagina etiqueta="Plazos" titulo="¿Cuándo vence un plazo de 5 días hábiles?" />
      <div class="contenedor contenedor--angosto cuerpo-pagina">
        <p class="texto-guia" data-tutorial-id="plazos-por-que">Los plazos de reclamo y de descargo se cuentan en días hábiles administrativos de Bolivia: los feriados no cuentan. Esta calculadora usa el mismo calendario que la plataforma.</p>
        <div data-tutorial-id="plazos-calculadora">
          @defer (hydrate on viewport) {
            <ap-calculadora-de-plazo />
          } @placeholder {
            <p role="status">Cargando la calculadora…</p>
          }
        </div>
      </div>
    </main>
  `,
})
export class Plazos {}
