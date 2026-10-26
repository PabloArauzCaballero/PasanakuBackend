import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { RouterLink } from '@angular/router'
import { Alerta } from '@aportaya/ui/alerta/alerta'
import { Boton } from '@aportaya/ui/boton/boton'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { Dialogo } from '@aportaya/ui/dialogo/dialogo'
import { ErrorDeEnvio } from '@aportaya/ui/error-de-envio/error-de-envio'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Icono } from '@aportaya/ui/icono/icono'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ConsultaDeConsumo, type PedidoDeConsumo } from '../consulta-de-consumo'
import { accionesDeLiquidacion, recursoDeConsumo } from '../dominio/cu113-cu114-consumo'
import { traducirError } from '../dominio/lotes-por-cursor'
import { textosPublicidad } from '../textos'
import type { SalidaLiquidacion } from 'clientes/angular/publicidad/model/salidaLiquidacion'
import { importeSinMoneda } from '../../operacion/comun/importe-sin-moneda'

/**
 * CU-114 · Liquidación. **Gate del carril**: consulta el mismo `recursoDeConsumo` que `PantallaDeDesempeno` antes de
 * liquidar. La factura es un registro financiero append-only: no hay editar ni eliminar, y el contrato no publica un
 * listado de liquidaciones. Antes, una liquidación rechazada (R-PUB-06: el período ya se facturó) cerraba en silencio;
 * ahora el error se queda en el diálogo y la factura generada se muestra con su moneda y su estado.
 */
@Component({
  selector: 'ap-pantalla-de-liquidacion',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Alerta, Boton, CabeceraDePagina, ConsultaDeConsumo, Dialogo, ErrorDeEnvio, EstadoDePantalla, Icono, RouterLink, TarjetaKPI],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Publicidad" [titulo]="t.titulo" [descripcion]="t.proposito" icono="recibo" />
      <ap-consulta-de-consumo textoAccion="Consultar consumo" (consulta)="consultar($event)" />
      <ap-estado-de-pantalla [recurso]="consumo" [mensajeVacio]="t.elegi" [etiquetaDeCarga]="t.cargando" (reintentar)="consumo.reload()">
        @if (valor(); as c) {
          <div class="resultado">
            <ap-tarjeta-kpi [etiqueta]="t.totalAFacturar" [valor]="cifra(c.total)" icono="moneda" [pista]="'Moneda no registrada · ' + pedido()?.anunciante + ' · ' + c.periodo" />
            <p class="nota">{{ t.sinMoneda }}</p>
            @if (factura(); as f) {
              <ap-alerta tono="ok" [titulo]="t.liquidada">{{ textoDeFactura(f) }}</ap-alerta>
            } @else if (c.total === '0.00') {
              <ap-alerta tono="info">{{ t.sinConsumo }}</ap-alerta>
            } @else {
              <ap-boton variante="primario" icono="recibo" (pulsado)="abrir()">{{ t.liquidar }}</ap-boton>
            }
            <a class="enlace" routerLink="/publicidad/desempeno" [queryParams]="{ cuenta: pedido()?.cuenta, periodo: pedido()?.periodo }"><ap-icono nombre="grafico" tamano="s4" />{{ t.verDesempeno }}</a>
          </div>
        }
      </ap-estado-de-pantalla>
    </section>

    <ap-dialogo [titulo]="t.liquidar" [textoDeConfirmar]="t.liquidar" [(abierto)]="dialogoAbierto" [cargando]="liquidando()" (confirmar)="confirmarLiquidar()" (cancelar)="dialogoAbierto.set(false)">
      <p>{{ t.confirmarLiquidar(pedido()?.periodo ?? '') }}</p>
      @if (valor(); as c) { <p><strong>{{ t.totalAFacturar }}: {{ cifra(c.total) }}</strong> · moneda no registrada</p> }
      <ap-error-de-envio [error]="error()" />
    </ap-dialogo>
  `,
  styles: `
    .pantalla { padding: var(--s5); display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .resultado { display: grid; gap: var(--s3); max-width: 32rem; }
    ap-boton { justify-self: start; }
    .nota { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
    .enlace { display: inline-flex; align-items: center; gap: var(--s1); min-height: var(--area-tactil); color: var(--brand-texto); font-weight: 600; justify-self: start; }
    .enlace:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class PantallaDeLiquidacion {
  /** El consumo no trae moneda (A-F4): la cifra agrupada y la aclaración, nunca una moneda supuesta (regla 91.1.2). */
  protected readonly cifra = importeSinMoneda
  private readonly acciones = accionesDeLiquidacion()
  protected readonly t = textosPublicidad.liquidacion
  protected readonly pedido = signal<PedidoDeConsumo | null>(null)
  protected readonly consumo = recursoDeConsumo(() => this.pedido()?.cuenta, () => this.pedido()?.periodo)
  /** En error, `value()` lanza: se lee solo si hay valor. */
  protected readonly valor = computed(() => (this.consumo.hasValue() ? this.consumo.value() : undefined))
  protected readonly dialogoAbierto = signal(false)
  protected readonly liquidando = signal(false)
  protected readonly error = signal<ErrorTraducido | null>(null)
  protected readonly factura = signal<SalidaLiquidacion | null>(null)

  protected readonly textoDeFactura = (f: SalidaLiquidacion) => this.t.facturaGenerada(f.montoTotal, f.moneda, this.t.estados[f.estado as keyof typeof this.t.estados] ?? f.estado)

  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }

  protected consultar(p: PedidoDeConsumo): void {
    const igual = this.pedido()?.cuenta === p.cuenta && this.pedido()?.periodo === p.periodo
    this.factura.set(null)
    this.pedido.set(p)
    if (igual) this.consumo.reload()
  }

  protected abrir(): void {
    this.error.set(null)
    this.dialogoAbierto.set(true)
  }

  async confirmarLiquidar(): Promise<void> {
    const p = this.pedido()
    if (!p || this.liquidando()) return
    this.liquidando.set(true)
    this.error.set(null)
    try {
      this.factura.set(await this.acciones.liquidar(p.cuenta, { periodo: p.periodo }))
      this.dialogoAbierto.set(false)
    } catch (e) {
      this.error.set(traducirError(e))
    } finally {
      this.liquidando.set(false)
    }
  }
}
