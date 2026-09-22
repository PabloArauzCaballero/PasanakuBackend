import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { FilaDeMovimiento, Movimiento } from '../fila-de-movimiento/fila-de-movimiento'
import { Monto } from '../monto/monto'

/**
 * El extracto **agrupado por día** (regla 2), con el neto del día y el saldo al cierre.
 * El neto se suma en centavos con BigInt: nunca con `number`.
 */
@Component({
  selector: 'ap-lista-de-movimientos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FilaDeMovimiento, Monto],
  template: `
    @for (g of grupos(); track g.dia) {
      <section [attr.aria-label]="g.legible">
        <div class="cabecera">
          <h3>{{ g.legible }}</h3>
          <p>Neto <ap-monto [monto]="g.neto" [moneda]="moneda()" etiqueta="Neto del día" /> @if (g.saldo) { · Saldo <ap-monto [monto]="g.saldo" [moneda]="moneda()" etiqueta="Saldo al cierre del día" class="saldo" /> }</p>
        </div>
        @for (m of g.movimientos; track m.id) { <ap-fila-de-movimiento [m]="m" /> }
      </section>
    }
  `,
  styles: `
    .cabecera { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s2); padding: var(--s3) 0 var(--s1); }
    h3 { font-size: .95em; color: var(--text-2); }
    p { margin: 0; color: var(--text-3); font-size: .85em; }
    ap-monto { color: var(--text-2); }
    .saldo { color: var(--brand-texto); }
  `,
})
export class ListaDeMovimientos {
  readonly movimientos = input.required<Movimiento[]>()
  readonly moneda = input.required<string>()
  readonly hoyIso = input.required<string>()
  readonly grupos = computed(() => {
    const porDia = new Map<string, Movimiento[]>()
    for (const m of this.movimientos()) {
      const dia = m.fechaIso.slice(0, 10)
      porDia.set(dia, [...(porDia.get(dia) ?? []), m])
    }
    return [...porDia.entries()]
      .sort(([a], [b]) => (a < b ? 1 : -1))
      .map(([dia, movimientos]) => ({ dia, legible: diaLegible(dia, this.hoyIso().slice(0, 10)), movimientos, neto: neto(movimientos.map((m) => m.monto)), saldo: movimientos[0]?.saldoCorrido }))
  })
}

/** Suma en centavos con BigInt y devuelve la cadena del contrato. */
export function neto(montos: string[]): string {
  let centavos = 0n
  for (const m of montos) {
    const negativo = m.startsWith('-')
    const [entero = '0', dec = '00'] = m.replace('-', '').split('.')
    const c = BigInt(entero) * 100n + BigInt(dec.padEnd(2, '0').slice(0, 2))
    centavos += negativo ? -c : c
  }
  const signo = centavos < 0n ? '-' : ''
  const abs = centavos < 0n ? -centavos : centavos
  return `${signo}${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`
}

export function diaLegible(dia: string, hoy: string): string {
  if (dia === hoy) return 'Hoy'
  const ayer = new Date(new Date(hoy + 'T12:00:00Z').getTime() - 86_400_000).toISOString().slice(0, 10)
  if (dia === ayer) return 'Ayer'
  const [a, m, d] = dia.split('-')
  return `${Number(d)} ${['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'][Number(m) - 1]} ${a}`
}
