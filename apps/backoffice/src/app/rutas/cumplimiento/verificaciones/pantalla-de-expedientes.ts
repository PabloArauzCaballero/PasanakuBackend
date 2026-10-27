import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core'
import { BarraDeFiltros, type Selector } from '@aportaya/ui/barra-de-filtros/barra-de-filtros'
import { CabeceraDePagina } from '@aportaya/ui/cabecera-de-pagina/cabecera-de-pagina'
import { EstadoDePantalla } from '@aportaya/ui/estado-de-pantalla/estado-de-pantalla'
import type { ColumnaExportable } from '@aportaya/ui/exportar-listado/exportar'
import { ExportarListado } from '@aportaya/ui/exportar-listado/exportar-listado'
import { crearListado } from '@aportaya/ui/listado/crear-listado'
import { Paginacion } from '@aportaya/ui/paginacion/paginacion'
import { CaraDelExpediente, ExpedienteEnRevisionEstadoEnum, type ExpedienteEnRevision } from 'clientes/angular/identidad'
import type { ErrorTraducido } from '../../../nucleo/errores'
import { ExplicadorDeVista } from '../../../nucleo/explicador-de-vista'
import { crearResolver, expedientesEnEstado, POR_DECIDIR, recortar, vencimientoDe } from '../dominio/cu02-expedientes'
import { textosCumplimiento } from '../textos'
import type { Decision } from './decision-de-expediente'
import { TarjetaDeExpediente } from './tarjeta-de-expediente'
import { Exportador } from '../../../nucleo/exportador'
import { nombreSegunCantidad } from '@aportaya/ui/listado/singular'

const T = textosCumplimiento.verificaciones
const CARAS = Object.values(CaraDelExpediente)
const VENCIMIENTO: Record<string, string> = { vigente: 'Vigente', vencido: T.documentoVencido, 'sin-fecha': T.sinVencimiento }
const textoVencimiento = (e: ExpedienteEnRevision) => VENCIMIENTO[vencimientoDe(e)]
const textoFotos = (e: ExpedienteEnRevision) => (CARAS.every((c) => e.fotos.includes(c)) ? 'Completas' : 'Incompletas')
/** El archivo lleva lo que la tarjeta muestra, ni más (sin ids internos ni fotos) ni distinto. Ver A3 del plan. */
const EXPORTABLES: ColumnaExportable<ExpedienteEnRevision>[] = [
  { clave: 'nombreCompleto', titulo: 'Nombre' },
  { clave: 'telefono', titulo: T.telefono },
  { clave: 'correo', titulo: T.correo, valor: (e) => e.correo || T.sinCorreo },
  { clave: 'documento', titulo: 'Documento', valor: (e) => e.documento ?? T.sinDocumento },
  { clave: 'vencimiento', titulo: 'Vencimiento del documento', valor: textoVencimiento },
  { clave: 'fotos', titulo: T.fotos, valor: textoFotos },
  { clave: 'estado', titulo: 'Estado', valor: (e) => T.estados[e.estado] ?? e.estado },
]

/**
 * CU-02 · el portal de riesgo: la cola de expedientes de identidad y la decisión. El estado viaja al servidor
 * (`?estado=`); «Por decidir» pide sin filtro y recorta los dos estados que esperan a una persona. Sobre lo cargado
 * se busca (nombre, teléfono, correo, documento), se filtra por vencimiento y fotos, se pagina y se descarga lo
 * filtrado tal como se ve. Un expediente no se edita ni se elimina: se aprueba o se rechaza.
 */
