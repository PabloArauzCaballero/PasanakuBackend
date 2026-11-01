import { ChangeDetectionStrategy, Component, input } from '@angular/core'

/**
 * La cabecera de toda pantalla del backoffice: título, descripción y un carril de
 * acciones. Va sobre el lienzo, no dentro de una tarjeta: un bloque llamativo que no significa nada compite con lo que
 * sí tiene algo que decir.
 *
 * El *eyebrow* (la sección) solo se ve en el celular: en pantallas anchas ya lo dicen las migas de la cabecera fija, y
 * repetido arriba del título era ruido (2026-10-27). El título va en el color del texto, no en el de la marca: el
 * verde queda para lo que se puede tocar.
 *
 * En un portal de decenas de vistas que comparten tipografía, el ícono es la única señal que
 * distingue una pantalla de otra antes de leerla. Las acciones se proyectan con `acciones`; en
 * el celular el carril se desplaza en horizontal en vez de apilarse y empujar el contenido.
 */
@Component({
  selector: 'ap-cabecera-de-pagina',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header>
      <div class="identidad">
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
    .textos { min-width: 0; }
    .eyebrow { margin: 0 0 var(--s1); font: var(--t-eyebrow); letter-spacing: var(--t-eyebrow-track); text-transform: uppercase; color: var(--text-3); }
    /* En el celular el título se recompone en vez de encogerse: el rol titulo-1 en un teléfono angosto parte en tres líneas. */
    h1 { margin: 0; font: var(--t-titulo-2); letter-spacing: var(--t-titulo-2-track); color: var(--text); overflow-wrap: anywhere; text-wrap: pretty; }
    .descripcion { margin: var(--s1) 0 0; max-width: 72ch; font: var(--t-cuerpo-chico); color: var(--text-2); overflow-wrap: anywhere; text-wrap: pretty; }
    /* En el celular las acciones se envuelven: en un carril desplazable la acción principal quedaba cortada en el borde (2026-10-27). */
    .acciones { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s2); }
    .acciones:empty { display: none; }
    @media (min-width: 40rem) {
        .eyebrow { display: none; }
      h1 { font: var(--t-titulo-1); letter-spacing: var(--t-titulo-1-track); }
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
}
