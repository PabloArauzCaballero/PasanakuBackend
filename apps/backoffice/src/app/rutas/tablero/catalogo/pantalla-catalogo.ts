import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import type { Filtro } from '@aportaya/ui/chips-de-filtro/chips-de-filtro'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { CatalogoDeDatosRemoto, filasDeTablas, filtrarTablas, pagina, type FilaDeTabla } from './dominio'
import { textosCatalogo } from './textos'

const POR_PAGINA = 20

/**
 * Sistemas → Catálogo de datos (H7.S2). Busca por tabla, columna o concepto, sin acentos ni mayúsculas, y filtra por
 * esquema; búsqueda, esquemas y página viven en la URL (`?q=&filtro=&pagina=`), así un enlace lleva a la misma vista.
 * Cada tabla lleva a su ficha. «Ninguna coincide» (la búsqueda) es distinto de un catálogo que no se pudo leer.
 */
@Component({
  selector: 'ap-pantalla-catalogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, BarraDeFiltros, EstadoDePantalla, Paginacion, RouterLink, TablaDeDatos],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <section class="pantalla">
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-pantalla [recurso]="remoto.recurso" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="remoto.recurso.reload()">
        <ap-barra-de-filtros [etiqueta]="t.filtroEsquemas" [etiquetaDeBusqueda]="t.busqueda" [definiciones]="esquemas()" />
        <p class="resultados" role="status">{{ t.resultados(encontradas().length, total()) }}</p>
        <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [estado]="{ tipo: 'lista', filas: filas() }" [textoVacio]="t.vacio" [identidad]="porId" densidad="compacta">
          <ng-template #celda let-f let-columna="columna">
            @if (columna.clave === 'tabla') {
              <a [routerLink]="['/tablero/catalogo-de-datos', f.tabla]">{{ f.tabla }}</a>
            } @else {
              {{ f[columna.clave] }}
            }
          </ng-template>
        </ap-tabla-de-datos>
        @if (encontradas().length > porPagina) {
          <ap-paginacion [pagina]="paginaActual()" [totalDeFilas]="encontradas().length" [porPagina]="porPagina" (paginaChange)="irA($event)" />
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { padding: var(--s5); max-width: 72rem; display: grid; gap: var(--s4); }
    h1 { margin: 0; }
    .resultados { margin: 0; color: var(--text-2); }
    a { color: var(--text); }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class PantallaCatalogo {
  protected readonly t = textosCatalogo
  protected readonly remoto = inject(CatalogoDeDatosRemoto)
  protected readonly porPagina = POR_PAGINA
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly consulta = toSignal(this.ruta.queryParamMap)

  private readonly catalogo = computed(() => (this.remoto.recurso.hasValue() ? this.remoto.recurso.value() : undefined))
  protected readonly total = computed(() => this.catalogo()?.tablas.length ?? 0)
  protected readonly esquemas = computed<Filtro[]>(() => (this.catalogo()?.esquemas ?? []).map((e) => ({ valor: e, texto: e })))
  private readonly elegidos = computed(() => this.consulta()?.get('filtro')?.split(',').filter(Boolean) ?? [])
  protected readonly encontradas = computed(() => filtrarTablas(this.catalogo(), this.consulta()?.get('q') ?? '', this.elegidos()))
  protected readonly paginaActual = computed(() => Number(this.consulta()?.get('pagina') ?? 1) || 1)
  protected readonly filas = computed(() => filasDeTablas(pagina(this.encontradas(), this.paginaActual(), POR_PAGINA)))

  protected readonly porId = (f: FilaDeTabla) => f.id
  protected readonly columnas: Columna<FilaDeTabla>[] = [
    { clave: 'tabla', titulo: textosCatalogo.columnasLista.tabla },
    { clave: 'esquema', titulo: textosCatalogo.columnasLista.esquema },
    { clave: 'servicio', titulo: textosCatalogo.columnasLista.servicio },
    { clave: 'columnas', titulo: textosCatalogo.columnasLista.columnas, numerica: true },
    { clave: 'queEs', titulo: textosCatalogo.columnasLista.queEs },
  ]

  protected irA(n: number): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { pagina: n }, queryParamsHandling: 'merge' })
  }
}