@Component({
  selector: 'ap-pantalla-de-expedientes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BarraDeFiltros, CabeceraDePagina, EstadoDePantalla, ExportarListado, Paginacion, TarjetaDeExpediente],
  template: `
    <section class="pantalla">
      <ap-cabecera-de-pagina eyebrow="Cumplimiento" [titulo]="t.titulo" [descripcion]="t.proposito" icono="verificado" />
      <ap-barra-de-filtros data-tutorial-id="cumplimiento-filtros" etiqueta="Filtros de expedientes" [etiquetaDeBusqueda]="t.buscarExpediente" ayudaDeBusqueda="El estado se pide al servidor; la búsqueda, el vencimiento y las fotos filtran lo ya cargado." [busqueda]="listado.busqueda()" [selectores]="selectores()" textoLimpiar="Limpiar filtros" (busquedaChange)="listado.buscar($event)" (selectorChange)="alFiltrar($event)" (limpiar)="limpiar()" />
      <div class="barra">
        <p class="resumen" aria-live="polite">{{ resumen() }}</p>
        @if (exportador.puedeExportar('expedientes-de-identidad')) { <ap-exportar-listado nombre="expedientes de identidad" [columnas]="EXPORTABLES" [filas]="listado.filtradas()" [deshabilitado]="!cola.hasValue()" /> }
      </div>
      <ap-estado-de-pantalla data-tutorial-id="cumplimiento-cola" [recurso]="cola" [vacio]="vacio" [mensajeVacio]="mensajeVacio()" [motivoVacio]="listado.hayCriterios() ? 'porFiltro' : 'sinDatos'" [accionVacio]="listado.hayCriterios() ? 'Quitar filtros' : undefined" (actuarEnVacio)="limpiar()" [etiquetaDeCarga]="t.cargando" (reintentar)="cola.reload()">
        @if (cola.hasValue()) {
          <ul class="cola">
            @for (e of listado.visibles(); track e.verificacionId) {
              <li><ap-tarjeta-de-expediente [expediente]="e" [ocupado]="resolviendo() !== null" [enCurso]="resolviendo()?.id === e.verificacionId ? resolviendo()!.decision : null" [error]="errores()[e.verificacionId] ?? null" (decidir)="resolver(e, $event)" /></li>
            }
          </ul>
          <ap-paginacion etiqueta="Paginación de expedientes" [pagina]="listado.pagina()" (paginaChange)="listado.irAPagina($event)" [totalDeFilas]="listado.total()" [porPagina]="listado.porPagina()" (porPaginaChange)="listado.cambiarPorPagina($event)" [opcionesPorPagina]="listado.opcionesPorPagina" nombreDeRegistros="expedientes" />
        }
      </ap-estado-de-pantalla>
    </section>
  `,
  styles: `
    .pantalla { display: flex; flex-direction: column; gap: var(--s4); min-width: 0; }
    .barra { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--s3); }
    .resumen { margin: 0; color: var(--text-2); font: var(--t-cuerpo-chico); }
    .cola { list-style: none; margin: 0 0 var(--s4); padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 26rem), 1fr)); gap: var(--s4); }
  `,
})
export class PantallaDeExpedientes {
  /** Datos de identidad sin enmascarar: la descarga en el navegador solo con `exportar:expedientes-de-identidad` (regla 90.2). */
  protected readonly exportador = inject(Exportador)
  constructor() {
    inject(ExplicadorDeVista).fijar(this.t.proposito)
  }
  protected readonly t = T
  protected readonly EXPORTABLES = EXPORTABLES
  /** La cola de trabajo primero y por omisión; después los del contrato, no una lista paralela. */
  protected readonly estado = signal<string>(POR_DECIDIR)
  protected readonly cola = expedientesEnEstado(this.estado)
  /**
   * `POR_DECIDIR` se recorta acá porque el backend filtra por UN estado y los que esperan una persona son dos. En
   * error, `value()` lanza (`ResourceValueError`): se lee solo con valor, y el error lo muestra `ap-estado-de-pantalla`.
   */
  private readonly recortados = computed(() => recortar(this.cola.hasValue() ? this.cola.value() : [], this.estado()))
  protected readonly listado = crearListado<ExpedienteEnRevision>({
    filas: this.recortados,
    camposDeBusqueda: ['nombreCompleto', 'telefono', 'correo', 'documento'],
    filtros: [
      { nombre: 'vencimiento', etiqueta: 'Documento', valor: textoVencimiento },
      { nombre: 'fotos', etiqueta: T.fotos, valor: textoFotos },
    ],
    porPagina: 10,
    opcionesPorPagina: [5, 10, 20, 50],
  })
  private readonly selectorDeEstado = computed<Selector>(() => ({
    nombre: 'estado',
    etiqueta: 'Estado',
    textoTodos: T.filtroPorDecidir,
    valor: this.estado() === POR_DECIDIR ? '' : this.estado(),
    opciones: Object.values(ExpedienteEnRevisionEstadoEnum).map((e) => ({ valor: e, texto: T.estados[e] ?? e })),
  }))
  protected readonly selectores = computed(() => [this.selectorDeEstado(), ...this.listado.selectores()])
  protected readonly resumen = computed(() => (this.cola.hasValue() ? `${this.listado.total()} de ${this.recortados().length} ${nombreSegunCantidad(this.recortados().length, 'expedientes')}` : ''))
  /** Vacío es lo que queda DESPUÉS del recorte y de los filtros: con `POR_DECIDIR` la respuesta trae también los resueltos. */
  protected readonly vacio = (): boolean => this.listado.total() === 0
  protected readonly mensajeVacio = computed(() => (this.listado.hayCriterios() ? this.t.sinCoincidencias : this.t.sinPendientes))
  /** El error de la última decisión, por expediente, entero: `ap-error-de-envio` distingue «no pasó» de «no sabemos». */
  protected readonly errores = signal<Readonly<Record<string, ErrorTraducido>>>({})
  /** La decisión en vuelo; mientras haya una, no se dispara otra. */
  protected readonly resolviendo = signal<{ id: string; decision: Decision['decision'] } | null>(null)
  private readonly enviarDecision = crearResolver()

  protected alFiltrar({ nombre, valor }: { nombre: string; valor: string }): void {
    if (nombre === 'estado') {
      // Otro estado es otra consulta: seguir en la página N de la anterior muestra una lista que nadie pidió.
      this.estado.set(valor || POR_DECIDIR)
      this.listado.irAPagina(1)
    } else this.listado.filtrar(nombre, valor)
  }

  protected limpiar(): void {
    this.listado.limpiar()
    this.estado.set(POR_DECIDIR)
  }

  protected resolver(e: ExpedienteEnRevision, { decision, motivo }: Decision): void {
    const id = e.verificacionId
    this.resolviendo.set({ id, decision })
    this.errores.update((m) => {
      const resto = { ...m }
      delete resto[id]
      return resto
    })
    this.enviarDecision(id, { decision, motivo }).subscribe({
      next: () => {
        this.resolviendo.set(null)
        this.cola.reload()
      },
      // Callarlo dejaba al operador creyendo que decidió: el error se ve en la tarjeta.
      error: (err: ErrorTraducido) => {
        this.resolviendo.set(null)
        this.errores.update((m) => ({ ...m, [id]: { ...err, mensaje: err?.mensaje ?? this.t.decisionFallo } }))
      },
    })
  }
}
