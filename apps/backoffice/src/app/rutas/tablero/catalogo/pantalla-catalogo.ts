import { httpResource } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router } from '@angular/router'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { Pestanas, type Pestana } from '@aportaya/ui/pestanas/pestanas'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { GATEWAY } from '../../../nucleo/gateway'
import { filasDeTablas, ResumenDelCatalogoRemoto, urlDeTablas, type FiltrosDeLista, type PaginaDeTablasDelCatalogo } from './dominio'
import { ListaDeTablas } from './lista-de-tablas'
import { ESTADOS, REVISIONES } from './gobierno'
import { textosCatalogo } from './textos'
import { VistaEsquemas } from './vista-esquemas'

const FILAS = [10, 20, 50, 100]
const VISTAS = ['tablas', 'esquemas'] as const

/**
 * Datos → Catálogo de datos, con la anatomía del catálogo del portal de Atlas: cabecera, cifras del inventario,
 * pestañas Tablas · Esquemas, barra de filtros y la tabla paginada **en el servidor**. Todo el estado vive en la URL,
 * así un enlace lleva a la misma vista. Las cifras salen del resumen (no de lo filtrado: son el estado del inventario).
 */
@Component({
  selector: 'ap-pantalla-catalogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, CabeceraDePagina, EstadoDePantalla, EstadoVacio, ListaDeTablas, Pestanas, TarjetaKPI, VistaEsquemas],
  template: `
    <ap-cabecera-de-pagina icono="baseDeDatos" [eyebrow]="t.eyebrow" [titulo]="t.titulo" [descripcion]="t.descripcion" />
    @if (resumen(); as r) {
      <div class="cifras">
        <ap-tarjeta-kpi [etiqueta]="t.metricas.total" [valor]="'' + r.tablas" icono="baseDeDatos" [pista]="t.metricas.pistaTablas(r.tablasDeNegocio, r.tablasDeInfraestructura)" />
        <ap-tarjeta-kpi [etiqueta]="t.metricas.columnas" [valor]="'' + r.columnas" icono="columnas" [pista]="t.metricas.pistaColumnas(r.columnasRevisadas)" />
        <ap-tarjeta-kpi [etiqueta]="t.metricas.relaciones" [valor]="'' + r.relaciones" icono="relacion" [pista]="t.metricas.pistaEsquemas(r.esquemas.length)" />
        <ap-tarjeta-kpi [etiqueta]="t.metricas.personales" [valor]="'' + r.conDatosPersonales" icono="persona" tono="pii" [pista]="t.metricas.pistaFinancieras(r.conDatosFinancieros)" />
        <ap-tarjeta-kpi [etiqueta]="t.metricas.porRevisar" [valor]="'' + (r.porRevision.NECESITA_REVISION + r.porRevision.DETECTADA)" icono="alerta" [tono]="r.porRevision.NECESITA_REVISION ? 'aviso' : 'neutro'" [pista]="t.metricas.pistaRevision(r.porRevision.APROBADA)" />
      </div>
    }
    <ap-pestanas [etiqueta]="t.etiquetaDeVistas" [pestanas]="pestanas()" [elegida]="vista()" (elegidaChange)="irA($event)" />
    <div role="tabpanel" [id]="'panel-' + vista()" [attr.aria-labelledby]="'pestana-' + vista()" class="panel">
      @if (vista() === 'esquemas') {
        <ap-estado-de-pantalla [recurso]="resumenRemoto.recurso" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="resumenRemoto.recurso.reload()">
          @if (resumen(); as r) { <ap-vista-esquemas [esquemas]="r.esquemas" /> }
        </ap-estado-de-pantalla>
      } @else {
        <ap-barra-de-filtros [etiquetaDeBusqueda]="t.busqueda" [ayudaDeBusqueda]="t.ayudaDeBusqueda" [busqueda]="filtros().q" [selectores]="selectores()" [textoLimpiar]="t.limpiar" (busquedaChange)="filtrar('q', $event)" (selectorChange)="filtrar($event.nombre, $event.valor)" (limpiar)="limpiarTodo()" />
        <ap-estado-de-pantalla [recurso]="pagina" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="pagina.reload()">
          @if (datos(); as p) {
            @if (p.total === 0) {
              <ap-estado-vacio motivo="porFiltro" [titulo]="t.vacio" [explicacion]="t.vacioExplicacion" (actuar)="limpiarTodo()" />
            } @else {
              <ap-lista-de-tablas [filas]="filas()" [pagina]="p.pagina" [porPagina]="p.limite" [total]="p.total" [opciones]="opcionesDeFilas" (paginaChange)="filtrar('pagina', '' + $event)" (porPaginaChange)="filtrar('filas', '' + $event)" />
            }
          }
        </ap-estado-de-pantalla>
      }
    </div>
  `,
  styles: `
    :host { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .cifras { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(min(100%, 9rem), 1fr)); }
    .panel, ap-estado-de-pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
  `,
})
export class PantallaCatalogo {
  protected readonly t = textosCatalogo
  protected readonly resumenRemoto = inject(ResumenDelCatalogoRemoto)
  protected readonly opcionesDeFilas = FILAS
  private readonly gateway = inject(GATEWAY)
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly consulta = toSignal(this.ruta.queryParamMap)

