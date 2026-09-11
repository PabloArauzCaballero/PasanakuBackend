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
  imports: [SimuladorDeCostos],
  template: `
    <main>
      <h1>Tarifas y comisiones</h1>
      <p class="bajada">
        Cotizá cuánto cobra AportaYa por una operación, con impuestos incluidos, antes de hacerla. El tarifario completo
        está sujeto a preaviso cuando sube (CU-34): si cambia, se te avisa con anticipación y con la fecha desde la que rige.
      </p>
      @defer (hydrate on interaction) {
        <ap-simulador-de-costos />
      } @placeholder {
        <button type="button">Cotizar una comisión</button>
      }
    </main>
  `,
  styles: `
    main { max-width: 70ch; margin: 0 auto; padding: var(--s6) var(--s5); }
    h1 { font-size: 2.25rem; margin-bottom: var(--s3); }
    .bajada { color: var(--text-2); font-size: 1.125rem; margin-bottom: var(--s5); }
    button { min-height: var(--area-tactil); padding: 0 var(--s5); border: 0; border-radius: var(--r-md); background: var(--accent); color: var(--accent-ink); font: inherit; font-weight: 600; }
  `,
})
export class Tarifas {}
