import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { BandaDeProposito } from '@aportaya/ui/banda-de-proposito/banda-de-proposito'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import { BarraDeFiltros } from '../../../nucleo/filtros/barra-de-filtros'
import { ListaDeOpciones, opcionesDe } from '../../cumplimiento/compartido/campos'
import { CatalogoDeDatosRemoto, filasDeTablas, filtrarTablas, pagina, type FilaDeTabla } from './dominio'
import { ESTADOS, REVISIONES, aplicarGobierno, metricas, modulos } from './gobierno'
import { textosCatalogo } from './textos'

const POR_PAGINA = 20

/**
 * Sistemas → Catálogo de datos, con la ficha de gobierno de cada tabla (CU-130), como el portal de Atlas: cifras del
 * inventario, filtros por estado, revisión y módulo, y columnas de estado y revisión dichas en palabras (no solo
 * color). Búsqueda, filtros y página viven en la URL, así un enlace lleva a la misma vista.
 */
@Component({
  selector: 'ap-pantalla-catalogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BandaDeProposito, BarraDeFiltros, ChipEstado, EstadoDePantalla, ListaDeOpciones, Paginacion, RouterLink, TablaDeDatos, TarjetaKPI],
  template: `
    <ap-banda-de-proposito [texto]="t.proposito" />
    <section class="pantalla">
      <h1>{{ t.titulo }}</h1>
      <ap-estado-de-pantalla [recurso]="remoto.recurso" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="remoto.recurso.reload()">
        <div class="cifras">
          <ap-tarjeta-kpi [etiqueta]="t.metricas.total" [valor]="'' + cifras().total" />
          <ap-tarjeta-kpi [etiqueta]="t.metricas.activas" [valor]="'' + cifras().activas" />
          <ap-tarjeta-kpi [etiqueta]="t.metricas.aRevisar" [valor]="'' + cifras().aRevisar" />
          <ap-tarjeta-kpi [etiqueta]="t.metricas.personales" [valor]="'' + cifras().conDatosPersonales" />
          <ap-tarjeta-kpi [etiqueta]="t.metricas.retiro" [valor]="'' + cifras().haciaElRetiro" />
        </div>
        <ap-barra-de-filtros [etiquetaDeBusqueda]="t.busqueda" [definiciones]="[]" />
        <div class="selectores">
          <ap-lista-de-opciones [etiqueta]="t.filtroEsquemas" textoVacio="Todos" [opciones]="esquemas()" [valor]="elegidos()[0] ?? ''" (valorChange)="filtrar('filtro', $event)" />
          <ap-lista-de-opciones [etiqueta]="t.filtros.estado" textoVacio="Todos" [opciones]="estados" [valor]="filtros().estado" (valorChange)="filtrar('estado', $event)" />
          <ap-lista-de-opciones [etiqueta]="t.filtros.revision" textoVacio="Todos" [opciones]="revisiones" [valor]="filtros().revision" (valorChange)="filtrar('revision', $event)" />
          <ap-lista-de-opciones [etiqueta]="t.filtros.modulo" textoVacio="Todos" [opciones]="opcionesDeModulo()" [valor]="filtros().modulo" (valorChange)="filtrar('modulo', $event)" />
        </div>
        <p class="resultados" role="status">{{ t.resultados(encontradas().length, total()) }}</p>
        <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [estado]="{ tipo: 'lista', filas: filas() }" [textoVacio]="t.vacio" [identidad]="porId" densidad="compacta">
          <ng-template #celda let-f let-columna="columna">
            @if (columna.clave === 'tabla') {
              <a [routerLink]="['/tablero/catalogo-de-datos', f.tabla]">{{ f.tabla }}</a>
            } @else if (columna.clave === 'estado') {
              <ap-chip-estado [tono]="f.tonoEstado">{{ f.estado }}</ap-chip-estado>
            } @else if (columna.clave === 'revision') {
              <ap-chip-estado [tono]="f.tonoRevision">{{ f.revision }}</ap-chip-estado>
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
    .pantalla { padding: var(--s5); max-width: 80rem; display: grid; gap: var(--s4); }
    /* Sin esto la tabla ensancha la grilla y la página entera desplaza en horizontal (prueba visual H9). */
    .pantalla > * { min-width: 0; }
    .cifras { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr)); }
    .selectores { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr)); }
    ap-barra-de-filtros { display: block; }
    h1 { margin: 0; }
    .resultados { margin: 0; color: var(--text-2); }
    a { color: var(--text); font-weight: 600; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class PantallaCatalogo {
  protected readonly t = textosCatalogo
  protected readonly remoto = inject(CatalogoDeDatosRemoto)
  protected readonly porPagina = POR_PAGINA
  protected readonly estados = opcionesDe(ESTADOS)
  protected readonly revisiones = opcionesDe(REVISIONES)
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly consulta = toSignal(this.ruta.queryParamMap)

  private readonly catalogo = computed(() => (this.remoto.recurso.hasValue() ? this.remoto.recurso.value() : undefined))
  protected readonly total = computed(() => this.catalogo()?.tablas.length ?? 0)
  protected readonly cifras = computed(() => metricas(this.catalogo()?.tablas ?? []))
  protected readonly opcionesDeModulo = computed(() => modulos(this.catalogo()?.tablas ?? []))
  protected readonly esquemas = computed(() => (this.catalogo()?.esquemas ?? []).map((e) => ({ valor: e, texto: e })))
  protected readonly filtros = computed(() => ({
    estado: this.consulta()?.get('estado') ?? '',
    revision: this.consulta()?.get('revision') ?? '',
    modulo: this.consulta()?.get('modulo') ?? '',
  }))
  protected readonly elegidos = computed(() => this.consulta()?.get('filtro')?.split(',').filter(Boolean) ?? [])
  protected readonly encontradas = computed(() =>
    aplicarGobierno(filtrarTablas(this.catalogo(), this.consulta()?.get('q') ?? '', this.elegidos()), this.filtros()),
  )
  private readonly retenciones = computed(() => new Map((this.catalogo()?.politicasDeRetencion ?? []).map((p) => [p.id, p.entidad])))
  protected readonly paginaActual = computed(() => Number(this.consulta()?.get('pagina') ?? 1) || 1)
  protected readonly filas = computed(() => filasDeTablas(pagina(this.encontradas(), this.paginaActual(), POR_PAGINA), this.retenciones()))

  protected readonly porId = (f: FilaDeTabla) => f.id
  // Estado y revisión van primero: es lo que se viene a mirar (como en el portal de Atlas) y no puede quedar cortado.
  protected readonly columnas: Columna<FilaDeTabla>[] = [
    { clave: 'tabla', titulo: textosCatalogo.columnasLista.tabla },
    { clave: 'estado', titulo: textosCatalogo.columnasLista.estado },
    { clave: 'revision', titulo: textosCatalogo.columnasLista.revision },
    { clave: 'responsable', titulo: textosCatalogo.columnasLista.responsable },
    { clave: 'personales', titulo: textosCatalogo.columnasLista.personales },
    { clave: 'financieros', titulo: textosCatalogo.columnasLista.financieros },
    { clave: 'retencion', titulo: textosCatalogo.columnasLista.retencion },
    { clave: 'esquema', titulo: textosCatalogo.columnasLista.esquema },
    { clave: 'modulo', titulo: textosCatalogo.columnasLista.modulo },
  ]

  protected filtrar(clave: 'filtro' | 'estado' | 'revision' | 'modulo', valor: string): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { [clave]: valor || null, pagina: null }, queryParamsHandling: 'merge' })
  }

  protected irA(n: number): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { pagina: n }, queryParamsHandling: 'merge' })
  }
}
