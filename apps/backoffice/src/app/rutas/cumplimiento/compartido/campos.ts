import { ChangeDetectionStrategy, Component, input, model } from '@angular/core'
import type { Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'

let secuencia = 0

const ESTILOS = `
  :host { display: block; }
  label { display: block; margin-bottom: var(--s1); font-weight: 600; color: var(--text-2); }
  textarea, select, input { width: 100%; min-height: var(--area-tactil); padding: var(--s2) var(--s3); border: var(--borde-fino) solid var(--borde-control); border-radius: var(--r-md); background: var(--field); color: var(--text); font: inherit; }
  textarea { min-height: 8rem; resize: vertical; }
  textarea:focus-visible, select:focus-visible, input:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  :host(.con-error) textarea, :host(.con-error) select, :host(.con-error) input { border-color: var(--err); }
  .ayuda { margin: var(--s1) 0 0; color: var(--text-3); font-size: .9em; }
  .error { margin: var(--s1) 0 0; color: var(--err); font-size: .9em; }
  .contador { margin: var(--s1) 0 0; color: var(--text-3); font-size: .85em; font-variant-numeric: tabular-nums; }
`

/** Área de texto y lista desplegable: viven en `@aportaya/ui` desde el plan backoffice-como-atlas (H1.S3.M5); acá se reexportan para no tocar a quien ya las importa. */
export { AreaDeTexto } from '@aportaya/ui/area-de-texto/area-de-texto'
export { ListaDeOpciones, type Opcion } from '@aportaya/ui/lista-de-opciones/lista-de-opciones'

/** Fecha y hora local (`datetime-local`): `ap-campo` no admite ese tipo. La zona la fija el dominio (`aIsoLaPaz`). */
@Component({
  selector: 'ap-fecha-y-hora',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.con-error]': '!!error()' },
  template: `
    <label [for]="id">{{ etiqueta() }}</label>
    <input type="datetime-local" [id]="id" [value]="valor()" [attr.aria-invalid]="error() ? 'true' : null" [attr.aria-describedby]="error() ? id + '-error' : null" (input)="valor.set($any($event.target).value)" />
    @if (error()) { <p class="error" [id]="id + '-error'" role="alert">{{ error() }}</p> }
  `,
  styles: ESTILOS,
})
export class FechaYHora {
  readonly id = `ap-fecha-hora-${++secuencia}`
  readonly etiqueta = input.required<string>()
  readonly valor = model('')
  readonly error = input<string>()
}

/** Opciones de una lista a partir de un diccionario de textos del contrato. */
export const opcionesDe = (textos: Readonly<Record<string, string>>): Opcion[] => Object.entries(textos).map(([valor, texto]) => ({ valor, texto }))
