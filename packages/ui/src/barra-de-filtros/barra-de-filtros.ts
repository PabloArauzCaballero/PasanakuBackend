import { ChangeDetectionStrategy, Component, OnDestroy, computed, input, output } from '@angular/core'
import { Boton } from '../boton/boton'
import { CampoBusqueda } from '../campo-busqueda/campo-busqueda'
import { ListaDeOpciones, type Opcion } from '../lista-de-opciones/lista-de-opciones'
import { Tooltip } from '../tooltip/tooltip'
import { Icono } from '../icono/icono'

/** Un selector de la barra: su nombre (la clave en la URL), su etiqueta, sus opciones y el valor vigente. */
export type Selector = { nombre: string; etiqueta: string; opciones: readonly Opcion[]; valor: string; ayuda?: string; textoTodos?: string }

const ESPERA_DE_BUSQUEDA_MS = 350

/**
 * La barra de filtros de un listado: el buscador (que espera a que se deje de escribir antes de avisar), un selector
 * por criterio con una opción «Todos» explicada, y «Limpiar». Va en una fila sobre el lienzo, sin tarjeta: la tarjeta
 * con media fila vacía pesaba más que la tabla que filtra (2026-10-27). Es presentacional: quien la aloja guarda el
 * estado y vuelve a la primera página al cambiar un filtro.
 */
@Component({
  selector: 'ap-barra-de-filtros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Boton, CampoBusqueda, ListaDeOpciones, Tooltip, Icono],
  host: { role: 'search', '[attr.aria-label]': 'etiqueta()' },
  template: `
    <div class="buscador">
      <ap-campo-busqueda [etiqueta]="etiquetaDeBusqueda()" [valor]="busqueda()" (valorChange)="escribir($event)" />
      @if (ayudaDeBusqueda()) {
        <ap-tooltip [texto]="ayudaDeBusqueda()!"><button type="button" class="ayuda" [attr.aria-label]="'Ayuda sobre ' + etiquetaDeBusqueda()"><ap-icono nombre="info" tamano="s4" /></button></ap-tooltip>
      }
    </div>
    @for (s of selectores(); track s.nombre) {
      <ap-lista-de-opciones [compacta]="true" [etiqueta]="s.etiqueta" [opciones]="s.opciones" [valor]="s.valor" [textoVacio]="s.textoTodos ?? 'Todos'" [tooltip]="s.ayuda" (valorChange)="selectorChange.emit({ nombre: s.nombre, valor: $event })" />
    }
    @if (hayFiltros()) {
      <ap-boton variante="fantasma" tamano="sm" (pulsado)="limpiar.emit()">{{ textoLimpiar() }}</ap-boton>
    }
  `,
  styles: `
    /* Fila flexible: el buscador crece (es el filtro que más se usa) y cada selector tiene un ancho acotado, así cinco
       filtros no estiran el último a toda la fila ni uno solo deja media barra vacía. */
    :host { display: flex; flex-wrap: wrap; align-items: flex-end; gap: var(--s3); }
    .buscador { position: relative; display: flex; align-items: center; flex: 2 1 calc(var(--s7) * 4.5); min-width: 0; }
    ap-campo-busqueda { flex: 1; min-width: 0; }
    /* La ayuda vive dentro del borde del campo, a la derecha, en vez de colgar suelta al costado. */
    .buscador:has(.ayuda) ap-campo-busqueda { --campo-busqueda-fin: var(--area-tactil); }
    .buscador > ap-tooltip { position: absolute; right: var(--s1); display: inline-flex; }
    .ayuda { display: inline-flex; align-items: center; justify-content: center; width: calc(var(--area-tactil) - var(--s2)); height: calc(var(--area-tactil) - var(--s2)); padding: 0; border: 0; border-radius: var(--r-sm); background: transparent; color: var(--text-3); cursor: help; }
    .ayuda:hover { color: var(--text); background: var(--surface-2); }
    .ayuda:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    ap-lista-de-opciones { flex: 1 1 calc(var(--s7) * 3); max-width: calc(var(--s7) * 5.5); min-width: 0; }
    ap-boton { flex: none; }
  `,
})
export class BarraDeFiltros implements OnDestroy {
  readonly etiqueta = input('Filtros')
  readonly etiquetaDeBusqueda = input('Buscar')
  readonly ayudaDeBusqueda = input<string>()
  readonly busqueda = input('')
  readonly selectores = input<readonly Selector[]>([])
  readonly textoLimpiar = input('Limpiar')
  readonly busquedaChange = output<string>()
  readonly selectorChange = output<{ nombre: string; valor: string }>()
  readonly limpiar = output<void>()
  readonly hayFiltros = computed(() => this.busqueda() !== '' || this.selectores().some((s) => s.valor !== ''))
  private temporizador: ReturnType<typeof setTimeout> | undefined

  escribir(valor: string): void {
    clearTimeout(this.temporizador)
    // Borrar con la cruz avisa al instante: la persona ya decidió.
    if (valor === '') return this.busquedaChange.emit(valor)
    this.temporizador = setTimeout(() => this.busquedaChange.emit(valor), ESPERA_DE_BUSQUEDA_MS)
  }

  ngOnDestroy(): void {
    clearTimeout(this.temporizador)
  }
}
