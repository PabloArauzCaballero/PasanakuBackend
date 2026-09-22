import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

/**
 * Fecha del contrato (ISO) en hora de Bolivia, **con la zona visible**: un plazo se
 * cuenta en La Paz aunque quien lo mira esté en otro lado. Mismo formato que Flutter.
 */
@Component({
  selector: 'ap-fecha',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<time [attr.datetime]="iso()">{{ texto() }}</time>`,
  styles: `:host { color: var(--text-3); font-variant-numeric: tabular-nums; }`,
})
export class Fecha {
  readonly iso = input.required<string>()
  readonly conHora = input(true)
  readonly texto = computed(() => formatearFecha(this.iso(), this.conHora()))
}

/** «2026-09-10T18:05:00Z» → «10 sep 2026, 14:05 (La Paz)». */
export function formatearFecha(iso: string, conHora = true): string {
  const utc = new Date(iso).getTime()
  const laPaz = new Date(utc - 4 * 60 * 60 * 1000)
  const d = `${laPaz.getUTCDate()} ${MESES[laPaz.getUTCMonth()]} ${laPaz.getUTCFullYear()}`
  if (!conHora) return d
  const hh = String(laPaz.getUTCHours()).padStart(2, '0')
  const mm = String(laPaz.getUTCMinutes()).padStart(2, '0')
  return `${d}, ${hh}:${mm} (La Paz)`
}
