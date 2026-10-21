import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { RouterLink } from '@angular/router'
import type { EsquemaDelCatalogo } from './dominio'
import { textosCatalogo } from './textos'

/**
 * Pestaña «Esquemas» del catálogo: cada esquema de la base con su servicio dueño, de qué trata y sus cifras. Es el
 * mapa antes de las tablas: un esquema por servicio (ADR-017) más los compartidos `catalogo` y `comun`. Cada tarjeta
 * lleva a la lista filtrada por ese esquema.
 */
@Component({
  selector: 'ap-vista-esquemas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <ul class="esquemas" [attr.aria-label]="t.titulo">
      @for (e of esquemas(); track e.nombre) {
        <li>
          <a class="esquema" [routerLink]="[]" [queryParams]="{ vista: null, esquema: e.nombre, pagina: null }" queryParamsHandling="merge">
            <span class="cabeza">
              <code class="nombre">{{ e.nombre }}</code>
              <span class="servicio">{{ e.servicio ? t.servicio(e.servicio) : t.compartido }}</span>
            </span>
            <span class="descripcion">{{ e.descripcion }}</span>
            <dl class="cifras">
              <div><dt>{{ t.tablas }}</dt><dd>{{ e.tablas }}</dd></div>
              <div><dt>{{ t.columnas }}</dt><dd>{{ e.columnas }}</dd></div>
              <div><dt>{{ t.relaciones }}</dt><dd>{{ e.relaciones }}</dd></div>
              <div><dt>{{ t.personales }}</dt><dd>{{ e.conDatosPersonales }}</dd></div>
            </dl>
            <span class="ver">{{ t.ver }}</span>
          </a>
        </li>
      }
    </ul>
  `,
  styles: `
    .esquemas { list-style: none; margin: 0; padding: 0; display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fill, minmax(min(100%, 18rem), 1fr)); }
    .esquema {
      display: grid; gap: var(--s2); height: 100%; padding: var(--s4); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg);
      background: var(--surface); color: var(--text); text-decoration: none; box-shadow: var(--sombra-1);
      transition: border-color var(--mov-micro) ease-out, box-shadow var(--mov-micro) ease-out;
    }
    .esquema:hover { border-color: var(--brand); box-shadow: var(--sombra-2); }
    .esquema:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .cabeza { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: var(--s2); }
    .nombre { font: var(--t-cuerpo-fuerte); font-family: var(--mono); }
    .servicio { color: var(--text-2); font: var(--t-etiqueta); letter-spacing: var(--t-etiqueta-track); }
    .descripcion { color: var(--text-2); font: var(--t-cuerpo-chico); }
    .cifras { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--s2) var(--s3); margin: 0; padding-top: var(--s2); border-top: var(--borde-fino) solid var(--border); }
    .cifras div { display: grid; gap: 2px; min-width: 0; }
    dt { color: var(--text-3); font: var(--t-eyebrow); letter-spacing: var(--t-eyebrow-track); text-transform: uppercase; overflow-wrap: anywhere; }
    dd { margin: 0; font: var(--t-cifra); font-variant-numeric: tabular-nums; }
    .ver { color: var(--brand-texto); font: var(--t-etiqueta); }
    @media (prefers-reduced-motion: reduce) { .esquema { transition: none; } }
  `,
})
export class VistaEsquemas {
  readonly esquemas = input.required<readonly EsquemaDelCatalogo[]>()
  protected readonly t = textosCatalogo.esquemas
}
