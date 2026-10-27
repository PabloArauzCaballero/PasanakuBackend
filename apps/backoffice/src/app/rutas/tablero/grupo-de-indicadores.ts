import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { Icono } from '@aportaya/ui/icono/icono'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import { ICONO_DE_GRUPO } from './dominio/catalogo-de-tableros'
import type { Tarjeta } from './dominio/indicadores'
import { textosTablero } from './textos'

/**
 * Un grupo de indicadores del tablero por rol, presentacional: título con su ícono y una tarjeta por indicador. Un
 * indicador con valor es una `ap-tarjeta-kpi`; uno suprimido o sin datos **no** se dibuja como cifra (borde punteado y
 * el motivo), para que nadie lea un cero que nadie midió.
 */
@Component({
  selector: 'ap-grupo-de-indicadores',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado, Icono, TarjetaKPI],
  template: `
    <section class="bloque" [attr.aria-label]="nombre()">
      <h2><ap-icono [nombre]="icono()" tamano="s5" />{{ nombre() }}</h2>
      <ul class="grilla">
        @for (k of tarjetas(); track k.codigo) {
          <li [attr.data-indicador]="k.codigo">
            @switch (k.estado) {
              @case ('valor') {
                <ap-tarjeta-kpi [etiqueta]="k.nombre" [valor]="k.moneda ? k.valor! : k.texto!" [moneda]="k.moneda" [variacion]="k.variacion" [icono]="icono()" />
                @if (k.casos !== undefined) { <p class="casos">{{ t.casos(k.casos) }}</p> }
              }
              @case ('suprimido') {
                <div class="sin-valor" role="group" [attr.aria-label]="k.nombre">
                  <p class="rotulo"><ap-icono nombre="candado" tamano="s4" />{{ k.nombre }}</p>
                  <ap-chip-estado tono="neutro">{{ t.suprimido }}</ap-chip-estado>
                  <p class="explicacion">{{ t.suprimidoExplicacion(k.minimoCasos) }}</p>
                </div>
              }
              @default {
                <div class="sin-valor" role="group" [attr.aria-label]="k.nombre">
                  <p class="rotulo"><ap-icono nombre="sinResultados" tamano="s4" />{{ k.nombre }}</p>
                  <ap-chip-estado tono="neutro">{{ t.sinDatos }}</ap-chip-estado>
                  <p class="explicacion">{{ t.sinDatosExplicacion }}</p>
                </div>
              }
            }
          </li>
        }
      </ul>
    </section>
  `,
  styles: `
    .bloque { display: grid; gap: var(--s3); }
    h2 { display: flex; align-items: center; gap: var(--s2); margin: 0; font-size: 1.15em; color: var(--text); }
    .grilla { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 15rem), 1fr)); gap: var(--s4); }
    .sin-valor { display: grid; gap: var(--s2); align-content: start; height: 100%; box-sizing: border-box; padding: var(--s4); border: var(--borde-fino) dashed var(--border); border-radius: var(--r-lg); background: var(--surface); }
    .sin-valor p, .casos { margin: 0; }
    .rotulo { display: flex; align-items: center; gap: var(--s1); color: var(--text-2); font-weight: 600; font-size: .9em; }
    .explicacion, .casos { color: var(--text-2); }
    .casos { margin-top: var(--s1); font-size: .85em; }
  `,
})
export class GrupoDeIndicadores {
  protected readonly t = textosTablero.porRol
  readonly nombre = input.required<string>()
  readonly tarjetas = input.required<readonly Tarjeta[]>()
  protected readonly icono = computed(() => ICONO_DE_GRUPO[this.nombre()] ?? 'grafico')
}
