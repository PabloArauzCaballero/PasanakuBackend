import { CabeceraDePagina } from '../../layout/cabecera-de-pagina'
import { ChangeDetectionStrategy, Component } from '@angular/core'
import { SimuladorDeCostos } from '../../verificadores/simulador-de-costos'

/**
 * CU-30/CU-34 — Tarifario vigente. `RenderMode.Server` (planes/14 F9.1): la página
 * en sí es liviana; el simulador que sí consulta el gateway va detrás de
 * `@defer (hydrate on interaction)`.
 *
 * **Hueco declarado:** el listado completo del tarifario vigente —cada concepto,
 * su método de cálculo, piso y techo (CU-34)— no tiene hoy un `GET` público en
 * `openapi/tarifas.yaml`: existe `GET /tarifas/vigentes/{codigo}` (devuelve solo
 * `{vigente, tarifarioId}`, sin el detalle) y `POST /comisiones/cotizaciones`
 * (cotiza una operación puntual, que es lo que expone el simulador de abajo). Mostrar
 * una tabla de conceptos inventada violaría la regla de "no se publica lo que no es
 * cierto hoy" (planes/14, regla 2). Pedido al carril de backend de `servicios/tarifas`:
 * un `GET /tarifas/vigentes/{codigo}/detalle` con los conceptos, para reemplazar este
 * simulador por la tabla completa que pide `planes/14` F9.1.
 */
@Component({
  selector: 'ap-tarifas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'pagina' },
  imports: [CabeceraDePagina, SimuladorDeCostos],
  template: `
    <main id="contenido">
      <ap-cabecera-de-pagina etiqueta="Tarifas" titulo="Tarifas y comisiones" />
      <div class="contenedor contenedor--angosto cuerpo-pagina">
        <p class="texto-guia" data-tutorial-id="tarifas-preaviso">Cotizá cuánto cobra AportaYa por una operación, con impuestos incluidos, antes de hacerla. El tarifario completo está sujeto a preaviso cuando sube (CU-34): si cambia, se te avisa con anticipación y con la fecha desde la que rige.</p>
        <div data-tutorial-id="tarifas-cotizador">
          @defer (hydrate on interaction) {
            <ap-simulador-de-costos />
          } @placeholder {
            <button type="button" class="boton boton--principal">Cotizar una comisión</button>
          }
        </div>
      </div>
    </main>
  `,
})
export class Tarifas {}
