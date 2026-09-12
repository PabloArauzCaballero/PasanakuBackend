import { ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { Campo } from '@aportaya/ui/campo/campo'
import { Monto } from '@aportaya/ui/monto/monto'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { recursoDeConsumo } from '../dominio/cu113-cu114-consumo'
import { textosPublicidad } from '../textos'

/**
 * CU-113 · Panel de desempeño (lectura). **Gate del carril**: usa exactamente el mismo
 * `recursoDeConsumo` (dominio compartido) que `PantallaDeLiquidacion` — un solo origen
 * de dato para las dos pantallas, nunca dos cálculos separados. Todo importe pasa por
 * `ap-monto`.
 */
@Component({
  selector: 'ap-pantalla-de-desempeno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, Campo, Monto, EstadoDePantalla],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
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
    </main>
  `,
  styles: `
    main { padding: var(--s5); max-width: 48rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
    .filtros { display: flex; flex-wrap: wrap; align-items: flex-end; gap: var(--s3); }
    .resultado { display: grid; gap: var(--s2); }
    .etiqueta { color: var(--text-2); margin: 0; }
    .nota { font-size: .85em; color: var(--text-2); }
  `,
})
export class PantallaDeDesempeno {
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
