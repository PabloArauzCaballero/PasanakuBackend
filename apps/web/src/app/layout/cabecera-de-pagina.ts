import { ChangeDetectionStrategy, Component, input } from '@angular/core'

/**
 * La banda de título de las páginas interiores: la misma voz visual que el hero de la
 * portada (etiqueta en mayúsculas, titular de la marca, bajada), en tamaño de página.
 * Lleva el único `h1` de la página.
 */
@Component({
  selector: 'ap-cabecera-de-pagina',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { style: 'display: contents' },
  template: `
    <section class="cabecera-pagina grano">
      <div class="contenedor contenedor--angosto">
        <span class="etiqueta">{{ etiqueta() }}</span>
        <h1>{{ titulo() }}</h1>
        @if (bajada()) { <p class="bajada">{{ bajada() }}</p> }
      </div>
    </section>
  `,
})
export class CabeceraDePagina {
  readonly etiqueta = input.required<string>()
  readonly titulo = input.required<string>()
  readonly bajada = input<string>()
}