  constructor() {
    inject(ExplicadorDeVista).fijar(textosCatalogo.proposito)
  }

  protected readonly resumen = computed(() => (this.resumenRemoto.recurso.hasValue() ? this.resumenRemoto.recurso.value() : undefined))
  protected readonly vista = computed(() => {
    const v = this.consulta()?.get('vista') ?? ''
    return (VISTAS as readonly string[]).includes(v) ? v : 'tablas'
  })
  protected readonly filtros = computed<FiltrosDeLista>(() => {
    const q = this.consulta()
    const de = (clave: string) => q?.get(clave) ?? ''
    return { q: de('q'), esquema: de('esquema'), tipo: de('tipo'), estado: de('estado'), revision: de('revision'), sensibilidad: de('sensibilidad') }
  })
  private readonly paginaPedida = computed(() => Number(this.consulta()?.get('pagina') ?? 1) || 1)
  private readonly porPagina = computed(() => {
    const n = Number(this.consulta()?.get('filas') ?? 20)
    return FILAS.includes(n) ? n : 20
  })
  /** Una página por pedido: cambia la URL, cambia la URL del recurso, y se pide solo esa página. */
  protected readonly pagina = httpResource<PaginaDeTablasDelCatalogo>(() =>
    this.vista() === 'tablas' ? urlDeTablas(this.gateway, this.filtros(), this.paginaPedida(), this.porPagina()) : undefined,
  )
  protected readonly datos = computed(() => (this.pagina.hasValue() ? this.pagina.value() : undefined))
  protected readonly filas = computed(() => filasDeTablas(this.datos()?.items ?? []))

  protected readonly pestanas = computed<Pestana[]>(() => {
    const r = this.resumen()
    return [
      { valor: 'tablas', texto: this.t.vistas.tablas, cuenta: r?.tablas },
      { valor: 'esquemas', texto: this.t.vistas.esquemas, cuenta: r?.esquemas.length },
    ]
  })

  /** El conteo va en la opción a propósito: delata de un vistazo qué esquema no aporta nada. */
  protected readonly selectores = computed<Selector[]>(() => {
    const r = this.resumen()
    const f = this.filtros()
    const t = textosCatalogo.filtros
    const de = (d: Readonly<Record<string, string>>) => Object.entries(d).map(([valor, texto]) => ({ valor, texto }))
    return [
      { nombre: 'esquema', etiqueta: t.esquema, valor: f.esquema, opciones: (r?.esquemas ?? []).map((e) => ({ valor: e.nombre, texto: `${e.nombre} (${e.tablas})` })), ayuda: t.ayudaEsquema },
      { nombre: 'estado', etiqueta: t.estado, valor: f.estado, opciones: de(ESTADOS), ayuda: t.ayudaEstado },
      { nombre: 'revision', etiqueta: t.revision, valor: f.revision, opciones: de(REVISIONES), ayuda: t.ayudaRevision },
      { nombre: 'sensibilidad', etiqueta: t.sensibilidad, valor: f.sensibilidad, opciones: de(t.sensibilidades), ayuda: t.ayudaSensibilidad },
      { nombre: 'tipo', etiqueta: t.tipo, valor: f.tipo, opciones: de(t.tipos), ayuda: t.ayudaTipo },
    ]
  })

  protected irA(vista: string): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { vista: vista === 'tablas' ? null : vista }, queryParamsHandling: 'merge' })
  }

  /** Cambiar un filtro o el tamaño vuelve a la primera página; cambiar la página, no. */
  protected filtrar(clave: string, valor: string): void {
    const cambios: Record<string, string | null> = { [clave]: valor || null }
    if (clave !== 'pagina') cambios['pagina'] = null
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: cambios, queryParamsHandling: 'merge' })
  }

  protected limpiarTodo(): void {
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: { q: null, esquema: null, tipo: null, estado: null, revision: null, sensibilidad: null, pagina: null },
      queryParamsHandling: 'merge',
    })
  }
}
