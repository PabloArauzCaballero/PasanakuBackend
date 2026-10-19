import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core'
import { toSignal } from '@angular/core/rxjs-interop'
import { ActivatedRoute, Router, RouterLink } from '@angular/router'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { ChipEstado } from '@aportaya/ui/chip-estado/chip-estado'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import { EstadoVacio } from '@aportaya/ui/estado-vacio/estado-vacio'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { TablaDeDatos, type Columna } from '@aportaya/ui/tabla-de-datos/tabla-de-datos'
import { TarjetaKPI } from '@aportaya/ui/tarjeta-kpi/tarjeta-kpi'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { CatalogoDeDatosRemoto, filasDeTablas, filtrarTablas, pagina, type FilaDeTabla } from './dominio'
import { ESTADOS, REVISIONES, aplicarGobierno, metricas, modulos } from './gobierno'
import { textosCatalogo } from './textos'

const FILAS = [10, 20, 50]

/**
 * Datos → Catálogo de datos, con la anatomía del catálogo del portal de Atlas: cabecera, cifras del
 * inventario, barra de filtros (esquema y módulo con conteo, estado, revisión), tabla con estado y
 * revisión en palabras y paginación completa. Todo el estado vive en la URL, así un enlace lleva a
 * la misma vista. «Ninguna coincide» (los filtros) es distinto de un catálogo que no se pudo leer.
 */
@Component({
  selector: 'ap-pantalla-catalogo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, CabeceraDePagina, ChipEstado, EstadoDePantalla, EstadoVacio, Paginacion, RouterLink, TablaDeDatos, TarjetaKPI],
  template: `
    <ap-cabecera-de-pagina icono="baseDeDatos" [eyebrow]="t.eyebrow" [titulo]="t.titulo" [descripcion]="t.descripcion" />
    <ap-estado-de-pantalla [recurso]="remoto.recurso" [mensajeVacio]="t.vacio" [etiquetaDeCarga]="t.cargando" (reintentar)="remoto.recurso.reload()">
      <div class="cifras">
        <ap-tarjeta-kpi [etiqueta]="t.metricas.total" [valor]="'' + cifras().total" icono="baseDeDatos" />
        <ap-tarjeta-kpi [etiqueta]="t.metricas.activas" [valor]="'' + cifras().activas" icono="verificado" tono="ok" [pista]="t.metricas.pista(cifras().activas, cifras().total)" />
        <ap-tarjeta-kpi [etiqueta]="t.metricas.aRevisar" [valor]="'' + cifras().aRevisar" icono="alerta" [tono]="cifras().aRevisar ? 'aviso' : 'ok'" />
        <ap-tarjeta-kpi [etiqueta]="t.metricas.personales" [valor]="'' + cifras().conDatosPersonales" icono="persona" tono="pii" />
        <ap-tarjeta-kpi [etiqueta]="t.metricas.retiro" [valor]="'' + cifras().haciaElRetiro" icono="reloj" [tono]="cifras().haciaElRetiro ? 'aviso' : 'neutro'" />
      </div>
      <ap-barra-de-filtros [etiquetaDeBusqueda]="t.busqueda" [ayudaDeBusqueda]="t.ayudaDeBusqueda" [busqueda]="filtros().q" [selectores]="selectores()" [textoLimpiar]="t.limpiar" (busquedaChange)="filtrar('q', $event)" (selectorChange)="filtrar($event.nombre, $event.valor)" (limpiar)="limpiarTodo()" />
      @if (encontradas().length === 0) {
        <ap-estado-vacio motivo="porFiltro" [titulo]="t.vacio" [explicacion]="t.vacioExplicacion" (actuar)="limpiarTodo()" />
      } @else {
        <ap-tabla-de-datos [titulo]="t.titulo" [columnas]="columnas" [estado]="{ tipo: 'lista', filas: filas() }" [identidad]="porId" densidad="compacta" [cabecera]="true" [textoRegistros]="t.registros" [totalDeRegistros]="encontradas().length">
          <ng-template #celda let-f let-columna="columna">
            @switch (columna.clave) {
              @case ('tabla') { <a [routerLink]="['/tablero/catalogo-de-datos', f.tabla]">{{ f.tabla }}</a> }
              @case ('estado') { <ap-chip-estado [tono]="f.tonoEstado">{{ f.estado }}</ap-chip-estado> }
              @case ('revision') { <ap-chip-estado [tono]="f.tonoRevision">{{ f.revision }}</ap-chip-estado> }
              @case ('personales') { @if (f.personales === 'Sí') { <ap-chip-estado tono="pii">{{ t.columnasLista.personales }}</ap-chip-estado> } @else { {{ f.personales }} } }
              @default { {{ f[columna.clave] }} }
            }
          </ng-template>
        </ap-tabla-de-datos>
        <ap-paginacion etiqueta="Paginación de tablas" [pagina]="paginaActual()" (paginaChange)="filtrar('pagina', '' + $event)" [totalDeFilas]="encontradas().length" [porPagina]="porPagina()" (porPaginaChange)="filtrar('filas', '' + $event)" [opcionesPorPagina]="opcionesDeFilas" nombreDeRegistros="tablas" />
      }
    </ap-estado-de-pantalla>
  `,
  styles: `
    :host { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    ap-estado-de-pantalla { display: grid; grid-template-columns: minmax(0, 1fr); gap: var(--s4); min-width: 0; }
    .cifras { display: grid; gap: var(--s3); grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr)); }
    a { color: var(--text); font-family: var(--mono); font-size: .92em; font-weight: 600; }
    a:focus-visible { outline: var(--borde-foco) solid var(--g300); outline-offset: var(--borde-desfase); }
  `,
})
export class PantallaCatalogo {
  protected readonly t = textosCatalogo
  protected readonly remoto = inject(CatalogoDeDatosRemoto)
  protected readonly opcionesDeFilas = FILAS
  private readonly router = inject(Router)
  private readonly ruta = inject(ActivatedRoute)
  private readonly consulta = toSignal(this.ruta.queryParamMap)

