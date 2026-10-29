import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Icono } from '@aportaya/ui/icono/icono'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { ConsultaDeConsumo, type PedidoDeConsumo } from '../consulta-de-consumo'
import { recursoDeConsumo } from '../dominio/cu113-cu114-consumo'
import { textosPublicidad } from '../textos'

/**
 * CU-113 · Panel de desempeño (lectura). **Gate del carril**: usa exactamente el mismo `recursoDeConsumo` (dominio
 * compartido) que `PantallaDeLiquidacion`. `SalidaConsumo` no trae la moneda: el total se muestra como cadena, sin
 * inventar «BOB» (regla 91.1.2), y se dice.
 */
@Component({
  selector: 'ap-pantalla-de-desempeno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CabeceraDePagina, ConsultaDeConsumo, EstadoDePantalla, Icono, RouterLink, TarjetaKPI],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Publicidad" [titulo]="t.titulo" [descripcion]="t.proposito" />
      <ap-consulta-de-consumo [textoAccion]="t.buscar" (consulta)="consultar($event)" />
      <ap-estado-de-pantalla [recurso]="consumo" [mensajeVacio]="t.elegi" [etiquetaDeCarga]="t.cargando" (reintentar)="consumo.reload()">
        @if (valor(); as c) {
          <div class="resultado">
            <ap-tarjeta-kpi [etiqueta]="t.totalConsumido" [valor]="c.total" icono="moneda" [pista]="pedido()?.anunciante + ' · ' + c.periodo" />
            <p class="nota">{{ t.mismoDatoQueLiquidacion }} {{ t.sinMoneda }}</p>
            <a class="enlace" routerLink="/publicidad/liquidacion" [queryParams]="{ cuenta: pedido()?.cuenta, periodo: pedido()?.periodo }"><ap-icono nombre="recibo" tamano="s4" />{{ t.irALiquidar }}</a>
          </div>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .resultado { display: grid; gap: var(--s3); max-width: 32rem; }
    .nota { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
    .enlace { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); color: var(--brand-texto); font-weight: 600; justify-self: start; }
    .enlace:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class PantallaDeDesempeno {
  protected readonly t = textosPublicidad.desempeno
  protected readonly pedido = signal<PedidoDeConsumo | null>(null)
  protected readonly consumo = recursoDeConsumo(() => this.pedido()?.cuenta, () => this.pedido()?.periodo)
  /** En error, `value()` lanza: se lee solo si hay valor. */
  protected readonly valor = computed(() => (this.consumo.hasValue() ? this.consumo.value() : undefined))

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  /** Consultar lo mismo otra vez vuelve a pedirlo: el operador espera ver el dato de ahora. */
  protected consultar(p: PedidoDeConsumo): void {
    const igual = this.pedido()?.cuenta === p.cuenta && this.pedido()?.periodo === p.periodo
    this.pedido.set(p)
    if (igual) this.consumo.reload()
  }
}
