import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { BarraDePasos } from '@aportaya/ui/barra-de-pasos/barra-de-pasos'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import { textosCumplimiento } from '../textos'

/**
 * CU-44 · La cabecera del expediente: qué caso es, en qué paso del debido proceso está y qué datos ya quedaron
 * firmes. Presentación pura: el contenedor le entrega todo ya derivado.
 *
 * La alerta de origen y el titular se nombran aunque falten: el contrato del caso todavía no los publica
 * (`dominio/cu44-caso.ts`, «Contrato pendiente») y un hueco mudo se leería como «no hay alerta». Se dice por qué
 * falta; no se inventa el dato.
 */
@Component({
  selector: 'ap-resumen-de-caso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDePasos, Fecha],
  template: `
    <section class="resumen" aria-labelledby="resumen-caso">
      <div class="identidad">
        <h2 id="resumen-caso"><span class="rotulo">{{ t.caso }}</span> <span class="codigo">{{ codigo() }}</span></h2>
        <ap-barra-de-pasos class="pasos" [pasos]="etapas()" [actual]="actual() + 1" />
      </div>
      <dl class="datos">
        <div [class.pendiente]="!causal()"><dt>{{ t.causal }}</dt><dd>{{ causal() ?? t.sinCausal }}</dd></div>
        <div [class.pendiente]="!notificadoEn()">
          <dt>{{ t.notificacion }}</dt>
          <dd>@if (notificadoEn(); as iso) { <ap-fecha [iso]="iso" /> } @else { {{ t.sinNotificar }} }</dd>
        </div>
        <div [class.pendiente]="!plazoVenceEn()">
          <dt>{{ t.plazo }}</dt>
          <dd>@if (plazoVenceEn(); as iso) { <ap-fecha [iso]="iso" /> } @else { {{ t.sinPlazo }} }</dd>
        </div>
        <div class="pendiente ancho"><dt>{{ t.alerta }} · {{ t.titular }}</dt><dd>{{ t.noPublicado }}</dd></div>
      </dl>
    </section>
  `,
  styles: `
    :host { display: block; container-type: inline-size; }
    .resumen { display: grid; gap: var(--s4); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    .identidad { display: grid; gap: var(--s3); min-width: 0; }
    h2 { display: flex; flex-wrap: wrap; align-items: baseline; gap: var(--s2); margin: 0; min-width: 0; }
    .rotulo { font: var(--t-etiqueta); letter-spacing: var(--t-etiqueta-track); text-transform: uppercase; color: var(--text-3); }
    .codigo { font: var(--t-cifra); font-family: var(--mono); letter-spacing: var(--t-cifra-track); color: var(--text); overflow-wrap: anywhere; }
    .datos { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--s3) var(--s4); margin: 0; padding-top: var(--s4); border-top: var(--borde-fino) solid var(--border); }
    .datos > div { display: grid; align-content: start; gap: var(--s1); min-width: 0; }
    .datos > div:first-child, .datos > .ancho { grid-column: 1 / -1; }
    dt { font: var(--t-etiqueta); letter-spacing: var(--t-etiqueta-track); text-transform: uppercase; color: var(--text-3); }
    dd { margin: 0; font: var(--t-cuerpo-fuerte); color: var(--text); overflow-wrap: anywhere; }
    .pendiente dd { font: var(--t-cuerpo-chico); color: var(--text-2); }
    @container (min-width: 44rem) {
      .resumen { padding: var(--s5); }
      .identidad { grid-template-columns: auto minmax(16rem, 28rem); justify-content: space-between; align-items: end; }
      .datos { grid-template-columns: repeat(4, minmax(0, 1fr)); }
      .datos > div:first-child, .datos > .ancho { grid-column: auto; }
    }
  `,
})
export class ResumenDeCaso {
  protected readonly t = textosCumplimiento.casos.resumen
  readonly codigo = input.required<string>()
  readonly etapas = input.required<string[]>()
  /** Índice de la etapa en curso, desde cero (el mismo que usa la escalera). */
  readonly actual = input.required<number>()
  readonly causal = input<string | null>(null)
  readonly notificadoEn = input<string | null>(null)
  readonly plazoVenceEn = input<string | null>(null)
}
