import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core'
import { CampoBusqueda } from '@aportaya/ui/campo-busqueda/campo-busqueda'
import { SelectorSegmentado, type Segmento } from '@aportaya/ui/selector-segmentado/selector-segmentado'
import type { FiltroDeEstado } from './vista-de-tutoriales'
import { textosAyuda } from './textos'

/** Buscador y filtros del centro de ayuda. No filtra: emite qué se eligió. */
@Component({
  selector: 'ap-filtros-de-ayuda',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CampoBusqueda, SelectorSegmentado],
  template: `
    <ap-campo-busqueda data-tutorial-id="ayuda-buscador" [etiqueta]="t.buscar" [(valor)]="texto" />
    <div class="grupos" data-tutorial-id="ayuda-filtros">
      <ap-selector-segmentado [etiqueta]="t.filtrarEstado" [segmentos]="estados" [(elegido)]="estado" />
      @if (categorias().length > 1) {
        <ap-selector-segmentado [etiqueta]="t.filtrarCategoria" [segmentos]="segmentosDeCategoria()" [(elegido)]="categoria" />
      }
    </div>
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: var(--s3); }
    .grupos { display: flex; flex-wrap: wrap; gap: var(--s3); }
  `,
})
export class FiltrosDeAyuda {
  protected readonly t = textosAyuda
  readonly texto = model('')
  readonly estado = model<FiltroDeEstado>('todos')
  /** Cadena vacía = todas. Se usa vacía y no `null` porque viaja en la dirección. */
  readonly categoria = model('')
  readonly categorias = input.required<readonly string[]>()

  protected readonly estados: Segmento[] = [
    { valor: 'todos', texto: textosAyuda.todos },
    { valor: 'pendiente', texto: textosAyuda.pendientes },
    { valor: 'en-progreso', texto: textosAyuda.enProgreso },
    { valor: 'completado', texto: textosAyuda.completados },
    { valor: 'obligatorio', texto: textosAyuda.obligatorios },
  ]

  protected readonly segmentosDeCategoria = computed<Segmento[]>(() => [
    { valor: '', texto: textosAyuda.todos },
    ...this.categorias().map((c) => ({ valor: c, texto: c })),
  ])
}
