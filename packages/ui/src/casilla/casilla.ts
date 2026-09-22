import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'

/** Casilla nativa vestida: el `input` real queda, así el teclado y el lector la ven. */
@Component({
  selector: 'ap-casilla',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <label>
      <input type="checkbox" [checked]="marcada()" [disabled]="deshabilitada()" (change)="marcada.set(entrada($event))" />
      <span class="caja" aria-hidden="true"></span>
      <span class="texto"><ng-content /></span>
    </label>
  `,
  styles: `
    label { display: inline-flex; align-items: center; gap: var(--s3); min-height: var(--area-tactil); cursor: pointer; color: var(--text); }
    input { position: absolute; width: var(--borde-fino); height: var(--borde-fino); opacity: 0; }
    .caja { width: var(--s5); height: var(--s5); flex: none; border: var(--borde-desfase) solid var(--field-border); border-radius: var(--r-sm); background: var(--field); }
    input:checked + .caja { background: var(--verde-solido); border-color: var(--verde-solido); background-image: linear-gradient(var(--sobre-verde-solido), var(--sobre-verde-solido)); background-size: 50% 18%; background-repeat: no-repeat; background-position: center; }
    input:focus-visible + .caja { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    input:disabled ~ * { opacity: .5; }
  `,
})
export class Casilla {
  readonly marcada = model(false)
  readonly deshabilitada = input(false)
  entrada(e: Event): boolean {
    return (e.target as HTMLInputElement).checked
  }
}
