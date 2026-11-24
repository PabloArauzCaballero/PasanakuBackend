import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import type { Etapa } from '@aportaya/ui/escalera-de-etapas/escalera-de-etapas'
import { Boton } from '@aportaya/ui/boton/boton'
import { EscaleraDeEtapas } from '@aportaya/ui/escalera-de-etapas/escalera-de-etapas'
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'
import { Icono } from '@aportaya/ui/icono/icono'

/**
 * Presentación pura de CU-44: contrato de vista tipado adentro, intenciones afuera.
 * Ninguna dependencia de negocio — sin `ServicioBorrador`, sin sesión, sin cliente HTTP,
 * directa ni transitiva (kill-test del carril, verificado por
 * `formulario-de-caso.spec.ts` § "dependencias prohibidas"). No decide NADA: ni guarda
 * el borrador, ni sabe qué pantalla la usa, ni valida — solo dibuja lo que el contenedor
 * le entrega y pide lo que el operador pidió.
 *
 * Disposición (2026-11-24): en un ancho de escritorio la escalera es un riel lateral fijo y el trabajo va al lado;
 * en uno angosto la escalera se pliega detrás de un `details` para que la causal quede en el primer pliegue (el
 * paso en curso ya lo dice el resumen del caso). Las dos escaleras nunca se ven juntas: la que no corresponde al
 * ancho queda en `display: none`, que también la saca del árbol de accesibilidad.
 */
@Component({
  selector: 'ap-formulario-de-caso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EscaleraDeEtapas, GrupoRadio, Boton, Icono],
  template: `
    <div class="disposicion">
      <aside class="riel" [attr.aria-label]="textoDebidoProceso()">
        <p class="rotulo" aria-hidden="true">{{ textoDebidoProceso() }}</p>
        <ap-escalera-de-etapas [etapas]="etapas()" [actual]="etapaActual()" />
      </aside>

      <div class="trabajo">
        <details class="plegable">
          <summary>{{ textoVerEtapas() }}<ap-icono class="galon" nombre="chevronAbajo" tamano="s4" /></summary>
          <ap-escalera-de-etapas [etapas]="etapas()" [actual]="etapaActual()" />
        </details>

        <div class="panel">
          <ap-grupo-radio
            variante="tarjetas"
            [etiqueta]="textoCausal()"
            [opciones]="causales()"
            [elegido]="causalElegida()"
            (elegidoChange)="seEligioCausal.emit($event)"
          />

          <div class="campo">
            <label for="narrativa">Narrativa</label>
            @if (textoNarrativaAyuda()) { <p id="narrativa-ayuda" class="ayuda">{{ textoNarrativaAyuda() }}</p> }
            <textarea id="narrativa" [value]="narrativa()" (input)="seEscribioNarrativa.emit($any($event.target).value)" rows="6" [attr.aria-describedby]="textoNarrativaAyuda() ? 'narrativa-ayuda' : null"></textarea>
            @if (borradorRecuperado()) {
              <p class="aviso" role="status">{{ textoBorradorRecuperado() }}</p>
            }
          </div>

          <div class="cierre">
            <ap-boton variante="primario" icono="balanza" [deshabilitado]="!puedeConfirmar()" (pulsado)="seQuiereConfirmar.emit()">
              {{ textoConfirmar() }}
            </ap-boton>
            @if (!puedeConfirmar()) {
              <p class="ayuda motivo">{{ textoSinCausal() }}</p>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: `
    :host { display: block; container-type: inline-size; }
    .disposicion { display: grid; gap: var(--s4); min-width: 0; }
    .riel { display: none; }
    .trabajo { display: grid; gap: var(--s3); min-width: 0; }
    .plegable { border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
    .plegable summary { display: flex; align-items: center; justify-content: space-between; gap: var(--s2); list-style: none; min-height: var(--area-tactil); padding: 0 var(--s4); cursor: pointer; color: var(--brand-texto); font: var(--t-cuerpo-chico); font-weight: 600; border-radius: var(--r-lg); }
    .plegable summary::-webkit-details-marker { display: none; }
    .galon { flex: none; transition: transform var(--mov-micro) var(--curva-llega); }
    .plegable[open] .galon { transform: rotate(180deg); }
    @media (prefers-reduced-motion: reduce) { .galon { transition: none; } }
    .plegable summary:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .plegable[open] summary { border-bottom: var(--borde-fino) solid var(--border); border-radius: var(--r-lg) var(--r-lg) 0 0; }
    .plegable ap-escalera-de-etapas { display: block; padding: var(--s4) var(--s4) 0; }
    .panel { display: grid; gap: var(--s5); min-width: 0; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    .campo { display: grid; gap: var(--s2); }
    label { font: var(--t-cuerpo-fuerte); color: var(--text); }
    textarea { width: 100%; min-height: 8rem; padding: var(--s3); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); font: inherit; background: var(--field); color: var(--text); resize: vertical; box-sizing: border-box; }
    textarea:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .cierre { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s2) var(--s4); padding-top: var(--s4); border-top: var(--borde-fino) solid var(--border); }
    .ayuda { color: var(--text-2); font: var(--t-cuerpo-chico); margin: 0; }
    .aviso { color: var(--info-texto); font: var(--t-cuerpo-chico); margin: 0; }
    @container (min-width: 52rem) {
      .disposicion { grid-template-columns: minmax(15rem, 18rem) minmax(0, 1fr); align-items: start; gap: var(--s5); }
      .riel { display: block; position: sticky; top: var(--s4); padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); }
      .rotulo { margin: 0 0 var(--s4); font: var(--t-etiqueta); letter-spacing: var(--t-etiqueta-track); text-transform: uppercase; color: var(--text-3); }
      .plegable { display: none; }
      .panel { padding: var(--s5); }
    }
  `,
})
export class FormularioDeCaso {
  readonly etapas = input.required<Etapa[]>()
  readonly etapaActual = input.required<number>()
  readonly causales = input.required<OpcionDeRadio[]>()
  readonly causalElegida = input.required<string | null>()
  readonly narrativa = input.required<string>()
  readonly borradorRecuperado = input.required<boolean>()
  /** Derivado UNA vez en el contenedor — acá se LEE, no se recalcula (H2 del carril). */
  readonly puedeConfirmar = input.required<boolean>()

  readonly textoCausal = input.required<string>()
  readonly textoSinCausal = input.required<string>()
  readonly textoBorradorRecuperado = input.required<string>()
  readonly textoConfirmar = input.required<string>()
  readonly textoNarrativaAyuda = input('')
  readonly textoDebidoProceso = input('Debido proceso')
  readonly textoVerEtapas = input('Ver las etapas del debido proceso')

  /** Intenciones: lo que sale de acá es una SOLICITUD, nunca un resultado ya ocurrido.
   * `string | null` porque `GrupoRadio.elegido` (`model`) lo tipa así, aunque en la
   * práctica el evento de `change` de un radio siempre trae un valor real. */
  readonly seEligioCausal = output<string | null>()
  readonly seEscribioNarrativa = output<string>()
  readonly seQuiereConfirmar = output<void>()
}
