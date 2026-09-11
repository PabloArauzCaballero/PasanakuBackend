import { toSignal } from '@angular/core/rxjs-interop'
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { ChipsDeFiltro, Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { CampoBusqueda } from '@aportaya/ui/campo-busqueda/campo-busqueda'

/**
 * Chips de filtro + búsqueda con **el estado en la `querystring`**: `?filtro=a,b&q=texto`.
 * Nunca un dato sensible en la URL — solo claves de filtro y el texto de búsqueda que el
 * propio operador tipeó — así que es compartible y sobrevive a recargar la página.
 *
 * Cambiar un filtro vuelve la tabla a la página 1: seguir en la página 6 de un filtro que
 * ya no aplica muestra una lista que no es la que el operador pidió.
 */
@Component({
  selector: 'ap-barra-de-filtros',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipsDeFiltro, CampoBusqueda],
  template: `
    <div class="barra">
      @if (incluirBusqueda()) {
        <ap-campo-busqueda [etiqueta]="etiquetaDeBusqueda()" [valor]="busqueda()" (valorChange)="cambiarBusqueda($event)" />
      }
      @if (definiciones().length > 0) {
        <ap-chips-de-filtro [etiqueta]="etiqueta()" [filtros]="definiciones()" [elegidos]="elegidos()" (elegidosChange)="cambiarElegidos($event)" />
      }
    </div>
  `,
  styles: `
    .barra { display: flex; flex-wrap: wrap; align-items: center; gap: var(--s3); }
  `,
})
export class BarraDeFiltros {
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly queryParamMap = toSignal(this.ruta.queryParamMap, { requireSync: false })

  readonly etiqueta = input('Filtrar')
  readonly etiquetaDeBusqueda = input('Buscar')
  readonly incluirBusqueda = input(true)
  readonly definiciones = input.required<Filtro[]>()

  readonly elegidos = computed(() => this.queryParamMap()?.get('filtro')?.split(',').filter(Boolean) ?? [])
  readonly busqueda = computed(() => this.queryParamMap()?.get('q') ?? '')

  cambiarElegidos(nuevos: string[]): void {
    this.navegar({ filtro: nuevos.length ? nuevos.join(',') : null, pagina: null })
  }

  cambiarBusqueda(nuevo: string): void {
    this.navegar({ q: nuevo || null, pagina: null })
  }

  private navegar(cambios: Record<string, string | null>): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: cambios, queryParamsHandling: 'merge' })
  }
}
