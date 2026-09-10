import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'
import { Icono } from '../icono/icono'

/** De una a cinco. Solo lectura muestra; con `editable` es un `radiogroup` con teclado. */
@Component({
  selector: 'ap-estrellas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  host: { '[attr.role]': 'editable() ? "radiogroup" : "img"', '[attr.aria-label]': 'lectura()' },
  template: `
    @for (n of cinco; track n) {
      @if (editable()) {
        <button type="button" role="radio" [attr.aria-checked]="valor() === n" [attr.aria-label]="n + ' de 5'" [class.llena]="n <= valor()" (click)="valor.set(n)"><ap-icono nombre="estrella" tamano="s5" /></button>
      } @else {
        <span [class.llena]="n <= valor()" aria-hidden="true"><ap-icono nombre="estrella" tamano="s4" /></span>
      }
    }
  `,
  styles: `
    :host { display: inline-flex; gap: var(--s1); color: var(--field-border); }
    .llena { color: var(--warn); }
    button { display: inline-flex; align-items: center; justify-content: center; width: var(--area-tactil); height: var(--area-tactil); border: 0; background: transparent; color: inherit; cursor: pointer; border-radius: var(--r-pill); }
    button.llena { color: var(--warn); }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); }
  `,
})
export class Estrellas {
  readonly cinco = [1, 2, 3, 4, 5]
  readonly valor = model(0)
  readonly editable = input(false)
  readonly etiqueta = input('Calificación')
  readonly lectura = computed(() => `${this.etiqueta()}: ${this.valor()} de 5`)
}
