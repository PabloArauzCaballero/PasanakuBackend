import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'

/** Sí/no inmediato (`role=switch`). Activo en verde **ok**: es un estado, no una acción. */
@Component({
  selector: 'ap-interruptor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button type="button" role="switch" [attr.aria-checked]="activo()" [disabled]="deshabilitado()" (click)="activo.set(!activo())">
      <span class="pista" aria-hidden="true"><span class="perilla"></span></span>
      <span class="texto"><ng-content /></span>
    </button>
  `,
  styles: `
    button { display: inline-flex; align-items: center; gap: var(--s3); min-height: var(--area-tactil); padding: 0; border: 0; background: transparent; color: var(--text); font: inherit; cursor: pointer; }
    .pista { position: relative; width: calc(var(--s7) - var(--s1)); height: var(--s5); flex: none; border-radius: var(--r-pill); background: var(--field-border); transition: background-color .15s; }
    .perilla { position: absolute; top: var(--borde-desfase); left: var(--borde-desfase); width: calc(var(--s5) - var(--s1)); height: calc(var(--s5) - var(--s1)); border-radius: var(--r-pill); background: var(--white); transition: transform .15s; }
    button[aria-checked="true"] .pista { background: var(--ok); }
    button[aria-checked="true"] .perilla { transform: translateX(calc(var(--s5) - var(--s1))); }
    button:disabled { opacity: .5; cursor: not-allowed; }
    button:focus-visible .pista { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    @media (prefers-reduced-motion: reduce) { .pista, .perilla { transition: none; } }
  `,
})
export class Interruptor {
  readonly activo = model(false)
  readonly deshabilitado = input(false)
}
