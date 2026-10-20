import { ChangeDetectionStrategy, Component, input } from '@angular/core'
import { Icono, NombreDeIcono } from '../icono/icono'

/**
 * La cabecera de toda pantalla del backoffice, con la anatomía del portal interno de Atlas:
 * loseta con el ícono del módulo, *eyebrow*, título en el acento, descripción y un carril de
 * acciones. Va sobre el lienzo, no dentro de una tarjeta: un bloque llamativo que no significa
 * nada compite con lo que sí tiene algo que decir.
 *
 * En un portal de decenas de vistas que comparten tipografía, el ícono es la única señal que
 * distingue una pantalla de otra antes de leerla. Las acciones se proyectan con `acciones`; en
 * el celular el carril se desplaza en horizontal en vez de apilarse y empujar el contenido.
 */
@Component({
  selector: 'ap-cabecera-de-pagina',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icono],
  template: `
    <header>
      <div class="identidad">
        @if (icono()) { <span class="loseta" aria-hidden="true"><ap-icono [nombre]="icono()!" tamano="s5" /></span> }
        <div class="textos">
          @if (eyebrow()) { <p class="eyebrow">{{ eyebrow() }}</p> }
          <h1>{{ titulo() }}</h1>
          @if (descripcion()) { <p class="descripcion">{{ descripcion() }}</p> }
        </div>
      </div>
      <div class="acciones"><ng-content select="[acciones]" /></div>
    </header>
  `,
  styles: `
    :host { display: block; }
    header { display: flex; flex-direction: column; gap: var(--s4); }
    .identidad { display: flex; align-items: flex-start; gap: var(--s3); min-width: 0; }
    .loseta { display: none; flex: none; width: calc(var(--s6) + var(--s2)); height: calc(var(--s6) + var(--s2)); align-items: center; justify-content: center; border-radius: var(--r-lg); background: linear-gradient(135deg, color-mix(in srgb, var(--brand-bg) 85%, var(--white)), var(--brand-bg)); border: var(--borde-fino) solid color-mix(in srgb, var(--brand) 28%, transparent); color: var(--brand-texto); margin-top: var(--s1); box-shadow: var(--sombra-2); }
    .textos { min-width: 0; }
    .eyebrow { margin: 0 0 var(--s1); font: var(--t-eyebrow); letter-spacing: var(--t-eyebrow-track); text-transform: uppercase; color: var(--text-3); }
    /* En el celular el título se recompone en vez de encogerse: el rol titulo-1 en un teléfono angosto parte en tres líneas. */
    h1 { margin: 0; font: var(--t-titulo-2); letter-spacing: var(--t-titulo-2-track); color: var(--text); overflow-wrap: anywhere; text-wrap: pretty; font-weight: 700; }
    .descripcion { margin: var(--s1) 0 0; max-width: 70ch; font: var(--t-cuerpo-chico); color: var(--text-2); overflow-wrap: anywhere; line-height: 1.5; }
    /* Carril: en el celular tres botones se envolvían en tres líneas y empujaban el contenido fuera de la primera pantalla. */
    .acciones { display: flex; align-items: center; gap: var(--s2); overflow-x: auto; scrollbar-width: none; margin: 0 calc(var(--s3) * -1); padding: 0 var(--s3) var(--s1); }
    .acciones:empty { display: none; }
    @media (min-width: 40rem) {
      .loseta { display: inline-flex; }
      h1 { font: var(--t-titulo-1); letter-spacing: var(--t-titulo-1-track); }
      .acciones { flex-wrap: wrap; overflow: visible; margin: 0; padding: 0; }
    }
    @media (min-width: 64rem) {
      header { flex-direction: row; align-items: flex-start; justify-content: space-between; }
      .acciones { flex: none; justify-content: flex-end; }
    }
  `,
})
export class CabeceraDePagina {
  readonly titulo = input.required<string>()
  readonly eyebrow = input<string>()
  readonly descripcion = input<string>()
  readonly icono = input<NombreDeIcono>()
}
