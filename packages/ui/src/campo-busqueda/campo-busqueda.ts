import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'
import { Icono } from '../icono/icono'

/** Búsqueda con lupa y borrado. `type=search` para que el teclado móvil ofrezca «Buscar». */
@Component({
  selector: 'ap-campo-busqueda',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  template: `
    <label>
      <ap-icono nombre="buscar" tamano="s4" />
      <span class="oculto">{{ etiqueta() }}</span>
      <input type="search" [value]="valor()" [placeholder]="etiqueta()" (input)="valor.set(entrada($event))" />
      @if (valor()) { <button type="button" aria-label="Borrar la búsqueda" (click)="valor.set('')"><ap-icono nombre="cerrar" tamano="s4" /></button> }
    </label>
  `,
  styles: `
    :host { display: block; }
    label { display: flex; align-items: center; gap: var(--s2); min-height: var(--area-tactil); padding: 0 var(--s3); border: var(--borde-fino) solid var(--field-border); border-radius: var(--r-pill); background: var(--field); color: var(--text-3); }
    label:focus-within { border-color: var(--brand); box-shadow: 0 0 0 var(--borde-foco) var(--g300); }
    .oculto { position: absolute; width: var(--borde-fino); height: var(--borde-fino); overflow: hidden; clip: rect(0 0 0 0); }
    input { flex: 1; min-width: 0; border: 0; background: transparent; color: var(--text); font: inherit; outline: none; }
    input::-webkit-search-cancel-button { display: none; }
    button { display: inline-flex; align-items: center; justify-content: center; width: var(--area-tactil); height: var(--area-tactil); margin-right: calc(var(--s3) * -1); border: 0; border-radius: var(--r-pill); background: transparent; color: var(--text-2); cursor: pointer; }
    button:focus-visible { outline: var(--borde-foco) solid var(--g300); }
  `,
})
export class CampoBusqueda {
  readonly etiqueta = input('Buscar')
  readonly valor = model('')
  entrada(e: Event): string {
    return (e.target as HTMLInputElement).value
  }
}
