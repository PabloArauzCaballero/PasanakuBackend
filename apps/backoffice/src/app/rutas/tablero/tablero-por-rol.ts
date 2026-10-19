import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { SelectorSegmentado } from '@aportaya/ui/selector-segmentado/selector-segmentado'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import { ExplicadorDeVista } from '../../nucleo/explicador-de-vista'
import {
  ESPERADOS_COMERCIAL,
  ESPERADOS_FINANCIERO,
  ESPERADOS_SISTEMAS,
  embudo,
  periodoCerrado,
  recursoTablero,
  tarjetas,
  type Granularidad,
  type SalidaTablero,
  type Tarjeta,
} from './dominio/indicadores'
import { TablaEndpointsDelDia } from './tabla-endpoints-del-dia'
import { textosTablero } from './textos'

export type TipoDeTablero = 'financiero' | 'comercial' | 'sistemas'

/**
 * Tableros por rol (H4.S2 · H5.S2) · el tablero financiero o el comercial, con los indicadores que publica CU-98
 * (`GET /indicadores`). La ruta la protege `requierePermiso` y el servidor filtra las familias por permiso: esta
 * pantalla oculta, no protege.
 *
 * Abre en el último período CERRADO (ayer, el mes pasado, el año pasado): los en curso no tienen valor guardado.
 * Un indicador que no llegó se muestra «sin datos»; uno suprimido por privacidad, sin valor y con el motivo. Nunca un
 * cero que nadie midió.
 */
