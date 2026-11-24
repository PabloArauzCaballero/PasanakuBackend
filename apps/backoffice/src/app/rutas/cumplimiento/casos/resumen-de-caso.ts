import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core'
import { BarraDePasos } from '@aportaya/ui/barra-de-pasos/barra-de-pasos'
import { Fecha } from '@aportaya/ui/fecha/fecha'
import type { AlertaDeOrigenLft } from 'clientes/angular/cumplimiento'
import { textosCumplimiento } from '../textos'

/** Lo que el contenedor sabe de la ficha del caso (`obtenerCasoLft`) al pintar el resumen. */
export type EstadoDeFicha =
  | { tipo: 'cargando' }
  | { tipo: 'error'; mensaje: string }
  | { tipo: 'lista'; alerta: AlertaDeOrigenLft | null; alertasEnlazadas: number; titularEnmascarado: string }

/**
 * CU-44 · La cabecera del expediente: qué caso es, en qué paso del debido proceso está y qué datos ya quedaron
 * firmes. Presentación pura: el contenedor le entrega todo ya derivado.
 *
 * La alerta de origen y el titular salen de la ficha del caso (`obtenerCasoLft`). El titular llega ENMASCARADO
 * desde el servidor (`****` + cuatro caracteres de un seudónimo) y así se pinta: ni nombre ni documento viajan.
 * Mientras carga, o si falla, se dice; un hueco mudo se leería como «no hay alerta».
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
        @switch (ficha().tipo) {
          @case ('lista') {
            @if (fichaLista(); as f) {
              <div class="ancho" [class.pendiente]="!f.alerta">
                <dt>{{ t.alerta }}</dt>
                <dd>
                  @if (f.alerta; as a) {
                    <span class="regla">{{ a.reglaCodigo }}</span> {{ a.tipologia }}
                    <span class="nota">{{ t.severidades[a.severidad] ?? a.severidad }} · <ap-fecha [iso]="a.detectadaEn" /> · {{ t.alertasEnlazadas(f.alertasEnlazadas) }}</span>
                  } @else { {{ t.sinAlerta }} }
                </dd>
              </div>
              <div class="ancho">
                <dt>{{ t.titular }}</dt>
                <dd><span class="regla">{{ f.titularEnmascarado }}</span> <span class="nota en-linea">{{ t.titularAyuda }}</span></dd>
              </div>
            }
          }
          @case ('error') {
            <div class="pendiente ancho"><dt>{{ t.alerta }} · {{ t.titular }}</dt><dd aria-live="assertive">{{ mensajeDeError() }}</dd></div>
          }
          @default {
            <div class="pendiente ancho"><dt>{{ t.alerta }} · {{ t.titular }}</dt><dd aria-live="polite">{{ t.cargandoFicha }}</dd></div>
          }
        }
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
    .regla { font-family: var(--mono); overflow-wrap: anywhere; }
    .nota { display: block; font: var(--t-cuerpo-chico); color: var(--text-2); }
    .nota.en-linea { display: inline; margin-inline-start: var(--s2); }
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
  readonly ficha = input<EstadoDeFicha>({ tipo: 'cargando' })
  protected readonly fichaLista = computed(() => {
    const f = this.ficha()
    return f.tipo === 'lista' ? f : null
  })
  protected readonly mensajeDeError = computed(() => {
    const f = this.ficha()
    return f.tipo === 'error' ? f.mensaje : ''
  })
}
