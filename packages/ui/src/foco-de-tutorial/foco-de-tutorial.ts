import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core'

/** El recuadro del elemento señalado, en coordenadas de la ventana. */
export interface RecuadroResaltado {
  readonly x: number
  readonly y: number
  readonly ancho: number
  readonly alto: number
}

/**
 * **El velo con un agujero.** Oscurece la pantalla salvo el elemento del que habla el
 * tutorial, y dibuja un anillo alrededor para que se vea también sin color (el foco no
 * se comunica solo con el oscurecimiento: hay un borde).
 *
 * El agujero **deja pasar el ratón** salvo que `bloquea` sea verdadero: un paso que
 * dice «pulsá este botón» tiene que dejar pulsarlo de verdad. El halo es decorativo;
 * cuando el paso bloquea la app, la cortina ofrece una salida accesible.
 */
@Component({
  selector: 'ap-foco-de-tutorial',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (bloquea()) {
      <button type="button" class="cortina" aria-label="Salir del tutorial" (click)="fueraDelFoco.emit()"></button>
    }
    <div class="halo" aria-hidden="true" [class.sin-recuadro]="recuadro() === null" [style.left.px]="caja().x" [style.top.px]="caja().y" [style.width.px]="caja().ancho" [style.height.px]="caja().alto"></div>
  `,
  styles: `
    :host { position: fixed; inset: 0; z-index: 40; pointer-events: none; }
    .cortina { position: absolute; inset: 0; pointer-events: auto; border: 0; background: transparent; cursor: pointer; }
    .halo {
      position: absolute;
      border-radius: var(--r-lg);
      box-shadow: 0 0 0 100vmax color-mix(in srgb, var(--ink) 62%, transparent);
      outline: var(--borde-foco) solid var(--accent);
      outline-offset: var(--borde-desfase);
      transition: left .22s ease, top .22s ease, width .22s ease, height .22s ease;
    }
    .halo.sin-recuadro { outline: 0; }
    @media (prefers-reduced-motion: reduce) { .halo { transition: none; } }
  `,
})
export class FocoDeTutorial {
  /** `null` mientras se busca el elemento o cuando el paso no señala nada. */
  readonly recuadro = input.required<RecuadroResaltado | null>()
  /** Con `true` nadie toca la aplicación; con `false`, solo el agujero es tocable. */
  readonly bloquea = input(true)
  /** Alguien pulsó fuera del foco: el anfitrión decide si eso es «salir». */
  readonly fueraDelFoco = output<void>()

  /** Sin recuadro, el velo se cierra sobre el centro: queda oscuro y sin anillo. */
  protected readonly caja = computed<RecuadroResaltado>(() => {
    const r = this.recuadro()
    if (r !== null) return { x: r.x - HOLGURA, y: r.y - HOLGURA, ancho: r.ancho + HOLGURA * 2, alto: r.alto + HOLGURA * 2 }
    return { x: 0, y: 0, ancho: 0, alto: 0 }
  })
}

/** Aire alrededor del elemento, para que el anillo no lo toque. */
const HOLGURA = 6
