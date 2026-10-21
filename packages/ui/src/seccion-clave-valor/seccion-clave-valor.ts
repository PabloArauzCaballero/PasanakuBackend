import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { CodigoCopiable } from '../codigo-copiable/codigo-copiable'
import type { Tono } from '../tono/tono'

/** Una fila: rótulo y valor. `mono` lo muestra como código copiable; `tono` colorea el valor según lo que significa. */
export type ClaveValor = { etiqueta: string; valor: string | number | boolean | null | undefined; mono?: boolean; tono?: Tono }

/**
 * La sección clave–valor de una ficha, como en el portal de Atlas: tarjeta con cabecera gris
 * (título y descripción) y una lista cebra de rótulos en mayúsculas con su valor. Un valor vacío se
 * dice («—»), nunca se deja un hueco; un booleano se dice en palabras.
 */
@Component({
  selector: 'ap-seccion-clave-valor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CodigoCopiable],
  host: { role: 'region', '[attr.aria-label]': 'titulo()' },
  template: `
    <div class="cabecera">
      <h2>{{ titulo() }}</h2>
      @if (descripcion()) { <p>{{ descripcion() }}</p> }
    </div>
    <dl>
      @for (i of items(); track i.etiqueta) {
        <div class="fila" [class]="'fila' + (i.tono ? ' tono-' + i.tono : '')">
          <dt>{{ i.etiqueta }}</dt>
          <dd>
            @if (i.mono && typeof i.valor === 'string' && i.valor) { <ap-codigo-copiable [valor]="i.valor" [nombre]="i.etiqueta" /> }
            @else { {{ enPalabras(i.valor) }} }
          </dd>
        </div>
      }
    </dl>
  `,
  styles: `
    :host { display: block; container-type: inline-size; border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); overflow: hidden; }
    .cabecera { padding: var(--s3) var(--s4); border-bottom: var(--borde-fino) solid var(--border); background: var(--surface-2); }
    h2 { margin: 0; font: var(--t-cuerpo-fuerte); color: var(--text); }
    .cabecera p { margin: var(--s1) 0 0; font: var(--t-cuerpo-chico); color: var(--text-3); }
    dl { margin: 0; }
    .fila { display: grid; gap: var(--s1); padding: var(--s3) var(--s4); }
    .fila:nth-child(even) { background: color-mix(in srgb, var(--surface-2) 50%, var(--surface)); }
    dt { font: var(--t-cabecera-tabla); letter-spacing: var(--t-cabecera-tabla-track); text-transform: uppercase; color: var(--text-3); }
    dd { margin: 0; min-width: 0; font: var(--t-cuerpo-chico); font-weight: 500; color: var(--text); overflow-wrap: break-word; }
    .tono-ok dd { color: var(--ok-texto); } .tono-aviso dd { color: var(--aviso-texto); } .tono-error dd { color: var(--err-texto); } .tono-info dd { color: var(--info-texto); } .tono-pii dd { color: var(--pii-texto); } .tono-apagado dd { color: var(--text-3); font-weight: 400; }
    /* Etiqueta al lado solo si la TARJETA es ancha (no la pantalla): en una columna de tres, apilada, el valor no se parte palabra por palabra. */
    @container (min-width: 32rem) { .fila { grid-template-columns: minmax(7rem, 11rem) minmax(0, 1fr); align-items: baseline; gap: var(--s4); } }
  `,
})
export class SeccionClaveValor {
  readonly titulo = input.required<string>()
  readonly descripcion = input<string>()
  readonly items = input.required<readonly ClaveValor[]>()
  readonly textoSi = input('Sí')
  readonly textoNo = input('No')
  readonly textoVacio = input('—')

  enPalabras(v: ClaveValor['valor']): string {
    if (v === null || v === undefined || v === '') return this.textoVacio()
    if (typeof v === 'boolean') return v ? this.textoSi() : this.textoNo()
    return String(v)
  }
}
