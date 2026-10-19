import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Campo } from '@aportaya/ui/campo/campo'
import { Monto } from '@aportaya/ui/monto/monto'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { recursoDeConsumo } from '../dominio/cu113-cu114-consumo'
import { textosPublicidad } from '../textos'

/**
 * CU-113 · Panel de desempeño (lectura). **Gate del carril**: usa exactamente el mismo
 * `recursoDeConsumo` (dominio compartido) que `PantallaDeLiquidacion`.
 */
@Component({
  selector: 'ap-pantalla-de-desempeno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CabeceraDePagina, Campo, Monto, EstadoDePantalla],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina [titulo]="t.titulo" [descripcion]="t.proposito" icono="comision" />
      <div class="filtros">
        <ap-campo [etiqueta]="t.cuenta" [(valor)]="cuentaId" />
        <ap-campo [etiqueta]="t.periodo" [(valor)]="periodo" marcador="2026-09" />
        <ap-boton (pulsado)="buscar()">{{ t.buscar }}</ap-boton>
      </div>

      <ap-estado-de-pantalla [recurso]="consumo" [mensajeVacio]="'Consultá una cuenta y un período.'" [etiquetaDeCarga]="t.cargando" (reintentar)="consumo.reload()">
        @if (consumo.value(); as c) {
          <section class="resultado">
            <p class="etiqueta">{{ t.totalConsumido }}</p>
            <ap-monto [monto]="c.total" moneda="BOB" />
            <p class="nota">{{ t.mismoDatoQueLiquidacion }}</p>
          </section>
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 48rem; display: grid; gap: var(--s4); }
    .filtros { display: flex; flex-wrap: wrap; align-items: flex-end; gap: var(--s3); }
    .resultado { display: grid; gap: var(--s2); }
    .etiqueta { color: var(--text-2); margin: 0; }
    .nota { font-size: .85em; color: var(--text-2); }
  `,
})
export class PantallaDeDesempeno {
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  protected readonly t = textosPublicidad.desempeno
  protected readonly cuentaId = signal('')
  protected readonly periodo = signal('')
  private readonly cuentaConsultada = signal<string | undefined>(undefined)
  private readonly periodoConsultado = signal<string | undefined>(undefined)
  protected readonly consumo = recursoDeConsumo(() => this.cuentaConsultada(), () => this.periodoConsultado())

  buscar(): void {
    this.cuentaConsultada.set(this.cuentaId() || undefined)
    this.periodoConsultado.set(this.periodo() || undefined)
  }
}