@Component({
  selector: 'ap-tablero-por-rol',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, ChipEstado, EstadoDePantalla, SelectorSegmentado, TablaEndpointsDelDia, TarjetaKPI],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="encabezado().titulo" [descripcion]="encabezado().proposito" [icono]="icono()" />

      <form class="periodo" (submit)="$event.preventDefault()">
        <ap-selector-segmentado [etiqueta]="t.granularidad" [segmentos]="t.segmentos" [(elegido)]="granularidad" />
        <div class="campo">
          <label for="tablero-periodo">{{ t.campo[granularidadElegida()] }}</label>
          @switch (granularidadElegida()) {
            @case ('dia') {
              <input id="tablero-periodo" type="date" [value]="periodo()" [max]="maximo()" (change)="elegir($any($event.target).value)" />
            }
            @case ('mes') {
              <input id="tablero-periodo" type="month" [value]="periodo()" [max]="maximo()" (change)="elegir($any($event.target).value)" />
            }
            @case ('anio') {
              <input id="tablero-periodo" type="number" inputmode="numeric" [value]="periodo()" [max]="maximo()" (change)="elegir($any($event.target).value)" />
            }
          }
        </div>
      </form>
      @if (enCurso()) { <p role="status" class="aviso">{{ t.enCurso }}</p> }

      <ap-estado-de-pantalla
        [recurso]="tablero"
        [vacio]="sinIndicadores"
        [mensajeVacio]="t.vacio"
        [etiquetaDeCarga]="t.cargando"
        (reintentar)="tablero.reload()"
      >
        @if (salida()?.provisorio) { <p role="status" class="aviso">{{ t.provisorio }}</p> }

        @if (tipo() === 'comercial') {
          <section class="bloque" aria-labelledby="tablero-embudo">
            <h2 id="tablero-embudo">{{ t.embudo.titulo }}</h2>
            <p class="explicacion">{{ t.embudo.explicacion }}</p>
            <ol class="embudo">
              @for (paso of pasos(); track paso.nombre) {
                <li>
                  <span class="paso">{{ paso.nombre }}</span>
                  <span class="barra" aria-hidden="true"><span class="relleno" [style.width.%]="paso.ancho"></span></span>
                  <span class="cifra">{{ paso.texto }}</span>
                </li>
              }
            </ol>
          </section>
        }

        @for (g of grupos(); track g.nombre) {
          <section class="bloque" [attr.aria-label]="g.nombre">
            <h2>{{ g.nombre }}</h2>
            <ul class="grilla">
              @for (k of g.tarjetas; track k.codigo) {
                <li [attr.data-indicador]="k.codigo">
                  @switch (k.estado) {
                    @case ('valor') {
                      <ap-tarjeta-kpi [etiqueta]="k.nombre" [valor]="k.moneda ? k.valor! : k.texto!" [moneda]="k.moneda" [variacion]="k.variacion" />
                      @if (k.casos !== undefined) { <p class="casos">{{ t.casos(k.casos) }}</p> }
                    }
                    @case ('suprimido') {
                      <div class="sin-valor" role="group" [attr.aria-label]="k.nombre">
                        <p class="rotulo">{{ k.nombre }}</p>
                        <ap-chip-estado tono="neutro">{{ t.suprimido }}</ap-chip-estado>
                        <p class="explicacion">{{ t.suprimidoExplicacion(k.minimoCasos) }}</p>
                      </div>
                    }
                    @default {
                      <div class="sin-valor" role="group" [attr.aria-label]="k.nombre">
                        <p class="rotulo">{{ k.nombre }}</p>
                        <ap-chip-estado tono="neutro">{{ t.sinDatos }}</ap-chip-estado>
                        <p class="explicacion">{{ t.sinDatosExplicacion }}</p>
                      </div>
                    }
                  }
                </li>
              }
            </ul>
          </section>
        }
        <p class="explicacion">{{ t.calculo }}</p>
      </ap-estado-de-pantalla>
      @if (tipo() === 'sistemas') { <ap-tabla-endpoints-del-dia /> }
    </section>
  `,
  styles: `
    .pantalla > * { min-width: 0; }
    .pantalla { padding: var(--s5); display: grid; gap: var(--s5); }
    h1, h2 { margin: 0; }
    h2 { font-size: 1.15em; }
    .periodo { display: flex; flex-wrap: wrap; align-items: end; gap: var(--s4); }
    .campo { display: grid; gap: var(--s1); font-weight: 600; color: var(--text-2); }
    .campo input { min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-sm); background: var(--surface); color: var(--text); font: inherit; }
    .campo input:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .aviso, .explicacion, .casos { margin: 0; color: var(--text-2); }
    .casos { margin-top: var(--s1); font-size: .85em; }
    .bloque { display: grid; gap: var(--s3); }
    .grilla { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(15rem, 1fr)); gap: var(--s4); }
    .sin-valor { display: grid; gap: var(--s2); align-content: start; height: 100%; box-sizing: border-box; padding: var(--s4); border: var(--borde-fino) dashed var(--border); border-radius: var(--r-lg); background: var(--surface); }
    .sin-valor p { margin: 0; }
    .rotulo { color: var(--text-2); font-weight: 600; font-size: .9em; }
    .embudo { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--s3); }
    .embudo li { display: grid; grid-template-columns: minmax(10rem, 14rem) 1fr auto; align-items: center; gap: var(--s3); }
    .barra { display: block; height: var(--s3); border-radius: var(--r-sm); background: var(--surface-2); overflow: hidden; }
    .relleno { display: block; height: 100%; background: var(--verde-solido); }
    .cifra { font-variant-numeric: tabular-nums; font-weight: 600; color: var(--text); }
    @media (max-width: 40rem) { .embudo li { grid-template-columns: 1fr auto; } .barra { grid-column: 1 / -1; grid-row: 2; } }
  `,
})
export class TableroPorRol {
  /** Lo fija la ruta (`data: { tipo }`): una sola pantalla para los dos tableros, no dos copias. */
  readonly tipo = input.required<TipoDeTablero>()

  protected readonly t = textosTablero.porRol
  private readonly hoy = new Date()
  private readonly explicador = inject(ExplicadorDeVista)
  protected readonly icono = computed(() => {
    switch (this.tipo()) {
      case 'financiero': return 'comision' as const
      case 'sistemas': return 'baseDeDatos' as const
      case 'comercial': return 'grupo' as const
    }
  })

  constructor() {
    effect(() => this.explicador.fijar(this.encabezado().proposito))
  }

  protected readonly granularidad = signal<string>('mes')
  protected readonly granularidadElegida = computed(() => this.granularidad() as Granularidad)
  private readonly elegido = signal<Partial<Record<Granularidad, string>>>({})

  /** El período pedido: el que eligió la persona para esa granularidad, o el último cerrado. */
  protected readonly periodo = computed(
    () => this.elegido()[this.granularidadElegida()] ?? periodoCerrado(this.hoy, this.granularidadElegida()),
  )
  protected readonly maximo = computed(() => periodoCerrado(this.hoy, this.granularidadElegida()))
  protected readonly enCurso = computed(() => this.periodo() > this.maximo())

  protected readonly tablero = recursoTablero(this.periodo)
  protected readonly sinIndicadores = (s: SalidaTablero) => s.indicadores.length === 0

  protected readonly encabezado = computed(() => this.t[this.tipo()])
  /** En error, `value()` lanza: se lee solo si hay valor. */
  protected readonly salida = computed(() => (this.tablero.hasValue() ? this.tablero.value() : undefined))
  protected readonly pasos = computed(() => embudo(this.salida()))
  protected readonly grupos = computed(() => {
    const esperados = { financiero: ESPERADOS_FINANCIERO, comercial: ESPERADOS_COMERCIAL, sistemas: ESPERADOS_SISTEMAS }[this.tipo()]
    const lista = tarjetas(this.salida(), esperados)
    const nombres = [...new Set(lista.map((k) => k.grupo))]
    return nombres.map((nombre) => ({ nombre, tarjetas: lista.filter((k: Tarjeta) => k.grupo === nombre) }))
  })

  protected elegir(valor: string): void {
    if (!valor) return
    this.elegido.update((actual) => ({ ...actual, [this.granularidadElegida()]: valor }))
  }
}
