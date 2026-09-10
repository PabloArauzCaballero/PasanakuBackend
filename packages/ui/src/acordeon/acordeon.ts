import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'
import { Icono } from '../icono/icono'

/** Un bloque que se abre y cierra. `details`/`summary` nativos: teclado y lector sin código. */
@Component({
  selector: 'ap-acordeon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  template: `
    <details [open]="abierto()" (toggle)="abierto.set(alternado($event))">
      <summary><span>{{ titulo() }}</span><ap-icono nombre="chevronAbajo" tamano="s5" /></summary>
      <div class="cuerpo"><ng-content /></div>
    </details>
  `,
  styles: `
    :host { display: block; border: var(--borde-fino) solid var(--border); border-radius: var(--r-md); background: var(--surface); }
    summary { display: flex; align-items: center; justify-content: space-between; gap: var(--s3); min-height: var(--area-tactil); padding: var(--s2) var(--s4); list-style: none; cursor: pointer; font-weight: 600; color: var(--text); }
    summary::-webkit-details-marker { display: none; }
    summary:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: calc(var(--borde-desfase) * -1); border-radius: var(--r-md); }
    details[open] summary ap-icono { transform: rotate(180deg); }
    .cuerpo { padding: 0 var(--s4) var(--s4); color: var(--text-2); }
  `,
})
export class Acordeon {
  readonly titulo = input.required<string>()
  readonly abierto = model(false)
  alternado(e: Event): boolean {
    return (e.target as HTMLDetailsElement).open
  }
}
