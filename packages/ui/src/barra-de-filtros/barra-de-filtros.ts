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
 * La barra de filtros de un listado, como en el portal de Atlas: una tarjeta con el buscador
 * (que espera a que se deje de escribir antes de avisar), un selector por criterio con una opción
 * «Todos» explicada, y «Limpiar». Es presentacional: quien la aloja guarda el estado (en la URL)
 * y vuelve a la primera página al cambiar un filtro.
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
    :host { display: flex; flex-wrap: wrap; align-items: flex-end; gap: var(--s3); padding: var(--s3); border: var(--borde-fino) solid var(--border); border-radius: var(--r-lg); background: var(--surface); box-shadow: var(--sombra-1); }
    /* El buscador conserva un ancho mínimo: con seis filtros la fila envuelve en vez de aplastarlo. */
    .buscador { display: flex; align-items: center; gap: var(--s1); flex: 1 1 14rem; min-width: min(14rem, 100%); }
    ap-campo-busqueda { flex: 1; min-width: 0; }
    .ayuda { display: inline-flex; align-items: center; justify-content: center; width: var(--area-tactil); height: var(--area-tactil); padding: 0; border: 0; border-radius: var(--r-pill); background: transparent; color: var(--text-3); cursor: help; }
    .ayuda:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    ap-lista-de-opciones { flex: 1 1 11rem; min-width: min(11rem, 100%); }
    ap-boton { margin-left: auto; }
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