  constructor() {
    inject(ExplicadorDeVista).fijar(textosCatalogo.proposito)
  }

  private readonly catalogo = computed(() => (this.remoto.recurso.hasValue() ? this.remoto.recurso.value() : undefined))
  protected readonly cifras = computed(() => metricas(this.catalogo()?.tablas ?? []))
  protected readonly filtros = computed(() => {
    const q = this.consulta()
    return { q: q?.get('q') ?? '', esquema: q?.get('esquema') ?? '', modulo: q?.get('modulo') ?? '', estado: q?.get('estado') ?? '', revision: q?.get('revision') ?? '' }
  })
  protected readonly paginaActual = computed(() => Number(this.consulta()?.get('pagina') ?? 1) || 1)
  protected readonly porPagina = computed(() => {
    const n = Number(this.consulta()?.get('filas') ?? 20)
    return FILAS.includes(n) ? n : 20
  })
  protected readonly encontradas = computed(() => {
    const f = this.filtros()
    return aplicarGobierno(filtrarTablas(this.catalogo(), f.q, f.esquema ? [f.esquema] : []), { estado: f.estado, revision: f.revision, modulo: f.modulo })
  })
  private readonly retenciones = computed(() => new Map((this.catalogo()?.politicasDeRetencion ?? []).map((p) => [p.id, p.entidad])))
  protected readonly filas = computed(() => filasDeTablas(pagina(this.encontradas(), this.paginaActual(), this.porPagina()), this.retenciones()))

  /** El conteo va en la opción a propósito: delata de un vistazo qué esquema o módulo no aporta nada. */
  protected readonly selectores = computed<Selector[]>(() => {
    const tablas = this.catalogo()?.tablas ?? []
    const f = this.filtros()
    const t = textosCatalogo.filtros
    const porEsquema = (this.catalogo()?.esquemas ?? []).map((e) => ({ valor: e, texto: `${e} (${tablas.filter((x) => x.esquema === e).length})` }))
    const porModulo = modulos(tablas).map((m) => ({ valor: m.valor, texto: `${m.texto} (${tablas.filter((x) => x.modulo === m.valor).length})` }))
    const de = (d: Readonly<Record<string, string>>) => Object.entries(d).map(([valor, texto]) => ({ valor, texto }))
    return [
      { nombre: 'esquema', etiqueta: t.esquema, valor: f.esquema, opciones: porEsquema, ayuda: t.ayudaEsquema },
      { nombre: 'modulo', etiqueta: t.modulo, valor: f.modulo, opciones: porModulo, ayuda: t.ayudaModulo },
      { nombre: 'estado', etiqueta: t.estado, valor: f.estado, opciones: de(ESTADOS), ayuda: t.ayudaEstado },
      { nombre: 'revision', etiqueta: t.revision, valor: f.revision, opciones: de(REVISIONES), ayuda: t.ayudaRevision },
    ]
  })

  protected readonly porId = (f: FilaDeTabla) => f.id
  // Estado y revisión van primero: es lo que se viene a mirar (como en el portal de Atlas).
  protected readonly columnas: Columna<FilaDeTabla>[] = [
    { clave: 'tabla', titulo: textosCatalogo.columnasLista.tabla, mono: true },
    { clave: 'estado', titulo: textosCatalogo.columnasLista.estado },
    { clave: 'revision', titulo: textosCatalogo.columnasLista.revision },
    { clave: 'responsable', titulo: textosCatalogo.columnasLista.responsable },
    { clave: 'personales', titulo: textosCatalogo.columnasLista.personales },
    { clave: 'financieros', titulo: textosCatalogo.columnasLista.financieros },
    { clave: 'retencion', titulo: textosCatalogo.columnasLista.retencion },
    { clave: 'esquema', titulo: textosCatalogo.columnasLista.esquema, mono: true },
    { clave: 'modulo', titulo: textosCatalogo.columnasLista.modulo },
  ]

  /** Cambiar un filtro o el tamaño vuelve a la primera página; cambiar la página, no. */
  protected filtrar(clave: string, valor: string): void {
    const cambios: Record<string, string | null> = { [clave]: valor || null }
    if (clave !== 'pagina') cambios['pagina'] = null
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: cambios, queryParamsHandling: 'merge' })
  }

  protected limpiarTodo(): void {
    void this.router.navigate([], { relativeTo: this.ruta, queryParams: { q: null, esquema: null, modulo: null, estado: null, revision: null, pagina: null }, queryParamsHandling: 'merge' })
  }
}
