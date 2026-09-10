import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { ChipEstado } from '../chip-estado/chip-estado'
import { Fecha, formatearFecha } from '../fecha/fecha'
import { Icono } from '../icono/icono'
import type { Tono } from '../tono/tono'

/**
 * Un plazo con fecha, restante y **de qué norma sale**. El vencimiento viene guardado del
 * servidor; el cliente solo lo muestra. `ahora` se inyecta para que la prueba sea fija.
 */
@Component({
  selector: 'ap-reloj-de-plazo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado, Fecha, Icono],
  host: { role: 'group', '[attr.aria-label]': 'etiqueta() + \': \' + restante()' },
  template: `
    <ap-icono nombre="reloj" tamano="s5" [class]="'tono-' + tono()" />
    <div class="cuerpo">
      <p class="etiqueta">{{ etiqueta() }}</p>
      <p class="fecha"><ap-fecha [iso]="venceIso()" [conHora]="false" /></p>
      @if (norma()) { <p class="norma">{{ norma() }}</p> }
    </div>
    <ap-chip-estado [tono]="tono()">{{ restante() }}</ap-chip-estado>
  `,
  styles: `
    :host { display: flex; align-items: center; gap: var(--s3); padding: var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); }
    .cuerpo { flex: 1; min-width: 0; }
    p { margin: 0; }
    .etiqueta { font-weight: 600; color: var(--text-2); }
    .fecha ap-fecha { color: var(--text); }
    .norma { color: var(--text-3); font-size: .85em; }
    .tono-error { color: var(--err); } .tono-aviso { color: var(--warn); } .tono-info { color: var(--info); }
  `,
})
export class RelojDePlazo {
  readonly etiqueta = input.required<string>()
  readonly venceIso = input.required<string>()
  readonly ahoraIso = input.required<string>()
  readonly norma = input<string>()
  readonly restante = computed(() => restanteDe(new Date(this.venceIso()), new Date(this.ahoraIso())))
  readonly tono = computed<Tono>(() => {
    const horas = (new Date(this.venceIso()).getTime() - new Date(this.ahoraIso()).getTime()) / 3_600_000
    return horas < 0 ? 'error' : horas < 24 ? 'aviso' : 'info'
  })
  readonly fechaLegible = computed(() => formatearFecha(this.venceIso(), false))
}

/** Mismas frases que Flutter: «Vencido», «Faltan 3 días», «Faltan 5 h», «Vence hoy». */
export function restanteDe(vence: Date, ahora: Date): string {
  const ms = vence.getTime() - ahora.getTime()
  if (ms < 0) return 'Vencido'
  const dias = Math.floor(ms / 86_400_000)
  if (dias >= 1) return `Faltan ${dias} ${dias === 1 ? 'día' : 'días'}`
  const horas = Math.floor(ms / 3_600_000)
  if (horas >= 1) return `Faltan ${horas} h`
  return 'Vence hoy'
}
