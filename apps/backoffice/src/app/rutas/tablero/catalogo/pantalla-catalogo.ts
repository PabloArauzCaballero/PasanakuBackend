import { httpResource } from '@angular/common/http'
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { Pestanas, type Pestana } from '@aportaya/ui/pestanas/pestanas'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { GATEWAY } from '../../../nucleo/gateway'
import { filasDeTablas, ResumenDelCatalogoRemoto, urlDeTablas, type FilaDeTabla, type FiltrosDeLista, type PaginaDeTablasDelCatalogo } from './dominio'
import { ESTADOS, REVISIONES } from './gobierno'
import { textosCatalogo } from './textos'
import { VistaEsquemas } from './vista-esquemas'

const FILAS = [10, 20, 50]
const VISTAS = ['tablas', 'esquemas'] as const

/**
 * Datos → Catálogo de datos, con la anatomía del catálogo del portal de Atlas: cabecera, cifras del inventario,
 * pestañas Tablas · Esquemas, barra de filtros y la tabla paginada **en el servidor**. Todo el estado vive en la URL,
 * así un enlace lleva a la misma vista. Las cifras salen del resumen (no de lo filtrado: son el estado del inventario).
 */
@Component({
  selector: 'ap-pantalla-catalogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, CabeceraDePagina, ChipEstado, EstadoDePantalla, EstadoVacio, Paginacion, Pestanas, RouterLink, TablaDeDatos, TarjetaKPI, VistaEsquemas],
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
              <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [estado]="{ tipo: 'lista', filas: filas() }" [identidad]="porId" densidad="compacta" [cabecera]="true" [textoRegistros]="t.registros" [totalDeRegistros]="p.total">
                <ng-template #celda let-f let-columna="columna">
                  @switch (columna.clave) {
                    @case ('tabla') {
                      <a class="nombre" [routerLink]="['/tablero/catalogo-de-datos', f.esquema, f.tabla]">{{ f.tabla }}</a>
                      <span class="que-es">{{ f.queEs }}</span>
                    }
                    @case ('esquema') {
                      <code>{{ f.esquema }}</code>
                      @if (f.infraestructura) { <span class="que-es">{{ t.infraestructura }}</span> }
                    }
                    @case ('estado') { <ap-chip-estado [tono]="f.tonoEstado">{{ f.estado }}</ap-chip-estado> }
                    @case ('revision') { <ap-chip-estado [tono]="f.tonoRevision">{{ f.revision }}</ap-chip-estado> }
                    @case ('sensible') { <ap-chip-estado [tono]="f.tonoSensible">{{ f.sensible }}</ap-chip-estado> }
                    @default { {{ f[columna.clave] }} }
                  }
                </ng-template>
              </ap-tabla-de-datos>
              <ap-paginacion etiqueta="Paginación de tablas" [pagina]="p.pagina" (paginaChange)="filtrar('pagina', '' + $event)" [totalDeFilas]="p.total" [porPagina]="p.limite" (porPaginaChange)="filtrar('filas', '' + $event)" [opcionesPorPagina]="opcionesDeFilas" nombreDeRegistros="tablas" />
            }
          }
        </ap-estado-de-pantalla>
      }
    </div>
  `,
  styles: `
    :host { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .cifras { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(min(100%, 11rem), 1fr)); }
    .panel, ap-estado-de-pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .nombre { display: block; color: var(--text); font-family: var(--mono); font-size: .92em; font-weight: 600; text-decoration: none; overflow-wrap: anywhere; }
    .nombre:hover { color: var(--brand-texto); text-decoration: underline; }
    .nombre:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
    .que-es { display: block; margin-top: var(--s1); min-width: 24ch; max-width: 40ch; color: var(--text-2); font: var(--t-cuerpo-chico); white-space: normal; }
    code { font-family: var(--mono); font-size: .92em; }
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

  protected readonly porId = (f: FilaDeTabla) => f.id
  // Lo que se viene a mirar va primero (como en el portal de Atlas): la tabla con lo que es, su estado y su revisión.
  protected readonly columnas: Columna<FilaDeTabla>[] = [
    { clave: 'tabla', titulo: textosCatalogo.columnasLista.tabla },
    { clave: 'esquema', titulo: textosCatalogo.columnasLista.esquema },
    { clave: 'estado', titulo: textosCatalogo.columnasLista.estado },
    { clave: 'revision', titulo: textosCatalogo.columnasLista.revision },
    { clave: 'sensible', titulo: textosCatalogo.columnasLista.sensible },
    { clave: 'responsable', titulo: textosCatalogo.columnasLista.responsable },
    { clave: 'columnas', titulo: textosCatalogo.columnasLista.columnas, numerica: true },
  ]

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
