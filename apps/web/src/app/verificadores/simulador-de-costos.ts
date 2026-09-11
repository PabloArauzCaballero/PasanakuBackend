import { httpResource } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core'
import { EntradaCotizacionReferenciaTipoEnum, type SalidaCotizacion } from 'clientes/angular/tarifas'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { GATEWAY } from '../nucleo/gateway'

const HECHOS = ['ENTREGA_FONDO', 'ORDEN_RECARGA', 'ORDEN_RETIRO'] as const

/**
 * CU-30 — Cotizar la comisión antes de operar, en `/tarifas`.
 *
 * Simulador público: cotiza contra el mismo endpoint que usa la app, pero sin
 * sesión ni referencia real a una operación (`referenciaId` es un identificador de
 * simulación, no una operación que exista — **supuesto declarado**: CU-30 no
 * contempla explícitamente un modo "simulación pública sin referencia"; se generó
 * un UUID sintético para no bloquear la publicación del simulador. Si el backend
 * de tarifas rechaza referencias inexistentes, hace falta un modo `simulacion:true`
 * en el contrato — pedido al carril dueño de `servicios/tarifas`).
 */
@Component({
  selector: 'ap-simulador-de-costos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EstadoDePantalla],
  template: `
    <form (submit)="$event.preventDefault(); cotizar()">
      <label for="hecho">Qué vas a hacer</label>
      <select id="hecho" [value]="hecho()" (change)="hecho.set(($any($event.target)).value)">
        @for (h of hechos; track h) { <option [value]="h">{{ etiquetaDe(h) }}</option> }
      </select>
      <label for="monto">Monto (Bs)</label>
      <input id="monto" type="number" min="1" step="0.01" [value]="monto()" (input)="monto.set(($any($event.target)).value)" />
      <button type="submit">Cotizar</button>
    </form>
    <ap-estado-de-pantalla [recurso]="cotizacion" mensajeVacio="Elegí una operación y un monto para ver la comisión." etiquetaDeCarga="Cotizando" (reintentar)="cotizacion.reload()">
      @if (cotizacion.hasValue() && cotizacion.value(); as c) {
        <p class="resultado" aria-live="polite">
          Comisión: Bs {{ c.montoComision.monto }} + Bs {{ c.montoImpuesto.monto }} de impuesto = <strong>Bs {{ c.montoTotal.monto }}</strong> (impuestos incluidos).
        </p>
        <ul class="desglose">
          @for (linea of c.desglose; track linea.concepto) { <li>{{ linea.concepto }}: {{ linea.detalle }} — Bs {{ linea.monto.monto }}</li> }
        </ul>
        <p class="ayuda">Válida hasta {{ c.validaHasta }}. Esta cotización es orientativa: el monto final se confirma antes de cada operación real.</p>
      }
    </ap-estado-de-pantalla>
  `,
  styles: `
    form { display: grid; gap: var(--s2); grid-template-columns: auto 1fr; align-items: center; margin-bottom: var(--s5); }
    select, input { min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); background: var(--field); color: var(--text); font: inherit; }
    button { grid-column: 1 / -1; min-height: var(--area-tactil); border: 0; border-radius: var(--r-md); background: var(--accent); color: var(--accent-ink); font: inherit; font-weight: 600; }
    .resultado { padding: var(--s4); background: var(--okbg); color: var(--ok-texto); border-radius: var(--r-md); }
    .desglose { margin-top: var(--s3); }
    .ayuda { color: var(--text-muted); font-size: 0.9rem; margin-top: var(--s3); }
  `,
})
export class SimuladorDeCostos {
  private readonly gateway = inject(GATEWAY)
  protected readonly hechos = HECHOS
  protected readonly hecho = signal<(typeof HECHOS)[number]>('ORDEN_RECARGA')
  protected readonly monto = signal('')
  private readonly consulta = signal<{ hecho: string; monto: string } | undefined>(undefined)

  protected readonly cotizacion = httpResource<SalidaCotizacion>(() => {
    const c = this.consulta()
    if (!c) return undefined
    return {
      url: `${this.gateway}/comisiones/cotizaciones`,
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: {
        codigoTarifario: 'GENERAL',
        hechoGenerador: c.hecho,
        referenciaTipo: c.hecho as EntradaCotizacionReferenciaTipoEnum,
        referenciaId: crypto.randomUUID(),
        montoBase: { monto: c.monto, moneda: 'BOB' },
      },
    }
  })

  protected etiquetaDe(hecho: string): string {
    switch (hecho) {
      case 'ENTREGA_FONDO': return 'Cobrar mi turno'
      case 'ORDEN_RECARGA': return 'Recargar'
      case 'ORDEN_RETIRO': return 'Retirar'
      default: return hecho
    }
  }

  protected cotizar(): void {
    if (this.monto()) this.consulta.set({ hecho: this.hecho(), monto: this.monto() })
  }
}
