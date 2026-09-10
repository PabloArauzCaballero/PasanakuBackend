import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { ChipEstado } from '../chip-estado/chip-estado'
import { CodigoQR } from '../codigo-qr/codigo-qr'
import { Fecha } from '../fecha/fecha'
import type { Tono } from '../tono/tono'

export type EstadoDeVale = 'vigente' | 'usado' | 'vencido'
const TONO: Record<EstadoDeVale, Tono> = { vigente: 'ok', usado: 'neutro', vencido: 'error' }
const NOMBRE: Record<EstadoDeVale, string> = { vigente: 'Vigente', usado: 'Ya usado', vencido: 'Vencido' }

/**
 * El vale de un partner: muesca de ticket, QR **rotativo** (el contenido cambia y la
 * vista solo lo pinta), estado y condiciones. Usado o vencido: QR apagado, sin borrarlo.
 */
@Component({
  selector: 'ap-vale',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipEstado, CodigoQR, Fecha],
  host: { role: 'article', '[attr.aria-label]': 'partner() + \': \' + beneficio()', '[class]': 'estado()' },
  template: `
    <section class="cabeza">
      <p class="partner">{{ partner() }}</p>
      <p class="beneficio">{{ beneficio() }}</p>
      <ap-chip-estado [tono]="tono()">{{ nombre() }}</ap-chip-estado>
    </section>
    <section class="cuerpo">
      <div class="qr"><ap-codigo-qr [contenido]="codigo()" [etiqueta]="'Código del vale de ' + partner()" /></div>
      <p class="codigo" aria-hidden="true">{{ codigo() }}</p>
      <p class="vence">Vence <ap-fecha [iso]="venceIso()" [conHora]="false" /></p>
      @if (condiciones()) { <p class="condiciones">{{ condiciones() }}</p> }
    </section>
  `,
  styles: `
    :host { display: block; max-width: calc(var(--s7) * 8); border-radius: var(--r-lg); background: var(--surface); border: var(--borde-fino) solid var(--border); overflow: hidden; }
    .cabeza { position: relative; padding: var(--s4); background: var(--verde-solido); color: var(--sobre-verde-solido); border-bottom: var(--borde-desfase) dashed var(--surface); }
    .cabeza::before, .cabeza::after { content: ''; position: absolute; bottom: calc(var(--s3) * -1); width: var(--s5); height: var(--s5); border-radius: var(--r-pill); background: var(--bg); }
    .cabeza::before { left: calc(var(--s3) * -1); } .cabeza::after { right: calc(var(--s3) * -1); }
    .cuerpo { padding: var(--s4); text-align: center; }
    p { margin: 0; }
    .partner { font-weight: 600; opacity: .9; }
    .beneficio { font-family: var(--font-d); font-size: 1.3em; margin: var(--s1) 0 var(--s2); }
    .qr { display: inline-block; max-width: calc(var(--s7) * 4); }
    :host(.usado) .qr, :host(.vencido) .qr { opacity: .3; filter: grayscale(1); }
    .codigo { margin-top: var(--s2); font-family: var(--mono); letter-spacing: .08em; color: var(--text-2); }
    .vence { margin-top: var(--s2); color: var(--text-2); }
    .condiciones { margin-top: var(--s2); color: var(--text-3); font-size: .85em; }
  `,
})
export class Vale {
  readonly partner = input.required<string>()
  readonly beneficio = input.required<string>()
  readonly codigo = input.required<string>()
  readonly venceIso = input.required<string>()
  readonly estado = input<EstadoDeVale>('vigente')
  readonly condiciones = input<string>()
  readonly tono = computed(() => TONO[this.estado()])
  readonly nombre = computed(() => NOMBRE[this.estado()])
}
