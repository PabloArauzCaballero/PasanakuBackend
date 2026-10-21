import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { CabeceraDeSeccion } from '@aportaya/ui/cabecera-de-seccion/cabecera-de-seccion'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import type { TablaDelCatalogo } from 'clientes/angular/auditoria'
import { textosCatalogo } from './textos'

/**
 * Pestaña «Operaciones», la de endpoints del portal de Atlas: qué casos de uso nombran la tabla y con qué endpoints se
 * ejecutan. No se infiere qué endpoint escribe la tabla: el generador encadena el `[[tabla]]` de cada caso de uso con
 * la matriz de endpoints, y la pantalla lo dice así.
 */
@Component({
  selector: 'ap-vista-operaciones',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDeSeccion, EstadoVacio],
  template: `
    <section class="operaciones">
      <ap-cabecera-de-seccion [titulo]="t.titulo" [descripcion]="t.descripcion" />
      @if (operaciones().length) {
        <ul class="casos" [attr.aria-label]="t.titulo">
          @for (o of operaciones(); track o.casoDeUso) {
            <li class="caso">
              <h3><span class="codigo">{{ o.casoDeUso }}</span> {{ o.titulo }}</h3>
              @if (o.endpoints.length) {
                <ul class="endpoints" [attr.aria-label]="t.registros + ' de ' + o.casoDeUso">
                  @for (e of o.endpoints; track e.operationId) {
                    <li>
                      <code class="metodo">{{ e.metodo }}</code>
                      <span class="destino"><code class="ruta">{{ e.ruta }}</code><span class="detalle">{{ e.operationId }} · {{ e.servicio }}</span></span>
                    </li>
                  }
                </ul>
              } @else {
                <p class="tenue">{{ t.sinEndpoint }}</p>
              }
            </li>
          }
        </ul>
      } @else {
        <ap-estado-vacio [titulo]="t.vacio" [explicacion]="t.vacioExplicacion" />
      }
    </section>
  `,
  styles: `
    .operaciones { display: grid; gap: var(--s3); grid-template-columns: minmax(0, 1fr); min-width: 0; }
    .casos { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fill, minmax(min(100%, 26rem), 1fr)); }
    .caso { display: grid; align-content: start; gap: var(--s2); min-width: 0; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    h3 { margin: 0; font: var(--t-cuerpo-fuerte); color: var(--text); overflow-wrap: anywhere; }
    .codigo { font-family: var(--mono); color: var(--brand-texto); }
    .endpoints { list-style: none; margin: 0; padding: 0; display: grid; }
    .endpoints li { display: grid; grid-template-columns: auto minmax(0, 1fr); gap: var(--s2); align-items: baseline; padding: var(--s2) 0; border-top: var(--borde-fino) solid var(--border); }
    code { font-family: var(--mono); font-size: .92em; }
    .metodo { padding: 0 var(--s1); border: var(--borde-fino) solid var(--border); border-radius: var(--r-sm); background: var(--surface-2); }
    .destino { display: grid; gap: calc(var(--s1) / 2); min-width: 0; }
    .ruta { overflow-wrap: anywhere; color: var(--text); }
    .detalle { color: var(--text-2); font: var(--t-ayuda); overflow-wrap: anywhere; }
    .tenue { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
  `,
})
export class VistaOperaciones {
  readonly tabla = input.required<TablaDelCatalogo>()
  protected readonly t = textosCatalogo.ficha.operaciones
  protected readonly operaciones = computed(() => this.tabla().operaciones ?? [])
}
