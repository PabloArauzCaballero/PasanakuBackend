import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { BotonCopiar } from '../boton-copiar/boton-copiar'

/** Un identificador o código en monoespaciado con su botón de copiar al lado (esquemas, tablas, trazas, claves). */
@Component({
  selector: 'ap-codigo-copiable',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BotonCopiar],
  template: `<code>{{ valor() }}</code><ap-boton-copiar [valor]="valor()" [etiqueta]="'Copiar ' + (nombre() ?? valor())" />`,
  styles: `
    :host { display: inline-flex; align-items: center; gap: var(--s1); max-width: 100%; }
    code { font-family: var(--mono); font-size: .92em; overflow-wrap: anywhere; }
  `,
})
export class CodigoCopiable {
  readonly valor = input.required<string>()
  /** Qué es lo que se copia, para el nombre accesible del botón («Copiar el esquema»). */
  readonly nombre = input<string>()
}
