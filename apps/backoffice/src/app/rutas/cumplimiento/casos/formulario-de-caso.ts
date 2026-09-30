import { ChangeDetectionStrategy, Component, input, output } from '@angular/core'
import type { Etapa } from '@aportaya/ui/escalera-de-etapas/escalera-de-etapas'
import { Boton } from '@aportaya/ui/boton/boton'
import { EscaleraDeEtapas } from '@aportaya/ui/escalera-de-etapas/escalera-de-etapas'
import { GrupoRadio, type OpcionDeRadio } from '@aportaya/ui/grupo-radio/grupo-radio'

/**
 * Presentación pura de CU-44: contrato de vista tipado adentro, intenciones afuera.
 * Ninguna dependencia de negocio — sin `ServicioBorrador`, sin sesión, sin cliente HTTP,
 * directa ni transitiva (kill-test del carril, verificado por
 * `formulario-de-caso.spec.ts` § "dependencias prohibidas"). No decide NADA: ni guarda
 * el borrador, ni sabe qué pantalla la usa, ni valida — solo dibuja lo que el contenedor
 * le entrega y pide lo que el operador pidió.
 */
@Component({
  selector: 'ap-formulario-de-caso',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [EscaleraDeEtapas, GrupoRadio, Boton],
  template: `
    <ap-escalera-de-etapas [etapas]="etapas()" [actual]="etapaActual()" />

    <ap-grupo-radio
      [etiqueta]="textoCausal()"
      [opciones]="causales()"
      [elegido]="causalElegida()"
      (elegidoChange)="seEligioCausal.emit($event)"
    />
    @if (!puedeConfirmar()) {
      <p class="ayuda">{{ textoSinCausal() }}</p>
    }

    <label for="narrativa">Narrativa</label>
    <textarea id="narrativa" [value]="narrativa()" (input)="seEscribioNarrativa.emit($any($event.target).value)" rows="6"></textarea>
    @if (borradorRecuperado()) {
      <p class="aviso">{{ textoBorradorRecuperado() }}</p>
    }

    <ap-boton variante="primario" [deshabilitado]="!puedeConfirmar()" (pulsado)="seQuiereConfirmar.emit()">
      {{ textoConfirmar() }}
    </ap-boton>
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: var(--s4); }
    textarea { width: 100%; min-height: 8rem; padding: var(--s3); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-md); font: inherit; background: var(--field); color: var(--text); }
    .ayuda { color: var(--text-3); font-size: .9em; margin: 0; }
    .aviso { color: var(--info); font-size: .9em; margin: 0; }
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

  /** Intenciones: lo que sale de acá es una SOLICITUD, nunca un resultado ya ocurrido.
   * `string | null` porque `GrupoRadio.elegido` (`model`) lo tipa así, aunque en la
   * práctica el evento de `change` de un radio siempre trae un valor real. */
  readonly seEligioCausal = output<string | null>()
  readonly seEscribioNarrativa = output<string>()
  readonly seQuiereConfirmar = output<void>()
}
