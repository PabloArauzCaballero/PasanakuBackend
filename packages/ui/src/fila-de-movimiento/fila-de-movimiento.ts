import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { Fecha } from '../fecha/fecha'
import { Icono } from '../icono/icono'
import { Monto } from '../monto/monto'
import { ICONO_DE_MOVIMIENTO, NOMBRE_DE_MOVIMIENTO, TipoDeMovimiento } from '../tipo-de-movimiento/tipo-de-movimiento'

export type Movimiento = { id: string; tipo: TipoDeMovimiento; concepto: string; monto: string; moneda: string; fechaIso: string; saldoCorrido?: string }

/** Una línea del extracto: ícono tipado, concepto, hora, monto con signo y saldo corrido. */
@Component({
  selector: 'ap-fila-de-movimiento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Fecha, Icono, Monto],
  host: { '[class.negativo]': 'negativo()' },
  template: `
    <span class="icono"><ap-icono [nombre]="icono()" [etiqueta]="nombre()" tamano="s5" /></span>
    <span class="cuerpo">
      <span class="concepto">{{ m().concepto }}</span>
      <ap-fecha [iso]="m().fechaIso" />
    </span>
    <span class="cifras">
      <ap-monto [monto]="m().monto" [moneda]="m().moneda" [etiqueta]="nombre()" [class.negativo]="negativo()" />
      @if (m().saldoCorrido) { <small>Saldo <ap-monto [monto]="m().saldoCorrido!" [moneda]="m().moneda" etiqueta="Saldo después del movimiento" /></small> }
    </span>
  `,
  styles: `
    :host { display: flex; align-items: center; gap: var(--s3); min-height: calc(var(--s7) + var(--s2)); padding: var(--s2) 0; border-bottom: var(--borde-fino) solid var(--border); }
    .icono { display: inline-flex; align-items: center; justify-content: center; width: calc(var(--s6) + var(--s2)); height: calc(var(--s6) + var(--s2)); flex: none; border-radius: var(--r-pill); background: var(--g100); color: var(--brand-ink); }
    :host(.negativo) .icono { background: var(--surface-2); color: var(--text-2); }
    .cuerpo { flex: 1; min-width: 0; display: flex; flex-direction: column; }
    .concepto { color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .cifras { display: flex; flex-direction: column; align-items: flex-end; flex: none; }
    ap-monto.negativo { color: var(--text); }
    ap-monto:not(.negativo) { color: var(--ok-texto); }
    small { color: var(--text-3); font-size: .8em; }
  `,
})
export class FilaDeMovimiento {
  readonly m = input.required<Movimiento>()
  readonly negativo = computed(() => this.m().monto.startsWith('-'))
  readonly icono = computed(() => ICONO_DE_MOVIMIENTO[this.m().tipo])
  readonly nombre = computed(() => NOMBRE_DE_MOVIMIENTO[this.m().tipo])
}
