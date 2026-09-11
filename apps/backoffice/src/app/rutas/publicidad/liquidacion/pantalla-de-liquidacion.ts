import { ChangeDetectionStrategy, Component, signal } from '@angular/core'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { Boton } from '@aportaya/ui/boton/boton'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { Campo } from '@aportaya/ui/campo/campo'
import { Monto } from '@aportaya/ui/monto/monto'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { accionesDeLiquidacion, recursoDeConsumo } from '../dominio/cu113-cu114-consumo'
import { textosPublicidad } from '../textos'

/**
 * CU-114 · Liquidación. **Gate del carril**: consulta el mismo `recursoDeConsumo` que
 * `PantallaDeDesempeno` antes de liquidar — el monto que se factura es el mismo que ya
 * se le mostró al operador en desempeño, nunca un cálculo aparte. `liquidarPeriodo`
 * exige `EntradaLiquidacion.periodo`; `facturaElectronicaId` y `cuentaPorCobrarId`
 * quedan nulos (declarado en el propio contrato: los emite otro módulo por fuera de
 * esta transacción, ver la nota de divergencias en `publicidad.yaml`).
 */
@Component({
  selector: 'ap-pantalla-de-liquidacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, Boton, Dialogo, Campo, Monto, EstadoDePantalla],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <main>
      <h1>{{ t.titulo }}</h1>
      <div class="filtros">
        <ap-campo [etiqueta]="t.cuenta" [(valor)]="cuentaId" />
        <ap-campo [etiqueta]="t.periodo" [(valor)]="periodo" marcador="2026-09" />
        <ap-boton (pulsado)="buscar()">{{ 'Consultar' }}</ap-boton>
      </div>

      <ap-estado-de-pantalla [recurso]="consumo" [mensajeVacio]="'Consultá una cuenta y un período.'" [etiquetaDeCarga]="t.cargando" (reintentar)="consumo.reload()">
        @if (consumo.value(); as c) {
          <section class="resultado">
            <p class="etiqueta">{{ t.totalAFacturar }}</p>
            <ap-monto [monto]="c.total" moneda="BOB" />
            @if (c.total === '0.00') {
              <p class="nota">{{ t.sinConsumo }}</p>
            } @else {
              <ap-boton (pulsado)="abrirDialogo()">{{ t.liquidar }}</ap-boton>
            }
          </section>
        }
      </ap-estado-de-pantalla>
    </main>

    <ap-dialogo [titulo]="t.liquidar" [textoDeConfirmar]="t.liquidar" [abierto]="dialogoAbierto()" [cargando]="liquidando()" (abiertoChange)="dialogoAbierto.set($event)" (confirmar)="confirmarLiquidar()" (cancelar)="dialogoAbierto.set(false)">
      <p>{{ t.confirmarLiquidar(periodo()) }}</p>
    </ap-dialogo>
  `,
  styles: `
    main { padding: var(--s5); max-width: 48rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
    .filtros { display: flex; flex-wrap: wrap; align-items: flex-end; gap: var(--s3); }
    .resultado { display: grid; gap: var(--s3); }
    .etiqueta { color: var(--text-2); margin: 0; }
    .nota { font-size: .85em; color: var(--text-2); }
  `,
})
export class PantallaDeLiquidacion {
  private readonly acciones = accionesDeLiquidacion()
  protected readonly t = textosPublicidad.liquidacion
  protected readonly cuentaId = signal('')
  protected readonly periodo = signal('')
  private readonly cuentaConsultada = signal<string | undefined>(undefined)
  private readonly periodoConsultado = signal<string | undefined>(undefined)
  protected readonly consumo = recursoDeConsumo(() => this.cuentaConsultada(), () => this.periodoConsultado())

  protected readonly dialogoAbierto = signal(false)
  protected readonly liquidando = signal(false)

  buscar(): void {
    this.cuentaConsultada.set(this.cuentaId() || undefined)
    this.periodoConsultado.set(this.periodo() || undefined)
  }

  abrirDialogo(): void {
    this.dialogoAbierto.set(true)
  }

  async confirmarLiquidar(): Promise<void> {
    this.liquidando.set(true)
    try {
      await this.acciones.liquidar(this.cuentaId(), { periodo: this.periodo() })
      this.dialogoAbierto.set(false)
      this.consumo.reload()
    } finally {
      this.liquidando.set(false)
    }
  }
}
